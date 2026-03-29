package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class MoveRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var moveRepository: MoveRepository

    @Test
    fun shouldQueryPageAndCrudMove() {
        val condition = MoveSpecification(internalName = "pound", typeId = "1", moveDamageClassId = "2")

        val results = moveRepository.listViews(condition)
        val page = moveRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("pound", results.first().internalName)
        assertEquals("一般", results.first().type?.name)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val saved = moveRepository.save(newMoveEntity(), SaveMode.INSERT_ONLY)
        val savedView = requireNotNull(moveRepository.loadViewById(saved.id))
        assertEquals("codex-test-move", savedView.internalName)
        assertEquals("变化", savedView.moveDamageClass?.name)

        moveRepository.save(Move(saved) { name = "更新后的招式" }, SaveMode.UPSERT)

        val updated = requireNotNull(moveRepository.loadViewById(saved.id))
        assertEquals("更新后的招式", updated.name)

        moveRepository.deleteById(saved.id)
        assertNull(moveRepository.loadViewById(saved.id))
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}

private fun newMoveEntity(): Move =
    Move {
        internalName = "codex-test-move"
        name = "测试招式"
        type =
            Type {
                id = 1L
            }
        accuracy = 100
        effectChance = 30
        pp = 15
        priority = 0
        power = 60
        moveDamageClass =
            MoveDamageClass {
                id = 1L
            }
        moveTarget =
            MoveTarget {
                id = 10L
            }
        text = "测试招式文本"
        shortEffect = "测试招式简称效果"
        effect = "测试招式效果"
        moveCategory =
            MoveCategory {
                id = 0L
            }
        moveAilment =
            MoveAilment {
                id = 0L
            }
        minHits = 1
        maxHits = 1
        minTurns = 1
        maxTurns = 1
        drain = 0
        healing = 0
        critRate = 0
        ailmentChance = 0
        flinchChance = 0
        statChance = 0
    }
