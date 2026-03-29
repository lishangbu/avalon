package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/** 属性仓储测试 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TypeRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var typeRepository: TypeRepository

    /**
     * 测试插入属性类型并生成主键
     *
     * 验证通过 `insert` 成功插入记录后自动生成主键 ID
     */
    @Test
    fun shouldInsertTypeSuccessfully() {
        // Arrange
        val type =
            Type {
                internalName = "unit_test_internal"
                name = "单元测试类型"
            }

        // Act
        val saved = typeRepository.save(type, SaveMode.INSERT_ONLY)

        // Assert
        Assertions.assertNotNull(saved.id)
        Assertions.assertTrue(saved.id > 0)
    }

    /**
     * 测试根据 ID 查询属性类型
     *
     * 验证 `selectById` 能够正确返回记录
     */
    @Test
    fun shouldFindTypeById() {
        val normalType = requireNotNull(typeRepository.loadViewById(1L))
        Assertions.assertEquals("1", normalType.id)
        Assertions.assertEquals("normal", normalType.internalName)
        Assertions.assertEquals("一般", normalType.name)
    }

    /**
     * 测试根据 ID 更新属性类型
     *
     * 验证 `updateById` 能正确修改记录字段
     */
    @Test
    fun shouldUpdateTypeById() {
        // Arrange - 插入用于更新的记录
        val type =
            typeRepository.save(
                Type {
                    internalName = "update_internal"
                    name = "原始名称"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = type.id

        // Act
        typeRepository.save(Type(type) { name = "更新后的名称" }, SaveMode.UPSERT)

        // Assert
        val updatedType = requireNotNull(typeRepository.findNullable(id))
        Assertions.assertEquals("更新后的名称", updatedType.name)
    }

    /**
     * 测试根据 ID 删除属性类型
     *
     * 验证 `deleteById` 成功删除记录后无法再查询到该记录
     */
    @Test
    fun shouldDeleteTypeById() {
        // Arrange - 插入用于删除的记录
        val type =
            typeRepository.save(
                Type {
                    internalName = "update_internal"
                    name = "原始名称"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = type.id
        Assertions.assertNotNull(typeRepository.findNullable(deleteRecordId))
        typeRepository.deleteById(deleteRecordId)
        Assertions.assertNull(typeRepository.findNullable(deleteRecordId))
    }

    /**
     * 测试动态条件查询 selectList
     *
     * 插入多条记录，通过部分字段模糊匹配验证动态 SQL 条件生效
     */
    @Test
    fun shouldSelectListWithDynamicCondition() {
        // Act - 使用部分 internalName 作为查询条件，期望匹配 normal
        val specification = TypeSpecification(internalName = "normal")
        val results = typeRepository.listViews(specification)

        // Assert
        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.any { it.name == "一般" })
    }

    @Test
    fun shouldReturnAllTypesWhenNoCondition() {
        val results = typeRepository.listViews(null)

        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.size >= 10)
        Assertions.assertTrue(results.any { it.internalName == "normal" })
    }
}
