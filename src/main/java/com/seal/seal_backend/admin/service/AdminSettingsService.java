package com.seal.seal_backend.admin.service;

/** FR-ADM-02 — Admin platform settings. */
public interface AdminSettingsService {

    /** Single source of truth for the auto-approve system_configs key (shared with AuthService). */
    String AUTO_APPROVE_KEY = "AUTO_APPROVE_ACCOUNTS";

    boolean isAutoApproveEnabled();
    boolean setAutoApprove(boolean enabled, Long actorId);
}
