package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class CreatureVariantRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var creatureVariantRepository: CreatureVariantRepository

    @Test
    fun shouldLoadCreatureVariantSeedData() {
        val pokemonForm = requireNotNull(creatureVariantRepository.findNullable(1L))

        assertEquals("bulbasaur", pokemonForm.internalName)
        assertEquals("bulbasaur", pokemonForm.name)
        assertEquals(true, pokemonForm.defaultForm)
        assertEquals(false, pokemonForm.battleOnly)
        assertEquals(false, pokemonForm.mega)
    }
}
