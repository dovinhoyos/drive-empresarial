package com.dovindev.driveempresarial.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DriveVersion entity representing the drive_versions table. */
@Entity
@Table(name = "drive_versions")
@Getter
@Setter
@NoArgsConstructor
public class DriveVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "drive_document_id", nullable = false)
  @JsonIgnore
  private DriveDocument document;

  @Column(name = "version_number")
  private String versionNumber;

  @Column(name = "s3_key")
  private String s3Key;

  @Column(name = "change_summary")
  private String changeSummary;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
