package com.bandmong.moderation;

import com.bandmong.user.UserRepository;
import com.bandmong.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 사용자 차단 (애플 심사 가이드라인 1.2).
 * 차단하면 그 사람의 채팅 메시지가 내 화면에서 사라지고, 그 사람이 방장인 밴드는 찾기에 나오지 않는다.
 * 같은 밴드에 있어도 탈퇴시키지는 않는다 — 필요하면 신고로 운영자에게.
 */
@Service
@Transactional
public class BlockService {

    private final UserBlockRepository blocks;
    private final UserRepository users;

    public BlockService(UserBlockRepository blocks, UserRepository users) {
        this.blocks = blocks;
        this.users = users;
    }

    public record BlockedView(Long userId, String nickname, Instant blockedAt) {
    }

    public List<BlockedView> block(Long userId, Long targetId) {
        if (userId.equals(targetId)) throw new ApiException(HttpStatus.BAD_REQUEST, "나 자신은 차단할 수 없어요");
        users.findById(targetId).filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "차단할 사용자를 찾을 수 없어요"));
        if (blocks.findByBlockerIdAndBlockedId(userId, targetId).isEmpty()) {
            blocks.save(new UserBlock(userId, targetId));
        }
        return list(userId);
    }

    public List<BlockedView> unblock(Long userId, Long targetId) {
        blocks.findByBlockerIdAndBlockedId(userId, targetId).ifPresent(blocks::delete);
        blocks.flush();
        return list(userId);
    }

    @Transactional(readOnly = true)
    public List<BlockedView> list(Long userId) {
        List<UserBlock> list = blocks.findByBlockerIdOrderByIdDesc(userId);
        Map<Long, String> names = new HashMap<>();
        users.findAllById(list.stream().map(UserBlock::getBlockedId).toList())
                .forEach(u -> names.put(u.getId(), u.getNickname()));
        return list.stream().map(b -> new BlockedView(b.getBlockedId(), names.getOrDefault(b.getBlockedId(), "알 수 없음"),
                b.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> blockedBy(Long userId) {
        return new HashSet<>(blocks.blockedBy(userId));
    }

    @Transactional(readOnly = true)
    public Set<Long> blockersAmong(Long senderId, Collection<Long> candidates) {
        if (candidates.isEmpty()) return Set.of();
        return new HashSet<>(blocks.blockersAmong(senderId, candidates));
    }
}
