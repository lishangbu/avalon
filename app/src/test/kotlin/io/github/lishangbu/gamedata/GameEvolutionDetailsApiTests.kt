package io.github.lishangbu.gamedata

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [GameDataApiPostgresTestContainer::class])
/**
 * 验证进化条件接口能让字符串形式的 Long 标识完整往返。
 */
class GameEvolutionDetailsApiTests(
	@Autowired userRepository: SecurityUserRepository,
	@Autowired sqlClient: KSqlClient,
	@Autowired webApplicationContext: WebApplicationContext,
) : GameDataApiTestSupport(userRepository, sqlClient, webApplicationContext) {
	@Test
	fun `game data admin can create and retrieve evolution detail with string identifiers`() {
		val token = issueGameDataToken("game-data-evolution-details-admin")
		var chainId: String? = null
		val speciesIds = mutableListOf<String>()
		var detailId: String? = null

		try {
			chainId = createResource(token, EVOLUTION_CHAINS_PATH, "{}")
			val fromSpeciesId = createResource(
				token,
				SPECIES_PATH,
				speciesJson(nextCode("tdd-evolution-from"), "测试起始种类", 2_000_001),
			).also(speciesIds::add)
			val toSpeciesId = createResource(
				token,
				SPECIES_PATH,
				speciesJson(nextCode("tdd-evolution-to"), "测试目标种类", 2_000_002),
			).also(speciesIds::add)
			val request = evolutionDetailsJson(chainId, fromSpeciesId, toSpeciesId)

			val createdResponse = mockMvc.perform(
				post(EVOLUTION_DETAILS_PATH)
					.header("avalon-token", token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(request),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.id").isString)
				.andExpect(jsonPath("$.chain_id").isString)
				.andExpect(jsonPath("$.chain_id").value(chainId))
				.andExpect(jsonPath("$.from_species_id").isString)
				.andExpect(jsonPath("$.from_species_id").value(fromSpeciesId))
				.andExpect(jsonPath("$.to_species_id").isString)
				.andExpect(jsonPath("$.to_species_id").value(toSpeciesId))
				.andExpect(jsonPath("$.min_level").value(42))
				.andExpect(jsonPath("$.time_of_day").value("night"))
				.andExpect(jsonPath("$.needs_overworld_rain").value(true))
				.andExpect(jsonPath("$.needs_multiplayer").value(true))
				.andReturn()
				.response
				.contentAsString
			detailId = JsonPath.read(createdResponse, "$.id")

			mockMvc.perform(
				get("$EVOLUTION_DETAILS_PATH/$detailId")
					.header("avalon-token", token),
			)
				.andExpect(status().isOk)
				.andExpect(jsonPath("$.id").value(detailId))
				.andExpect(jsonPath("$.chain_id").isString)
				.andExpect(jsonPath("$.chain_id").value(chainId))
				.andExpect(jsonPath("$.from_species_id").value(fromSpeciesId))
				.andExpect(jsonPath("$.to_species_id").value(toSpeciesId))
				.andExpect(jsonPath("$.min_level").value(42))
				.andExpect(jsonPath("$.time_of_day").value("night"))
				.andExpect(jsonPath("$.needs_overworld_rain").value(true))
				.andExpect(jsonPath("$.needs_multiplayer").value(true))
		} finally {
			detailId?.let { id -> deleteResource(token, EVOLUTION_DETAILS_PATH, id) }
			speciesIds.asReversed().forEach { id -> deleteResource(token, SPECIES_PATH, id) }
			chainId?.let { id -> deleteResource(token, EVOLUTION_CHAINS_PATH, id) }
		}
	}

	private fun createResource(token: String, path: String, body: String): String {
		val response = mockMvc.perform(
			post(path)
				.header("avalon-token", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").isString)
			.andReturn()
			.response
			.contentAsString
		return JsonPath.read(response, "$.id")
	}

	private fun deleteResource(token: String, path: String, id: String) {
		mockMvc.perform(
			delete("$path/$id")
				.header("avalon-token", token),
		).andExpect(status().isNoContent)
	}

	private fun speciesJson(code: String, name: String, nationalNumber: Int): String =
		"""
		{
		  "code": "$code",
		  "name": "$name",
		  "national_number": $nationalNumber
		}
		""".trimIndent()

	private fun evolutionDetailsJson(chainId: String, fromSpeciesId: String, toSpeciesId: String): String =
		"""
		{
		  "chain_id": "$chainId",
		  "from_species_id": "$fromSpeciesId",
		  "to_species_id": "$toSpeciesId",
		  "min_level": 42,
		  "time_of_day": "night",
		  "needs_overworld_rain": true,
		  "turn_upside_down": false,
		  "near_special_rock": false,
		  "needs_multiplayer": true,
		  "is_default": false
		}
		""".trimIndent()

	private companion object {
		private const val EVOLUTION_DETAILS_PATH = "/api/game-data/evolution-details"
		private const val EVOLUTION_CHAINS_PATH = "/api/game-data/evolution-chains"
		private const val SPECIES_PATH = "/api/game-data/species"
	}
}
