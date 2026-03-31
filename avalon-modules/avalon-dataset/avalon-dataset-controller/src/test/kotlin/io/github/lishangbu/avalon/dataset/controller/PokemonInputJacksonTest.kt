package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class PokemonInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updatePokemonInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "2",
              "internalName": "ivysaur",
              "name": "ivysaur",
              "height": 10,
              "weight": 130,
              "baseExperience": 142,
              "sortingOrder": 2,
              "pokemonSpeciesId": "2"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdatePokemonInput::class.java)

        assertEquals("2", input.id)
        assertEquals("ivysaur", input.internalName)
        assertEquals(142, input.baseExperience)
        assertEquals("2", input.pokemonSpeciesId)
    }
}
