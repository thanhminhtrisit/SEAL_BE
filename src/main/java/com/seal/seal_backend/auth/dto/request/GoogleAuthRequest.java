package com.seal.seal_backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** ID token issued by Google Identity Services on the FE (credential field of the GIS callback). */
public record GoogleAuthRequest(@NotBlank String idToken) {}
