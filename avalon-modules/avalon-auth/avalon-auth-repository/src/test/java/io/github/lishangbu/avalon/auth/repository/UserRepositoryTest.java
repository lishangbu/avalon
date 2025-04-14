package io.github.lishangbu.avalon.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.auth.configuration.AuthRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.auth.entity.User;
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
@ContextConfiguration(classes = AuthRepositoryTestEnvironmentConfiguration.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class UserRepositoryTest {
  @Autowired private UserRepository userRepository;

  @Test
  void testFindByUsername() {
    Optional<User> foundUser = userRepository.findByUsername("test");
    assertTrue(foundUser.isPresent());
    assertEquals(1L, foundUser.get().getId());
  }
}
