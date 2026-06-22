package com.seal.seal_backend.auth;

import com.seal.seal_backend.auth.dto.request.CreateGuestJudgeRequest;
import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.auth.dto.response.GuestJudgeResponse;
import com.seal.seal_backend.auth.dto.response.PendingAccountResponse;
import com.seal.seal_backend.auth.security.JwtTokenProvider;
import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.auth.service.impl.AuthServiceImpl;
import com.seal.seal_backend.common.api.PageResponse;
import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.domain.entity.Role;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.AccountType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

        // ── email không tồn tại → tạo mới ────────────────────────────────

        @Test
        void newEmail_weakPassword_noUppercase_throws_BR_USR_05() {
            assertThatThrownBy(() -> authService.register(fptReq("a@b.com", "password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-05");
        }

        @Test
        void newEmail_weakPassword_noDigit_throws_BR_USR_05() {
            assertThatThrownBy(() -> authService.register(fptReq("a@b.com", "Password!")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-05");
        }

        @Test
        void newEmail_fptStudent_missingStudentId_throws_BR_USR_03() {
            RegisterRequest req = new RegisterRequest(
                    "a@b.com", "Password1", "Name", null, true, null, null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        @Test
        void newEmail_externalStudent_missingStudentId_throws_BR_USR_03() {
            RegisterRequest req = new RegisterRequest(
                    "a@b.com", "Password1", "Name", null, false, null, "Some Uni");

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        @Test
        void newEmail_externalStudent_missingUniversity_throws_BR_USR_03() {
            RegisterRequest req = new RegisterRequest(
                    "a@b.com", "Password1", "Name", null, false, "EXT001", null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        @Test
        void newEmail_fptStudent_validRequest_savesAsPending_andReturnsId() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
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
        void newEmail_externalStudent_validRequest_savesAsPending() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(roleRepository.findByCode("TEAM_MEMBER")).thenReturn(Optional.of(teamMemberRole));
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any())).thenReturn(savedUser(99L, UserStatus.PENDING));

            RegisterRequest req = new RegisterRequest(
                    "ext@uni.edu", "Password1", "Ext User", null, false, "EXT001", "Partner Uni");
            Long id = authService.register(req);

            assertThat(id).isEqualTo(99L);
        }

        // ── email REJECTED → reactivate ───────────────────────────────────

        @Test
        void rejectedEmail_validRequest_reactivatesUser_returnsSameId() {
            User rejected = userWithStatus("rej@x.com", UserStatus.REJECTED);
            rejected.setId(77L);
            when(userRepository.findByEmail("rej@x.com")).thenReturn(Optional.of(rejected));
            when(passwordEncoder.encode(any())).thenReturn("$2a$new$hash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Long id = authService.register(fptReq("rej@x.com", "Password1"));

            assertThat(id).isEqualTo(77L);
            assertThat(rejected.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(rejected.getApprovedBy()).isNull();
            assertThat(rejected.getApprovedAt()).isNull();
            assertThat(rejected.getPasswordHash()).isEqualTo("$2a$new$hash");
            verify(auditPublisher).log(eq(rejected),
                    eq(AuditAction.ACCOUNT_REACTIVATED), eq("USER"), eq(77L),
                    any(), any(), isNull(), isNull());
        }

        @Test
        void rejectedEmail_weakPassword_throws_BR_USR_05() {
            // validation fires BEFORE checking DB — findByEmail not called
            assertThatThrownBy(() -> authService.register(fptReq("rej@x.com", "nodigit")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-05");
        }

        @Test
        void rejectedEmail_missingStudentId_throws_BR_USR_03() {
            RegisterRequest req = new RegisterRequest(
                    "rej@x.com", "Password1", "Name", null, true, null, null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-03");
        }

        // ── email PENDING / ACTIVE → 409 BR-USR-01 ───────────────────────

        @Test
        void pendingEmail_throws_BR_USR_01() {
            User pending = userWithStatus("dup@x.com", UserStatus.PENDING);
            when(userRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> authService.register(fptReq("dup@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-01");
        }

        @Test
        void activeEmail_throws_BR_USR_01() {
            User active = userWithStatus("dup@x.com", UserStatus.ACTIVE);
            when(userRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(active));

            assertThatThrownBy(() -> authService.register(fptReq("dup@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-01");
        }

        @Test
        void lockedEmail_throws_BR_USR_01() {
            User locked = userWithStatus("dup@x.com", UserStatus.LOCKED);
            when(userRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(locked));

            assertThatThrownBy(() -> authService.register(fptReq("dup@x.com", "Password1")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-01");
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

    // ─────────────────────── ACCOUNT APPROVAL ────────────────────────────

    @Nested
    class AccountApproval {

        @Test
        void approvePendingUser_setsActiveAndAuditsAccountApproved() {
            User target = userWithStatus("target@x.com", UserStatus.PENDING);
            target.setId(10L);
            User approver = userWithStatus("coord@x.com", UserStatus.ACTIVE);
            approver.setId(99L);

            when(userRepository.findById(10L)).thenReturn(Optional.of(target));
            when(userRepository.findById(99L)).thenReturn(Optional.of(approver));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.approveAccount(10L, 99L);

            assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(target.getApprovedBy()).isEqualTo(approver);
            assertThat(target.getApprovedAt()).isNotNull();
            verify(auditPublisher).log(eq(approver), eq(AuditAction.ACCOUNT_APPROVED),
                    eq("USER"), eq(10L), any(), any(), isNull(), isNull());
        }

        @Test
        void selfApprove_throws_BR_GOV_02() {
            User self = userWithStatus("coord@x.com", UserStatus.PENDING);
            self.setId(5L);
            when(userRepository.findById(5L)).thenReturn(Optional.of(self));

            assertThatThrownBy(() -> authService.approveAccount(5L, 5L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-GOV-02");
        }

        @Test
        void approveNonPendingUser_throws_BR_AUTH_06() {
            User target = userWithStatus("active@x.com", UserStatus.ACTIVE);
            target.setId(20L);
            User approver = userWithStatus("coord@x.com", UserStatus.ACTIVE);
            approver.setId(99L);
            when(userRepository.findById(20L)).thenReturn(Optional.of(target));
            when(userRepository.findById(99L)).thenReturn(Optional.of(approver));

            assertThatThrownBy(() -> authService.approveAccount(20L, 99L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-06");
        }

        @Test
        void rejectPendingUser_setsRejectedAndAuditsAccountRejected() {
            User target = userWithStatus("target@x.com", UserStatus.PENDING);
            target.setId(30L);
            User approver = userWithStatus("coord@x.com", UserStatus.ACTIVE);
            approver.setId(99L);

            when(userRepository.findById(30L)).thenReturn(Optional.of(target));
            when(userRepository.findById(99L)).thenReturn(Optional.of(approver));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.rejectAccount(30L, 99L, "Incomplete information");

            assertThat(target.getStatus()).isEqualTo(UserStatus.REJECTED);
            verify(auditPublisher).log(eq(approver), eq(AuditAction.ACCOUNT_REJECTED),
                    eq("USER"), eq(30L), any(), any(), eq("Incomplete information"), isNull());
        }

        @Test
        void rejectNonPendingUser_throws_BR_AUTH_07() {
            User target = userWithStatus("active@x.com", UserStatus.ACTIVE);
            target.setId(40L);
            User approver = userWithStatus("coord@x.com", UserStatus.ACTIVE);
            approver.setId(99L);
            when(userRepository.findById(40L)).thenReturn(Optional.of(target));
            when(userRepository.findById(99L)).thenReturn(Optional.of(approver));

            assertThatThrownBy(() -> authService.rejectAccount(40L, 99L, "some reason"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-AUTH-07");
        }

        @Test
        void listPendingAccounts_returnsMappedPage() {
            User pending = userWithStatus("p@x.com", UserStatus.PENDING);
            pending.setId(55L);
            Page<User> page = new PageImpl<>(List.of(pending), PageRequest.of(0, 20), 1);
            when(userRepository.findAllByStatus(eq(UserStatus.PENDING), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PendingAccountResponse> result =
                    authService.listPendingAccounts(PageRequest.of(0, 20));

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).id()).isEqualTo(55L);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    // ─────────────────────── CREATE GUEST JUDGE ──────────────────────────

    @Nested
    class CreateGuestJudge {

        private Role judgeRole;

        @BeforeEach
        void setup() {
            judgeRole = new Role();
            judgeRole.setId(4L);
            judgeRole.setCode("JUDGE");
        }

        @Test
        void createGuestJudge_createsActiveGuestJudge() {
            when(userRepository.findByEmail("judge@ext.com")).thenReturn(Optional.empty());
            when(roleRepository.findByCode("JUDGE")).thenReturn(Optional.of(judgeRole));
            User creator = userWithStatus("coord@x.com", UserStatus.ACTIVE);
            creator.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");
            User saved = new User();
            saved.setId(50L);
            saved.setEmail("judge@ext.com");
            saved.setFullName("Guest Judge");
            saved.setAccountType(AccountType.GUEST_JUDGE);
            saved.setPrimaryRole(judgeRole);
            saved.setStatus(UserStatus.ACTIVE);
            when(userRepository.save(any())).thenReturn(saved);

            GuestJudgeResponse resp = authService.createGuestJudge(
                    new CreateGuestJudgeRequest("judge@ext.com", "Guest Judge", null), 1L);

            assertThat(resp.userId()).isEqualTo(50L);
            assertThat(resp.email()).isEqualTo("judge@ext.com");
            assertThat(resp.temporaryPassword()).isNotBlank();
            verify(userRepository).save(argThat(u ->
                    u.getStatus() == UserStatus.ACTIVE
                    && u.getAccountType() == AccountType.GUEST_JUDGE
                    && u.getPrimaryRole() == judgeRole));
            verify(auditPublisher).log(eq(creator), eq(AuditAction.STAFF_ACCOUNT_CREATED),
                    eq("USER"), eq(50L), isNull(), any(), isNull(), isNull());
        }

        @Test
        void createGuestJudge_passwordMeetsStrengthRequirement() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(roleRepository.findByCode("JUDGE")).thenReturn(Optional.of(judgeRole));
            User creator = userWithStatus("coord@x.com", UserStatus.ACTIVE);
            creator.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(passwordEncoder.encode(any())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));
            User saved = new User();
            saved.setId(51L);
            saved.setEmail("j2@ext.com");
            saved.setFullName("J2");
            when(userRepository.save(any())).thenReturn(saved);

            GuestJudgeResponse resp = authService.createGuestJudge(
                    new CreateGuestJudgeRequest("j2@ext.com", "J2", null), 1L);

            String tempPwd = resp.temporaryPassword();
            assertThat(tempPwd).hasSizeGreaterThanOrEqualTo(8);
            assertThat(tempPwd).matches(".*[A-Z].*");
            assertThat(tempPwd).matches(".*\\d.*");
        }

        @Test
        void createGuestJudge_duplicateEmail_throws_BR_USR_01() {
            User existing = userWithStatus("taken@ext.com", UserStatus.ACTIVE);
            when(userRepository.findByEmail("taken@ext.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authService.createGuestJudge(
                    new CreateGuestJudgeRequest("taken@ext.com", "Name", null), 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-USR-01");
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
