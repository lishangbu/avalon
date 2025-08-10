package io.github.lishangbu.avalon.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.auth.configuration.AuthRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.auth.entity.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lishangbu
 * @since 2025/4/11
 */
@DataJdbcTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ContextConfiguration(classes = AuthRepositoryTestEnvironmentConfiguration.class)
class RoleRepositoryTest {
  @Autowired private RoleRepository roleRepository;

  @Autowired private JdbcAggregateTemplate jdbcAggregateTemplate;

  @Test
  void testFindByCode() {
    Optional<Role> foundRole = roleRepository.findByCode("ROLE_TEST");
    assertTrue(foundRole.isPresent());
    assertEquals(1, foundRole.get().getId());
  }

  @Test
  @Transactional(rollbackFor = Exception.class)
  void testSave() {
    Role role = new Role();
    role.setId(2);
    role.setCode("ROLE_TEST2");
    jdbcAggregateTemplate.insert(role);
    assertEquals(2, role.getId());
    assertEquals(2, roleRepository.count());
  }
}
