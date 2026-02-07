package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Role;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

/// 角色信息实体的 Repository 测试
///
/// 验证角色查询、插入、更新与删除流程，依赖数据库初始化的测试数据
///
/// @author lishangbu
/// @since 2025/8/25
@Transactional(rollbackFor = Exception.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleRepositoryTest extends AbstractRepositoryTest {
  @Resource private RoleRepository roleRepository;

  private static Long insertId;

  @Test
  @Order(1)
  void testSelectRoleById() {
    Optional<Role> roleOptional = roleRepository.findById(1L);
    Assertions.assertTrue(roleOptional.isPresent());
    Role role = roleOptional.get();
    Assertions.assertEquals("ROLE_SUPER_ADMIN", role.getCode());
    Assertions.assertEquals("超级管理员", role.getName());
    Assertions.assertEquals(1L, role.getId());
    Assertions.assertTrue(role.getEnabled());
  }

  @Test
  @Order(2)
  // 确保新增操作后能够提交事务
  @Commit
  void testInsertRole() {
    Role role = new Role();
    role.setCode("unit_test");
    role.setName("为单元测试而生");
    role.setEnabled(true);
    roleRepository.save(role);
    insertId = role.getId();
  }

  @Test
  @Order(3)
  // 确保更新操作后能够提交事务
  @Commit
  void testUpdateRoleById() {
    Optional<Role> roleOptional = roleRepository.findById(insertId);
    Assertions.assertTrue(roleOptional.isPresent());
    Role role = roleOptional.get();
    role.setName("测试员1");
    role.setEnabled(false);
    role.setCode("ROLE_TEST1");
    roleRepository.save(role);
  }

  @Test
  @Order(4)
  void testSelectUpdatedRoleById() {
    Optional<Role> roleOptional = roleRepository.findById(insertId);
    Assertions.assertTrue(roleOptional.isPresent());
    Role role = roleOptional.get();
    Assertions.assertEquals("ROLE_TEST1", role.getCode());
    Assertions.assertEquals("测试员1", role.getName());
    Assertions.assertEquals(insertId, role.getId());
    Assertions.assertFalse(role.getEnabled());
  }

  @Test
  @Order(5)
  void testDeleteById() {
    roleRepository.deleteById(insertId);
  }
}
