package com.bandmong.chat;

import com.bandmong.chat.dto.MessagePage;
import com.bandmong.chat.dto.MessageView;
import com.bandmong.chat.dto.ReadRequest;
import com.bandmong.chat.dto.SendRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bands/{bandId}/messages")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @GetMapping
    public MessagePage page(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                            @RequestParam(required = false) Long before,
                            @RequestParam(required = false) Integer limit) {
        return service.page(userId, bandId, before, limit);
    }

    @PostMapping
    public MessageView send(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                            @Valid @RequestBody SendRequest req) {
        return service.send(userId, bandId, req);
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal Long userId, @PathVariable Long bandId,
                     @Valid @RequestBody ReadRequest req) {
        service.read(userId, bandId, req.lastId());
    }
}
