package com.seal.seal_backend.auth.service.impl;

import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.auth.dto.response.PendingAccountResponse;
import com.seal.seal_backend.auth.security.JwtTokenProvider;
import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.auth.service.AuthService;
import com.seal.seal_backend.common.api.PageResponse;
import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.Role;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.AccountType;
import com.seal.seal_backend.domain.enums.UserStatus;
import com.seal.seal_backend.domain.repository.RoleRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditPublisher auditPublisher;

    @Override
    @Transactional
    public Long register(RegisterRequest req) {
        validatePasswordStrength(req.password());
        validateStudentFields(req);

        return userRepository.findByEmail(req.email())
                .map(existing -> reactivateRejected(existing, req))
                .orElseGet(() -> createNew(req));
    }

    private Long reactivateRejected(User existing, RegisterRequest req) {
        if (existing.getStatus() != UserStatus.REJECTED) {
            throw new BusinessRuleException("BR-USR-01",
                    "Email is already registered: " + req.email());
        }
        String oldJson = "{\"status\":\"REJECTED\"}";
        existing.setFullName(req.fullName());
        existing.setPhone(req.phone());
        existing.setStudentId(req.studentId());
        existing.setUniversity(req.university());
        existing.setIsFptStudent(req.fptStudent());
        existing.setPasswordHash(passwordEncoder.encode(req.password()));
        existing.setStatus(UserStatus.PENDING);
        existing.setApprovedBy(null);
        existing.setApprovedAt(null);
        userRepository.save(existing);

        auditPublisher.log(existing, AuditAction.ACCOUNT_REACTIVATED, "USER", existing.getId(),
                oldJson, "{\"status\":\"PENDING\"}", null, null);
        return existing.getId();
    }

    private Long createNew(RegisterRequest req) {
        Role teamMemberRole = roleRepository.findByCode("TEAM_MEMBER")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System role TEAM_MEMBER not found — ensure db/schema.sql seed data is loaded."));

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setPhone(req.phone());
        user.setIsFptStudent(req.fptStudent());
        user.setStudentId(req.studentId());
        user.setUniversity(req.university());
        user.setAccountType(AccountType.PARTICIPANT);
        user.setPrimaryRole(teamMemberRole);
        user.setStatus(UserStatus.PENDING);

        return userRepository.save(user).getId();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        // Generic message for email-not-found to avoid user enumeration
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessRuleException("BR-AUTH-01", "Invalid email or password."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessRuleException("BR-AUTH-01", "Invalid email or password.");
        }

        // FR-AUTH-05: block non-ACTIVE accounts with clear per-status messages
        switch (user.getStatus()) {
            case PENDING ->
                throw new BusinessRuleException("BR-AUTH-02",
                        "Your account is awaiting admin approval.");
            case LOCKED ->
                throw new BusinessRuleException("BR-AUTH-03",
                        "Your account is locked. Reason: "
                        + (user.getLockedReason() != null ? user.getLockedReason() : "contact admin."));
            case REJECTED ->
                throw new BusinessRuleException("BR-AUTH-04",
                        "Your account registration was rejected.");
            case INACTIVE ->
                throw new BusinessRuleException("BR-AUTH-05",
                        "Your account is inactive. Contact admin.");
            case ACTIVE -> { /* proceed */ }
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserPrincipal principal = UserPrincipal.from(user);
        return new AuthResponse(
                jwtTokenProvider.generateAccessToken(principal),
                jwtTokenProvider.generateRefreshToken(principal));
    }

    @Override
    public void logout(String accessToken) {
        /*
         * JWT is stateless — the schema has no token-blacklist table.
         * True server-side revocation would require a dedicated table (future scope).
         * Current contract: client MUST discard both tokens after calling this endpoint.
         * If a refresh-token table is added later, revoke it here by token hash lookup.
         */
    }

    @Override
    @Transactional
    public void approveAccount(Long userId, Long approverId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver", approverId));

        // BR-GOV-02: cannot approve your own account
        if (userId.equals(approverId)) {
            throw new BusinessRuleException("BR-GOV-02", "You cannot approve your own account.");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("BR-AUTH-06",
                    "Only PENDING accounts can be approved. Current status: " + user.getStatus());
        }

        String oldJson = "{\"status\":\"PENDING\"}";
        user.setStatus(UserStatus.ACTIVE);
        user.setApprovedBy(approver);
        user.setApprovedAt(LocalDateTime.now());
        userRepository.save(user);

        auditPublisher.log(approver, AuditAction.ACCOUNT_APPROVED, "USER", userId,
                oldJson, "{\"status\":\"ACTIVE\"}", null, null);
    }

    @Override
    @Transactional
    public void rejectAccount(Long userId, Long approverId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver", approverId));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("BR-AUTH-07",
                    "Only PENDING accounts can be rejected. Current status: " + user.getStatus());
        }

        String oldJson = "{\"status\":\"PENDING\"}";
        user.setStatus(UserStatus.REJECTED);
        user.setApprovedBy(approver);
        user.setApprovedAt(LocalDateTime.now());
        userRepository.save(user);

        auditPublisher.log(approver, AuditAction.ACCOUNT_REJECTED, "USER", userId,
                oldJson, "{\"status\":\"REJECTED\"}", reason, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PendingAccountResponse> listPendingAccounts(Pageable pageable) {
        return PageResponse.of(
                userRepository.findAllByStatus(UserStatus.PENDING, pageable)
                        .map(PendingAccountResponse::from));
    }

    private void validatePasswordStrength(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessRuleException("BR-USR-05",
                    "Password must be at least 8 characters and contain at least 1 uppercase letter and 1 digit.");
        }
    }

    private void validateStudentFields(RegisterRequest req) {
        if (req.fptStudent()) {
            if (!hasText(req.studentId())) {
                throw new BusinessRuleException("BR-USR-03", "FPT students must provide a student ID.");
            }
        } else {
            if (!hasText(req.studentId())) {
                throw new BusinessRuleException("BR-USR-03", "External participants must provide a student ID.");
            }
            if (!hasText(req.university())) {
                throw new BusinessRuleException("BR-USR-03", "External participants must provide a university name.");
            }
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
