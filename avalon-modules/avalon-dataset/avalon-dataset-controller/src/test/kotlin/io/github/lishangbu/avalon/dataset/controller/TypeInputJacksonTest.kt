package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class TypeInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updateTypeInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "10002",
              "internalName": "shadow",
              "name": "暗",
              "battleOnly": true
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdateTypeInput::class.java)

        assertEquals("10002", input.id)
        assertEquals("shadow", input.internalName)
        assertEquals("暗", input.name)
        assertEquals(true, input.battleOnly)
    }
}
