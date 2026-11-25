package com.bandmong.moderation;

import com.bandmong.moderation.dto.ReportRequest;
import com.bandmong.moderation.dto.ReportView;
import com.bandmong.moderation.dto.ResolveRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ModerationController {

    private final ReportService reports;
    private final AdminService admin;

    public ModerationController(ReportService reports, AdminService admin) {
        this.reports = reports;
        this.admin = admin;
    }

    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> report(@AuthenticationPrincipal Long userId, @Valid @RequestBody ReportRequest req) {
        return reports.report(userId, req);
    }

    // ── 관리자 (/api/admin/** 은 SecurityConfig에서 ROLE_ADMIN만) ──

    @GetMapping("/api/admin/reports")
    public List<ReportView> list(@RequestParam(defaultValue = "OPEN") ReportStatus status) {
        return admin.list(status);
    }

    @PostMapping("/api/admin/reports/{id}/resolve")
    public ReportView resolve(@AuthenticationPrincipal Long adminId, @PathVariable Long id,
                              @Valid @RequestBody ResolveRequest req) {
        return admin.resolve(adminId, id, req);
    }

    @PostMapping("/api/admin/users/{userId}/lift")
    public Map<String, Object> lift(@AuthenticationPrincipal Long adminId, @PathVariable Long userId) {
        return admin.lift(adminId, userId);
    }
}
