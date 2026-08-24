package com.ecom.foundation.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.auth.entity.Account;


public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByPublicId(UUID publicId);
    Optional<Account> findByEmailIgnoreCase(String email);
    Optional<Account> findByMobile(String mobile);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
}