package com.seal.seal_backend.auth.dto.response;

/**
 * Google auth outcome. Two shapes:
 * - PENDING_APPROVAL: account was just created (or already awaited approval) — no tokens yet,
 *   FR-AUTH-03 coordinator approval still applies exactly like normal registration.
 * - AUTHENTICATED: ACTIVE account — same JWT pair as /api/auth/login.
 */
public record GoogleAuthResponse(String status, Long userId, String accessToken, String refreshToken) {

    public static GoogleAuthResponse pending(Long userId) {
        return new GoogleAuthResponse("PENDING_APPROVAL", userId, null, null);
    }

    public static GoogleAuthResponse authenticated(Long userId, String accessToken, String refreshToken) {
        return new GoogleAuthResponse("AUTHENTICATED", userId, accessToken, refreshToken);
    }
}
