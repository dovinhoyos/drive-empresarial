package com.dovindev.driveempresarial.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

/** Serialization test verifying @JsonIgnore on bidirectional collections. */
class DriveProcessSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializeDriveProcess_excludesDocuments() {
    DriveProcess process = new DriveProcess();
    process.setId(1L);
    process.setName("Calidad");
    process.setPrefix("CAL");

    ObjectNode json = objectMapper.valueToTree(process);

    assertThat(json.has("documents")).isFalse();
    assertThat(json.get("name").asText()).isEqualTo("Calidad");
    assertThat(json.get("prefix").asText()).isEqualTo("CAL");
  }

  @Test
  void serializeDriveDocument_excludesVersionsAndLogs() {
    DriveDocument document = new DriveDocument();
    document.setId(1L);
    document.setTitle("Test Document");
    document.setStatus(com.dovindev.driveempresarial.model.enums.DocumentStatus.DRAFT);

    ObjectNode json = objectMapper.valueToTree(document);

    assertThat(json.has("versions")).isFalse();
    assertThat(json.has("logs")).isFalse();
    assertThat(json.get("title").asText()).isEqualTo("Test Document");
  }

  @Test
  void serializeDriveDocumentLog_excludesDocument() {
    DriveDocumentLog log = new DriveDocumentLog();
    log.setId(1L);
    log.setAction(com.dovindev.driveempresarial.model.enums.DocumentAction.UPLOAD);

    ObjectNode json = objectMapper.valueToTree(log);

    assertThat(json.has("document")).isFalse();
  }

  @Test
  void serializeDriveVersion_excludesDocument() {
    DriveVersion version = new DriveVersion();
    version.setId(1L);
    version.setVersionNumber("1.0");

    ObjectNode json = objectMapper.valueToTree(version);

    assertThat(json.has("document")).isFalse();
  }
}
