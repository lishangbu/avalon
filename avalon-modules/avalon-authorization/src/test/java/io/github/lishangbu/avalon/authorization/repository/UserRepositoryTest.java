package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.TestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.User;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@ContextConfiguration(classes = {TestEnvironmentAutoConfiguration.class})
@DataJpaTest
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
    Optional<User> userInfoOptional = userRepository.findUserWithRolesByUsername("test");
    Assertions.assertTrue(userInfoOptional.isPresent());
    User user = userInfoOptional.get();
    Assertions.assertEquals("test", user.getUsername());
    Assertions.assertTrue(user.getPassword().startsWith("{bcrypt}"));
  }
}
