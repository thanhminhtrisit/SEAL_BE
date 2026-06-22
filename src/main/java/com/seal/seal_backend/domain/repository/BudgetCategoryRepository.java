package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    List<BudgetCategory> findAllByOrderByNameAsc();
}
