package com.dovindev.driveempresarial.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DriveProcess entity representing the drive_processes table. Maps to V001 schema exactly: id
 * (BIGSERIAL), name, prefix, group_type, created_at, updated_at.
 */
@Entity
@Table(name = "drive_processes")
@Getter
@Setter
@NoArgsConstructor
public class DriveProcess {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String prefix;

  @Column(name = "group_type")
  private String groupType;

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
