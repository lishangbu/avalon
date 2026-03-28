package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Ability
import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveAbilityInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateAbilityInput
import io.github.lishangbu.avalon.dataset.repository.AbilityRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AbilityServiceImplTest {
    private val repository = mock(AbilityRepository::class.java)
    private val service = AbilityServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = AbilitySpecification(id = "1", internalName = "stench")
        `when`(repository.findAll(specification)).thenReturn(listOf(abilityEntity(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("stench", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<Ability>(), SaveMode.INSERT_ONLY)).thenReturn(abilityEntity(1L))

        val result =
            service.save(
                SaveAbilityInput(
                    internalName = "stench",
                    name = "恶臭",
                    effect = "effect",
                    introduction = "introduction",
                ),
            )

        assertEquals("1", result.id)
        verify(repository).save(any<Ability>(), SaveMode.INSERT_ONLY)
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<Ability>(), SaveMode.UPSERT)).thenReturn(abilityEntity(1L))

        val result =
            service.update(
                UpdateAbilityInput(
                    id = "1",
                    internalName = "stench",
                    name = "恶臭",
                    effect = "effect",
                    introduction = "introduction",
                ),
            )

        assertEquals("1", result.id)
        verify(repository).save(any<Ability>(), SaveMode.UPSERT)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).removeById(1L)
    }
}

private fun abilityEntity(id: Long): Ability =
    Ability {
        this.id = id
        internalName = "stench"
        name = "恶臭"
        effect = "effect"
        introduction = "introduction"
    }
