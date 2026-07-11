package io.github.lishangbu.battlerules

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** 验证 Runtime 容量耗尽会映射为稳定 503 协议与 Retry-After。 */
@BattleRulesIntegrationTest
@TestPropertySource(
	properties = [
		"backend.battle-session.runtime.max-active-sessions=1",
		"backend.battle-session.runtime.retry-after=7s",
	],
)
class BattleSessionCapacityApiTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	@Test
	fun `battle session api reports exhausted runtime capacity with retry advice`() {
		mockMvc.perform(
			post("/api/battle-sessions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(sessionCreateJson()),
		).andExpect(status().isCreated)

		mockMvc.perform(
			post("/api/battle-sessions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(sessionCreateJson()),
		)
			.andExpect(status().isServiceUnavailable)
			.andExpect(header().string(HttpHeaders.RETRY_AFTER, "7"))
			.andExpect(jsonPath("$.code").value("battle-session.capacity-exhausted"))
	}

	private fun sessionCreateJson(): String =
		"""
		{
		  "formatCode": "official-double",
		  "sides": [
		    {
		      "activeParticipantIndexes": [0, 1],
		      "participants": [
		        {"creatureId": 1, "level": 50, "skillIds": [1], "itemId": 10},
		        {"creatureId": 2, "level": 50, "skillIds": [1], "itemId": 11}
		      ]
		    },
		    {
		      "activeParticipantIndexes": [0, 1],
		      "participants": [
		        {"creatureId": 3, "level": 50, "skillIds": [1], "itemId": 12},
		        {"creatureId": 4, "level": 50, "skillIds": [1], "itemId": 13}
		      ]
		    }
		  ]
		}
		""".trimIndent()
}
