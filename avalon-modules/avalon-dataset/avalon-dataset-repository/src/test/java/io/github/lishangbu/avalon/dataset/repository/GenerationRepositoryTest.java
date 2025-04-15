package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.dataset.entity.Generation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/14
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJpaTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class GenerationRepositoryTest {

  @Autowired private GenerationRepository generationRepository;

  @Test
  public void testSave() {
    Generation generation = new Generation();
    generation.setId(3999);
    generation.setCode("MMMDCLXVI");
    generation.setName("第三千九百九十九世代");
    generationRepository.save(generation);
    assertEquals(10L, generationRepository.count());
  }

  @Test
  public void testFindByName() {
    Optional<Generation> secondaryGenerationOptional = generationRepository.findByName("第二世代");
    assertTrue(secondaryGenerationOptional.isPresent());
    Generation generation = secondaryGenerationOptional.get();
    assertEquals(2, generation.getId());
    assertEquals("II", generation.getCode());
    assertEquals("第二世代", generation.getName());
  }
}
