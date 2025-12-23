package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.AbstractMapperTest;
import io.github.lishangbu.avalon.authorization.entity.Role;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 角色数据访问层测试类
 *
 * <p>测试 RoleMapper 的基本 CRUD 操作 继承 AbstractMapperTest 复用 PostgreSQL 容器实例
 *
 * @author lishangbu
 * @since 2025/8/25
 */
@MybatisPlusTest
class RoleMapperTest extends AbstractMapperTest {

  @Resource private RoleMapper roleMapper;

  /**
   * 测试根据ID查询角色
   *
   * <p>验证能够通过ID正确查询到角色信息
   */
  @Test
  void shouldFindRoleById() {
    // Act
    Role role = roleMapper.selectById(2);

    // Assert
    Assertions.assertNotNull(role);
    Assertions.assertEquals("ROLE_TEST", role.getCode());
    Assertions.assertEquals("测试员", role.getName());
    Assertions.assertEquals(2, role.getId());
    Assertions.assertTrue(role.getEnabled());
  }

  /**
   * 测试插入角色记录
   *
   * <p>验证插入操作成功后自动生成主键ID
   */
  @Test
  void shouldInsertRoleSuccessfully() {
    // Arrange
    Role role = new Role();
    role.setCode("unit_test");
    role.setName("为单元测试而生");
    role.setEnabled(true);

    // Act
    roleMapper.insert(role);

    // Assert
    Assertions.assertNotNull(role.getId());
    Assertions.assertTrue(role.getId() > 0);
  }

  /**
   * 测试根据ID更新角色
   *
   * <p>验证更新操作成功后角色信息被正确修改
   */
  @Test
  void shouldUpdateRoleById() {
    Long updateId = 3L;
    Role role = roleMapper.selectById(updateId);
    // 更新记录
    role.setName("测试员1");
    role.setEnabled(false);
    role.setCode("ROLE_TEST1");

    // Act
    roleMapper.updateById(role);

    // Assert - 验证更新后的数据
    Role updatedRole = roleMapper.selectById(updateId);
    Assertions.assertNotNull(updatedRole);
    Assertions.assertEquals("ROLE_TEST1", updatedRole.getCode());
    Assertions.assertEquals("测试员1", updatedRole.getName());
    Assertions.assertEquals(updateId, updatedRole.getId());
    Assertions.assertFalse(updatedRole.getEnabled());
  }

  /**
   * 测试根据ID删除角色
   *
   * <p>验证删除操作成功后无法再查询到该角色
   */
  @Test
  void shouldDeleteRoleById() {
    Long deleteId = 4L;
    // Act
    int deleted = roleMapper.deleteById(deleteId);

    // Assert
    Assertions.assertEquals(1, deleted);

    Role deletedRole = roleMapper.selectById(deleteId);
    Assertions.assertNull(deletedRole);
  }
}
