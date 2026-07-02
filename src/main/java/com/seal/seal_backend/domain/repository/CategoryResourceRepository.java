package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.CategoryResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryResourceRepository extends JpaRepository<CategoryResource, Long> {
    List<CategoryResource> findByCategoryIdOrderByCreatedAtAsc(Long categoryId);
}
