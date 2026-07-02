package com.seal.seal_backend.scoring.service.impl;

import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.EvaluationStatus;
import com.seal.seal_backend.domain.enums.AssignmentStatus;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.ScoreItemRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.dto.response.EvaluationAuditEntryResponse;
import com.seal.seal_backend.scoring.dto.response.EvaluationResponse;
import com.seal.seal_backend.scoring.dto.response.JudgeAssignedSubmissionResponse;
import com.seal.seal_backend.scoring.dto.response.ScoreResponse;
import com.seal.seal_backend.scoring.exception.BusinessException;
import com.seal.seal_backend.scoring.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ScoreRepository scoreRepository;
    private final ScoringCriterionRepository scoringCriterionRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditPublisher auditPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignedSubmissionResponse> getAssignedSubmissions(Long currentUserId) {
        User judge = getJudgeOrThrow(currentUserId);
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeIdAndStatus(judge.getId(), AssignmentStatus.ACTIVE);

        if (assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> roundIds = assignments.stream()
                .map(assignment -> assignment.getRound().getId())
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, List<ScoringCriterion>> activeCriteriaByRound = new java.util.HashMap<>();
        for (Long roundId : roundIds) {
            activeCriteriaByRound.put(roundId, findActiveCriteriaForRound(roundId));
        }

        Map<String, Submission> latestSubmitted = new LinkedHashMap<>();
        for (Submission submission : submissionRepository.findSubmittedByRoundIds(roundIds, SubmissionStatus.SUBMITTED)) {
            String key = submission.getRound().getId() + ":" + submission.getTeam().getId();
            latestSubmitted.putIfAbsent(key, submission);
        }

        List<JudgeAssignedSubmissionResponse> responses = new ArrayList<>();
        for (Submission submission : latestSubmitted.values()) {
            if (!isSubmissionCoveredByAssignments(submission, assignments)) {
                continue;
            }

            Evaluation evaluation = evaluationRepository
                    .findByJudge_IdAndSubmission_IdAndRound_Id(
                            judge.getId(),
                            submission.getId(),
                            submission.getRound().getId()
                    )
                    .orElse(null);

            int totalCriteriaCount = activeCriteriaByRound.getOrDefault(submission.getRound().getId(), List.of()).size();
            int scoredCriteriaCount = evaluation == null
                    ? 0
                    : scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(evaluation.getId()).size();

            responses.add(JudgeAssignedSubmissionResponse.builder()
                    .submissionId(submission.getId())
                    .teamId(submission.getTeam().getId())
                    .teamName(submission.getTeam().getName())
                    .categoryId(submission.getTeam().getCategory().getId())
                    .categoryName(submission.getTeam().getCategory().getName())
                    .roundId(submission.getRound().getId())
                    .roundName(submission.getRound().getName())
                    .eventId(submission.getRound().getEvent().getId())
                    .eventName(submission.getRound().getEvent().getName())
                    .attemptNumber(submission.getAttemptNumber())
                    .submittedAt(submission.getSubmittedAt())
                    .repoUrl(submission.getRepoUrl())
                    .demoUrl(submission.getDemoUrl())
                    .slideUrl(submission.getSlideUrl())
                    .reportUrl(submission.getReportUrl())
                    .evaluationId(evaluation != null ? evaluation.getId() : null)
                    .evaluationStatus(evaluation != null ? evaluation.getStatus().name() : "NOT_STARTED")
                    .scoredCriteriaCount(scoredCriteriaCount)
                    .totalCriteriaCount(totalCriteriaCount)
                    .build());
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationAuditEntryResponse> getEvaluationAudit(Long currentUserId, Long evaluationId) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);
        validateEvaluationOwner(evaluation, currentUserId);

        List<AuditLog> evaluationLogs = auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAscIdAsc("EVALUATION", evaluationId);
        List<Long> scoreIds = scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(evaluationId)
                .stream()
                .map(Score::getId)
                .toList();
        List<AuditLog> scoreLogs = scoreIds.isEmpty()
                ? List.of()
                : auditLogRepository.findByTargetTypeAndTargetIdInOrderByCreatedAtAscIdAsc("SCORE", scoreIds);

        return java.util.stream.Stream.concat(evaluationLogs.stream(), scoreLogs.stream())
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).thenComparing(AuditLog::getId))
                .map(this::toAuditEntryResponse)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationResponse startEvaluation(Long currentUserId, StartEvaluationRequest request) {
        User judge = getJudgeOrThrow(currentUserId);

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new BusinessException("Only a submitted submission attempt can be evaluated");
        }

        JudgeAssignment judgeAssignment = resolveActiveJudgeAssignment(judge.getId(), submission);
        Round round = submission.getRound();

        Evaluation existingEvaluation = evaluationRepository
                .findByJudge_IdAndSubmission_IdAndRound_Id(
                        judge.getId(),
                        submission.getId(),
                        round.getId()
                )
                .orElse(null);

        if (existingEvaluation != null) {
            return toEvaluationResponse(existingEvaluation);
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setJudge(judge);
        evaluation.setSubmission(submission);
        evaluation.setRound(round);
        evaluation.setStatus(EvaluationStatus.DRAFT);
        evaluation.setStartedAt(LocalDateTime.now());
        evaluation.setJudgeAssignment(judgeAssignment);

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        auditPublisher.log(
                judge,
                AuditAction.EVALUATION_STARTED,
                "EVALUATION",
                savedEvaluation.getId(),
                null,
                buildEvaluationStartedJson(savedEvaluation),
                null,
                null
        );

        return toEvaluationResponse(savedEvaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluationById(Long currentUserId, Long evaluationId) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);
        validateEvaluationOwner(evaluation, currentUserId);
        return toEvaluationResponse(evaluation);
    }

    @Override
    @Transactional
    public EvaluationResponse saveDraftScores(Long currentUserId, Long evaluationId, SaveScoresRequest request) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);

        validateEvaluationOwner(evaluation, currentUserId);
        resolveActiveJudgeAssignment(evaluation.getJudge().getId(), evaluation.getSubmission());
        validateEvaluationIsDraft(evaluation);

        String originalGeneralComment = evaluation.getGeneralComment();
        boolean evaluationCommentChanged = request.getGeneralComment() != null
                && !Objects.equals(originalGeneralComment, request.getGeneralComment());
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

            Score existingScore = scoreRepository
                    .findByEvaluation_IdAndCriterion_Id(evaluation.getId(), criterion.getId())
                    .orElse(null);

            boolean created = existingScore == null;
            BigDecimal oldScoreValue = created ? null : existingScore.getScoreValue();
            String oldComment = created ? null : existingScore.getComment();
            boolean scoreValueChanged = created || !sameNumericScore(oldScoreValue, item.getScoreValue());
            boolean commentChanged = created || !Objects.equals(oldComment, item.getComment());

            if (!scoreValueChanged && !commentChanged) {
                continue;
            }

            Score score = created ? new Score() : existingScore;

            score.setEvaluation(evaluation);
            score.setCriterion(criterion);
            score.setScoreValue(item.getScoreValue());
            score.setComment(item.getComment());

            Score savedScore = scoreRepository.save(score);

            auditPublisher.log(
                    evaluation.getJudge(),
                    created ? AuditAction.SCORE_CREATED : AuditAction.SCORE_UPDATED,
                    "SCORE",
                    savedScore.getId(),
                    buildScoreChangeJson(evaluation, criterion.getId(), oldScoreValue, oldComment, "oldScoreValue", "oldComment"),
                    buildScoreChangeJson(evaluation, criterion.getId(), item.getScoreValue(), item.getComment(), "newScoreValue", "newComment"),
                    null,
                    null
            );
        }

        evaluationRepository.save(evaluation);

        if (evaluationCommentChanged) {
            auditPublisher.log(
                    evaluation.getJudge(),
                    AuditAction.EVALUATION_UPDATED,
                    "EVALUATION",
                    evaluation.getId(),
                    buildEvaluationCommentJson(evaluation.getId(), evaluation.getSubmission().getId(), evaluation.getRound().getId(), originalGeneralComment),
                    buildEvaluationCommentJson(evaluation.getId(), evaluation.getSubmission().getId(), evaluation.getRound().getId(), evaluation.getGeneralComment()),
                    null,
                    null
            );
        }

        return toEvaluationResponse(evaluation);
    }

    @Override
    @Transactional
    public EvaluationResponse submitEvaluation(Long currentUserId, Long evaluationId, SubmitEvaluationRequest request) {
        Evaluation evaluation = getEvaluationOrThrow(evaluationId);

        validateEvaluationOwner(evaluation, currentUserId);
        resolveActiveJudgeAssignment(evaluation.getJudge().getId(), evaluation.getSubmission());
        validateEvaluationIsDraft(evaluation);

        if (request.getGeneralComment() != null) {
            evaluation.setGeneralComment(request.getGeneralComment());
        }

        validateAllCriteriaScored(evaluation);

        evaluation.setStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmittedAt(LocalDateTime.now());
        evaluation.setLockedAt(LocalDateTime.now());

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        EvaluationStatus oldStatus = EvaluationStatus.DRAFT;
        auditPublisher.log(
                evaluation.getJudge(),
                AuditAction.EVALUATION_SUBMITTED,
                "EVALUATION",
                savedEvaluation.getId(),
                buildEvaluationStatusJson(savedEvaluation.getId(), oldStatus, null, null),
                buildEvaluationStatusJson(savedEvaluation.getId(), savedEvaluation.getStatus(), savedEvaluation.getSubmittedAt(), savedEvaluation.getLockedAt()),
                null,
                null
        );

        return toEvaluationResponse(savedEvaluation);
    }

    private Evaluation getEvaluationOrThrow(Long evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
    }

    private User getJudgeOrThrow(Long judgeId) {
        return userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Judge not found"));
    }

    private void validateEvaluationOwner(Evaluation evaluation, Long currentUserId) {
        if (!evaluation.getJudge().getId().equals(currentUserId)) {
            throw new BusinessException("You can only access your own evaluation");
        }
    }

    private boolean isSubmissionCoveredByAssignments(Submission submission, List<JudgeAssignment> assignments) {
        Long roundId = submission.getRound().getId();
        Long categoryId = submission.getTeam().getCategory().getId();

        return assignments.stream()
                .filter(assignment -> assignment.getRound().getId().equals(roundId))
                .anyMatch(assignment -> assignment.getCategory() == null
                        || assignment.getCategory().getId().equals(categoryId));
    }

    private JudgeAssignment resolveActiveJudgeAssignment(Long judgeId, Submission submission) {
        Long roundId = submission.getRound().getId();
        Long categoryId = submission.getTeam().getCategory().getId();

        List<JudgeAssignment> activeAssignments = judgeAssignmentRepository
                .findByJudgeIdAndRoundIdAndStatus(judgeId, roundId, AssignmentStatus.ACTIVE);

        return activeAssignments.stream()
                .filter(assignment -> assignment.getCategory() == null
                        || assignment.getCategory().getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Judge is not assigned to this submission's round/category"
                ));
    }

    private List<ScoringCriterion> getActiveCriteriaForRound(Long roundId) {
        List<ScoringCriterion> criteria = scoringCriterionRepository
                .findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(roundId);

        if (criteria.isEmpty()) {
            throw new BusinessException("No active scoring criteria found for this round");
        }

        return criteria;
    }

    private List<ScoringCriterion> findActiveCriteriaForRound(Long roundId) {
        return scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(roundId);
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
        List<Score> existingScores = scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(evaluation.getId());
        List<ScoringCriterion> activeCriteria = findActiveCriteriaForRound(evaluation.getRound().getId());
        if (activeCriteria.isEmpty()) {
            activeCriteria = existingScores.stream()
                    .map(Score::getCriterion)
                    .filter(Objects::nonNull)
                    .toList();
        }

        Map<Long, Score> scoreMap = existingScores.stream()
                .filter(score -> score.getCriterion() != null)
                .collect(Collectors.toMap(score -> score.getCriterion().getId(), score -> score, (left, right) -> left, LinkedHashMap::new));

        List<ScoreResponse> scoreResponses = activeCriteria.stream()
                .map(criterion -> toScoreResponse(criterion, scoreMap.get(criterion.getId())))
                .toList();

        BigDecimal totalRawScore = scoreResponses.stream()
                .map(ScoreResponse::getScoreValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeightedScore = scoreResponses.stream()
                .map(ScoreResponse::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long judgeAssignmentId = evaluation.getJudgeAssignment() != null
                ? evaluation.getJudgeAssignment().getId()
                : null;

        Submission submission = evaluation.getSubmission();
        Team team = submission != null ? submission.getTeam() : null;
        Category category = team != null ? team.getCategory() : null;
        Round round = evaluation.getRound();
        Event event = round != null ? round.getEvent() : null;

        return EvaluationResponse.builder()
                .id(evaluation.getId())
                .judgeAssignmentId(judgeAssignmentId)
                .judgeId(evaluation.getJudge().getId())
                .submissionId(submission.getId())
                .roundId(round != null ? round.getId() : null)
                .eventId(event != null ? event.getId() : null)
                .eventName(event != null ? event.getName() : null)
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : null)
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .attemptNumber(submission.getAttemptNumber())
                .repoUrl(submission != null ? submission.getRepoUrl() : null)
                .demoUrl(submission != null ? submission.getDemoUrl() : null)
                .slideUrl(submission != null ? submission.getSlideUrl() : null)
                .reportUrl(submission != null ? submission.getReportUrl() : null)
                .submittedById(submission != null && submission.getSubmittedBy() != null ? submission.getSubmittedBy().getId() : null)
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
                .criteria(scoreResponses)
                .build();
    }

    private ScoreResponse toScoreResponse(ScoringCriterion criterion, Score score) {
        BigDecimal scoreValue = score != null ? score.getScoreValue() : null;

        BigDecimal weightedScore = calculateWeightedScore(
                scoreValue,
                criterion.getMaxScore(),
                criterion.getWeight()
        );

        return ScoreResponse.builder()
                .id(score != null ? score.getId() : null)
                .criterionId(criterion.getId())
                .displayOrder(criterion.getDisplayOrder())
                .criterionName(criterion.getName())
                .criterionDescription(criterion.getDescription())
                .maxScore(criterion.getMaxScore())
                .weight(criterion.getWeight())
                .scoreValue(scoreValue)
                .weightedScore(weightedScore)
                .comment(score != null ? score.getComment() : null)
                .scoredAt(score != null ? score.getScoredAt() : null)
                .updatedAt(score != null ? score.getUpdatedAt() : null)
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

    private boolean sameNumericScore(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private String buildEvaluationStartedJson(Evaluation evaluation) {
        return "{"
                + "\"evaluationId\":" + evaluation.getId()
                + ",\"submissionId\":" + evaluation.getSubmission().getId()
                + ",\"roundId\":" + evaluation.getRound().getId()
                + ",\"judgeId\":" + evaluation.getJudge().getId()
                + ",\"judgeAssignmentId\":" + (evaluation.getJudgeAssignment() != null ? evaluation.getJudgeAssignment().getId() : "null")
                + ",\"status\":\"" + evaluation.getStatus() + "\""
                + ",\"startedAt\":\"" + evaluation.getStartedAt() + "\""
                + "}";
    }

    private String buildScoreChangeJson(
            Evaluation evaluation,
            Long criterionId,
            BigDecimal scoreValue,
            String comment,
            String scoreValueFieldName,
            String commentFieldName
    ) {
        return "{"
                + "\"evaluationId\":" + evaluation.getId()
                + ",\"submissionId\":" + evaluation.getSubmission().getId()
                + ",\"criterionId\":" + criterionId
                + ",\"" + scoreValueFieldName + "\":" + (scoreValue == null ? "null" : scoreValue.toPlainString())
                + ",\"" + commentFieldName + "\":" + jsonString(comment)
                + "}";
    }

    private String buildEvaluationCommentJson(Long evaluationId, Long submissionId, Long roundId, String generalComment) {
        return "{"
                + "\"evaluationId\":" + evaluationId
                + ",\"submissionId\":" + submissionId
                + ",\"roundId\":" + roundId
                + ",\"generalComment\":" + jsonString(generalComment)
                + "}";
    }

    private String buildEvaluationStatusJson(Long evaluationId, EvaluationStatus evaluationStatus,
                                             LocalDateTime submittedAt, LocalDateTime lockedAt) {
        return "{"
                + "\"evaluationId\":" + evaluationId
                + ",\"status\":\"" + evaluationStatus + "\""
                + ",\"submittedAt\":" + jsonString(submittedAt == null ? null : submittedAt.toString())
                + ",\"lockedAt\":" + jsonString(lockedAt == null ? null : lockedAt.toString())
                + "}";
    }

    private EvaluationAuditEntryResponse toAuditEntryResponse(AuditLog log) {
        return EvaluationAuditEntryResponse.builder()
                .id(log.getId())
                .actionType(log.getActionType())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .actorId(log.getActor() != null ? log.getActor().getId() : null)
                .actorName(log.getActor() != null ? log.getActor().getFullName() : null)
                .actorEmail(log.getActor() != null ? log.getActor().getEmail() : null)
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }
}
