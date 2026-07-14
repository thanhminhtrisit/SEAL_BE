package com.seal.seal_backend.admin.service;

/** FR-ADM-02 — Admin platform settings. */
public interface AdminSettingsService {
    boolean isAutoApproveEnabled();
    boolean setAutoApprove(boolean enabled, Long actorId);
}
