package com.dovindev.driveempresarial.model;

import com.dovindev.driveempresarial.model.enums.DocumentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * AttributeConverter for DocumentStatus enum mapping between Java uppercase and database lowercase
 * storage.
 */
@Converter(autoApply = true)
public class DocumentStatusConverter implements AttributeConverter<DocumentStatus, String> {

  @Override
  public String convertToDatabaseColumn(DocumentStatus attribute) {
    return attribute == null ? null : attribute.name().toLowerCase();
  }

  @Override
  public DocumentStatus convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    try {
      return DocumentStatus.valueOf(dbData.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Unknown DocumentStatus value in database: '"
              + dbData
              + "'. "
              + "Expected one of: draft, under_review, published, archived");
    }
  }
}
