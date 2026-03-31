package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class PokemonFormRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var pokemonFormRepository: PokemonFormRepository

    @Test
    fun shouldLoadPokemonFormSeedData() {
        val pokemonForm = requireNotNull(pokemonFormRepository.findNullable(1L))

        assertEquals("bulbasaur", pokemonForm.internalName)
        assertEquals("bulbasaur", pokemonForm.name)
        assertEquals(true, pokemonForm.defaultForm)
        assertEquals(false, pokemonForm.battleOnly)
        assertEquals(false, pokemonForm.mega)
    }
}
