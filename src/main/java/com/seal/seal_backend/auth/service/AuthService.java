package com.seal.seal_backend.auth.service;

import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;

/** OWNER: M1. Registration, login (JWT), participant approval. */
public interface AuthService {
    Long register(RegisterRequest request);     // returns new user id (status PENDING)
    AuthResponse login(LoginRequest request);
    void approveAccount(Long userId, Long approverId);   // FR-AUTH-03/08
    void rejectAccount(Long userId, Long approverId, String reason);
}
