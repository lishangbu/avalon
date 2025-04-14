package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/15
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJpaTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class MoveCategoryRepositoryTest {
  @Autowired private MoveCategoryRepository moveCategoryRepository;

  @Test
  void findByCategory() {
    Optional<MoveCategory> moveCategoryOptional = moveCategoryRepository.findByCategory("Physical");
    assertTrue(moveCategoryOptional.isPresent());
    assertEquals("物理", moveCategoryOptional.get().getName());
  }

  @Test
  void findByName() {
    Optional<MoveCategory> moveCategoryOptional = moveCategoryRepository.findByName("变化");
    assertTrue(moveCategoryOptional.isPresent());
    assertEquals("Status", moveCategoryOptional.get().getCategory());
  }
}
