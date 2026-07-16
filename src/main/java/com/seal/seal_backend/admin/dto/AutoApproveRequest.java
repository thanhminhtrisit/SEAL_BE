package com.seal.seal_backend.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Toggle body for AUTO_APPROVE_ACCOUNTS.
 * Boxed Boolean + @NotNull so an empty body {} or a wrong field name is rejected (400) instead of
 * silently unboxing to false and disabling this sensitive flag. Field name stays "enabled" on the
 * wire (no is-prefix trap).
 */
public record AutoApproveRequest(@NotNull Boolean enabled) {}
