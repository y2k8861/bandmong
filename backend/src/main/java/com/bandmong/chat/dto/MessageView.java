package com.bandmong.chat.dto;

import com.bandmong.chat.ChatMessage;
import com.bandmong.chat.MessageType;

import java.time.Instant;

public record MessageView(
        Long id,
        Long bandId,
        MessageType type,
        Long senderId,
        String senderNickname,
        String body,
        Instant createdAt,
        /** 보낸 기기가 자기 메시지(미리 그려 둔 것)를 찾아 바꿔 끼우는 데 쓴다 */
        String clientId,
        boolean hidden
) {
    public static final String HIDDEN_TEXT = "운영 정책에 따라 가려진 메시지예요";

    public static MessageView of(ChatMessage m) {
        var s = m.getSender();
        return new MessageView(m.getId(), m.getBand().getId(), m.getType(),
                s == null ? null : s.getId(), s == null ? null : s.getNickname(),
                m.isHidden() ? HIDDEN_TEXT : m.getBody(), m.getCreatedAt(), m.getClientId(), m.isHidden());
    }
}
