package com.ecom.foundation.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.auth.entity.AuthenticationSession;

public interface SessionRepository extends JpaRepository<AuthenticationSession, Long> {
    Optional<AuthenticationSession> findBySecretHash(String secretHash);

    Optional<AuthenticationSession> findbyAccountId(String accountId);

    Optional<AuthenticationSession> findByIdAndRevokedAtIsNull(Long id);

}
