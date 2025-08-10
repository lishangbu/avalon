package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/15
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJdbcTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class MoveCategoryRepositoryTest {
  @Autowired private MoveCategoryRepository moveCategoryRepository;

  @Test
  void testFindByCategory() {
    Optional<MoveCategory> moveCategoryOptional =
        moveCategoryRepository.findByInternalName("damage");
    assertTrue(moveCategoryOptional.isPresent());
    assertEquals("damage", moveCategoryOptional.get().getName());
    assertEquals("Inflicts damage", moveCategoryOptional.get().getDescription());
  }

  @Test
  void testFindById() {
    Optional<MoveCategory> moveCategoryOptional = moveCategoryRepository.findById(0L);
    assertTrue(moveCategoryOptional.isPresent());
    assertEquals("damage", moveCategoryOptional.get().getName());
    assertEquals("Inflicts damage", moveCategoryOptional.get().getDescription());
  }
}
