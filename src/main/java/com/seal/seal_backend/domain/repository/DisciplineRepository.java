package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {
    List<Discipline> findByIsActiveTrueOrderByNameAsc();
}
