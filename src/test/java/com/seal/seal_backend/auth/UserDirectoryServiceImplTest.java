package com.seal.seal_backend.auth;

import com.seal.seal_backend.auth.dto.response.JudgeResponse;
import com.seal.seal_backend.auth.dto.response.MentorResponse;
import com.seal.seal_backend.auth.service.impl.UserDirectoryServiceImpl;
import com.seal.seal_backend.domain.entity.Role;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.AccountType;
import com.seal.seal_backend.domain.enums.UserStatus;
import com.seal.seal_backend.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceImplTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserDirectoryServiceImpl service;

    @Test
    void listJudges_returnsActiveJudges() {
        User judge = buildUser(4L, "Internal Judge", "judge@seal.local", "JUDGE", AccountType.STAFF);

        when(userRepository.findByPrimaryRole_CodeAndStatus("JUDGE", UserStatus.ACTIVE))
                .thenReturn(List.of(judge));

        List<JudgeResponse> result = service.listJudges();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(4L);
        assertThat(result.get(0).email()).isEqualTo("judge@seal.local");
        assertThat(result.get(0).accountType()).isEqualTo(AccountType.STAFF);
    }

    @Test
    void listJudges_includesGuestJudge() {
        User guestJudge = buildUser(5L, "Guest Judge", "guest@test.local", "JUDGE", AccountType.GUEST_JUDGE);

        when(userRepository.findByPrimaryRole_CodeAndStatus("JUDGE", UserStatus.ACTIVE))
                .thenReturn(List.of(guestJudge));

        List<JudgeResponse> result = service.listJudges();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).accountType()).isEqualTo(AccountType.GUEST_JUDGE);
    }

    @Test
    void listJudges_whenNone_returnsEmpty() {
        when(userRepository.findByPrimaryRole_CodeAndStatus("JUDGE", UserStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(service.listJudges()).isEmpty();
    }

    @Test
    void listMentors_returnsActiveMentors() {
        User mentor = buildUser(10L, "Active Mentor", "mentor@seal.local", "MENTOR", AccountType.STAFF);

        when(userRepository.findByPrimaryRole_CodeAndStatus("MENTOR", UserStatus.ACTIVE))
                .thenReturn(List.of(mentor));

        List<MentorResponse> result = service.listMentors();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).email()).isEqualTo("mentor@seal.local");
        assertThat(result.get(0).fullName()).isEqualTo("Active Mentor");
    }

    @Test
    void listMentors_whenNone_returnsEmpty() {
        when(userRepository.findByPrimaryRole_CodeAndStatus("MENTOR", UserStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(service.listMentors()).isEmpty();
    }

    private User buildUser(Long id, String fullName, String email, String roleCode, AccountType accountType) {
        Role role = new Role();
        role.setCode(roleCode);

        User u = new User();
        u.setId(id);
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPrimaryRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setAccountType(accountType);
        return u;
    }
}
