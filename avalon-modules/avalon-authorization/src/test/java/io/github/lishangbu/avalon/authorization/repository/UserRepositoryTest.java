package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.TestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.User;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@ContextConfiguration(classes = {TestEnvironmentAutoConfiguration.class})
@DataJdbcTest
class UserRepositoryTest {

  @Resource private UserRepository userRepository;

  @Test
  void testInsert() {
    User user = new User();
    user.setPassword("{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G");
    user.setUsername("test2");
    userRepository.save(user);
    System.out.println(user);
  }

  @Test
  void testFindByUsername() {
    Optional<User> userOptional = userRepository.findByUsername("test");
    Assertions.assertTrue(userOptional.isPresent());
    User user = userOptional.get();
    Assertions.assertEquals("test", user.getUsername());
    Assertions.assertTrue(user.getPassword().startsWith("{bcrypt}"));
    Assertions.assertEquals(2, user.getUserRoles().size());
  }
}
