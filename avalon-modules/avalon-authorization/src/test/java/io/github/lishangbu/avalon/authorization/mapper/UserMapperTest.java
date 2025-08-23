package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.MapperTestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@ContextConfiguration(classes = {MapperTestEnvironmentAutoConfiguration.class})
@MybatisTest
class UserMapperTest {

  @Resource private UserMapper userMapper;

  @Test
  void testInsert() {
    User user = new User();
    user.setPassword("{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G");
    user.setUsername("test2");
    userMapper.insert(user);
    System.out.println(user);
  }

  @Test
  void testSelectByUsername() {
    Optional<UserInfo> userInfoOptional = userMapper.selectByUsername("test");
    Assertions.assertTrue(userInfoOptional.isPresent());
    UserInfo userInfo = userInfoOptional.get();
    Assertions.assertEquals("test", userInfo.getUsername());
    Assertions.assertTrue(userInfo.getPassword().startsWith("{bcrypt}"));
    Assertions.assertFalse(userInfo.getAuthorities().isEmpty());
    Assertions.assertEquals("ROLE_TEST", userInfo.getRoleCodes());
    Assertions.assertEquals(
        "ROLE_TEST", userInfo.getAuthorities().stream().findFirst().get().getAuthority());
  }
}
