package com.bandmong.gig;

import com.bandmong.gig.dto.GigRequests;
import com.bandmong.gig.dto.GigView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GigController {

    private final GigService service;

    public GigController(GigService service) {
        this.service = service;
    }

    @GetMapping("/api/bands/{bandId}/gigs")
    public List<GigView> list(@AuthenticationPrincipal Long userId, @PathVariable Long bandId) {
        return service.list(userId, bandId);
    }

    @PostMapping("/api/bands/{bandId}/gigs")
    @ResponseStatus(HttpStatus.CREATED)
    public GigView create(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                          @Valid @RequestBody GigRequests.Create req) {
        return service.create(userId, bandId, req);
    }

    @GetMapping("/api/gigs/{gigId}")
    public GigView detail(@AuthenticationPrincipal Long userId, @PathVariable Long gigId) {
        return service.detail(userId, gigId);
    }

    @PatchMapping("/api/gigs/{gigId}")
    public GigView update(@AuthenticationPrincipal Long userId, @PathVariable Long gigId,
                          @Valid @RequestBody GigRequests.Update req) {
        return service.update(userId, gigId, req);
    }

    @PostMapping("/api/gigs/{gigId}/cancel")
    public GigView cancel(@AuthenticationPrincipal Long userId, @PathVariable Long gigId) {
        return service.cancel(userId, gigId);
    }

    @PutMapping("/api/gigs/{gigId}/items")
    public GigView items(@AuthenticationPrincipal Long userId, @PathVariable Long gigId,
                         @Valid @RequestBody GigRequests.Items req) {
        return service.saveItems(userId, gigId, req);
    }

    @PostMapping("/api/gigs/{gigId}/checks")
    public GigView addCheck(@AuthenticationPrincipal Long userId, @PathVariable Long gigId,
                            @Valid @RequestBody GigRequests.AddCheck req) {
        return service.addCheck(userId, gigId, req.text());
    }

    @PutMapping("/api/gigs/{gigId}/checks/{checkId}")
    public GigView markCheck(@AuthenticationPrincipal Long userId, @PathVariable Long gigId, @PathVariable Long checkId,
                             @Valid @RequestBody GigRequests.Done req) {
        return service.markCheck(userId, gigId, checkId, req.done());
    }

    @DeleteMapping("/api/gigs/{gigId}/checks/{checkId}")
    public GigView deleteCheck(@AuthenticationPrincipal Long userId, @PathVariable Long gigId, @PathVariable Long checkId) {
        return service.deleteCheck(userId, gigId, checkId);
    }
}
