package com.bandmong.chat;

import com.bandmong.band.Band;
import com.bandmong.band.BandMember;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.chat.dto.MessagePage;
import com.bandmong.chat.dto.MessageView;
import com.bandmong.chat.dto.SendRequest;
import com.bandmong.stream.AfterCommit;
import com.bandmong.stream.SseHub;
import com.bandmong.web.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    static final int DEFAULT_PAGE = 30;
    static final int MAX_PAGE = 100;

    private final ChatMessageRepository messages;
    private final BandRepository bands;
    private final BandMemberRepository members;
    private final SseHub hub;
    private final com.bandmong.moderation.BlockService blocks;
    private final com.bandmong.web.ProfanityFilter profanity;

    public ChatService(ChatMessageRepository messages, BandRepository bands, BandMemberRepository members, SseHub hub,
                       com.bandmong.moderation.BlockService blocks, com.bandmong.web.ProfanityFilter profanity) {
        this.messages = messages;
        this.bands = bands;
        this.members = members;
        this.hub = hub;
        this.blocks = blocks;
        this.profanity = profanity;
    }

    @Transactional(readOnly = true)
    public MessagePage page(Long userId, Long bandId, Long before, Integer limit) {
        BandMember me = requireMember(bandId, userId);
        int size = limit == null ? DEFAULT_PAGE : Math.max(1, Math.min(MAX_PAGE, limit));
        List<ChatMessage> desc = messages.findPage(bandId, before == null ? Long.MAX_VALUE : before,
                PageRequest.of(0, size + 1));
        boolean hasMore = desc.size() > size;
        // 내가 차단한 사람의 메시지는 빼고 보여 준다 (페이지 경계는 원래 id 기준이라 커서가 흐트러지지 않음)
        java.util.Set<Long> blocked = blocks.blockedBy(userId);
        List<MessageView> asc = new ArrayList<>(desc.subList(0, Math.min(size, desc.size())).stream()
                .filter(m -> m.getSender() == null || !blocked.contains(m.getSender().getId()))
                .map(MessageView::of).toList());
        Collections.reverse(asc);
        return new MessagePage(asc, hasMore, me.getLastReadMessageId());
    }

    public MessageView send(Long userId, Long bandId, SendRequest r) {
        BandMember me = requireMember(bandId, userId);
        String body = profanity.mask(r.body().strip()); // 욕설은 *로 가려서 저장
        if (body.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "메시지를 입력해 주세요");

        if (r.clientId() != null) {
            Optional<ChatMessage> again = messages.findByBandIdAndSenderIdAndClientId(bandId, userId, r.clientId());
            if (again.isPresent()) return MessageView.of(again.get()); // 재전송: 저장도 방송도 다시 하지 않음
        }
        ChatMessage saved = messages.save(new ChatMessage(me.getBand(), me.getUser(), MessageType.TEXT, body, r.clientId()));
        me.markRead(saved.getId()); // 내가 보낸 건 읽은 것
        MessageView view = MessageView.of(saved);
        broadcast(bandId, view);
        return view;
    }

    /** "OO님이 들어왔어요" 같은 안내 메시지 */
    public MessageView system(Band band, String text) {
        ChatMessage saved = messages.save(new ChatMessage(band, null, MessageType.SYSTEM, text, null));
        MessageView view = MessageView.of(saved);
        broadcast(band.getId(), view);
        return view;
    }

    public void read(Long userId, Long bandId, long lastId) {
        BandMember me = requireMember(bandId, userId);
        me.markRead(Math.min(lastId, messages.maxId(bandId))); // 없는 미래 id로 앞질러 가지 않게
    }

    /** 새로 들어온 멤버는 지금까지의 대화를 안 읽은 것으로 치지 않는다 */
    public void startFromNow(BandMember member) {
        member.markRead(messages.maxId(member.getBand().getId()));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> unreadCounts(Long userId, Collection<Long> bandIds) {
        if (bandIds.isEmpty()) return Map.of();
        return messages.unreadByBand(userId, bandIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> ((Long) r[1]).intValue()));
    }

    private void broadcast(Long bandId, MessageView view) {
        List<Long> all = members.findByBandWithUser(bandId).stream().map(m -> m.getUser().getId()).toList();
        // 보낸 사람을 차단한 멤버에게는 보내지 않는다
        java.util.Set<Long> skip = view.senderId() == null ? java.util.Set.of() : blocks.blockersAmong(view.senderId(), all);
        List<Long> to = all.stream().filter(id -> !skip.contains(id)).toList();
        AfterCommit.run(() -> hub.sendAll(to, "message", view));
    }

    private BandMember requireMember(Long bandId, Long userId) {
        if (!bands.existsById(bandId)) throw new ApiException(HttpStatus.NOT_FOUND, "밴드를 찾을 수 없어요");
        return members.findByBandIdAndUserId(bandId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "밴드 멤버만 채팅할 수 있어요"));
    }
}
