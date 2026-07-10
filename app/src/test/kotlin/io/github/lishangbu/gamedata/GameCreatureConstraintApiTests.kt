package io.github.lishangbu.gamedata

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
 * 验证精灵资料接口对数据库约束的错误响应。
 */
class GameCreatureConstraintApiTests(
	@Autowired userRepository: SecurityUserRepository,
	@Autowired sqlClient: KSqlClient,
	@Autowired webApplicationContext: WebApplicationContext,
) : GameDataApiTestSupport(userRepository, sqlClient, webApplicationContext) {
	@Test
	fun `game data api rejects duplicate creature code`() {
		val token = issueGameDataToken("game-data-duplicate-admin")
		val code = nextCode("codex-duplicate-creature")
		val createdResponse = mockMvc.perform(
			post("/api/game-data/creatures")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(creatureJson(code = code, name = "重复测试精灵")),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString
		val creatureId = JsonPath.read<String>(createdResponse, "$.id")

		mockMvc.perform(
			post("/api/game-data/creatures")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(creatureJson(code = code, name = "重复测试精灵二号")),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value("resource.conflict"))

		mockMvc.perform(
			delete("/api/game-data/creatures/$creatureId")
				.header("Authorization", "Bearer $token"),
		).andExpect(status().isNoContent)
	}

	@Test
	fun `game data api rejects unknown creature species foreign key`() {
		val token = issueGameDataToken("game-data-foreign-key-admin")
		val code = nextCode("codex-invalid-species")

		mockMvc.perform(
			post("/api/game-data/creatures")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(creatureJson(code = code, name = "无效种类精灵", speciesId = 999999999)),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value("resource.conflict"))
			.andExpect(jsonPath("$.message", containsString("无法完成操作")))
	}
}
