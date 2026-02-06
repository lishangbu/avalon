package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.entity.User;
import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// 用户 Repository 测试
///
/// 覆盖用户的插入与按用户名查询逻辑，依赖 Liquibase 初始化的测试数据与关系
///
/// @author lishangbu
/// @since 2025/8/20
class UserRepositoryTest extends AbstractRepositoryTest {

  @Resource private UserRepository userRepository;
  @Resource private RoleRepository roleRepository;

  @Test
  void testInsert() {
    User user = new User();
    user.setPassword("{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G");
    user.setUsername("test2");
    roleRepository
        .findById(1L)
        .ifPresent(
            role -> {
              Set<Role> roles = new HashSet<>();
              roles.add(role);
              user.setRoles(roles);
            });
    userRepository.saveAndFlush(user);
    Optional<User> userOptional = userRepository.findUserWithRolesByUsername("test2");
    Assertions.assertTrue(userOptional.isPresent());
    User savedUser = userOptional.get();
    Assertions.assertEquals("test2", savedUser.getUsername());
    Assertions.assertTrue(savedUser.getPassword().startsWith("{bcrypt}"));
    Assertions.assertEquals(1, savedUser.getRoles().size());
  }

  @Test
  void testFindByUsername() {
    Optional<User> userOptional = userRepository.findUserWithRolesByUsername("admin");
    Assertions.assertTrue(userOptional.isPresent());
    User user = userOptional.get();
    Assertions.assertEquals("admin", user.getUsername());
    Assertions.assertTrue(user.getPassword().startsWith("{bcrypt}"));
    Assertions.assertEquals(2, user.getRoles().size());
  }
}
