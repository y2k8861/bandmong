package com.bandmong.app;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginCodeRepository extends JpaRepository<LoginCode, Long> {
    Optional<LoginCode> findByCodeHash(String codeHash);
}
