package com.seal.seal_backend.event.service;

import com.seal.seal_backend.domain.enums.AssignmentStatus;
import com.seal.seal_backend.domain.repository.JudgeAssignmentRepository;
import com.seal.seal_backend.shared.contract.JudgeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeQueryAdapter implements JudgeQueryPort {

    private final JudgeAssignmentRepository judgeAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isJudgeAssignedToRound(Long judgeId, Long roundId) {
        return judgeAssignmentRepository.existsByJudgeIdAndRoundIdAndStatus(
                judgeId, roundId, AssignmentStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> judgeIdsForRound(Long roundId) {
        return judgeAssignmentRepository.findJudgeIdsByRoundId(roundId);
    }
}
