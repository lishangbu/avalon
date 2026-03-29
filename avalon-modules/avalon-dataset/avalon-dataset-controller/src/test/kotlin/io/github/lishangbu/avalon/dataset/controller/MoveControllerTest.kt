package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveInput
import io.github.lishangbu.avalon.dataset.service.MoveService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class MoveControllerTest {
    @Test
    fun getMovePage_delegatesToService() {
        val service = FakeMoveService()
        val controller = MoveController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<MoveView> = Page(listOf(moveView()), 1, 1)
        service.pageResult = page
        val specification =
            MoveSpecification(
                id = "1",
                internalName = "pound",
                name = "拍击",
                typeId = "1",
                moveDamageClassId = "2",
                moveTargetId = "10",
                moveCategoryId = "0",
                moveAilmentId = "0",
                accuracy = 100,
                power = 40,
                pp = 35,
                priority = 0,
            )

        val result = controller.getMovePage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("10", service.pageCondition!!.moveTargetId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeMoveService()
        val controller = MoveController(service)
        val command = saveMoveInput()
        service.saveResult = moveView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveService()
        val controller = MoveController(service)
        val command = updateMoveInput()
        service.updateResult = moveView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeMoveService()
        val controller = MoveController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeMoveService : MoveService {
        var pageCondition: MoveSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveMoveInput? = null
        var updatedCommand: UpdateMoveInput? = null
        var removedId: Long? = null

        var pageResult: Page<MoveView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: MoveView
        lateinit var updateResult: MoveView

        override fun getPageByCondition(
            specification: MoveSpecification,
            pageable: Pageable,
        ): Page<MoveView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveMoveInput): MoveView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateMoveInput): MoveView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun moveView(): MoveView =
    MoveView(
        Move {
            id = 1L
            internalName = "pound"
            name = "拍击"
            type =
                Type {
                    id = 1L
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
                    id = 2L
                    internalName = "physical"
                    name = "物理"
                }
            moveTarget =
                MoveTarget {
                    id = 10L
                    internalName = "selected-pokemon"
                    name = "selected-pokemon"
                }
            text = "使用长长的尾巴或手等拍打对手进行攻击。"
            shortEffect = "Inflicts regular damage with no additional effect."
            effect = "Inflicts regular damage."
            moveCategory =
                MoveCategory {
                    id = 0L
                    internalName = "damage"
                    name = "damage"
                }
            moveAilment =
                MoveAilment {
                    id = 0L
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
        },
    )

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
