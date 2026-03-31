package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureEvolutionInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class CreatureEvolutionInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updateCreatureEvolutionInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "1",
              "branchSortOrder": 1,
              "detailSortOrder": 1,
              "needsMultiplayer": false,
              "needsOverworldRain": false,
              "turnUpsideDown": false,
              "timeOfDay": null,
              "minLevel": 16,
              "evolutionChainId": "1",
              "fromCreatureSpeciesId": "1",
              "toCreatureSpeciesId": "2",
              "triggerId": "1"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdateCreatureEvolutionInput::class.java)

        assertEquals("1", input.id)
        assertEquals(1, input.branchSortOrder)
        assertEquals("1", input.evolutionChainId)
        assertEquals("2", input.toCreatureSpeciesId)
    }
}
