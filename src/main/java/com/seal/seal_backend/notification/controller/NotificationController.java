package com.seal.seal_backend.notification.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/** Flow: result/award notifications (FR-AWD-02, BR-DQ-04). OWNER: M3 — Nguyễn Công Thiên Ân. */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification")
public class NotificationController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() { return ApiResponse.ok("Notification module is alive"); }
}
