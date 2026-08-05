package com.dovindev.driveempresarial.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dovindev.driveempresarial.model.enums.DocumentStatus;
import org.junit.jupiter.api.Test;

/** Test for DocumentStatusConverter attribute converter - unit test approach. */
class DocumentStatusConverterTest {

  private final DocumentStatusConverter converter = new DocumentStatusConverter();

  @Test
  void toDb_DRAFT_mapsToDraft() {
    assertThat(converter.convertToDatabaseColumn(DocumentStatus.DRAFT)).isEqualTo("draft");
  }

  @Test
  void toDb_UNDER_REVIEW_mapsToUnderReview() {
    assertThat(converter.convertToDatabaseColumn(DocumentStatus.UNDER_REVIEW))
        .isEqualTo("under_review");
  }

  @Test
  void toDb_PUBLISHED_mapsToPublished() {
    assertThat(converter.convertToDatabaseColumn(DocumentStatus.PUBLISHED)).isEqualTo("published");
  }

  @Test
  void toDb_ARCHIVED_mapsToArchived() {
    assertThat(converter.convertToDatabaseColumn(DocumentStatus.ARCHIVED)).isEqualTo("archived");
  }

  @Test
  void fromDb_draft_mapsToDRAFT() {
    assertThat(converter.convertToEntityAttribute("draft")).isEqualTo(DocumentStatus.DRAFT);
  }

  @Test
  void fromDb_under_review_mapsToUNDER_REVIEW() {
    assertThat(converter.convertToEntityAttribute("under_review"))
        .isEqualTo(DocumentStatus.UNDER_REVIEW);
  }

  @Test
  void fromDb_published_mapsToPUBLISHED() {
    assertThat(converter.convertToEntityAttribute("published")).isEqualTo(DocumentStatus.PUBLISHED);
  }

  @Test
  void fromDb_archived_mapsToARCHIVED() {
    assertThat(converter.convertToEntityAttribute("archived")).isEqualTo(DocumentStatus.ARCHIVED);
  }

  @Test
  void fromDb_null_mapsToNull() {
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }

  @Test
  void fromDb_unknownValue_throwsException() {
    assertThatThrownBy(() -> converter.convertToEntityAttribute("invalid_status"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown DocumentStatus value")
        .hasMessageContaining("invalid_status");
  }
}
