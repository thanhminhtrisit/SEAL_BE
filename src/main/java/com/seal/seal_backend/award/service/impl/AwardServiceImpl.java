package com.seal.seal_backend.award.service.impl;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import com.seal.seal_backend.award.service.AwardService;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.repository.AwardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements AwardService {

    private final AwardRepository awardRepository;

    @Override
    @Transactional
    public AwardResponse createAward(AwardCreateRequest request, Long userId) {
        log.info("Coordinator (ID:{}) đang gán giải {} cho Team ID: {}", userId, request.awardType(), request.teamId());

        // 1. Dùng kỹ thuật Dummy Object rỗng để map khóa ngoại (Foreign Keys)
        Event eventRef = new Event(); eventRef.setId(request.eventId());
        Team teamRef = new Team(); teamRef.setId(request.teamId());
        User userRef = new User(); userRef.setId(userId);

        Ranking rankingRef = null;
        if (request.rankingId() != null) {
            rankingRef = new Ranking();
            rankingRef.setId(request.rankingId());
        }

        // 2. Build Award Entity
        Award award = Award.builder()
                .event(eventRef)
                .team(teamRef)
                .ranking(rankingRef)
                .awardType(request.awardType())
                .description(request.description())
                .awardedBy(userRef)
                .build();

        // 3. Lưu xuống Database
        Award savedAward = awardRepository.save(award);

        // 4. Map sang DTO trả về cho Client
        return new AwardResponse(
                savedAward.getId(),
                request.eventId(),
                request.teamId(),
                "Team-" + request.teamId(), // Đợi M1 (Team Module) ghép Port để lấy tên thật
                savedAward.getAwardType(),
                savedAward.getDescription(),
                userId,
                LocalDateTime.now() // Lúc trả về tạm lấy giờ hiện tại (JPA sẽ tự cập nhật vào entity sau)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AwardResponse> getAwardsByEvent(Long eventId) {
        log.info("Lấy danh sách giải thưởng của Event ID: {}", eventId);

        return awardRepository.findByEventIdOrderByAwardedAtDesc(eventId)
                .stream()
                .map(a -> new AwardResponse(
                        a.getId(),
                        a.getEvent().getId(),
                        a.getTeam().getId(),
                        "Team-" + a.getTeam().getId(),
                        a.getAwardType(),
                        a.getDescription(),
                        a.getAwardedBy().getId(),
                        a.getAwardedAt()
                ))
                .collect(Collectors.toList());
    }
}