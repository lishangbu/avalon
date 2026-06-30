package io.github.lishangbu

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 验证运行时 OpenAPI 文档能直接服务于接口说明、联调和前端类型生成。
 */
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [io.github.lishangbu.security.SecurityManagementApiPostgresTestContainer::class])
class OpenApiDocumentationTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	private val mockMvc: MockMvc by lazy {
		MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	@Test
	fun `openapi document exposes system api contract and oauth password security`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.info.title").value("Avalon Backend API"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("oauth2"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.tokenUrl").value("/oauth2/token"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['security:admin']").value("系统管理 API 访问权限"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-rules:admin']").value("战斗规则管理 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].get.summary").value("查询用户列表"))
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].post.summary").value("创建用户"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/clients/{clientId}/secret'].put.summary").value("重置 OAuth client secret"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens'].get.summary").value("查询 OAuth 令牌列表"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens'].get.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens/{authorizationId}'].get.summary").value("查询 OAuth 令牌详情"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens/{authorizationId}/revoke'].post.summary").value("撤销 OAuth 令牌"))
			.andExpect(jsonPath("$.paths['/api/system/scheduler/tasks/{taskId}/trigger'].post.responses['202'].description").value("已接受触发请求"))
	}

	@Test
	fun `admin openapi group exposes game data api contract and scope`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['game-data:admin']").value("游戏资料管理 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].get.summary").value("分页查询生物资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].post.summary").value("新增生物资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].post.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureRequest"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].get.responses['200'].content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureResponse"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].put.summary").value("修改生物资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].put.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureRequest"))
			.andExpect(jsonPath("$.components.schemas.GameCreatureRequest.properties.species_id.description").value("种类 ID"))
			.andExpect(jsonPath("$.components.schemas.GameCreatureResponse.properties.species_id.description").value("种类 ID"))
	}

	@Test
	fun `battle rules openapi group exposes battle rules api contract and scope`() {
		mockMvc.perform(get("/v3/api-docs/battle-rules"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-rules:admin']").value("战斗规则管理 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/battle-formats'].get.summary").value("分页查询战斗赛制"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/battle-formats'].post.summary").value("新增战斗赛制"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/status-rules'].get.summary").value("分页查询状态规则"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/weather-rules'].get.summary").value("分页查询天气规则"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/skill-rules'].get.summary").value("分页查询技能规则"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/skill-status-effects'].post.summary").value("新增技能状态效果"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/skill-stat-stage-effects'].post.summary").value("新增技能能力阶级效果"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/ability-rules'].get.summary").value("分页查询特性规则"))
			.andExpect(jsonPath("$.paths['/api/battle-rules/item-rules'].get.summary").value("分页查询道具规则"))
	}

	@Test
	fun `swagger ui token request uses backend password grant type`() {
		mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
			.andExpect(status().isOk)
			.andExpect(content().string(containsString("requestInterceptor")))
			.andExpect(content().string(containsString("urn:security:params:oauth:grant-type:password")))
	}
}
