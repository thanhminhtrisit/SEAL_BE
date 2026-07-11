package com.seal.seal_backend.auth.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Verifies Google Identity Services ID tokens (signature via Google JWKs,
 * audience = our OAuth Client ID, expiry). The client SECRET is not used in this flow.
 * Wrapped in a Spring bean so AuthServiceImpl stays unit-testable with a mock.
 */
@Component
public class GoogleTokenVerifier {

    /** Minimal projection of the verified Google payload used by SEAL. */
    public record GoogleUser(String email, String fullName, boolean emailVerified) {}

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${google.client-id:}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /** @throws BusinessRuleException BR-AUTH-09 if the token is invalid, expired or for another client. */
    public GoogleUser verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BusinessRuleException("BR-AUTH-09", "Invalid or expired Google token.");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            return new GoogleUser(
                    payload.getEmail(),
                    (String) payload.get("name"),
                    Boolean.TRUE.equals(payload.getEmailVerified()));
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("BR-AUTH-09", "Google token verification failed.");
        }
    }
}
