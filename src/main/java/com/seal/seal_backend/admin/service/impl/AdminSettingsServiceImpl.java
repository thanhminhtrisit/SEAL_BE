package com.seal.seal_backend.admin.service.impl;

import com.seal.seal_backend.admin.service.AdminSettingsService;
import com.seal.seal_backend.domain.entity.SystemConfig;
import com.seal.seal_backend.domain.repository.SystemConfigRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettingsServiceImpl implements AdminSettingsService {

    private static final String KEY = AdminSettingsService.AUTO_APPROVE_KEY;

    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isAutoApproveEnabled() {
        return systemConfigRepository.findByConfigKey(KEY)
                .map(c -> c.getConfigValue() != null && "true".equalsIgnoreCase(c.getConfigValue().trim()))
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean setAutoApprove(boolean enabled, Long actorId) {
        SystemConfig cfg = systemConfigRepository.findByConfigKey(KEY).orElseGet(() -> {
            SystemConfig c = new SystemConfig();
            c.setConfigKey(KEY);
            c.setDescription("Auto-activate accounts that pass registration validation (no manual coordinator approval).");
            return c;
        });
        cfg.setConfigValue(enabled ? "true" : "false");
        cfg.setUpdatedBy(userRepository.getReferenceById(actorId));
        systemConfigRepository.save(cfg);
        return enabled;
    }
}
