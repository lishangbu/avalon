package io.github.lishangbu

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnParticipant
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnSkillSlot
import io.github.lishangbu.battlerules.dto.BattleWeatherRuleResponse
import io.github.lishangbu.scheduler.ManagedScheduledTaskExecutionResponse
import io.github.lishangbu.system.dto.OAuthClientResponse
import io.github.lishangbu.system.dto.UserResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
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
import tools.jackson.databind.ObjectMapper
import java.time.Instant

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
	@Autowired private val objectMapper: ObjectMapper,
) {
	private val mockMvc: MockMvc by lazy {
		MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	@Test
	fun `openapi document exposes system api contract and oauth password security`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.info.title").value("Avalon Backend API"))
			.andExpect(jsonPath("$['x-access-node-codes']", hasItem("security:admin")))
			.andExpect(jsonPath("$['x-access-node-codes']", hasItem("system.rbac.users")))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("oauth2"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.tokenUrl").value("/oauth2/token"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['security:admin']").value("系统管理 API 访问权限"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-rules:admin']").value("战斗规则管理 API 访问权限"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-sandbox:run']").value("战斗沙盒执行 API 访问权限"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-sessions:run']").value("战斗会话执行 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].get.summary").value("查询用户列表"))
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].post.summary").value("创建用户"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/clients/{clientId}/secret'].put.summary").value("重置 OAuth client secret"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens'].get.summary").value("查询 OAuth 令牌列表"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens'].get.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens/{authorizationId}'].get.summary").value("查询 OAuth 令牌详情"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/tokens/{authorizationId}/revoke'].post.summary").value("撤销 OAuth 令牌"))
			.andExpect(jsonPath("$.paths['/api/system/scheduler/tasks/{taskId}/trigger'].post.responses['202'].description").value("已接受触发请求"))
			.andExpect(jsonPath("$.components.schemas.UserResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.AccessNodeResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.AccessNodeResponse.properties.code.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.AccessNodeResponse.properties.name.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.AccessNodeResponse.properties.enabled.type").value("boolean"))
			.andExpect(jsonPath("$.components.schemas.AccessNodeResponse.properties.type").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.AccessNodeResponse.properties.path").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.SessionResponse.properties.menus").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.SessionResponse.required", hasItem("accessNodeCodes")))
			.andExpect(jsonPath("$.components.schemas.OAuthClientResponse.properties.accessTokenTtlSeconds.type").value("integer"))
			.andExpect(jsonPath("$.components.schemas.ManagedScheduledTaskResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.ManagedScheduledTaskResponse.required").value(hasItem("id")))
	}

	@Test
	fun `player openapi group exposes trainer team and minimal public profile contracts`() {
		mockMvc.perform(get("/v3/api-docs/player"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.paths['/api/player/trainer-team'].get").exists())
			.andExpect(jsonPath("$.paths['/api/player/trainer-team'].put").exists())
			.andExpect(jsonPath("$.components.schemas.TrainerTeamResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.TrainerTeamResponse.properties.trainerId.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.TrainerTeamMemberResponse.properties.creatureId.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.TrainerTeamMemberResponse.properties.skillIds.items.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.TrainerTeamResponse.properties.revision.type").value("integer"))
			.andExpect(jsonPath("$.paths['/api/player/public-trainers'].get").exists())
			.andExpect(jsonPath("$.paths['/api/player/trainer-session/heartbeat'].post.responses['204']").exists())
			.andExpect(jsonPath("$.components.schemas.PublicTrainerProfile.properties.displayName.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.PublicTrainerProfile.properties.online.type").value("boolean"))
			.andExpect(jsonPath("$.components.schemas.PublicTrainerProfile.properties.challengeable.type").value("boolean"))
			.andExpect(jsonPath("$.components.schemas.PublicTrainerProfile.properties.id").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/player/challenges'].post").exists())
			.andExpect(jsonPath("$.paths['/api/player/challenges'].get").exists())
			.andExpect(jsonPath("$.paths['/api/player/challenges/{challengeId}'].get").exists())
			.andExpect(jsonPath("$.components.schemas.ChallengeResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.ChallengeResponse.properties.members").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.ChallengeResponse.properties.leadPosition").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/player/challenges/{challengeId}/accept'].post").exists())
			.andExpect(jsonPath("$.paths['/api/player/challenges/{challengeId}/accept'].post.responses['503'].content['application/json'].schema['\$ref']")
				.value("#/components/schemas/ChallengeErrorResponse"))
			.andExpect(jsonPath("$.components.schemas.ChallengeErrorResponse.properties.matchId.type").value(hasItem("string")))
			.andExpect(jsonPath("$.components.schemas.MatchResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.MatchResponse.properties.battleSessionId").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/player/matches/current'].get").exists())
			.andExpect(jsonPath("$.paths['/api/player/matches/{matchId}'].get").exists())
			.andExpect(jsonPath("$.paths['/api/player/matches/{matchId}/turns'].post").exists())
			.andExpect(jsonPath("$.paths['/api/player/matches/{matchId}/forfeit'].post").exists())
			.andExpect(jsonPath("$.components.schemas.MatchViewResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.MatchViewSideResponse.properties.side").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.MatchViewResponse.properties.battleSessionId").doesNotExist())
	}

	@Test
	fun `admin openapi group exposes game data api contract and scope`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['game-data:admin']").value("游戏资料管理 API 访问权限"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-sandbox:run']").value("战斗沙盒执行 API 访问权限"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-sessions:run']").value("战斗会话执行 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/turn'].post.summary").value("结算沙盒回合"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/replays'].get.summary").value("分页查询沙盒复盘"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].get.summary").value("分页查询精灵资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].post.summary").value("新增精灵资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures'].post.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureRequest"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].get.responses['200'].content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureResponse"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].put.summary").value("修改精灵资料"))
			.andExpect(jsonPath("$.paths['/api/game-data/creatures/{id}'].put.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/GameCreatureRequest"))
			.andExpect(jsonPath("$.components.schemas.GameCreatureRequest.properties.species_id.description").value("种类 ID"))
			.andExpect(jsonPath("$.components.schemas.GameCreatureResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.GameCreatureResponse.properties.species_id.description").value("种类 ID"))
	}

	@Test
	fun `admin openapi marks non nullable response properties as required`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.schemas.GameCreatureResponse.required").value(hasItem("id")))
			.andExpect(jsonPath("$.components.schemas.GameCreatureResponse.required").value(hasItem("species_id")))
			.andExpect(jsonPath("$.components.schemas.GameSpeciesResponse.required").value(hasItem("national_number")))
			.andExpect(
				jsonPath("$.components.schemas.GameCreatureResponse.required")
					.value(not(hasItem("inherits_from_creature_id"))),
			)
			.andExpect(jsonPath("$.components.schemas.BattleAbilityRuleResponse.required").value(hasItem("abilityId")))
			.andExpect(jsonPath("$.components.schemas.SessionResponse.required").value(hasItem("user")))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxTurnResponse.required").value(hasItem("state")))
	}

	@Test
	fun `game evolution detail request documents reference identifiers as strings`() {
		val document = mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString
		val properties = JsonPath.read<Map<String, Map<String, Any?>>>(
			document,
			"$.components.schemas.GameEvolutionDetailsRequest.properties",
		)
		val identifierTypes = properties
			.filterKeys { property -> property.endsWith("_id") }
			.mapValues { (_, schema) -> schema["type"] }

		assertThat(identifierTypes).isNotEmpty
		identifierTypes.forEach { (property, type) ->
			assertThat(type)
				.describedAs("%s OpenAPI type", property)
				.isEqualTo("string")
		}
	}

	@Test
	fun `game evolution detail paths document identifiers as strings`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(
				jsonPath("$.paths['/api/game-data/evolution-details/{id}'].get.parameters[0].schema.type")
					.value("string"),
			)
			.andExpect(
				jsonPath("$.paths['/api/game-data/evolution-details/{id}'].put.parameters[0].schema.type")
					.value("string"),
			)
			.andExpect(
				jsonPath("$.paths['/api/game-data/evolution-details/{id}'].delete.parameters[0].schema.type")
					.value("string"),
			)
	}

	@Test
	fun `admin openapi documents identifiers as strings in both directions`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.schemas.GameCreatureRequest.properties.species_id.type").value("string"))
			.andExpect(
				jsonPath("$.paths['/api/game-data/creatures/{id}'].get.parameters[0].schema.type")
					.value("string"),
			)
			.andExpect(
				jsonPath("$.components.schemas.OAuthClientResponse.properties.accessTokenTtlSeconds.type")
					.value("integer"),
			)
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
			.andExpect(jsonPath("$.components.schemas.BattleWeatherRuleResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.BattleSkillStatusEffectResponse.properties.skillRuleId.type").value("string"))
	}

	@Test
	fun `battle sandbox openapi group exposes sandbox execution contract and scope`() {
		mockMvc.perform(get("/v3/api-docs/battle-sandbox"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['battle-sandbox:run']").value("战斗沙盒执行 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/turn'].post.tags[0]").value("战斗沙盒"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/turn'].post.security[0].bearerAuth").isArray)
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/turn'].post.summary").value("结算沙盒回合"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/replays'].post.summary").value("保存沙盒复盘"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/replays/{id}'].get.summary").value("读取沙盒复盘"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/turn'].post.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/BattleSandboxTurnRequest"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/turn'].post.responses['200'].content['*/*'].schema['\$ref']").value("#/components/schemas/BattleSandboxTurnResponse"))
			.andExpect(jsonPath("$.paths['/api/battle-sandbox/replays'].post.requestBody.content['application/json'].schema['\$ref']").value("#/components/schemas/BattleSandboxReplayRequest"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxReplayRequest.properties.requestJson.description").value("产生该响应的沙盒回合请求 JSON 文本，用于确定性重放校验。"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxReplayValidationResponse.properties.deterministicReplayChecked.description").value("是否已经使用原始请求执行确定性重放。"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxTurnResponse.description").value("战斗沙盒回合结算响应。"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxTurnResponse.properties.sides.items['\$ref']").value("#/components/schemas/BattleSandboxTurnSide"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxStateSnapshot.properties.sides.items['\$ref']").value("#/components/schemas/BattleSandboxStateSide"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxTurnSide.properties.participants.items['\$ref']").value("#/components/schemas/BattleSandboxTurnParticipant"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxStateSide.properties.participants.items['\$ref']").value("#/components/schemas/BattleSandboxStateParticipant"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxReplayResponse.properties.id.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.BattleSandboxTurnParticipant.properties.creatureId.type").value("string"))
	}

	@Test
	fun `admin openapi exposes temporary battle session contract`() {
		mockMvc.perform(get("/v3/api-docs/admin"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.paths['/api/battle-sessions'].get.summary").value("分页查询战斗会话"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions'].get.operationId").value("listBattleSessions"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions'].post.summary").value("创建战斗会话"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions'].post.operationId").value("createBattleSession"))
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions'].post.requestBody.content['application/json'].schema['\$ref']")
					.value("#/components/schemas/BattleSessionCreateRequest"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions'].post.responses['201'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/BattleSessionResponse"),
			)
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}'].get.summary").value("读取战斗会话"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}'].get.operationId").value("getBattleSession"))
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}'].get.responses['200'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/BattleSessionResponse"),
			)
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.summary").value("结算战斗会话回合"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.operationId").value("submitBattleSessionTurn"))
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.requestBody.content['application/json'].schema['\$ref']")
					.value("#/components/schemas/BattleSessionTurnCommandRequest"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.responses['200'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/BattleSessionTurnResponse"),
			)
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].get.summary").value("分页查询战斗会话回合"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].get.operationId").value("listBattleSessionTurns"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}/termination'].post.summary").value("终止战斗会话"))
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}/termination'].post.operationId").value("terminateBattleSession"))
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/termination'].post.requestBody.content['application/json'].schema['\$ref']")
					.value("#/components/schemas/BattleSessionTerminationRequest"),
			)
			.andExpect(jsonPath("$.paths['/api/battle-sessions/{sessionId}'].get.parameters[0].schema.type").value("string"))
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions'].post.responses['503'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(jsonPath("$.paths['/api/battle-sessions'].post.responses['503'].headers['Retry-After'].schema.type").value("string"))
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.responses['409'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}'].get.responses['404'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions'].get.responses['400'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions'].post.responses['401'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}'].get.responses['403'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].get.responses['404'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.responses['400'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/turns'].post.responses['404'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(
				jsonPath("$.paths['/api/battle-sessions/{sessionId}/termination'].post.responses['409'].content['application/json'].schema['\$ref']")
					.value("#/components/schemas/ApiErrorResponse"),
			)
			.andExpect(jsonPath("$.paths['/api/battle-sessions'].get.security[0].bearerAuth").value(hasItem("battle-sessions:run")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionResponse.properties.sessionId.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.BattleSessionResponse.properties.revision.type").value("integer"))
			.andExpect(jsonPath("$.components.schemas.BattleSessionResponse.required").value(hasItem("sessionId")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionResponse.required").value(hasItem("turnRequirements")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionParticipant.properties.creatureId.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.BattleSessionTurnCommandRequest.required").value(hasItem("commandId")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionTurnCommandRequest.required").value(hasItem("expectedRevision")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionTurnCommandRequest.required").value(hasItem("actions")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionTerminationRequest.required").value(hasItem("expectedRevision")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionSummaryResponse.properties.status.enum").value(hasItem("ACTIVE")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionCreateRequest.required").value(hasItem("formatCode")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionCreateRequest.required").value(hasItem("sides")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionRosterSideRequest.required").value(hasItem("activeParticipantIndexes")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionRosterSideRequest.required").value(hasItem("participants")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionRosterParticipantRequest.required").value(hasItem("creatureId")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionRosterParticipantRequest.required").value(hasItem("level")))
			.andExpect(jsonPath("$.components.schemas.BattleSessionRosterParticipantRequest.required").value(hasItem("skillIds")))
	}

	@Test
	fun `jimmer converters serialize identifiers as strings without changing ordinary long values`() {
		val user = UserResponse {
			id = JAVASCRIPT_UNSAFE_LONG
			username = "admin"
			displayName = "系统管理员"
			enabled = true
			accountNonLocked = true
			roleCodes = listOf("system-admin")
		}
		val client = OAuthClientResponse {
			id = JAVASCRIPT_UNSAFE_LONG
			clientId = "system-admin-jwt"
			clientName = "系统管理 JWT Client"
			clientAuthenticationMethods = listOf("client_secret_basic")
			authorizationGrantTypes = listOf("password")
			scopes = listOf("security:admin")
			accessTokenFormat = "self-contained"
			accessTokenTtlSeconds = 3600
			refreshTokenTtlSeconds = 7200
		}
		val execution = ManagedScheduledTaskExecutionResponse {
			id = JAVASCRIPT_UNSAFE_LONG
			taskId = JAVASCRIPT_UNSAFE_LONG
			taskCode = "cleanup"
			handlerCode = "cleanup"
			scheduledFireTime = null
			actualFireTime = Instant.EPOCH
			finishedAt = null
			status = "SUCCEEDED"
			durationMs = 125
			refireCount = 0
			payloadSnapshot = emptyMap()
			errorMessage = null
		}
		val weather = BattleWeatherRuleResponse {
			id = JAVASCRIPT_UNSAFE_LONG
			code = "rain"
			name = "下雨"
			effectPolicy = "weather-rain"
			defaultDurationTurns = 5
			description = null
			enabled = true
			sortOrder = 10
		}
		val participant = BattleSandboxTurnParticipant {
			actorId = "side-a-1"
			creatureId = JAVASCRIPT_UNSAFE_LONG
			active = true
			level = 50
			currentHp = 100
			maxHp = 100
			majorStatus = null
			statStages = emptyMap()
			skillSlots = listOf(
				BattleSandboxTurnSkillSlot {
					skillId = JAVASCRIPT_UNSAFE_LONG
					name = "测试技能"
					remainingPp = 10
					maxPp = 10
				},
			)
		}

		assertStringId(user, "id")
		assertStringId(client, "id")
		assertThat(objectMapper.readTree(objectMapper.writeValueAsString(client)).get("accessTokenTtlSeconds").isNumber).isTrue()
		assertStringId(execution, "id")
		assertStringId(execution, "taskId")
		assertThat(objectMapper.readTree(objectMapper.writeValueAsString(execution)).get("durationMs").isNumber).isTrue()
		assertStringId(weather, "id")
		assertStringId(participant, "creatureId")
		assertStringId(participant.skillSlots.single(), "skillId")
	}

	@Test
	fun `battle action identifiers serialize as the strings published by openapi`() {
		val action = BattleActionRequest(
			type = "USE_SKILL",
			actorId = "side-a-1",
			skillId = JAVASCRIPT_UNSAFE_LONG,
			targetActorId = "side-b-1",
		)

		assertStringId(action, "skillId")
	}

	private fun assertStringId(value: Any, property: String) {
		val node = objectMapper.readTree(objectMapper.writeValueAsString(value)).get(property)
		assertThat(node.isString).isTrue()
		assertThat(node.stringValue()).isEqualTo(JAVASCRIPT_UNSAFE_LONG.toString())
	}

	@Test
	fun `swagger ui token request uses backend password grant type`() {
		mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
			.andExpect(status().isOk)
			.andExpect(content().string(containsString("requestInterceptor")))
			.andExpect(content().string(containsString("urn:security:params:oauth:grant-type:password")))
	}

	private companion object {
		private const val JAVASCRIPT_UNSAFE_LONG = 9_007_199_254_740_993L
	}
}
