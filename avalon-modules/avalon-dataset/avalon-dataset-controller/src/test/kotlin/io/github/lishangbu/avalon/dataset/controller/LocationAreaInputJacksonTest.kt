package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationAreaInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class LocationAreaInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updateLocationAreaInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "1",
              "gameIndex": 1,
              "internalName": "canalave-city-area",
              "name": "Canalave City",
              "locationId": "1"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdateLocationAreaInput::class.java)

        assertEquals("1", input.id)
        assertEquals(1, input.gameIndex)
        assertEquals("1", input.locationId)
    }
}
