package io.github.lishangbu.gamedata

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext

@Tag("integration")
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [GameDataApiPostgresTestContainer::class])
/**
 * 验证精灵资料接口的标准 CRUD 流程。
 */
class GameCreatureCrudApiTests(
	@Autowired userRepository: SecurityUserRepository,
	@Autowired sqlClient: KSqlClient,
	@Autowired webApplicationContext: WebApplicationContext,
) : GameDataApiTestSupport(userRepository, sqlClient, webApplicationContext) {
	@Test
	fun `game data admin can create update read and delete creature`() {
		val token = issueGameDataToken("game-data-crud-admin")
		val code = nextCode("codex-test-creature")

		val createdResponse = mockMvc.perform(
			post("/api/game-data/creatures")
				.header("avalon-token", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(creatureJson(code = code, name = "测试精灵", height = 12, weight = 34)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value(code))
			.andExpect(jsonPath("$.name").value("测试精灵"))
			.andExpect(jsonPath("$.species_id").value(1))
			.andReturn()
			.response
			.contentAsString
		val creatureId = JsonPath.read<String>(createdResponse, "$.id")

		mockMvc.perform(
			put("/api/game-data/creatures/$creatureId")
				.header("avalon-token", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(creatureJson(code = code, name = "测试精灵改", height = 16, weight = 40)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(creatureId))
			.andExpect(jsonPath("$.name").value("测试精灵改"))
			.andExpect(jsonPath("$.height").value(16))

		mockMvc.perform(
			get("/api/game-data/creatures/$creatureId")
				.header("avalon-token", token),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value(code))
			.andExpect(jsonPath("$.weight").value(40))

		mockMvc.perform(
			delete("/api/game-data/creatures/$creatureId")
				.header("avalon-token", token),
		).andExpect(status().isNoContent)

		mockMvc.perform(
			get("/api/game-data/creatures/$creatureId")
				.header("avalon-token", token),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("resource.not_found"))
	}
}
