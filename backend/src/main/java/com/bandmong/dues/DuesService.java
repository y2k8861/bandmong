package com.bandmong.dues;

import com.bandmong.band.Band;
import com.bandmong.band.BandMember;
import com.bandmong.band.BandMemberRepository;
import com.bandmong.band.BandRepository;
import com.bandmong.config.TimeConfig;
import com.bandmong.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DuesService {

    private final BandRepository bands;
    private final BandMemberRepository members;
    private final DuesPaymentRepository payments;
    private final Clock clock;

    public DuesService(BandRepository bands, BandMemberRepository members, DuesPaymentRepository payments, Clock clock) {
        this.bands = bands;
        this.members = members;
        this.payments = payments;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DuesView view(Long userId, Long bandId, String month) {
        YearMonth m = month(month);
        Band band = band(bandId);
        List<BandMember> list = requireMember(bandId, userId);
        Map<Long, DuesPayment> paid = payments.findByBandIdAndMonth(bandId, m.toString()).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
        List<DuesView.Row> rows = list.stream().map(x -> {
            DuesPayment p = paid.get(x.getUser().getId());
            return new DuesView.Row(x.getUser().getId(), x.getUser().getNickname(), p != null, p == null ? null : p.getPaidAt());
        }).toList();
        return new DuesView(m.toString(), band.getDuesAmount(), band.getDuesAccount(), band.isHost(userId),
                (int) rows.stream().filter(DuesView.Row::paid).count(), rows.size(), rows);
    }

    public DuesView settings(Long userId, Long bandId, DuesSettingsRequest r) {
        Band band = band(bandId);
        if (!band.isHost(userId)) throw new ApiException(HttpStatus.FORBIDDEN, "방장만 회비를 정할 수 있어요");
        band.setDues(r.amount(), r.account());
        return view(userId, bandId, null);
    }

    /** 나는 내 것만, 방장은 누구 것이든 */
    public DuesView mark(Long userId, Long bandId, String month, Long targetId, boolean paid) {
        YearMonth m = month(month);
        Band band = band(bandId);
        List<BandMember> list = requireMember(bandId, userId);
        if (!userId.equals(targetId) && !band.isHost(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "다른 사람 회비는 방장만 바꿀 수 있어요");
        }
        if (band.getDuesAmount() == null) throw new ApiException(HttpStatus.CONFLICT, "회비가 정해져 있지 않아요");
        BandMember target = list.stream().filter(x -> x.getUser().getId().equals(targetId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이 밴드 멤버가 아니에요"));

        var existing = payments.findByBandIdAndUserIdAndMonth(bandId, targetId, m.toString());
        if (paid && existing.isEmpty()) {
            payments.save(new DuesPayment(band, target.getUser(), m.toString(), band.getDuesAmount(), userId, clock.instant()));
        } else if (!paid) {
            existing.ifPresent(payments::delete);
        }
        payments.flush();
        return view(userId, bandId, m.toString());
    }

    /** 비우면 이번 달(한국). 다음 달까지만 미리 볼 수 있다. */
    private YearMonth month(String s) {
        YearMonth now = YearMonth.from(ZonedDateTime.now(clock).withZoneSameInstant(TimeConfig.KST));
        if (s == null || s.isBlank()) return now;
        try {
            YearMonth m = YearMonth.parse(s);
            if (m.isAfter(now.plusMonths(1))) throw new ApiException(HttpStatus.BAD_REQUEST, "다음 달까지만 볼 수 있어요");
            return m;
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "달은 2026-11 처럼 적어 주세요");
        }
    }

    private Band band(Long id) {
        return bands.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "밴드를 찾을 수 없어요"));
    }

    private List<BandMember> requireMember(Long bandId, Long userId) {
        List<BandMember> list = members.findByBandWithUser(bandId);
        if (list.stream().noneMatch(x -> x.getUser().getId().equals(userId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "밴드 멤버만 볼 수 있어요");
        }
        return list;
    }
}
