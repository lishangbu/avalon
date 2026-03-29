package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveAilment
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveAilmentRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var moveAilmentRepository: MoveAilmentRepository

    @Test
    fun shouldInsertMoveAilmentSuccessfully() {
        val moveAilment =
            MoveAilment {
                internalName = "unit-test-move-ailment"
                name = "单元测试异常"
            }

        val saved = moveAilmentRepository.save(moveAilment, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindMoveAilmentById() {
        val moveAilment = requireNotNull(moveAilmentRepository.loadViewById(1L))

        assertEquals("1", moveAilment.id)
        assertEquals("paralysis", moveAilment.internalName)
        assertEquals("paralysis", moveAilment.name)
    }

    @Test
    fun shouldUpdateMoveAilmentById() {
        val moveAilment =
            moveAilmentRepository.save(
                MoveAilment {
                    internalName = "move-ailment-update"
                    name = "原始异常"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = moveAilment.id

        moveAilmentRepository.save(
            MoveAilment(moveAilment) {
                name = "更新后的异常"
            },
            SaveMode.UPSERT,
        )

        val updatedMoveAilment = requireNotNull(moveAilmentRepository.findNullable(id))
        assertEquals("更新后的异常", updatedMoveAilment.name)
    }

    @Test
    fun shouldDeleteMoveAilmentById() {
        val moveAilment =
            moveAilmentRepository.save(
                MoveAilment {
                    internalName = "move-ailment-delete"
                    name = "待删除异常"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = moveAilment.id

        assertNotNull(moveAilmentRepository.findNullable(deleteRecordId))
        moveAilmentRepository.deleteById(deleteRecordId)
        assertNull(moveAilmentRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = MoveAilmentSpecification(internalName = "paralysis")

        val results = moveAilmentRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "paralysis" })
    }

    @Test
    fun shouldReturnAllMoveAilmentsWhenNoCondition() {
        val results = moveAilmentRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "paralysis" })
    }
}
