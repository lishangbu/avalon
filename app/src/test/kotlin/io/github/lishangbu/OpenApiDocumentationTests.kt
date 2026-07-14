package io.github.lishangbu

import io.github.lishangbu.security.SecurityApiAccessPostgresTestContainer
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** 验证运行时 OpenAPI 能直接服务于接口联调和前端类型生成。 */
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = ["spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml"],
)
@ContextConfiguration(initializers = [SecurityApiAccessPostgresTestContainer::class])
class OpenApiDocumentationTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	private val mockMvc: MockMvc by lazy {
		MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	@Test
	fun `openapi publishes sa token header security and login contract`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.info.title").value("Avalon Backend API"))
			.andExpect(jsonPath("$.components.securitySchemes.tokenAuth.type").value("apiKey"))
			.andExpect(jsonPath("$.components.securitySchemes.tokenAuth.name").value("avalon-token"))
			.andExpect(jsonPath("$.components.securitySchemes.tokenAuth.in").value("header"))
			.andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
			.andExpect(jsonPath("$.paths['/api/system/oauth/clients']").doesNotExist())
			.andExpect(jsonPath("$['x-access-node-codes']", hasItem("security:admin")))
	}

	@Test
	fun `admin group keeps the management api schemas`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].get").exists())
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].get").exists())
			.andExpect(jsonPath("$.components.schemas.UserResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.SessionResponse.required", hasItem("accessNodeCodes")))
	}

	@Test
	fun `player group keeps trainer and match contracts`() {
		mockMvc.perform(get("/v3/api-docs/player"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.paths['/api/player/trainer-team'].get").exists())
			.andExpect(jsonPath("$.paths['/api/player/challenges'].post").exists())
			.andExpect(jsonPath("$.paths['/api/player/matches/{matchId}/turns'].post").exists())
	}
}
