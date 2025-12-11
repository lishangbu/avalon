package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.TestEnvironmentApplication;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserVO;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@Testcontainers
@MybatisPlusTest
@ContextConfiguration(classes = TestEnvironmentApplication.class)
class UserMapperTest {
  @Container @ServiceConnection
  static PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:18");

  @Resource private UserMapper userMapper;

  @Test
  void testInsert() {
    User user = new User();
    user.setPassword("{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G");
    user.setUsername("test2");
    userMapper.insert(user);
    Assertions.assertNotNull(user.getId());
    Assertions.assertTrue(user.getId() > 0);
  }

  @Test
  void testFindByUsername() {
    Optional<UserVO> userOptional = userMapper.selectByUsername("admin");
    Assertions.assertTrue(userOptional.isPresent());
    UserVO user = userOptional.get();
    Assertions.assertEquals("admin", user.getUsername());
    Assertions.assertEquals(1L, user.getId());
    Assertions.assertTrue(user.getPassword().startsWith("{bcrypt}"));
    Assertions.assertEquals(2L, user.getRoles().size());
  }
}
