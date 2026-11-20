package com.bandmong.dues;

import java.time.Instant;
import java.util.List;

public record DuesView(String month, Integer amount, String account, boolean host, int paidCount, int total,
                       List<Row> members) {
    public record Row(Long userId, String nickname, boolean paid, Instant paidAt) {
    }
}
