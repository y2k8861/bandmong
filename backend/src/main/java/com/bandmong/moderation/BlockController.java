package com.bandmong.moderation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    public record BlockRequest(@NotNull(message = "차단할 사용자를 골라 주세요") Long userId) {
    }

    private final BlockService service;

    public BlockController(BlockService service) {
        this.service = service;
    }

    @GetMapping
    public List<BlockService.BlockedView> list(@AuthenticationPrincipal Long userId) {
        return service.list(userId);
    }

    @PostMapping
    public List<BlockService.BlockedView> block(@AuthenticationPrincipal Long userId, @Valid @RequestBody BlockRequest req) {
        return service.block(userId, req.userId());
    }

    @DeleteMapping("/{targetId}")
    public List<BlockService.BlockedView> unblock(@AuthenticationPrincipal Long userId, @PathVariable Long targetId) {
        return service.unblock(userId, targetId);
    }
}
