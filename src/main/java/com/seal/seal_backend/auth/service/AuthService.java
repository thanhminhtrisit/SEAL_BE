package com.seal.seal_backend.auth.service;

import com.seal.seal_backend.auth.dto.request.CreateGuestJudgeRequest;
import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.auth.dto.response.GuestJudgeResponse;
import com.seal.seal_backend.auth.dto.response.MeResponse;
import com.seal.seal_backend.auth.dto.response.PendingAccountResponse;
import com.seal.seal_backend.common.api.PageResponse;
import com.seal.seal_backend.domain.enums.UserStatus;
import org.springframework.data.domain.Pageable;

public interface AuthService {
    Long register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String accessToken);
    void approveAccount(Long userId, Long approverId);
    void rejectAccount(Long userId, Long approverId, String reason);
    PageResponse<PendingAccountResponse> listPendingAccounts(Pageable pageable);
    PageResponse<PendingAccountResponse> listAccountsByStatus(UserStatus status, Pageable pageable);
    GuestJudgeResponse createGuestJudge(CreateGuestJudgeRequest req, Long creatorId);
    MeResponse getCurrentUser(Long userId);
}
