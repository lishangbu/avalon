package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.AbilityView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveAbilityInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateAbilityInput
import io.github.lishangbu.avalon.dataset.service.AbilityService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AbilityControllerTest {
    @Test
    fun listAbilities_delegatesToService() {
        val service = FakeAbilityService()
        val controller = AbilityController(service)
        val list = listOf(abilityView())
        service.listResult = list
        val specification = AbilitySpecification(id = "1", internalName = "stench", name = "恶臭")

        val result = controller.listAbilities(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeAbilityService()
        val controller = AbilityController(service)
        val command =
            SaveAbilityInput(
                internalName = "stench",
                name = "恶臭",
                effect = "effect",
                introduction = "introduction",
            )
        service.saveResult = abilityView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeAbilityService()
        val controller = AbilityController(service)
        val command =
            UpdateAbilityInput(
                id = "1",
                internalName = "stench",
                name = "恶臭",
                effect = "effect",
                introduction = "introduction",
            )
        service.updateResult = abilityView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeAbilityService()
        val controller = AbilityController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeAbilityService : AbilityService {
        var listCondition: AbilitySpecification? = null
        var savedCommand: SaveAbilityInput? = null
        var updatedCommand: UpdateAbilityInput? = null
        var removedId: Long? = null

        var listResult: List<AbilityView> = emptyList()
        lateinit var saveResult: AbilityView
        lateinit var updateResult: AbilityView

        override fun save(command: SaveAbilityInput): AbilityView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateAbilityInput): AbilityView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: AbilitySpecification): List<AbilityView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun abilityView(): AbilityView =
    AbilityView(
        id = "1",
        internalName = "stench",
        name = "恶臭",
        effect = "effect",
        introduction = "introduction",
    )
