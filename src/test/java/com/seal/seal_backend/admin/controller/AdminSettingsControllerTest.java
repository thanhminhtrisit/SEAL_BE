package com.seal.seal_backend.admin.controller;

import com.seal.seal_backend.admin.service.AdminSettingsService;
import com.seal.seal_backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Item 3 — an empty body {} (or a wrong field name) must be rejected (400) by @Valid/@NotNull,
 * NOT silently unboxed to false and used to disable the sensitive auto-approve flag.
 * Standalone MockMvc: exercises bean validation + GlobalExceptionHandler without the security/JWT stack.
 */
class AdminSettingsControllerTest {

    private AdminSettingsService adminSettingsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        adminSettingsService = Mockito.mock(AdminSettingsService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminSettingsController(adminSettingsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void setAutoApprove_emptyBody_returns400_andDoesNotChangeFlag() throws Exception {
        mockMvc.perform(put("/api/admin/settings/auto-approve")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(adminSettingsService, never()).setAutoApprove(anyBoolean(), anyLong());
    }
}
