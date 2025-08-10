package io.github.lishangbu.avalon.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.auth.configuration.AuthRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.auth.model.UserDTO;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/11
 */
@DataJdbcTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ContextConfiguration(classes = AuthRepositoryTestEnvironmentConfiguration.class)
class UserRepositoryTest {
  @Autowired private UserRepository userRepository;

  @Test
  void testFindByUsername() {
    Optional<UserDTO> foundUser = userRepository.findByUsername("test");
    assertTrue(foundUser.isPresent());
    assertEquals(1L, foundUser.get().id());
  }
}
