package com.seal.seal_backend.auth.service.impl;

import com.seal.seal_backend.auth.dto.response.JudgeResponse;
import com.seal.seal_backend.auth.dto.response.MentorResponse;
import com.seal.seal_backend.auth.service.UserDirectoryService;
import com.seal.seal_backend.domain.enums.UserStatus;
import com.seal.seal_backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDirectoryServiceImpl implements UserDirectoryService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JudgeResponse> listJudges() {
        return userRepository.findByPrimaryRole_CodeAndStatus("JUDGE", UserStatus.ACTIVE)
                .stream().map(JudgeResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorResponse> listMentors() {
        return userRepository.findByPrimaryRole_CodeAndStatus("MENTOR", UserStatus.ACTIVE)
                .stream().map(MentorResponse::from).toList();
    }
}
