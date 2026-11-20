package com.bandmong.studio;

import com.bandmong.studio.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class StudioController {

    private final BookingService service;

    public StudioController(BookingService service) {
        this.service = service;
    }

    @GetMapping("/api/studios")
    public List<StudioView> studios(@RequestParam(required = false) String area) {
        return service.studios(area);
    }

    @GetMapping("/api/rooms/{roomId}/slots")
    public SlotsView slots(@PathVariable Long roomId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.slots(roomId, date);
    }

    @GetMapping("/api/bands/{bandId}/bookings")
    public List<BookingView> list(@AuthenticationPrincipal Long userId, @PathVariable Long bandId) {
        return service.list(userId, bandId);
    }

    @PostMapping("/api/bands/{bandId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView book(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                            @Valid @RequestBody BookingRequest req) {
        return service.book(userId, bandId, req);
    }

    @PostMapping("/api/bookings/{bookingId}/cancel")
    public BookingView cancel(@AuthenticationPrincipal Long userId, @PathVariable Long bookingId) {
        return service.cancel(userId, bookingId);
    }

    @PutMapping("/api/bookings/{bookingId}/settles/{targetId}")
    public BookingView settle(@AuthenticationPrincipal Long userId, @PathVariable Long bookingId,
                              @PathVariable Long targetId, @Valid @RequestBody PaidRequest req) {
        return service.settle(userId, bookingId, targetId, req.paid());
    }
}
