package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsBySlug(String slug);
    List<Event> findAllByOrderByCreatedAtDesc();
}
