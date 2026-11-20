package com.bandmong.dues;

import com.bandmong.studio.dto.PaidRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bands/{bandId}/dues")
public class DuesController {

    private final DuesService service;

    public DuesController(DuesService service) {
        this.service = service;
    }

    @GetMapping
    public DuesView view(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                         @RequestParam(required = false) String month) {
        return service.view(userId, bandId, month);
    }

    @PutMapping("/settings")
    public DuesView settings(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                             @Valid @RequestBody DuesSettingsRequest req) {
        return service.settings(userId, bandId, req);
    }

    @PutMapping("/{month}/members/{targetId}")
    public DuesView mark(@AuthenticationPrincipal Long userId, @PathVariable Long bandId, @PathVariable String month,
                         @PathVariable Long targetId, @Valid @RequestBody PaidRequest req) {
        return service.mark(userId, bandId, month, targetId, req.paid());
    }
}
