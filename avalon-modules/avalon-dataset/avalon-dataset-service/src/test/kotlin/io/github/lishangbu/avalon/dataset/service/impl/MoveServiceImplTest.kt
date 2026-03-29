package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveInput
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class MoveServiceImplTest {
    private val repository = mock(MoveRepository::class.java)
    private val service = MoveServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = MoveSpecification(id = "1", internalName = "pound", typeId = "1", moveDamageClassId = "2")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(moveView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals("拍击", result.rows.first().name)
        assertEquals(
            "一般",
            result.rows
                .first()
                .type
                ?.name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Move>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(moveView(1L))

        val result = service.save(saveMoveInput())

        assertEquals("1", result.id)
        assertEquals("normal", result.type?.internalName)
        assertEquals("physical", result.moveDamageClass?.internalName)
        verify(repository).save(any<Move>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Move>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(moveView(1L))

        val result = service.update(updateMoveInput())

        assertEquals("1", result.id)
        assertEquals("selected-pokemon", result.moveTarget?.internalName)
        assertEquals("damage", result.moveCategory?.internalName)
        assertEquals("none", result.moveAilment?.internalName)
        verify(repository).save(any<Move>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun saveMoveInput(): SaveMoveInput =
    SaveMoveInput(
        internalName = "pound",
        name = "拍击",
        typeId = "1",
        accuracy = 100,
        effectChance = 0,
        pp = 35,
        priority = 0,
        power = 40,
        moveDamageClassId = "2",
        moveTargetId = "10",
        text = "使用长长的尾巴或手等拍打对手进行攻击。",
        shortEffect = "Inflicts regular damage with no additional effect.",
        effect = "Inflicts regular damage.",
        moveCategoryId = "0",
        moveAilmentId = "0",
        minHits = null,
        maxHits = null,
        minTurns = null,
        maxTurns = null,
        drain = 0,
        healing = 0,
        critRate = 0,
        ailmentChance = 0,
        flinchChance = 0,
        statChance = 0,
    )

private fun updateMoveInput(): UpdateMoveInput =
    UpdateMoveInput(
        id = "1",
        internalName = "pound",
        name = "拍击",
        typeId = "1",
        accuracy = 100,
        effectChance = 0,
        pp = 35,
        priority = 0,
        power = 40,
        moveDamageClassId = "2",
        moveTargetId = "10",
        text = "使用长长的尾巴或手等拍打对手进行攻击。",
        shortEffect = "Inflicts regular damage with no additional effect.",
        effect = "Inflicts regular damage.",
        moveCategoryId = "0",
        moveAilmentId = "0",
        minHits = null,
        maxHits = null,
        minTurns = null,
        maxTurns = null,
        drain = 0,
        healing = 0,
        critRate = 0,
        ailmentChance = 0,
        flinchChance = 0,
        statChance = 0,
    )

private fun moveSavedEntity(id: Long): Move =
    Move {
        this.id = id
        internalName = "pound"
        name = "拍击"
        type =
            Type {
                this.id = 1L
            }
        accuracy = 100
        effectChance = 0
        pp = 35
        priority = 0
        power = 40
        moveDamageClass =
            MoveDamageClass {
                this.id = 2L
            }
        moveTarget =
            MoveTarget {
                this.id = 10L
            }
        text = "使用长长的尾巴或手等拍打对手进行攻击。"
        shortEffect = "Inflicts regular damage with no additional effect."
        effect = "Inflicts regular damage."
        moveCategory =
            MoveCategory {
                this.id = 0L
            }
        moveAilment =
            MoveAilment {
                this.id = 0L
            }
        minHits = null
        maxHits = null
        minTurns = null
        maxTurns = null
        drain = 0
        healing = 0
        critRate = 0
        ailmentChance = 0
        flinchChance = 0
        statChance = 0
    }

private fun moveWithAssociations(id: Long): Move =
    Move {
        this.id = id
        internalName = "pound"
        name = "拍击"
        type =
            Type {
                this.id = 1L
                internalName = "normal"
                name = "一般"
            }
        accuracy = 100
        effectChance = 0
        pp = 35
        priority = 0
        power = 40
        moveDamageClass =
            MoveDamageClass {
                this.id = 2L
                internalName = "physical"
                name = "物理"
            }
        moveTarget =
            MoveTarget {
                this.id = 10L
                internalName = "selected-pokemon"
                name = "selected-pokemon"
            }
        text = "使用长长的尾巴或手等拍打对手进行攻击。"
        shortEffect = "Inflicts regular damage with no additional effect."
        effect = "Inflicts regular damage."
        moveCategory =
            MoveCategory {
                this.id = 0L
                internalName = "damage"
                name = "damage"
            }
        moveAilment =
            MoveAilment {
                this.id = 0L
                internalName = "none"
                name = "none"
            }
        minHits = null
        maxHits = null
        minTurns = null
        maxTurns = null
        drain = 0
        healing = 0
        critRate = 0
        ailmentChance = 0
        flinchChance = 0
        statChance = 0
    }

private fun moveView(id: Long): MoveView = MoveView(moveWithAssociations(id))
