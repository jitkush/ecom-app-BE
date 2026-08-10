package com.ecom.foundation.terms.Entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
    name = "terms_acceptance",
    schema = "legal",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_terms_acceptance_account_terms",
            columnNames = {"account_id", "terms_id"}
        )
    }
)
@Getter
public class TermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "terms_id",
        nullable = false,
        foreignKey = @jakarta.persistence.ForeignKey(
            name = "fk_terms_acceptance_terms"
        )
    )
    private Terms terms;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    protected TermsAcceptance() {
        // Required by JPA
    }

    public TermsAcceptance(Long accountId, Terms terms) {
        this.accountId =
            Objects.requireNonNull(accountId, "Account ID cannot be null");

        this.terms =
            Objects.requireNonNull(terms, "Terms cannot be null");

        this.acceptedAt = Instant.now();
    }
}