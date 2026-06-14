package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.ResultPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultPublicationRepository extends JpaRepository<ResultPublication, Long> {
}
