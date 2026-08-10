package com.ecom.foundation.terms.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.terms.Entity.TermsAcceptance;

public interface TermsAcceptanceRepository
        extends JpaRepository<TermsAcceptance, Long> {

    boolean existsByAccountIdAndTerms_Id(
        Long accountId,
        UUID termsId
    );

    Optional<TermsAcceptance> findByAccountIdAndTerms_Id(
        Long accountId,
        UUID termsId
    );

    List<TermsAcceptance> findAllByAccountIdOrderByAcceptedAtDesc(
        Long accountId
    );
}