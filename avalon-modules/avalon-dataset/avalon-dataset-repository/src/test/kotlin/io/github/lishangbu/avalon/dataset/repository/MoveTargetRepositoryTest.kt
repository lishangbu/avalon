package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveTarget
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveTargetRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var moveTargetRepository: MoveTargetRepository

    @Test
    fun shouldInsertMoveTargetSuccessfully() {
        val moveTarget =
            MoveTarget {
                internalName = "unit-test-move-target"
                name = "单元测试目标"
                description = "单元测试描述"
            }

        val saved = moveTargetRepository.save(moveTarget, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindMoveTargetById() {
        val moveTarget = requireNotNull(moveTargetRepository.loadViewById(1L))

        assertEquals("1", moveTarget.id)
        assertEquals("specific-move", moveTarget.internalName)
        assertEquals("specific-move", moveTarget.name)
    }

    @Test
    fun shouldUpdateMoveTargetById() {
        val moveTarget =
            moveTargetRepository.save(
                MoveTarget {
                    internalName = "move-target-update"
                    name = "原始目标"
                    description = "原始描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = moveTarget.id

        moveTargetRepository.save(
            MoveTarget(moveTarget) {
                name = "更新后的目标"
            },
            SaveMode.UPSERT,
        )

        val updatedMoveTarget = requireNotNull(moveTargetRepository.findNullable(id))
        assertEquals("更新后的目标", updatedMoveTarget.name)
    }

    @Test
    fun shouldDeleteMoveTargetById() {
        val moveTarget =
            moveTargetRepository.save(
                MoveTarget {
                    internalName = "move-target-delete"
                    name = "待删除目标"
                    description = "待删除描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = moveTarget.id

        assertNotNull(moveTargetRepository.findNullable(deleteRecordId))
        moveTargetRepository.deleteById(deleteRecordId)
        assertNull(moveTargetRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = MoveTargetSpecification(internalName = "specific-move")

        val results = moveTargetRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "specific-move" })
    }

    @Test
    fun shouldReturnAllMoveTargetsWhenNoCondition() {
        val results = moveTargetRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "specific-move" })
    }
}
