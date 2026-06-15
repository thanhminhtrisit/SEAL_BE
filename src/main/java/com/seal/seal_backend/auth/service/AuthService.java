package com.seal.seal_backend.auth.service;

import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;

public interface AuthService {
    Long register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String accessToken);
    void approveAccount(Long userId, Long approverId);
    void rejectAccount(Long userId, Long approverId, String reason);
}
