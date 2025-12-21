package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.TestEnvironmentApplication;
import io.github.lishangbu.avalon.authorization.entity.Role;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 角色信息(Role)实体类测试
 *
 * @author lishangbu
 * @since 2025/8/25
 */
@Testcontainers
@ContextConfiguration(classes = TestEnvironmentApplication.class)
@MybatisPlusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleMapperTest {

  @Container @ServiceConnection
  static PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:latest");

  @Resource private RoleMapper roleMapper;

  private static Long insertId;

  @Test
  @Order(1)
  void testSelectRoleById() {
    Role role = roleMapper.selectById(2);
    Assertions.assertNotNull(role);
    Assertions.assertEquals("ROLE_TEST", role.getCode());
    Assertions.assertEquals("测试员", role.getName());
    Assertions.assertEquals(2, role.getId());
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
    roleMapper.insert(role);
    insertId = role.getId();
  }

  @Test
  @Order(3)
  // 确保更新操作后能够提交事务
  @Commit
  void testUpdateRoleById() {
    Role role = roleMapper.selectById(insertId);
    Assertions.assertNotNull(role);
    role.setName("测试员1");
    role.setEnabled(false);
    role.setCode("ROLE_TEST1");
    roleMapper.updateById(role);
  }

  @Test
  @Order(4)
  void testSelectUpdatedRoleById() {
    Role role = roleMapper.selectById(insertId);
    Assertions.assertNotNull(role);
    Assertions.assertEquals("ROLE_TEST1", role.getCode());
    Assertions.assertEquals("测试员1", role.getName());
    Assertions.assertEquals(insertId, role.getId());
    Assertions.assertFalse(role.getEnabled());
  }

  @Test
  @Order(5)
  void testDeleteById() {
    roleMapper.deleteById(insertId);
  }
}
