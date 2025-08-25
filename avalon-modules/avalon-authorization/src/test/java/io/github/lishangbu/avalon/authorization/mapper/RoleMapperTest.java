package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.MapperTestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.Role;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色信息(Role)实体类测试
 *
 * @author lishangbu
 * @since 2025/8/25
 */
@Transactional(rollbackFor = Exception.class)
@ContextConfiguration(classes = {MapperTestEnvironmentAutoConfiguration.class})
@MybatisTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleMapperTest {
  @Resource private RoleMapper roleMapper;

  @Test
  @Order(1)
  void testSelectRoleById() {
    Optional<Role> roleOptional = roleMapper.selectById(1L);
    Assertions.assertTrue(roleOptional.isPresent());
    Role role = roleOptional.get();
    Assertions.assertEquals("ROLE_TEST", role.getCode());
    Assertions.assertEquals("测试员", role.getName());
    Assertions.assertEquals(1, role.getId());
    Assertions.assertTrue(role.getEnabled());
  }

  @Test
  @Order(2)
  void testInsertRole() {
    Role role = new Role();
    role.setCode("unit_test");
    role.setName("为单元测试而生");
    role.setEnabled(true);
    roleMapper.insert(role);
  }

  @Test
  @Order(3)
  @Commit // 确保更新操作后能够提交事务
  void testUpdateRoleById() {
    Optional<Role> roleOptional = roleMapper.selectById(1L);
    Assertions.assertTrue(roleOptional.isPresent());
    Role role = roleOptional.get();
    role.setName("测试员1");
    role.setEnabled(false);
    role.setCode("ROLE_TEST1");
    roleMapper.updateById(role);
  }

  @Test
  @Order(4)
  void testSelectUpdatedRoleById() {
    Optional<Role> roleOptional = roleMapper.selectById(1L);
    Assertions.assertTrue(roleOptional.isPresent());
    Role role = roleOptional.get();
    Assertions.assertEquals("ROLE_TEST1", role.getCode());
    Assertions.assertEquals("测试员1", role.getName());
    Assertions.assertEquals(1, role.getId());
    Assertions.assertFalse(role.getEnabled());
  }

  @Test
  @Order(5)
  void testDeleteById() {
    Assertions.assertEquals(1, roleMapper.deleteById(1L));
  }
}
