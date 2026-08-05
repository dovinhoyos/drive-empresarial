package com.dovindev.driveempresarial.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dovindev.driveempresarial.model.DriveProcess;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for DriveProcessRepository. Uses @DataJpaTest to load only the JPA layer in
 * isolation. Requires PostgreSQL running via docker compose up postgres.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class DriveProcessRepositoryTest {

  @Autowired private DriveProcessRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  @Test
  void save_persistsProcessWithTimestamps() {
    DriveProcess process = new DriveProcess();
    process.setName("Calidad");
    process.setPrefix("CAL");
    process.setGroupType("ESTRATEGICO");

    DriveProcess saved = repository.save(process);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("Calidad");
    assertThat(saved.getPrefix()).isEqualTo("CAL");
    assertThat(saved.getGroupType()).isEqualTo("ESTRATEGICO");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  void findAllByOrderByNameAsc_returnsSortedByName() {
    DriveProcess p2 = new DriveProcess();
    p2.setName("Gestión");
    p2.setPrefix("GES");
    repository.save(p2);

    DriveProcess p1 = new DriveProcess();
    p1.setName("Calidad");
    p1.setPrefix("CAL");
    repository.save(p1);

    List<DriveProcess> result = repository.findAllByOrderByNameAsc();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Calidad");
    assertThat(result.get(1).getName()).isEqualTo("Gestión");
  }

  @Test
  void findAllByOrderByNameAsc_whenEmpty_returnsEmptyList() {
    List<DriveProcess> result = repository.findAllByOrderByNameAsc();

    assertThat(result).isEmpty();
  }

  @Test
  void save_prePersistSetsCreatedAtAndUpdatedAt() {
    DriveProcess process = new DriveProcess();
    process.setName("Auditoría");
    process.setPrefix("AUD");

    DriveProcess saved = repository.save(process);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }
}
