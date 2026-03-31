package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionChainInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionChainInput
import io.github.lishangbu.avalon.dataset.repository.EvolutionChainRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class EvolutionChainServiceImplTest {
    private val repository = mock(EvolutionChainRepository::class.java)
    private val service = EvolutionChainServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = EvolutionChainSpecification(id = "100", babyTriggerItemId = "232")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(evolutionChainView(100L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals(
            "232",
            result.rows
                .first()
                .babyTriggerItem
                ?.id,
        )
    }

    @Test
    fun listByCondition_callsRepository() {
        val specification = EvolutionChainSpecification(babyTriggerItemId = "232")
        `when`(repository.listViews(specification)).thenReturn(listOf(evolutionChainView(100L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("lax-incense", result.first().babyTriggerItem?.internalName)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<EvolutionChain>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(evolutionChainEntity(100L))
        `when`(repository.loadViewById(100L)).thenReturn(evolutionChainView(100L))

        val result = service.save(SaveEvolutionChainInput("232"))

        assertEquals("100", result.id)
        verify(repository).save(any<EvolutionChain>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<EvolutionChain>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(evolutionChainEntity(100L))
        `when`(repository.loadViewById(100L)).thenReturn(evolutionChainView(100L))

        val result = service.update(UpdateEvolutionChainInput("100", "232"))

        assertEquals("232", result.babyTriggerItem?.id)
        verify(repository).save(any<EvolutionChain>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(100L)

        verify(repository).deleteById(100L)
    }
}

private fun evolutionChainEntity(id: Long): EvolutionChain =
    EvolutionChain {
        this.id = id
        babyTriggerItem =
            Item {
                this.id = 232L
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
