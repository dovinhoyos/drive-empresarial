package com.dovindev.driveempresarial.model.enums;

/**
 * Document status values mapping to lowercase storage in database via AttributeConverter. Values:
 * draft, under_review, published, archived
 */
public enum DocumentStatus {
  DRAFT,
  UNDER_REVIEW,
  PUBLISHED,
  ARCHIVED
}
