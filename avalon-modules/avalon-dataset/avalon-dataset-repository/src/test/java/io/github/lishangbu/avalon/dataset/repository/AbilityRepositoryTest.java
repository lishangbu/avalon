package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/17
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJpaTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class AbilityRepositoryTest {
  @Autowired private AbilityRepository abilityRepository;

  @Test
  public void testCount() {
    assertEquals(1, abilityRepository.count());
  }
}
