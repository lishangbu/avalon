package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Type;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.domain.Example;

/// 属性数据访问层单元测试
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TypeRepositoryTest extends AbstractRepositoryTest {

    @Resource private TypeRepository typeRepository;

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
        typeRepository.saveAndFlush(type);

        // Assert
        Assertions.assertNotNull(type.getId());
        Assertions.assertTrue(type.getId() > 0);
    }

    /// 测试根据 ID 查询属性类型
    ///
    /// 验证 `selectById` 能够正确返回记录
    @Test
    void shouldFindTypeById() {
        Optional<Type> normalTypeOptional = typeRepository.findById(1L);
        // Assert
        Assertions.assertTrue(normalTypeOptional.isPresent());
        Type normalType = normalTypeOptional.get();
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
        typeRepository.saveAndFlush(type);
        Long id = type.getId();

        // 修改字段
        type.setId(id);
        type.setName("更新后的名称");

        // Act
        typeRepository.saveAndFlush(type);

        // Assert
        Optional<Type> updatedTypeOptional = typeRepository.findById(id);
        Assertions.assertTrue(updatedTypeOptional.isPresent());
        Type updatedType = updatedTypeOptional.get();
        Assertions.assertNotNull(updatedType);
        Assertions.assertEquals("更新后的名称", updatedType.getName());
    }

    /// 测试根据 ID 删除属性类型
    ///
    /// 验证 `deleteById` 成功删除记录后无法再查询到该记录
    @Test
    void shouldDeleteTypeById() {
        // Arrange - 插入用于删除的记录
        Type type = new Type();
        type.setInternalName("update_internal");
        type.setName("原始名称");
        typeRepository.saveAndFlush(type);
        Long deleteRecordId = type.getId();
        Optional<Type> fireTypeOptional = typeRepository.findById(deleteRecordId);
        Assertions.assertTrue(fireTypeOptional.isPresent());
        typeRepository.deleteById(deleteRecordId);
        typeRepository.flush();
        Optional<Type> deletedTypeOptional = typeRepository.findById(deleteRecordId);
        Assertions.assertTrue(deletedTypeOptional.isEmpty());
    }

    /// 测试动态条件查询 selectList
    ///
    /// 插入多条记录，通过部分字段模糊匹配验证动态 SQL 条件生效
    @Test
    void shouldSelectListWithDynamicCondition() {
        // Act - 使用部分 internalName 作为查询条件，期望匹配 normal
        Type cond = new Type();
        cond.setInternalName("normal");
        List<Type> results = typeRepository.findAll(Example.of(cond));

        // Assert
        Assertions.assertNotNull(results);
        Assertions.assertTrue(results.stream().anyMatch(r -> "一般".equals(r.getName())));
    }
}
