package io.github.lishangbu.avalon.auth.repository;

import static org.junit.jupiter.api.Assertions.*;

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
// @SpringBootTest
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
    role.setCode("ROLE_TEST2");
    jdbcAggregateTemplate.insert(role);
    assertNotNull(role.getId());
    assertInstanceOf(Long.class, role.getId());
    assertEquals(2L, roleRepository.count());
  }
}
