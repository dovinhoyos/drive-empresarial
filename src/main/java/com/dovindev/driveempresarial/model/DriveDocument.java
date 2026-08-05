package com.dovindev.driveempresarial.model;

import com.dovindev.driveempresarial.model.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DriveDocument entity representing the drive_documents table. */
@Entity
@Table(name = "drive_documents")
@Getter
@Setter
@NoArgsConstructor
public class DriveDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = true, unique = true)
  private String documentNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "drive_process_id", nullable = false)
  private DriveProcess process;

  private String mainCategory;
  private String groupType;
  private String documentType;

  @Convert(converter = DocumentStatusConverter.class)
  private DocumentStatus status = DocumentStatus.DRAFT;

  private String rejectionNotes;

  @Column(name = "current_version")
  private String currentVersion = "1.0";

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "document")
  @JsonIgnore
  private List<DriveVersion> versions;

  @OneToMany(mappedBy = "document")
  @JsonIgnore
  private List<DriveDocumentLog> logs;

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
