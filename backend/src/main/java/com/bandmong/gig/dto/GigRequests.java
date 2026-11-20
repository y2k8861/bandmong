package com.bandmong.gig.dto;

import com.bandmong.gig.CueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;

public final class GigRequests {

    private GigRequests() {
    }

    public record Create(
            @NotBlank(message = "공연 이름을 입력해 주세요") @Size(max = 30, message = "공연 이름은 30자 이내로 적어 주세요") String title,
            @NotNull(message = "공연 날짜와 시간을 골라 주세요") OffsetDateTime startsAt,
            @Size(max = 40, message = "공연장은 40자 이내로 적어 주세요") String venue,
            @Min(value = 10, message = "공연 시간은 10분 이상이에요") @Max(value = 120, message = "공연 시간은 120분까지예요") int slotMinutes
    ) {
    }

    public record Update(
            @Size(min = 1, max = 30, message = "공연 이름은 1~30자로 적어 주세요") String title,
            OffsetDateTime startsAt,
            @Size(max = 40, message = "공연장은 40자 이내로 적어 주세요") String venue,
            @Min(value = 10, message = "공연 시간은 10분 이상이에요") @Max(value = 120, message = "공연 시간은 120분까지예요") Integer slotMinutes
    ) {
    }

    public record Item(
            @NotNull CueType type,
            @NotBlank(message = "곡·멘트 이름을 입력해 주세요") @Size(max = 60, message = "이름은 60자 이내로 적어 주세요") String title,
            @Min(value = 15, message = "한 줄은 15초 이상이에요") @Max(value = 1200, message = "한 줄은 20분까지예요") int durationSec
    ) {
    }

    /** version: 내가 불러왔던 큐시트 버전 */
    public record Items(
            @NotNull(message = "큐시트 버전이 없어요") Long version,
            @NotNull @Size(max = 40, message = "큐시트는 40줄까지예요") List<@Valid Item> items
    ) {
    }

    public record AddCheck(@NotBlank(message = "할 일을 입력해 주세요") @Size(max = 60, message = "60자 이내로 적어 주세요") String text) {
    }

    public record Done(@NotNull Boolean done) {
    }
}
