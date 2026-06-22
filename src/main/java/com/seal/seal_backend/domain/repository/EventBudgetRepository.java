package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.EventBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventBudgetRepository extends JpaRepository<EventBudget, Long> {
    Optional<EventBudget> findByEventId(Long eventId);
}
