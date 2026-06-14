package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.EventBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventBudgetRepository extends JpaRepository<EventBudget, Long> {
}
