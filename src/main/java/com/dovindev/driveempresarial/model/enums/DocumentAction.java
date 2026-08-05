package com.dovindev.driveempresarial.model.enums;

/**
 * Document actions mapping to uppercase storage in database via @Enumerated(STRING). Values:
 * UPLOAD, REVIEW_REQUEST, PUBLISH, REJECT, VERSION_UPDATE, OBSOLETE
 */
public enum DocumentAction {
  UPLOAD,
  REVIEW_REQUEST,
  PUBLISH,
  REJECT,
  VERSION_UPDATE,
  OBSOLETE
}
