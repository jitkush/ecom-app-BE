package com.ecom.foundation.terms.Entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "terms", schema = "legal")
@Getter
public class Terms {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "content", nullable = false)
  private String content;

  @Column(name = "version", nullable = false, length = 20)
  private String version;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TermStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  protected Terms() {
    // Required by JPA
  }

  public Terms(String content, String version) {
    this.content = Objects.requireNonNull(content, "Content cannot be null");
    this.version = Objects.requireNonNull(version, "Version cannot be null");
    this.status = TermStatus.DRAFT;
    this.createdAt = Instant.now();
    this.publishedAt = null;
  }

  public void publish() {
    if (status != TermStatus.DRAFT) {
      throw new IllegalStateException("Only a draft term can be published");
    }

    this.status = TermStatus.PUBLISHED;
    this.publishedAt = Instant.now();
  }

  public void retire() {
    if (status != TermStatus.PUBLISHED) {
      throw new IllegalStateException("Only a published term can be retired");
    }

    this.status = TermStatus.RETIRED;
  }
}
