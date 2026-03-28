package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class StatInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updateStatInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "2",
              "internalName": "attack",
              "name": "攻击",
              "gameIndex": 2,
              "battleOnly": false,
              "moveDamageClassId": "2"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdateStatInput::class.java)

        assertEquals("2", input.id)
        assertEquals("attack", input.internalName)
        assertEquals("2", input.moveDamageClassId)
    }
}
