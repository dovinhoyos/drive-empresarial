package com.dovindev.driveempresarial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dovindev.driveempresarial.model.DriveDocument;
import com.dovindev.driveempresarial.model.DriveProcess;
import com.dovindev.driveempresarial.model.enums.DocumentStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

/** Integration test for DriveDocumentRepository. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class DriveDocumentRepositoryTest {

  @Autowired private DriveDocumentRepository repository;
  @Autowired private DriveProcessRepository processRepository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    processRepository.deleteAll();
  }

  @Test
  void findAllByDeletedAtIsNull_excludesDeleted() {
    // Given
    DriveProcess process = new DriveProcess();
    process.setName("Test Process");
    process.setPrefix("TEST");
    processRepository.save(process);

    DriveDocument activeDoc = new DriveDocument();
    activeDoc.setTitle("Active Document");
    activeDoc.setProcess(process);
    activeDoc.setStatus(DocumentStatus.DRAFT);

    DriveDocument deletedDoc = new DriveDocument();
    deletedDoc.setTitle("Deleted Document");
    deletedDoc.setProcess(process);
    deletedDoc.setStatus(DocumentStatus.PUBLISHED);
    deletedDoc.setDeletedAt(java.time.LocalDateTime.now());

    repository.save(activeDoc);
    repository.save(deletedDoc);

    // When
    List<DriveDocument> result = repository.findAllByDeletedAtIsNull();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Active Document");
    assertThat(result.get(0).getDeletedAt()).isNull();
  }

  @Test
  void findByProcessIdAndDeletedAtIsNull_returnsActiveDocsForProcess() {
    // Given
    DriveProcess process = new DriveProcess();
    process.setName("Test Process");
    process.setPrefix("TEST");
    processRepository.save(process);

    DriveDocument activeDoc1 = new DriveDocument();
    activeDoc1.setTitle("Active Doc 1");
    activeDoc1.setProcess(process);
    activeDoc1.setStatus(DocumentStatus.UNDER_REVIEW);

    DriveDocument activeDoc2 = new DriveDocument();
    activeDoc2.setTitle("Active Doc 2");
    activeDoc2.setProcess(process);
    activeDoc2.setStatus(DocumentStatus.PUBLISHED);

    DriveDocument deletedDoc = new DriveDocument();
    deletedDoc.setTitle("Deleted Doc");
    deletedDoc.setProcess(process);
    deletedDoc.setStatus(DocumentStatus.ARCHIVED);
    deletedDoc.setDeletedAt(java.time.LocalDateTime.now());

    repository.save(activeDoc1);
    repository.save(activeDoc2);
    repository.save(deletedDoc);

    // When
    List<DriveDocument> result = repository.findByProcessIdAndDeletedAtIsNull(process.getId());

    // Then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(DriveDocument::getTitle)
        .containsExactlyInAnyOrder("Active Doc 1", "Active Doc 2");
  }

  @Test
  void statusRoundtrip_persistsAndReloadsStatus() {
    // Given
    DriveProcess process = new DriveProcess();
    process.setName("Test Process");
    process.setPrefix("TEST");
    processRepository.save(process);

    DriveDocument doc = new DriveDocument();
    doc.setTitle("Status Test Doc");
    doc.setProcess(process);
    doc.setStatus(DocumentStatus.UNDER_REVIEW);

    repository.save(doc);

    // When - reload from repository
    DriveDocument reloaded = repository.findById(doc.getId()).orElseThrow();

    // Then
    assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.UNDER_REVIEW);
  }
}
