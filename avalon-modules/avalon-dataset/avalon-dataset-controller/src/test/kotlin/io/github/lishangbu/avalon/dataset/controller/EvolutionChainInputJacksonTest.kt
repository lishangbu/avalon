package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionChainInput
import io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class EvolutionChainInputJacksonTest {
    private val objectMapper =
        JsonMapper
            .builder()
            .findAndAddModules()
            .addModule(ImmutableModuleV3())
            .addModule(InputModuleV3())
            .build()

    @Test
    fun updateEvolutionChainInput_canBeDeserializedFromFlatPayload() {
        val payload =
            """
            {
              "id": "100",
              "babyTriggerItemId": "232"
            }
            """.trimIndent()

        val input = objectMapper.readValue(payload, UpdateEvolutionChainInput::class.java)

        assertEquals("100", input.id)
        assertEquals("232", input.babyTriggerItemId)
    }
}
