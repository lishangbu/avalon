package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/17
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJdbcTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class AbilityRepositoryTest {
  @Autowired private AbilityRepository abilityRepository;

  @Test
  public void testCount() {
    assertEquals(0, abilityRepository.count());
  }
}
