package com.seal.seal_backend.scoring;

import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.domain.entity.Category;
import com.seal.seal_backend.domain.entity.CriteriaSet;
import com.seal.seal_backend.domain.entity.Evaluation;
import com.seal.seal_backend.domain.entity.JudgeAssignment;
import com.seal.seal_backend.domain.entity.Round;
import com.seal.seal_backend.domain.entity.Score;
import com.seal.seal_backend.domain.entity.ScoringCriterion;
import com.seal.seal_backend.domain.entity.Submission;
import com.seal.seal_backend.domain.entity.Team;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.AssignmentStatus;
import com.seal.seal_backend.domain.enums.EvaluationStatus;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.repository.EvaluationRepository;
import com.seal.seal_backend.domain.repository.JudgeAssignmentRepository;
import com.seal.seal_backend.domain.repository.ScoreRepository;
import com.seal.seal_backend.domain.repository.ScoringCriterionRepository;
import com.seal.seal_backend.domain.repository.SubmissionRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.ScoreItemRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.exception.BusinessException;
import com.seal.seal_backend.scoring.service.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock EvaluationRepository evaluationRepository;
    @Mock ScoreRepository scoreRepository;
    @Mock ScoringCriterionRepository scoringCriterionRepository;
    @Mock UserRepository userRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock AuditPublisher auditPublisher;

    @InjectMocks EvaluationServiceImpl service;

    @Test
    void startEvaluationRequestDoesNotAcceptJudgeId() {
        List<String> fieldNames = Arrays.stream(StartEvaluationRequest.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).containsExactly("submissionId");
    }

    @Test
    void startEvaluationDerivesJudgeFromCurrentUser() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        JudgeAssignment assignment = activeAssignment(11L, 4L, 1L, 10L);

        StartEvaluationRequest request = new StartEvaluationRequest();
        request.setSubmissionId(9L);

        when(userRepository.findById(4L)).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(9L)).thenReturn(Optional.of(submission));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(evaluationRepository.findByJudge_IdAndSubmission_IdAndRound_Id(4L, 9L, 1L))
                .thenReturn(Optional.empty());
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> saveEvaluation(invocation.getArgument(0), 100L));

        var response = service.startEvaluation(4L, request);

        assertThat(response.getJudgeId()).isEqualTo(4L);
        assertThat(response.getSubmissionId()).isEqualTo(9L);
        assertThat(response.getStatus()).isEqualTo(EvaluationStatus.DRAFT);

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getJudge().getId()).isEqualTo(4L);
        assertThat(captor.getValue().getJudgeAssignment().getId()).isEqualTo(11L);
        verify(auditPublisher).log(
                any(),
                org.mockito.ArgumentMatchers.eq(AuditAction.EVALUATION_STARTED),
                org.mockito.ArgumentMatchers.eq("EVALUATION"),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void reusedEvaluationDoesNotLogStartedAgain() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(100L, judge, submission, EvaluationStatus.DRAFT);
        JudgeAssignment assignment = activeAssignment(11L, 4L, 1L, 10L);

        StartEvaluationRequest request = new StartEvaluationRequest();
        request.setSubmissionId(9L);

        when(userRepository.findById(4L)).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(9L)).thenReturn(Optional.of(submission));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(evaluationRepository.findByJudge_IdAndSubmission_IdAndRound_Id(4L, 9L, 1L))
                .thenReturn(Optional.of(evaluation));

        var response = service.startEvaluation(4L, request);

        assertThat(response.getId()).isEqualTo(100L);
        verify(auditPublisher, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void judgeWithoutAssignmentCannotStartEvaluation() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);

        StartEvaluationRequest request = new StartEvaluationRequest();
        request.setSubmissionId(9L);

        when(userRepository.findById(4L)).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(9L)).thenReturn(Optional.of(submission));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.startEvaluation(4L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not assigned");

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void wrongCategoryAssignmentIsRejected() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        JudgeAssignment assignment = activeAssignment(11L, 4L, 1L, 99L);

        StartEvaluationRequest request = new StartEvaluationRequest();
        request.setSubmissionId(9L);

        when(userRepository.findById(4L)).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(9L)).thenReturn(Optional.of(submission));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));

        assertThatThrownBy(() -> service.startEvaluation(4L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not assigned");

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void judgeCanOnlyViewOwnEvaluation() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> service.getEvaluationById(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own evaluation");
    }

    @Test
    void judgeCannotUpdateAnotherJudgesEvaluation() {
        User owner = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, owner, submission, EvaluationStatus.DRAFT);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, BigDecimal.ONE, "ok")));

        assertThatThrownBy(() -> service.saveDraftScores(5L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own evaluation");
    }

    @Test
    void judgeCannotSubmitAnotherJudgesEvaluation() {
        User owner = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, owner, submission, EvaluationStatus.DRAFT);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> service.submitEvaluation(5L, 1L, new SubmitEvaluationRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own evaluation");
    }

    @Test
    void judgeCannotEditAfterSubmit() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.SUBMITTED);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, BigDecimal.ONE, "ok")));

        assertThatThrownBy(() -> service.saveDraftScores(4L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("draft evaluation");
    }

    @Test
    void scoreAboveMaxIsRejected() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion = criterion(1L, "Code", 10L, 1);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion));

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, new BigDecimal("12"), "too high")));

        assertThatThrownBy(() -> service.saveDraftScores(4L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must not exceed");

        verify(auditPublisher, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void newScoreCreatesAuditLog() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion = criterion(1L, "Code", 10L, 1);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion));
        when(scoreRepository.findByEvaluation_IdAndCriterion_Id(1L, 1L)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> saveScore(invocation.getArgument(0), 200L));
        when(scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(1L)).thenReturn(List.of());

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, BigDecimal.ONE, "first")));

        service.saveDraftScores(4L, 1L, request);

        verify(auditPublisher).log(
                org.mockito.ArgumentMatchers.eq(judge),
                org.mockito.ArgumentMatchers.eq(AuditAction.SCORE_CREATED),
                org.mockito.ArgumentMatchers.eq("SCORE"),
                org.mockito.ArgumentMatchers.eq(200L),
                any(),
                any(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void scoreIncreaseCreatesUpdatedAuditLog() {
        assertScoreUpdateAudit(new BigDecimal("6"), new BigDecimal("7"), "same note", "same note");
    }

    @Test
    void scoreDecreaseCreatesUpdatedAuditLog() {
        assertScoreUpdateAudit(new BigDecimal("7"), new BigDecimal("6"), "same note", "same note");
    }

    @Test
    void commentOnlyChangeCreatesUpdatedAuditLog() {
        assertScoreUpdateAudit(new BigDecimal("7"), new BigDecimal("7"), "old", "new");
    }

    @Test
    void identicalSaveCreatesNoAuditLog() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion = criterion(1L, "Code", 10L, 1);
        Score score = score(100L, evaluation, criterion, new BigDecimal("7"), "same");

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion));
        when(scoreRepository.findByEvaluation_IdAndCriterion_Id(1L, 1L)).thenReturn(Optional.of(score));
        when(scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(1L)).thenReturn(List.of(score));

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, new BigDecimal("7.00"), "same")));

        service.saveDraftScores(4L, 1L, request);

        verify(auditPublisher, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidScoreSaveCreatesNoAuditLog() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion = criterion(1L, "Code", 10L, 1);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion));

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, new BigDecimal("12"), "too high")));

        assertThatThrownBy(() -> service.saveDraftScores(4L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must not exceed");

        verify(auditPublisher, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitFailsIfAnyActiveCriterionIsMissing() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion1 = criterion(1L, "Code", 10L, 1);
        ScoringCriterion criterion2 = criterion(2L, "Demo", 10L, 2);
        Score score = new Score();
        score.setId(100L);
        score.setEvaluation(evaluation);
        score.setCriterion(criterion1);
        score.setScoreValue(BigDecimal.TEN);
        score.setComment("ok");

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion1, criterion2));
        when(scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(1L))
                .thenReturn(List.of(score));

        assertThatThrownBy(() -> service.submitEvaluation(4L, 1L, new SubmitEvaluationRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Missing scores");

        verify(auditPublisher, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitLogsEvaluationSubmitted() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion = criterion(1L, "Code", 10L, 1);
        Score score = score(100L, evaluation, criterion, BigDecimal.TEN, "ok");

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion));
        when(scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(1L))
                .thenReturn(List.of(score));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.submitEvaluation(4L, 1L, new SubmitEvaluationRequest());

        verify(auditPublisher).log(
                org.mockito.ArgumentMatchers.eq(judge),
                org.mockito.ArgumentMatchers.eq(AuditAction.EVALUATION_SUBMITTED),
                org.mockito.ArgumentMatchers.eq("EVALUATION"),
                org.mockito.ArgumentMatchers.eq(1L),
                any(),
                any(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void startEvaluationRejectsNonSubmittedSubmission() {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        submission.setStatus(SubmissionStatus.LOCKED);

        StartEvaluationRequest request = new StartEvaluationRequest();
        request.setSubmissionId(9L);

        when(userRepository.findById(4L)).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(9L)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.startEvaluation(4L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("submitted");
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Category category(Long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private Round round(Long id) {
        Round round = new Round();
        round.setId(id);
        return round;
    }

    private Submission submittedSubmission(Long submissionId, Long roundId, Long categoryId) {
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setRound(round(roundId));
        Team team = new Team();
        team.setCategory(category(categoryId));
        submission.setTeam(team);
        return submission;
    }

    private JudgeAssignment activeAssignment(Long id, Long judgeId, Long roundId, Long categoryId) {
        JudgeAssignment assignment = new JudgeAssignment();
        assignment.setId(id);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setJudge(user(judgeId));
        assignment.setRound(round(roundId));
        assignment.setCategory(category(categoryId));
        return assignment;
    }

    private Evaluation evaluation(Long id, User judge, Submission submission, EvaluationStatus status) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);
        evaluation.setJudge(judge);
        evaluation.setSubmission(submission);
        evaluation.setRound(submission.getRound());
        evaluation.setStatus(status);
        return evaluation;
    }

    private ScoringCriterion criterion(Long id, String name, long maxScore, int order) {
        ScoringCriterion criterion = new ScoringCriterion();
        criterion.setId(id);
        criterion.setName(name);
        criterion.setMaxScore(BigDecimal.valueOf(maxScore));
        criterion.setWeight(BigDecimal.valueOf(50));
        criterion.setDisplayOrder(order);
        criterion.setIsActive(true);
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.setId(100L + id);
        criteriaSet.setRound(round(1L));
        criterion.setCriteriaSet(criteriaSet);
        return criterion;
    }

    private ScoreItemRequest scoreItem(Long criterionId, BigDecimal scoreValue, String comment) {
        ScoreItemRequest item = new ScoreItemRequest();
        item.setCriterionId(criterionId);
        item.setScoreValue(scoreValue);
        item.setComment(comment);
        return item;
    }

    private Score score(Long id, Evaluation evaluation, ScoringCriterion criterion, BigDecimal value, String comment) {
        Score score = new Score();
        score.setId(id);
        score.setEvaluation(evaluation);
        score.setCriterion(criterion);
        score.setScoreValue(value);
        score.setComment(comment);
        return score;
    }

    private Evaluation saveEvaluation(Evaluation evaluation, Long id) {
        evaluation.setId(id);
        return evaluation;
    }

    private Score saveScore(Score score, Long id) {
        score.setId(id);
        return score;
    }

    private void assertScoreUpdateAudit(BigDecimal oldScore, BigDecimal newScore, String oldComment, String newComment) {
        User judge = user(4L);
        Submission submission = submittedSubmission(9L, 1L, 10L);
        Evaluation evaluation = evaluation(1L, judge, submission, EvaluationStatus.DRAFT);
        ScoringCriterion criterion = criterion(1L, "Code", 10L, 1);
        Score existing = score(100L, evaluation, criterion, oldScore, oldComment);

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(judgeAssignmentRepository.findByJudgeIdAndRoundIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment(11L, 4L, 1L, 10L)));
        when(scoringCriterionRepository.findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(criterion));
        when(scoreRepository.findByEvaluation_IdAndCriterion_Id(1L, 1L)).thenReturn(Optional.of(existing));
        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoreRepository.findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(1L)).thenReturn(List.of(existing));

        SaveScoresRequest request = new SaveScoresRequest();
        request.setScores(List.of(scoreItem(1L, newScore, newComment)));

        service.saveDraftScores(4L, 1L, request);

        ArgumentCaptor<String> oldJson = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newJson = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).log(
                org.mockito.ArgumentMatchers.eq(judge),
                org.mockito.ArgumentMatchers.eq(AuditAction.SCORE_UPDATED),
                org.mockito.ArgumentMatchers.eq("SCORE"),
                org.mockito.ArgumentMatchers.eq(100L),
                oldJson.capture(),
                newJson.capture(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        );
        assertThat(oldJson.getValue()).contains("\"evaluationId\":1");
        assertThat(oldJson.getValue()).contains("\"submissionId\":9");
        assertThat(oldJson.getValue()).contains("\"criterionId\":1");
        assertThat(oldJson.getValue()).contains("\"oldScoreValue\":" + oldScore.toPlainString());
        assertThat(oldJson.getValue()).contains("\"oldComment\":" + (oldComment == null ? "null" : "\"" + oldComment + "\""));
        assertThat(newJson.getValue()).contains("\"newScoreValue\":" + newScore.toPlainString());
        assertThat(newJson.getValue()).contains("\"newComment\":" + (newComment == null ? "null" : "\"" + newComment + "\""));
    }
}
