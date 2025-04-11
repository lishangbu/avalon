package io.github.lishangbu.avalon.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.auth.configuration.TestEnvironmentConfiguration;
import io.github.lishangbu.avalon.auth.entity.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/11
 */
@DataJpaTest
@ContextConfiguration(classes = TestEnvironmentConfiguration.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class RoleRepositoryTest {
  @Autowired private RoleRepository roleRepository;

  @Test
  void testFindByCode() {
    Optional<Role> foundRole = roleRepository.findByCode("ROLE_TEST");
    assertTrue(foundRole.isPresent());
    assertEquals(1L, foundRole.get().getId());
  }
}
