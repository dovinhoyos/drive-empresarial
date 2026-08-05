package com.dovindev.driveempresarial.repository;

import com.dovindev.driveempresarial.model.DriveDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for DriveDocument entity. */
@Repository
public interface DriveDocumentRepository extends JpaRepository<DriveDocument, Long> {

  @Transactional(readOnly = true)
  List<DriveDocument> findAllByDeletedAtIsNull();

  @Transactional(readOnly = true)
  List<DriveDocument> findByProcessIdAndDeletedAtIsNull(Long processId);
}
