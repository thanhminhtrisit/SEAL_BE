package com.seal.seal_backend.admin.service.impl;

import com.seal.seal_backend.domain.entity.SystemConfig;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.repository.SystemConfigRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceImplTest {

    private static final String KEY = "AUTO_APPROVE_ACCOUNTS";

    @Mock SystemConfigRepository systemConfigRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AdminSettingsServiceImpl service;

    private SystemConfig config(String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(KEY);
        c.setConfigValue(value);
        return c;
    }

    // ─────────────────────── isAutoApproveEnabled ─────────────────────────

    @Test
    void enabled_whenConfigValueTrue() {
        when(systemConfigRepository.findByConfigKey(KEY)).thenReturn(Optional.of(config("true")));
        assertThat(service.isAutoApproveEnabled()).isTrue();
    }

    @Test
    void disabled_whenConfigMissing() {
        when(systemConfigRepository.findByConfigKey(KEY)).thenReturn(Optional.empty());
        assertThat(service.isAutoApproveEnabled()).isFalse();
    }

    @Test
    void disabled_whenConfigValueNotTrue() {
        when(systemConfigRepository.findByConfigKey(KEY)).thenReturn(Optional.of(config("false")));
        assertThat(service.isAutoApproveEnabled()).isFalse();
    }

    // ─────────────────────── setAutoApprove ───────────────────────────────

    @Test
    void setAutoApprove_createsConfig_whenMissing() {
        when(systemConfigRepository.findByConfigKey(KEY)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(anyLong())).thenReturn(new User());

        boolean result = service.setAutoApprove(true, 7L);

        assertThat(result).isTrue();
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        SystemConfig saved = captor.getValue();
        assertThat(saved.getConfigKey()).isEqualTo(KEY);
        assertThat(saved.getConfigValue()).isEqualTo("true");
    }

    @Test
    void setAutoApprove_updatesExistingConfig_toFalse() {
        SystemConfig existing = config("true");
        when(systemConfigRepository.findByConfigKey(KEY)).thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(anyLong())).thenReturn(new User());

        boolean result = service.setAutoApprove(false, 7L);

        assertThat(result).isFalse();
        verify(systemConfigRepository).save(any(SystemConfig.class));
        assertThat(existing.getConfigValue()).isEqualTo("false");
    }
}
