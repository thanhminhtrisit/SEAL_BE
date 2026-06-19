package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.CriteriaSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CriteriaSetRepository extends JpaRepository<CriteriaSet, Long> {
    List<CriteriaSet> findByEventId(Long eventId);
    Optional<CriteriaSet> findFirstByRoundId(Long roundId);
}
