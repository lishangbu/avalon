package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.dataset.entity.Type;
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
class TypeRepositoryTest {

  @Autowired private TypeRepository typeRepository;

  @Test
  public void testCount() {
    assertEquals(21, typeRepository.count());
  }

  @Test
  public void testFindByType() {
    Optional<Type> normalTypeOptional = typeRepository.findByType("Normal");
    assertTrue(normalTypeOptional.isPresent());
    Type type = normalTypeOptional.get();
    assertEquals("一般", type.getName());
  }

  @Test
  public void testFindById() {
    Optional<Type> normalTypeOptional = typeRepository.findById("Fire");
    assertTrue(normalTypeOptional.isPresent());
    Type type = normalTypeOptional.get();
    assertEquals("火", type.getName());
  }
}
