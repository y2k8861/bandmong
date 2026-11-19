package com.bandmong.band;

import com.bandmong.band.dto.*;
import com.bandmong.user.Instrument;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BandController {

    private final BandService service;

    public BandController(BandService service) {
        this.service = service;
    }

    @GetMapping("/api/bands")
    public List<BandSummary> explore(@AuthenticationPrincipal Long userId,
                                     @RequestParam(required = false) Genre genre,
                                     @RequestParam(required = false) Instrument instrument,
                                     @RequestParam(required = false) String area) {
        return service.explore(userId, genre, instrument, area);
    }

    @PostMapping("/api/bands")
    @ResponseStatus(HttpStatus.CREATED)
    public BandDetail create(@AuthenticationPrincipal Long userId, @Valid @RequestBody BandCreateRequest req) {
        return service.create(userId, req);
    }

    @GetMapping("/api/bands/{bandId}")
    public BandDetail detail(@AuthenticationPrincipal Long userId, @PathVariable Long bandId) {
        return service.detail(userId, bandId);
    }

    @PatchMapping("/api/bands/{bandId}")
    public BandDetail update(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                             @Valid @RequestBody BandUpdateRequest req) {
        return service.update(userId, bandId, req);
    }

    @GetMapping("/api/me/bands")
    public MyBandsResponse myBands(@AuthenticationPrincipal Long userId) {
        return service.myBands(userId);
    }

    @PostMapping("/api/bands/{bandId}/applications")
    public BandDetail apply(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                            @Valid @RequestBody ApplyRequest req) {
        return service.apply(userId, bandId, req);
    }

    @DeleteMapping("/api/bands/{bandId}/applications/mine")
    public BandDetail cancel(@AuthenticationPrincipal Long userId, @PathVariable Long bandId) {
        return service.cancel(userId, bandId);
    }

    @PostMapping("/api/bands/{bandId}/applications/{applicationId}/accept")
    public BandDetail accept(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                             @PathVariable Long applicationId) {
        return service.accept(userId, bandId, applicationId);
    }

    @PostMapping("/api/bands/{bandId}/applications/{applicationId}/reject")
    public BandDetail reject(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                             @PathVariable Long applicationId) {
        return service.reject(userId, bandId, applicationId);
    }

    @DeleteMapping("/api/bands/{bandId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal Long userId, @PathVariable Long bandId) {
        service.leave(userId, bandId);
    }
}
