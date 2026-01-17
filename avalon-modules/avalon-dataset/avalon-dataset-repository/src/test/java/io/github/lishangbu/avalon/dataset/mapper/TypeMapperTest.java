package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.dataset.AbstractMapperTest;
import io.github.lishangbu.avalon.dataset.entity.Type;
import jakarta.annotation.Resource;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// 属性数据访问层单元测试
///
/// 测试 `TypeMapper` 的基本 CRUD 操作以及动态条件查询（`selectList`）
/// 继承 `AbstractMapperTest` 以复用共享的测试容器与上下文配置
///
/// 测试采用 AAA 模式：准备数据（Arrange）→ 执行被测方法（Act）→ 断言结果（Assert）
@MybatisPlusTest
class TypeMapperTest extends AbstractMapperTest {

  @Resource private TypeMapper typeMapper;

  /// 测试插入属性类型并生成主键
  ///
  /// 验证通过 `insert` 成功插入记录后自动生成主键 ID
  @Test
  void shouldInsertTypeSuccessfully() {
    // Arrange
    Type type = new Type();
    type.setInternalName("unit_test_internal");
    type.setName("单元测试类型");

    // Act
    int inserted = typeMapper.insert(type);

    // Assert
    Assertions.assertEquals(1, inserted);
    Assertions.assertNotNull(type.getId());
    Assertions.assertTrue(type.getId() > 0);
  }

  /// 测试根据 ID 查询属性类型
  ///
  /// 验证 `selectById` 能够正确返回记录
  @Test
  void shouldFindTypeById() {
    Type normalType = typeMapper.selectById(1L);
    // Assert
    Assertions.assertNotNull(normalType);
    Assertions.assertEquals(1L, normalType.getId());
    Assertions.assertEquals("normal", normalType.getInternalName());
    Assertions.assertEquals("一般", normalType.getName());
  }

  /// 测试根据 ID 更新属性类型
  ///
  /// 验证 `updateById` 能正确修改记录字段
  @Test
  void shouldUpdateTypeById() {
    // Arrange - 插入用于更新的记录
    Type type = new Type();
    type.setInternalName("update_internal");
    type.setName("原始名称");
    typeMapper.insert(type);
    Long id = type.getId();

    // 修改字段
    type.setId(id);
    type.setName("更新后的名称");

    // Act
    int updated = typeMapper.updateById(type);

    // Assert
    Assertions.assertEquals(1, updated);
    Type updatedType = typeMapper.selectById(id);
    Assertions.assertNotNull(updatedType);
    Assertions.assertEquals("更新后的名称", updatedType.getName());
  }

  /// 测试根据 ID 删除属性类型
  ///
  /// 验证 `deleteById` 成功删除记录后无法再查询到该记录
  @Test
  void shouldDeleteTypeById() {
    Long deleteRecordId = 10L;
    Type fireType = typeMapper.selectById(deleteRecordId);
    Assertions.assertNotNull(fireType);
    int deleted = typeMapper.deleteById(deleteRecordId);
    Assertions.assertEquals(1, deleted);
    Type deletedType = typeMapper.selectById(deleteRecordId);
    Assertions.assertNull(deletedType);
  }

  /// 测试动态条件查询 selectList
  ///
  /// 插入多条记录，通过部分字段模糊匹配验证动态 SQL 条件生效
  @Test
  void shouldSelectListWithDynamicCondition() {
    // Act - 使用部分 internalName 作为查询条件，期望匹配 water
    Type cond = new Type();
    cond.setInternalName("water");
    List<Type> results = typeMapper.selectList(cond);

    // Assert
    Assertions.assertNotNull(results);
    Assertions.assertTrue(results.stream().anyMatch(r -> "水".equals(r.getName())));
  }
}
