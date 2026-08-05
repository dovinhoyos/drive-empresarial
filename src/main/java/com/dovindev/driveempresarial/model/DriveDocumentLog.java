package com.dovindev.driveempresarial.model;

import com.dovindev.driveempresarial.model.enums.DocumentAction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DriveDocumentLog entity representing the drive_document_logs table. */
@Entity
@Table(name = "drive_document_logs")
@Getter
@Setter
@NoArgsConstructor
public class DriveDocumentLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "drive_document_id", nullable = false)
  @JsonIgnore
  private DriveDocument document;

  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action")
  private DocumentAction action;

  @Column(name = "version_number")
  private String versionNumber;

  @Column(name = "notes")
  private String notes;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
