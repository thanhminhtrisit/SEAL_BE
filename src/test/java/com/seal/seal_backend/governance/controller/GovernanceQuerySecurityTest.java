package com.seal.seal_backend.governance.controller;

import com.seal.seal_backend.governance.service.GovernanceQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Item 1 — verifies the @PreAuthorize gate on GovernanceQueryController.listDisciplines:
 * includeInactive=true is Super-Coordinator only; plain authenticated users may only read active.
 * Minimal method-security context (no web layer / JWT) so we test authorization in isolation.
 */
@SpringBootTest(classes = GovernanceQuerySecurityTest.Config.class)
class GovernanceQuerySecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        GovernanceQueryService governanceQueryService() {
            return Mockito.mock(GovernanceQueryService.class);
        }

        @Bean
        GovernanceQueryController governanceQueryController(GovernanceQueryService s) {
            return new GovernanceQueryController(s);
        }
    }

    @Autowired GovernanceQueryService service;
    @Autowired GovernanceQueryController controller;

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void teamMember_includeInactiveTrue_isDenied() {
        assertThatThrownBy(() -> controller.listDisciplines(true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void teamMember_activeOnly_isAllowed() {
        when(service.listDisciplines(false)).thenReturn(List.of());
        assertThatCode(() -> controller.listDisciplines(false)).doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(roles = "SUPER_COORDINATOR")
    void superCoordinator_includeInactiveTrue_isAllowed() {
        when(service.listDisciplines(true)).thenReturn(List.of());
        assertThatCode(() -> controller.listDisciplines(true)).doesNotThrowAnyException();
    }
}
