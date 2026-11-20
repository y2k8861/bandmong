package com.bandmong.chat.dto;

import java.util.List;

/** 오래된 → 최신 순. hasMore면 messages[0].id를 before로 다시 부른다. */
public record MessagePage(List<MessageView> messages, boolean hasMore, Long lastReadId) {
}
