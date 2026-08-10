package com.ecom.foundation.terms.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.terms.Entity.TermStatus;
import com.ecom.foundation.terms.Entity.Terms;

public interface TermsRepository extends JpaRepository<Terms, UUID> {

  Optional<Terms> findByStatus(TermStatus status);

  Optional<Terms> findByVersion(String version);
}
