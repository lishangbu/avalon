package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonFormInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class PokemonFormInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updatePokemonFormInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "1",
              "internalName": "bulbasaur",
              "name": "bulbasaur",
              "formName": null,
              "formOrder": 1,
              "sortingOrder": 1,
              "defaultForm": true,
              "battleOnly": false,
              "mega": false,
              "frontDefault": "https://example.com/front.png",
              "frontShiny": "https://example.com/front-shiny.png",
              "backDefault": "https://example.com/back.png",
              "backShiny": "https://example.com/back-shiny.png",
              "pokemonId": "1"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdatePokemonFormInput::class.java)

        assertEquals("1", input.id)
        assertEquals("bulbasaur", input.internalName)
        assertEquals(true, input.defaultForm)
        assertEquals(false, input.battleOnly)
        assertEquals("1", input.pokemonId)
    }
}
