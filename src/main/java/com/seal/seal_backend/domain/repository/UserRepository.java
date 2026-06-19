package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findAllByStatus(UserStatus status, Pageable pageable);
}
