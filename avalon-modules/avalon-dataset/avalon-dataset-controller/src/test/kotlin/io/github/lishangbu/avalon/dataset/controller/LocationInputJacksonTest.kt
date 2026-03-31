package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class LocationInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updateLocationInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "1",
              "internalName": "canalave-city",
              "name": "Canalave City",
              "regionId": "4"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdateLocationInput::class.java)

        assertEquals("1", input.id)
        assertEquals("canalave-city", input.internalName)
        assertEquals("4", input.regionId)
    }
}
