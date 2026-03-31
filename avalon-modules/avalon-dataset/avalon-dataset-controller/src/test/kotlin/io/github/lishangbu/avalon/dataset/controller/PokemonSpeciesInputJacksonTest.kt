package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonSpeciesInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class PokemonSpeciesInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updatePokemonSpeciesInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "2",
              "internalName": "ivysaur",
              "name": "妙蛙草",
              "sortingOrder": 2,
              "genderRate": 1,
              "captureRate": 45,
              "baseHappiness": 70,
              "baby": false,
              "legendary": false,
              "mythical": false,
              "hatchCounter": 20,
              "hasGenderDifferences": false,
              "formsSwitchable": false,
              "evolvesFromSpeciesId": "1",
              "evolutionChainId": "1",
              "growthRateId": "4",
              "pokemonColorId": "5",
              "pokemonHabitatId": "3",
              "pokemonShapeId": "8"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdatePokemonSpeciesInput::class.java)

        assertEquals("2", input.id)
        assertEquals("ivysaur", input.internalName)
        assertEquals("1", input.evolvesFromSpeciesId?.toString())
        assertEquals("1", input.evolutionChainId?.toString())
        assertEquals("4", input.growthRateId)
        assertEquals("8", input.pokemonShapeId)
    }
}
