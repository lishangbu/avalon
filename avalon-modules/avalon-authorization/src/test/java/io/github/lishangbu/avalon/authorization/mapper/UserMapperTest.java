package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.AbstractMapperTest;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserVO;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 用户数据访问层测试类
 *
 * <p>测试 UserMapper 的基本 CRUD 操作和自定义查询方法 继承 AbstractMapperTest 复用 PostgreSQL 容器实例
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@MybatisPlusTest
class UserMapperTest extends AbstractMapperTest {

  @Resource private UserMapper userMapper;

  /**
   * 测试插入用户记录
   *
   * <p>验证插入操作成功后自动生成主键ID
   */
  @Test
  void shouldInsertUserSuccessfully() {
    // Arrange
    User user = new User();
    user.setPassword("{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G");
    user.setUsername("test2");

    // Act
    userMapper.insert(user);

    // Assert
    Assertions.assertNotNull(user.getId());
    Assertions.assertTrue(user.getId() > 0);
  }

  /**
   * 测试根据用户名查询用户详情
   *
   * <p>验证查询结果包含用户基本信息和关联的角色列表
   */
  @Test
  void shouldFindUserWithRolesByUsername() {
    // Act
    Optional<UserVO> userOptional = userMapper.selectByUsername("admin");

    // Assert
    Assertions.assertTrue(userOptional.isPresent());
    UserVO user = userOptional.get();
    Assertions.assertEquals("admin", user.getUsername());
    Assertions.assertEquals(1L, user.getId());
    Assertions.assertTrue(user.getPassword().startsWith("{bcrypt}"));
    Assertions.assertEquals(2L, user.getRoles().size());
  }
}
