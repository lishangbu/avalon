package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionChainInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionChainInput
import io.github.lishangbu.avalon.dataset.service.EvolutionChainService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class EvolutionChainControllerTest {
    @Test
    fun getEvolutionChainPage_delegatesToService() {
        val service = FakeEvolutionChainService()
        val controller = EvolutionChainController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<EvolutionChainView> = Page(listOf(evolutionChainView(100L)), 1, 1)
        service.pageResult = page
        val specification = EvolutionChainSpecification(id = "100", babyTriggerItemId = "232")

        val result = controller.getEvolutionChainPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("232", service.pageCondition!!.babyTriggerItemId)
    }

    @Test
    fun listEvolutionChains_delegatesToService() {
        val service = FakeEvolutionChainService()
        val controller = EvolutionChainController(service)
        val list = listOf(evolutionChainView(100L))
        service.listResult = list
        val specification = EvolutionChainSpecification(id = "100")

        val result = controller.listEvolutionChains(specification)

        assertSame(list, result)
        assertSame(specification, service.listCondition)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeEvolutionChainService()
        val controller = EvolutionChainController(service)
        val command = SaveEvolutionChainInput("232")
        service.saveResult = evolutionChainView(100L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeEvolutionChainService()
        val controller = EvolutionChainController(service)
        val command = UpdateEvolutionChainInput("100", "232")
        service.updateResult = evolutionChainView(100L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeEvolutionChainService()
        val controller = EvolutionChainController(service)

        controller.deleteById(100L)

        assertEquals(100L, service.removedId)
    }

    private class FakeEvolutionChainService : EvolutionChainService {
        var pageCondition: EvolutionChainSpecification? = null
        var listCondition: EvolutionChainSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveEvolutionChainInput? = null
        var updatedCommand: UpdateEvolutionChainInput? = null
        var removedId: Long? = null

        var pageResult: Page<EvolutionChainView> = Page(emptyList(), 0, 0)
        var listResult: List<EvolutionChainView> = emptyList()
        lateinit var saveResult: EvolutionChainView
        lateinit var updateResult: EvolutionChainView

        override fun getPageByCondition(
            specification: EvolutionChainSpecification,
            pageable: Pageable,
        ): Page<EvolutionChainView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveEvolutionChainInput): EvolutionChainView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateEvolutionChainInput): EvolutionChainView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: EvolutionChainSpecification): List<EvolutionChainView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun evolutionChainView(id: Long): EvolutionChainView =
    EvolutionChainView(
        EvolutionChain {
            this.id = id
            babyTriggerItem =
                Item {
                    this.id = 232L
                    internalName = "lax-incense"
                    name = "Lax Incense"
                }
        },
    )
