package com.seal.seal_backend.auth;

import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.auth.security.JwtTokenProvider;
import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.auth.service.impl.AuthServiceImpl;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.domain.entity.Role;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.UserStatus;
import com.seal.seal_backend.domain.repository.RoleRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuditPublisher auditPublisher;

    @InjectMocks AuthServiceImpl authService;

    private Role teamMemberRole;

    @BeforeEach
    void setup() {
        teamMemberRole = new Role();
        teamMemberRole.setId(7L);
        teamMemberRole.setCode("TEAM_MEMBER");
    }

    // ─────────────────────────── REGISTER ────────────────────────────────

    @Nested
    class Register {

        @Test
        void duplicateEmail_throws_BR_USR_01() {
            when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(fptReq("dup@test.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-01");
        }

        @Test
        void weakPassword_noUppercase_throws_BR_USR_05() {
            when(userRepository.existsByEmail(any())).thenReturn(false);

            // "password1" has digit but no uppercase
            assertThatThrownBy(() -> authService.register(fptReq("a@b.com", "password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-05");
        }

        @Test
        void weakPassword_noDigit_throws_BR_USR_05() {
            when(userRepository.existsByEmail(any())).thenReturn(false);

            // "Password!" has uppercase but no digit
            assertThatThrownBy(() -> authService.register(fptReq("a@b.com", "Password!")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-05");
        }

        @Test
        void fptStudent_missingStudentId_throws_BR_USR_03() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            RegisterRequest req = new RegisterRequest(
                    "a@b.com", "Password1", "Name", null, true, null, null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        @Test
        void externalStudent_missingStudentId_throws_BR_USR_03() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            RegisterRequest req = new RegisterRequest(
                    "a@b.com", "Password1", "Name", null, false, null, "Some Uni");

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        @Test
        void externalStudent_missingUniversity_throws_BR_USR_03() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            RegisterRequest req = new RegisterRequest(
                    "a@b.com", "Password1", "Name", null, false, "EXT001", null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        @Test
        void fptStudent_validRequest_savesAsPending_andReturnsId() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(roleRepository.findByCode("TEAM_MEMBER")).thenReturn(Optional.of(teamMemberRole));
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            User saved = savedUser(42L, UserStatus.PENDING);
            when(userRepository.save(any())).thenReturn(saved);

            Long id = authService.register(fptReq("fpt@student.local", "Password1"));

            assertThat(id).isEqualTo(42L);
            verify(userRepository).save(argThat(u ->
                    u.getStatus() == UserStatus.PENDING && u.getPrimaryRole() == teamMemberRole));
        }

        @Test
        void externalStudent_validRequest_savesAsPending() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(roleRepository.findByCode("TEAM_MEMBER")).thenReturn(Optional.of(teamMemberRole));
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any())).thenReturn(savedUser(99L, UserStatus.PENDING));

            RegisterRequest req = new RegisterRequest(
                    "ext@uni.edu", "Password1", "Ext User", null, false, "EXT001", "Partner Uni");
            Long id = authService.register(req);

            assertThat(id).isEqualTo(99L);
        }
    }

    // ─────────────────────────── LOGIN ───────────────────────────────────

    @Nested
    class Login {

        @Test
        void emailNotFound_throws_BR_AUTH_01() {
            when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-01");
        }

        @Test
        void wrongPassword_throws_BR_AUTH_01() {
            when(userRepository.findByEmail("u@x.com"))
                    .thenReturn(Optional.of(userWithStatus("u@x.com", UserStatus.ACTIVE)));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("u@x.com", "wrong")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-01");
        }

        @Test
        void pendingAccount_throws_BR_AUTH_02() {
            when(userRepository.findByEmail("u@x.com"))
                    .thenReturn(Optional.of(userWithStatus("u@x.com", UserStatus.PENDING)));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("u@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-02");
        }

        @Test
        void lockedAccount_throws_BR_AUTH_03() {
            when(userRepository.findByEmail("u@x.com"))
                    .thenReturn(Optional.of(userWithStatus("u@x.com", UserStatus.LOCKED)));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("u@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-03");
        }

        @Test
        void rejectedAccount_throws_BR_AUTH_04() {
            when(userRepository.findByEmail("u@x.com"))
                    .thenReturn(Optional.of(userWithStatus("u@x.com", UserStatus.REJECTED)));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("u@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-04");
        }

        @Test
        void activeAccount_correctPassword_returnsTokens() {
            User active = userWithStatus("u@x.com", UserStatus.ACTIVE);
            when(userRepository.findByEmail("u@x.com")).thenReturn(Optional.of(active));
            when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);
            when(userRepository.save(any())).thenReturn(active);
            when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-tok");
            when(jwtTokenProvider.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh-tok");

            AuthResponse resp = authService.login(new LoginRequest("u@x.com", "Password1"));

            assertThat(resp.accessToken()).isEqualTo("access-tok");
            assertThat(resp.refreshToken()).isEqualTo("refresh-tok");
            verify(userRepository).save(argThat(u -> u.getLastLoginAt() != null));
        }
    }

    // ─────────────────────────── helpers ─────────────────────────────────

    private RegisterRequest fptReq(String email, String password) {
        return new RegisterRequest(email, password, "Full Name", null, true, "SE190099", null);
    }

    private User savedUser(Long id, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setStatus(status);
        u.setPrimaryRole(teamMemberRole);
        return u;
    }

    private User userWithStatus(String email, UserStatus status) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setStatus(status);
        u.setPrimaryRole(teamMemberRole);
        return u;
    }
}
