package com.seal.seal_backend.ranking.service.scheduler;

import com.seal.seal_backend.domain.entity.Round;
import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.domain.repository.RoundRepository;
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoundAutomationTask {

    private final RoundRepository roundRepository;
    private final RankingService rankingService;

    // Chạy mỗi 60 giây
    @Scheduled(fixedRate = 60000)
    public void processScoringDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        // Tìm các vòng thi đang mở chấm điểm (SCORING_OPEN) và đã quá hạn
        List<Round> expiredRounds = roundRepository.findByStatusAndScoringDeadlineBeforeOrderByOrderNumberAsc(
                RoundStatus.SCORING_OPEN, now);

        for (Round round : expiredRounds) {
            log.info("Đã đến hạn chốt điểm vòng: {}. Bắt đầu tự động hóa...", round.getName());

            try {
                // Tự động tính toán xếp hạng và lấy danh sách kết quả
                List<RankingResponse> rankings = rankingService.computeRankingForRound(round.getId(), 0L, null);

                // Lọc ra ID của các đội được thăng hạng (isPromoted == true)
                List<Long> promotedTeamIds = rankings.stream()
                        .filter(RankingResponse::isPromoted)
                        .map(RankingResponse::teamId)
                        .collect(Collectors.toList());

                // Gọi hàm thăng hạng nếu có đội đỗ và đây KHÔNG phải vòng Chung kết
                if (!promotedTeamIds.isEmpty() && Boolean.FALSE.equals(round.getIsFinalRound())) {
                    log.info("Phát hiện {} đội đạt tiêu chuẩn. Đang tiến hành tạo dữ liệu cho vòng tiếp theo...", promotedTeamIds.size());
                    rankingService.promoteTeamsToNextRound(round.getId(), promotedTeamIds);
                }

                // Khóa vòng thi sau khi mọi thủ tục hoàn tất
                round.setStatus(RoundStatus.SCORING_LOCKED);
                roundRepository.save(round);

                log.info("Vòng thi {} đã được khóa và xếp hạng thành công.", round.getName());

            } catch (Exception e) {
                // Dùng try-catch để nếu vòng này có lỗi (ví dụ chưa set Next Round), hệ thống không bị crash và vẫn chạy tiếp các vòng khác
                log.error("Lỗi trong quá trình tự động hóa vòng thi {}: {}", round.getName(), e.getMessage());
            }
        }
    }
}