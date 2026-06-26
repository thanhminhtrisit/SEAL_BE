package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByEventIdOrderByOrderNumberAsc(Long eventId);

    Optional<Round> findByEventIdAndOrderNumber(Long eventId, Integer orderNumber);
}
