package io.github.lishangbu

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.path
import io.github.lishangbu.security.entity.type
import io.github.lishangbu.security.entity.visible
import org.assertj.core.api.Assertions.assertThat
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
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
	@Autowired private val sqlClient: KSqlClient,
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
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].get.summary").value("分页查询精灵资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].post.summary").value("新增精灵资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].post.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureRequest"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].get.responses['200'].content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureResponse"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].put.summary").value("修改精灵资料"))
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

	/**
	 * 验证后端菜单 ROUTE 节点和 OpenAPI 集合路径保持同步。
	 *
	 * 管理端菜单从 `/api/session` 读取，前端 OpenAPI 类型从 `/v3/api-docs/admin` 生成；两者任意一边漏同步，
	 * 都会造成“菜单能进入但请求类型缺失”或“接口已存在但没有维护页面”。这里直接用数据库里的可见 ROUTE
	 * 节点作为页面事实来源，让新增资料表或战斗规则表时测试自动覆盖对应集合接口。
	 */
	@Test
	fun `openapi collection paths match visible management route menus`() {
		val adminPaths = openApiPaths("/v3/api-docs/admin")
		val battleRulePaths = openApiPaths("/v3/api-docs/battle-rules")

		assertThat(collectionPaths(adminPaths, "/api/game-data/"))
			.containsExactlyInAnyOrderElementsOf(visibleGameDataApiPaths())
		assertThat(collectionPaths(battleRulePaths, "/api/battle-rules/"))
			.containsExactlyInAnyOrderElementsOf(visibleBattleRuleApiPaths())
		assertThat(adminPaths)
			.containsAll(visibleSystemApiPaths())
	}

	@Test
	fun `swagger ui token request uses backend password grant type`() {
		mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
			.andExpect(status().isOk)
			.andExpect(content().string(containsString("requestInterceptor")))
			.andExpect(content().string(containsString("urn:security:params:oauth:grant-type:password")))
	}

	private fun openApiPaths(documentPath: String): Set<String> {
		val response = mockMvc.perform(get(documentPath))
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString
		val paths = JsonPath.read<Map<String, Any?>>(response, "$.paths")
		return paths.keys
	}

	private fun collectionPaths(paths: Set<String>, prefix: String): Set<String> =
		paths
			.filter { path -> path.startsWith(prefix) && !path.contains("{") }
			.toSet()

	private fun visibleGameDataApiPaths(): Set<String> =
		visibleRoutePaths("/game-data/")
			.map { path -> path.replaceFirst("/game-data", "/api/game-data") }
			.toSet()

	private fun visibleBattleRuleApiPaths(): Set<String> =
		visibleRoutePaths("/battle-rules/")
			.map(::battleRuleApiPath)
			.toSet()

	private fun visibleSystemApiPaths(): Set<String> =
		visibleRoutePaths("/system/")
			.mapNotNull(::systemApiPath)
			.toSet()

	private fun visibleRoutePaths(prefix: String): List<String> =
		sqlClient.executeQuery(SecurityAccessNode::class) {
			where(table.enabled eq true)
			where(table.visible eq true)
			where(table.type eq "ROUTE")
			select(table)
		}
			.mapNotNull { node -> node.path }
			.filter { path -> path.startsWith(prefix) }

	private fun battleRuleApiPath(routePath: String): String =
		when (routePath) {
			"/battle-rules/action-validation" -> "/api/battle-rules/runtime/action-validation"
			"/battle-rules/preparation-validation" -> "/api/battle-rules/runtime/preparation-validation"
			else -> routePath.replaceFirst("/battle-rules", "/api/battle-rules")
		}

	private fun systemApiPath(routePath: String): String? =
		when (routePath) {
			"/system/oauth/clients" -> "/api/system/oauth/clients"
			"/system/oauth/jwks" -> "/api/system/oauth/jwks"
			"/system/oauth/tokens" -> "/api/system/oauth/tokens"
			"/system/rbac/access-nodes" -> "/api/system/rbac/access-nodes"
			"/system/rbac/roles" -> "/api/system/rbac/roles"
			"/system/rbac/users" -> "/api/system/rbac/users"
			"/system/scheduler/tasks" -> "/api/system/scheduler/tasks"
			else -> null
		}
}
