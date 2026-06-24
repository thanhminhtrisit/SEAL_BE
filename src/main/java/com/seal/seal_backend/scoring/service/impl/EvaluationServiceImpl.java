package com.seal.seal_backend.scoring.service.impl;

import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.EvaluationStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.ScoreItemRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.dto.response.EvaluationResponse;
import com.seal.seal_backend.scoring.dto.response.ScoreResponse;
import com.seal.seal_backend.scoring.exception.BusinessException;
import com.seal.seal_backend.scoring.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ScoreRepository scoreRepository;
    private final ScoringCriterionRepository scoringCriterionRepository;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    @Override
    @Transactional
    public EvaluationResponse startEvaluation(StartEvaluationRequest request) {
        User judge = userRepository.findById(request.getJudgeId())
                .orElseThrow(() -> new ResourceNotFoundException("Judge not found"));

        SubmissionVersion submissionVersion = submissionVersionRepository.findById(request.getSubmissionVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission version not found"));

        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        Evaluation existingEvaluation = evaluationRepository
                .findByJudge_IdAndSubmissionVersion_IdAndRound_Id(
                        judge.getId(),
                        submissionVersion.getId(),
                        round.getId()
                )
                .orElse(null);

        if (existingEvaluation != null) {
            return toEvaluationResponse(existingEvaluation);
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setJudge(judge);
        evaluation.setSubmissionVersion(submissionVersion);
        evaluation.setRound(round);
        evaluation.setStatus(EvaluationStatus.DRAFT);
        evaluation.setStartedAt(LocalDateTime.now());

        if (request.getJudgeAssignmentId() != null) {
            JudgeAssignment judgeAssignment = judgeAssignmentRepository.findById(request.getJudgeAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Judge assignment not found"));

            evaluation.setJudgeAssignment(judgeAssignment);
        }

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        return toEvaluationResponse(savedEvaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluationById(Long evaluationId) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);
        return toEvaluationResponse(evaluation);
    }

    @Override
    @Transactional
    public EvaluationResponse saveDraftScores(Long evaluationId, SaveScoresRequest request) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);

        validateEvaluationIsDraft(evaluation);

        if (request.getGeneralComment() != null) {
            evaluation.setGeneralComment(request.getGeneralComment());
        }

        List<ScoringCriterion> activeCriteria = getActiveCriteriaForRound(evaluation.getRound().getId());

        Map<Long, ScoringCriterion> criterionMap = activeCriteria
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ScoringCriterion::getId,
                        criterion -> criterion
                ));

        for (ScoreItemRequest item : request.getScores()) {
            ScoringCriterion criterion = criterionMap.get(item.getCriterionId());

            if (criterion == null) {
                throw new BusinessException(
                        "Criterion id " + item.getCriterionId() + " does not belong to this round or is inactive"
                );
            }

            validateScoreValue(item.getScoreValue(), criterion);

            Score score = scoreRepository
                    .findByEvaluation_IdAndCriterion_Id(evaluation.getId(), criterion.getId())
                    .orElseGet(Score::new);

            score.setEvaluation(evaluation);
            score.setCriterion(criterion);
            score.setScoreValue(item.getScoreValue());
            score.setComment(item.getComment());

            scoreRepository.save(score);
        }

        evaluationRepository.save(evaluation);

        return toEvaluationResponse(evaluation);
    }

    @Override
    @Transactional
    public EvaluationResponse submitEvaluation(Long evaluationId, SubmitEvaluationRequest request) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);

        validateEvaluationIsDraft(evaluation);

        if (request.getGeneralComment() != null) {
            evaluation.setGeneralComment(request.getGeneralComment());
        }

        validateAllCriteriaScored(evaluation);

        evaluation.setStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmittedAt(LocalDateTime.now());
        evaluation.setLockedAt(LocalDateTime.now());

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        return toEvaluationResponse(savedEvaluation);
    }

    private Evaluation getEvaluationOrThrow(Long evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
    }

    private List<ScoringCriterion> getActiveCriteriaForRound(Long roundId) {
        List<ScoringCriterion> criteria = scoringCriterionRepository
                .findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(roundId);

        if (criteria.isEmpty()) {
            throw new BusinessException("No active scoring criteria found for this round");
        }

        return criteria;
    }

    private void validateEvaluationIsDraft(Evaluation evaluation) {
        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException("Only draft evaluation can be modified");
        }
    }

    private void validateScoreValue(BigDecimal scoreValue, ScoringCriterion criterion) {
        if (scoreValue == null) {
            throw new BusinessException("Score value must not be null");
        }

        if (scoreValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Score value must be greater than or equal to 0");
        }

        if (criterion.getMaxScore() != null && scoreValue.compareTo(criterion.getMaxScore()) > 0) {
            throw new BusinessException(
                    "Score for criterion '" + criterion.getName() + "' must not exceed max score " + criterion.getMaxScore()
            );
        }
    }

    private void validateAllCriteriaScored(Evaluation evaluation) {
        List<ScoringCriterion> activeCriteria = getActiveCriteriaForRound(evaluation.getRound().getId());

        List<Score> scores = scoreRepository
                .findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(evaluation.getId());

        Set<Long> scoredCriterionIds = new HashSet<>();

        for (Score score : scores) {
            validateScoreValue(score.getScoreValue(), score.getCriterion());
            scoredCriterionIds.add(score.getCriterion().getId());
        }

        List<String> missingCriteriaNames = activeCriteria
                .stream()
                .filter(criterion -> !scoredCriterionIds.contains(criterion.getId()))
                .map(ScoringCriterion::getName)
                .toList();

        if (!missingCriteriaNames.isEmpty()) {
            throw new BusinessException("Missing scores for criteria: " + String.join(", ", missingCriteriaNames));
        }
    }

    private EvaluationResponse toEvaluationResponse(Evaluation evaluation) {
        List<Score> scores = scoreRepository
                .findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(evaluation.getId());

        List<ScoreResponse> scoreResponses = scores
                .stream()
                .map(this::toScoreResponse)
                .toList();

        BigDecimal totalRawScore = scoreResponses
                .stream()
                .map(ScoreResponse::getScoreValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeightedScore = scoreResponses
                .stream()
                .map(ScoreResponse::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long judgeAssignmentId = evaluation.getJudgeAssignment() != null
                ? evaluation.getJudgeAssignment().getId()
                : null;

        return EvaluationResponse.builder()
                .id(evaluation.getId())
                .judgeAssignmentId(judgeAssignmentId)
                .judgeId(evaluation.getJudge().getId())
                .submissionVersionId(evaluation.getSubmissionVersion().getId())
                .roundId(evaluation.getRound().getId())
                .status(evaluation.getStatus())
                .generalComment(evaluation.getGeneralComment())
                .totalRawScore(totalRawScore)
                .totalWeightedScore(totalWeightedScore)
                .startedAt(evaluation.getStartedAt())
                .submittedAt(evaluation.getSubmittedAt())
                .lockedAt(evaluation.getLockedAt())
                .createdAt(evaluation.getCreatedAt())
                .updatedAt(evaluation.getUpdatedAt())
                .scores(scoreResponses)
                .build();
    }

    private ScoreResponse toScoreResponse(Score score) {
        ScoringCriterion criterion = score.getCriterion();

        BigDecimal weightedScore = calculateWeightedScore(
                score.getScoreValue(),
                criterion.getMaxScore(),
                criterion.getWeight()
        );

        return ScoreResponse.builder()
                .id(score.getId())
                .criterionId(criterion.getId())
                .criterionName(criterion.getName())
                .criterionDescription(criterion.getDescription())
                .maxScore(criterion.getMaxScore())
                .weight(criterion.getWeight())
                .scoreValue(score.getScoreValue())
                .weightedScore(weightedScore)
                .comment(score.getComment())
                .scoredAt(score.getScoredAt())
                .updatedAt(score.getUpdatedAt())
                .build();
    }

    private BigDecimal calculateWeightedScore(BigDecimal scoreValue, BigDecimal maxScore, BigDecimal weight) {
        if (scoreValue == null || maxScore == null || weight == null) {
            return BigDecimal.ZERO;
        }

        if (maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return scoreValue
                .multiply(weight)
                .divide(maxScore, 2, RoundingMode.HALF_UP);
    }
}
