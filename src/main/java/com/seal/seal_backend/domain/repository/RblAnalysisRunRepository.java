package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.RblAnalysisRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RblAnalysisRunRepository extends JpaRepository<RblAnalysisRun, Long> {
}
