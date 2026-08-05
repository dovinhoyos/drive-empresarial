package com.dovindev.driveempresarial.repository;

import com.dovindev.driveempresarial.model.DriveProcess;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for DriveProcess entity. */
@Repository
public interface DriveProcessRepository extends JpaRepository<DriveProcess, Long> {

  /** Finds all processes ordered by name ascending. Used by process-api for ordered listing. */
  @Transactional(readOnly = true)
  List<DriveProcess> findAllByOrderByNameAsc();
}
