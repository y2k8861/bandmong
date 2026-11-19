package com.bandmong.jam;

import com.bandmong.jam.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class JamController {

    private final JamService service;

    public JamController(JamService service) {
        this.service = service;
    }

    @GetMapping("/api/bands/{bandId}/availability")
    public AvailabilityView availability(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return service.availability(userId, bandId, week);
    }

    @PutMapping("/api/bands/{bandId}/availability")
    public AvailabilityView saveAvailability(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                                             @Valid @RequestBody AvailabilityRequest req) {
        return service.saveAvailability(userId, bandId, req);
    }

    @GetMapping("/api/bands/{bandId}/jams")
    public JamListResponse list(@AuthenticationPrincipal Long userId, @PathVariable Long bandId) {
        return service.list(userId, bandId);
    }

    @PostMapping("/api/bands/{bandId}/jams")
    @ResponseStatus(HttpStatus.CREATED)
    public JamView create(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                          @Valid @RequestBody JamCreateRequest req) {
        return service.create(userId, bandId, req);
    }

    @GetMapping("/api/jams/{jamId}")
    public JamView detail(@AuthenticationPrincipal Long userId, @PathVariable Long jamId) {
        return service.detail(userId, jamId);
    }

    @PatchMapping("/api/jams/{jamId}")
    public JamView update(@AuthenticationPrincipal Long userId, @PathVariable Long jamId,
                          @Valid @RequestBody JamUpdateRequest req) {
        return service.update(userId, jamId, req);
    }

    @PostMapping("/api/jams/{jamId}/cancel")
    public JamView cancel(@AuthenticationPrincipal Long userId, @PathVariable Long jamId) {
        return service.cancel(userId, jamId);
    }

    @PutMapping("/api/jams/{jamId}/rsvp")
    public JamView rsvp(@AuthenticationPrincipal Long userId, @PathVariable Long jamId,
                        @Valid @RequestBody RsvpRequest req) {
        return service.rsvp(userId, jamId, req.answer());
    }
}
