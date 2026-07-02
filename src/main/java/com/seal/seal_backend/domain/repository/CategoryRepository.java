package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByEventIdOrderByCreatedAtAsc(Long eventId);
    boolean existsByEventIdAndName(Long eventId, String name);

    @Query("SELECT COUNT(DISTINCT c.mentor.id) FROM Category c " +
           "WHERE c.event.id = :eventId AND c.mentor IS NOT NULL")
    long countDistinctMentorsByEventId(@Param("eventId") Long eventId);
}
