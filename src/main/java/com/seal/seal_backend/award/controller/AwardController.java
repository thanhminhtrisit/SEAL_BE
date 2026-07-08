package com.seal.seal_backend.award.controller;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import com.seal.seal_backend.award.service.AwardService;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/awards")
@Tag(name = "Award & Results", description = "Quản lý Giải thưởng và Công bố Kết quả")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("Award module is running!"));
    }

    @PostMapping
    @Operation(summary = "Gán giải thưởng cho một đội thi")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<AwardResponse>> createAward(
            @Valid @RequestBody AwardCreateRequest request,
            Authentication authentication // Sử dụng Authentication gốc để bốc thông tin chuẩn bảo mật
    ) {
        // Dự án của bạn lưu thông tin User đăng nhập trong Principal (như đã thấy lỗi Email ở log trước)
        // Cách bốc ID an toàn từ Security Context:
        Long userId = null;

        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();

            try {
                java.lang.reflect.Method getIdMethod = principal.getClass().getMethod("getId");
                userId = (Long) getIdMethod.invoke(principal);
            } catch (Exception e) {
                throw new IllegalStateException("Hệ thống chưa cấu hình đồng bộ Argument Resolver cho module Award. Lỗi: " + e.getMessage());
            }
        }

        AwardResponse response = awardService.createAward(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Xem danh sách giải thưởng của một sự kiện")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AwardResponse>>> getAwardsByEvent(@PathVariable Long eventId) {
        List<AwardResponse> awards = awardService.getAwardsByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok(awards));
    }

    @GetMapping("/types")
    @Operation(summary = "Lấy danh sách loại giải thưởng")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAwardTypes() {
        List<Map<String, Object>> awardTypes = awardService.getAwardTypes();
        return ResponseEntity.ok(ApiResponse.ok(awardTypes));
    }

    @GetMapping("/events/{eventId}/categories")
    @Operation(summary = "Lấy danh sách các Hạng mục của một sự kiện")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoriesByEvent(@PathVariable Long eventId) {
        // Tạm thời gọi qua một hàm mới trong AwardService bằng JdbcTemplate hoặc Inject CategoryService/Repository vào đây
        List<Map<String, Object>> categories = awardService.getCategoriesByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping("/events/{eventId}/eligible-teams")
    @Operation(summary = "Lấy danh sách đội thi từ Vòng Chung Kết theo Hạng mục để trao giải")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEligibleTeamsForAward(
            @PathVariable Long eventId,
            @RequestParam Long categoryId
    ) {
        List<Map<String, Object>> teams = awardService.getEligibleTeamsForAward(eventId, categoryId);
        return ResponseEntity.ok(ApiResponse.ok(teams));
    }

    @PostMapping("/events/{eventId}/publish")
    @Operation(summary = "Công bố kết quả xếp hạng và giải thưởng cho thí sinh")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<String>> publishResults(
            @PathVariable Long eventId,
            @CurrentUser Long userId
    ) {
        awardService.publishEventResults(eventId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã công bố kết quả thành công!"));
    }

    @GetMapping("/events/{eventId}/suggestions")
    @Operation(summary = "Lấy gợi ý giải thưởng (Top 3) cho một hạng mục")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestedAwards(
            @PathVariable Long eventId,
            @RequestParam Long categoryId
    ) {
        List<Map<String, Object>> suggestions = awardService.getSuggestedAwards(eventId, categoryId);
        return ResponseEntity.ok(ApiResponse.ok(suggestions));
    }
}