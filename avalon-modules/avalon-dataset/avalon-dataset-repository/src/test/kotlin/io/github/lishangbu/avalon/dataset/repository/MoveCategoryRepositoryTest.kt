package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveCategory
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveCategoryRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var moveCategoryRepository: MoveCategoryRepository

    @Test
    fun shouldInsertMoveCategorySuccessfully() {
        val moveCategory =
            MoveCategory {
                internalName = "unit-test-move-category"
                name = "单元测试类别"
                description = "单元测试描述"
            }

        val saved = moveCategoryRepository.save(moveCategory, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindMoveCategoryById() {
        val moveCategory = requireNotNull(moveCategoryRepository.loadViewById(0L))

        assertEquals("0", moveCategory.id)
        assertEquals("damage", moveCategory.internalName)
        assertEquals("damage", moveCategory.name)
    }

    @Test
    fun shouldUpdateMoveCategoryById() {
        val moveCategory =
            moveCategoryRepository.save(
                MoveCategory {
                    internalName = "move-category-update"
                    name = "原始类别"
                    description = "原始描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = moveCategory.id

        moveCategoryRepository.save(
            MoveCategory(moveCategory) {
                name = "更新后的类别"
            },
            SaveMode.UPSERT,
        )

        val updatedMoveCategory = requireNotNull(moveCategoryRepository.findNullable(id))
        assertEquals("更新后的类别", updatedMoveCategory.name)
    }

    @Test
    fun shouldDeleteMoveCategoryById() {
        val moveCategory =
            moveCategoryRepository.save(
                MoveCategory {
                    internalName = "move-category-delete"
                    name = "待删除类别"
                    description = "待删除描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = moveCategory.id

        assertNotNull(moveCategoryRepository.findNullable(deleteRecordId))
        moveCategoryRepository.deleteById(deleteRecordId)
        assertNull(moveCategoryRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = MoveCategorySpecification(internalName = "damage")

        val results = moveCategoryRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "damage" })
    }

    @Test
    fun shouldReturnAllMoveCategoriesWhenNoCondition() {
        val results = moveCategoryRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "damage" })
    }
}
