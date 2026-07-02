package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Award;
import com.seal.seal_backend.domain.enums.AwardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwardRepository extends JpaRepository<Award, Long> {

    // Lấy danh sách giải thưởng của một Event
    @Query("SELECT a FROM Award a WHERE a.event.id = :eventId ORDER BY a.awardedAt DESC")
    List<Award> findByEventIdOrderByAwardedAtDesc(@Param("eventId") Long eventId);

    // Kiểm tra xem Đội này đã nhận Giải thưởng này trong Sự kiện này chưa bằng loại
    boolean existsByEventIdAndTeamIdAndAwardType(Long eventId, Long teamId, AwardType awardType);

    // Đếm xem Giải thưởng này đã được trao cho bao nhiêu đội trong Sự kiện này
    long countByEventIdAndAwardType(Long eventId, AwardType awardType);
}
