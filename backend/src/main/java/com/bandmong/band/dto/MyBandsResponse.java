package com.bandmong.band.dto;

import java.util.List;

/** 내 밴드 탭: 속한 밴드 + 답을 기다리는 지원 */
public record MyBandsResponse(List<BandSummary> bands, List<BandSummary> applied) {
}
