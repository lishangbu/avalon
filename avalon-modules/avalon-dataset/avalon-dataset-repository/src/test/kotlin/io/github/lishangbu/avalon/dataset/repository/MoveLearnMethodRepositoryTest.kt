package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveLearnMethodRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var moveLearnMethodRepository: MoveLearnMethodRepository

    @Test
    fun shouldInsertMoveLearnMethodSuccessfully() {
        val moveLearnMethod =
            MoveLearnMethod {
                internalName = "unit-test-move-learn-method"
                name = "单元测试学习方式"
                description = "单元测试描述"
            }

        val saved = moveLearnMethodRepository.save(moveLearnMethod, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindMoveLearnMethodById() {
        val moveLearnMethod = requireNotNull(moveLearnMethodRepository.loadViewById(1L))

        assertEquals("1", moveLearnMethod.id)
        assertEquals("level-up", moveLearnMethod.internalName)
        assertEquals("Level up", moveLearnMethod.name)
    }

    @Test
    fun shouldUpdateMoveLearnMethodById() {
        val moveLearnMethod =
            moveLearnMethodRepository.save(
                MoveLearnMethod {
                    internalName = "move-learn-method-update"
                    name = "原始学习方式"
                    description = "原始描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = moveLearnMethod.id

        moveLearnMethodRepository.save(
            MoveLearnMethod(moveLearnMethod) {
                name = "更新后的学习方式"
            },
            SaveMode.UPSERT,
        )

        val updatedMoveLearnMethod = requireNotNull(moveLearnMethodRepository.findNullable(id))
        assertEquals("更新后的学习方式", updatedMoveLearnMethod.name)
    }

    @Test
    fun shouldDeleteMoveLearnMethodById() {
        val moveLearnMethod =
            moveLearnMethodRepository.save(
                MoveLearnMethod {
                    internalName = "move-learn-method-delete"
                    name = "待删除学习方式"
                    description = "待删除描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = moveLearnMethod.id

        assertNotNull(moveLearnMethodRepository.findNullable(deleteRecordId))
        moveLearnMethodRepository.deleteById(deleteRecordId)
        assertNull(moveLearnMethodRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = MoveLearnMethodSpecification(internalName = "level-up")

        val results = moveLearnMethodRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Level up" })
    }

    @Test
    fun shouldReturnAllMoveLearnMethodsWhenNoCondition() {
        val results = moveLearnMethodRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "level-up" })
    }
}
