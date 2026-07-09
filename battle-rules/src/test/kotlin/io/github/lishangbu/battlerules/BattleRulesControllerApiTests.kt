package io.github.lishangbu.battlerules

import com.jayway.jsonpath.JsonPath
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@BattleRulesIntegrationTest
/**
 * 战斗规则 HTTP API 的生产边界回归测试。
 *
 * Service 测试已经覆盖每张表的业务校验，这里只选四种最容易在前后端之间漂移的接口形态：
 * - 普通主表 CRUD：确认 `201/200/204/404` 和分页 JSON 字段稳定。
 * - 绑定表 CRUD：确认外键型请求体、过滤参数和响应 ID 不会退回动态资源接口。
 * - 技能子规则 CRUD：确认技能规则、状态规则这类二级资源仍可通过独立 Controller 维护。
 * - 运行时入口：确认快照读取和统一异常处理通过真实 MVC 层输出，而不是只在 Service 单测里成立。
 */
class BattleRulesControllerApiTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
	@Autowired private val sqlClient: KSqlClient,
) {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	@Test
	fun `battle format api supports create update list get and delete`() {
		val code = nextCode("api-format")
		val createdResponse = mockMvc.perform(
			post("/api/battle-rules/battle-formats")
				.contentType(MediaType.APPLICATION_JSON)
				.content(formatJson(code, "接口测试赛制", battleMode = "single")),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.code").value(code))
			.andExpect(jsonPath("$.battleMode").value("SINGLE"))
			.andReturn()
			.response
			.contentAsString
		val formatId = idAt(createdResponse, "$.id")

		mockMvc.perform(
			put("/api/battle-rules/battle-formats/$formatId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(formatJson(code, "接口测试赛制改", battleMode = "double")),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.name").value("接口测试赛制改"))
			.andExpect(jsonPath("$.battleMode").value("DOUBLE"))

		mockMvc.perform(get("/api/battle-rules/battle-formats").param("q", code))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].id").value(formatId))
			.andExpect(jsonPath("$.totalRowCount").value(1))

		mockMvc.perform(get("/api/battle-rules/battle-formats/$formatId"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value(code))

		mockMvc.perform(delete("/api/battle-rules/battle-formats/$formatId"))
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/battle-rules/battle-formats/$formatId"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("resource.not_found"))
			.andExpect(jsonPath("$.field").value("id"))
	}

	@Test
	fun `format clause binding api keeps explicit relation endpoints`() {
		val formatId = createFormat(nextCode("api-binding-format"), "接口绑定赛制")
		val clauseId = createClause(nextCode("api-binding-clause"), "接口绑定条款")
		val createdResponse = mockMvc.perform(
			post("/api/battle-rules/format-clause-bindings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "formatId": $formatId,
					  "clauseId": $clauseId,
					  "required": true,
					  "sortOrder": 321
					}
					""".trimIndent(),
				),
			)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.formatId").value(formatId))
			.andExpect(jsonPath("$.clauseId").value(clauseId))
			.andReturn()
			.response
			.contentAsString
		val bindingId = idAt(createdResponse, "$.id")

		mockMvc.perform(
			get("/api/battle-rules/format-clause-bindings")
				.param("formatId", formatId.toString())
				.param("clauseId", clauseId.toString()),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].id").value(bindingId))

		mockMvc.perform(delete("/api/battle-rules/format-clause-bindings/$bindingId"))
			.andExpect(status().isNoContent)
		mockMvc.perform(delete("/api/battle-rules/format-clauses/$clauseId"))
			.andExpect(status().isNoContent)
		mockMvc.perform(delete("/api/battle-rules/battle-formats/$formatId"))
			.andExpect(status().isNoContent)
	}

	@Test
	fun `skill status effect api keeps independent child resource endpoint`() {
		val skillRuleId = firstId("/api/battle-rules/skill-rules")
		val statusRuleId = createStatusRule(nextCode("api-status"), "接口测试状态")
		val createdResponse = mockMvc.perform(
			post("/api/battle-rules/skill-status-effects")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "skillRuleId": $skillRuleId,
					  "statusRuleId": $statusRuleId,
					  "targetScope": "TARGET",
					  "effectTiming": "AFTER_DAMAGE",
					  "chancePercent": 25,
					  "enabled": true,
					  "sortOrder": 654
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.skillRuleId").value(skillRuleId))
			.andExpect(jsonPath("$.statusRuleId").value(statusRuleId))
			.andReturn()
			.response
			.contentAsString
		val effectId = idAt(createdResponse, "$.id")

		mockMvc.perform(
			get("/api/battle-rules/skill-status-effects")
				.param("skillRuleId", skillRuleId.toString())
				.param("statusRuleId", statusRuleId.toString()),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].id").value(effectId))

		mockMvc.perform(delete("/api/battle-rules/skill-status-effects/$effectId"))
			.andExpect(status().isNoContent)
		mockMvc.perform(delete("/api/battle-rules/status-rules/$statusRuleId"))
			.andExpect(status().isNoContent)
	}

	@Test
	fun `runtime api returns snapshot and stable validation errors`() {
		mockMvc.perform(get("/api/battle-rules/runtime/formats/official-double"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.format.code").value("official-double"))
			.andExpect(jsonPath("$.rules.uniqueCreatureRequired").value(true))

		mockMvc.perform(
			post("/api/battle-rules/runtime/preparation-validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"formatCode":"official-double","sides":[]}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("sides"))
	}

	@Test
	fun `sandbox api resolves real rule data and returns stable boundary errors`() {
		mockMvc.perform(
			post("/api/battle-sandbox/turn")
				.contentType(MediaType.APPLICATION_JSON)
				.content(sandboxTurnJson()),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.resolved").value(true))
			.andExpect(jsonPath("$.turnNumber").value(1))
			.andExpect(jsonPath("$.state.turnNumber").value(1))
			.andExpect(jsonPath("$.events[?(@.type == 'BattleStarted')]").exists())
			.andExpect(jsonPath("$.events[?(@.type == 'DamageApplied')]").exists())
			.andExpect(jsonPath("$.ruleHits[?(@.familyCode == 'turn-flow-action-ordering' && @.itemCode == 'SkillUsed')]").exists())
			.andExpect(jsonPath("$.ruleHits[?(@.familyCode == 'damage-formula-stat-element-rounding' && @.itemCode == 'DamageApplied')]").exists())
			.andExpect(jsonPath("$.randomTrace").isNotEmpty)

		mockMvc.perform(
			post("/api/battle-sandbox/turn")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"formatCode":"official-double","sides":[],"actions":[]}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("sides"))
	}

	@Test
	fun `sandbox replay api persists lists reads and deletes saved responses`() {
		val turnRequest = sandboxTurnJson()
		val turnResponse = mockMvc.perform(
			post("/api/battle-sandbox/turn")
				.contentType(MediaType.APPLICATION_JSON)
				.content(turnRequest),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString
		val browserResponseJson = turnResponse.replace("\"effectiveness\":1.0", "\"effectiveness\":1")
		assertTrue(browserResponseJson != turnResponse, "测试夹具需要覆盖浏览器 JSON.stringify 的 1.0 到 1 归一化。")

		val replayCreateResult = mockMvc.perform(
			post("/api/battle-sandbox/replays")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "接口测试复盘",
					  "formatCode": "official-double",
					  "requestJson": "${turnRequest.jsonStringLiteral()}",
					  "responseJson": "${browserResponseJson.jsonStringLiteral()}"
					}
					""".trimIndent(),
				),
		)
			.andReturn()
		if (replayCreateResult.response.status != 201) {
			error(replayCreateResult.response.contentAsString)
		}
		val replayResponse = replayCreateResult.response.contentAsString
		assertEquals("接口测试复盘", JsonPath.read(replayResponse, "$.title"))
		assertEquals("official-double", JsonPath.read(replayResponse, "$.formatCode"))
		assertTrue(JsonPath.read<String>(replayResponse, "$.responseJson").contains("\"turnNumber\":1"))
		val replayIdText = JsonPath.read<String>(replayResponse, "$.id")
		val replayId = replayIdText.toLong()

		mockMvc.perform(get("/api/battle-sandbox/replays").param("q", "接口测试复盘"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].id").value(replayIdText))
			.andExpect(jsonPath("$.rows[0].title").value("接口测试复盘"))
			.andExpect(jsonPath("$.rows[0].response").doesNotExist())

		mockMvc.perform(get("/api/battle-sandbox/replays/$replayIdText"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(replayIdText))
			.andExpect(jsonPath("$.requestJson").value(containsString("\"formatCode\"")))
			.andExpect(jsonPath("$.responseJson").value(containsString("\"state\"")))

		mockMvc.perform(post("/api/battle-sandbox/replays/$replayIdText/validation"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.eventCount").isNumber)
			.andExpect(jsonPath("$.turnCount").value(1))
			.andExpect(jsonPath("$.ruleHitFamilyCodes").isNotEmpty)
			.andExpect(jsonPath("$.deterministicReplayChecked").value(true))
			.andExpect(jsonPath("$.deterministicReplayMatched").value(true))
			.andExpect(jsonPath("$.violations").isEmpty)

		sqlClient.executeTestSql(
			"update battle_sandbox_replay set response_json = ? where id = ?",
			browserResponseJson.replace("\"resolved\":true", "\"resolved\":false"),
			replayId,
		)
		mockMvc.perform(post("/api/battle-sandbox/replays/$replayIdText/validation"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.deterministicReplayChecked").value(true))
			.andExpect(jsonPath("$.deterministicReplayMatched").value(false))
			.andExpect(jsonPath("$.violations[0]").value(containsString("确定性重放结果与保存响应不一致")))
			.andExpect(jsonPath("$.warnings[0]").value(containsString("首个差异")))

		sqlClient.executeTestSql(
			"update battle_sandbox_replay set response_json = ? where id = ?",
			"""{"turnNumber":1}""",
			replayId,
		)
		mockMvc.perform(post("/api/battle-sandbox/replays/$replayIdText/validation"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.violations[0]").value("复盘响应缺少 resolved"))

		mockMvc.perform(delete("/api/battle-sandbox/replays/$replayIdText"))
			.andExpect(status().isNoContent)
		mockMvc.perform(get("/api/battle-sandbox/replays/$replayIdText"))
			.andExpect(status().isNotFound)
	}

	@Test
	fun `sandbox replay api rejects invalid saved responses`() {
		mockMvc.perform(
			post("/api/battle-sandbox/replays")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "坏复盘",
					  "formatCode": "official-double",
					  "requestJson": "${sandboxTurnJson().jsonStringLiteral()}",
					  "responseJson": "{\"turnNumber\":1}"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("responseJson"))
	}

	private fun createFormat(code: String, name: String): Long {
		val response = mockMvc.perform(
			post("/api/battle-rules/battle-formats")
				.contentType(MediaType.APPLICATION_JSON)
				.content(formatJson(code, name, battleMode = "single")),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response
			.contentAsString
		return idAt(response, "$.id")
	}

	private fun createClause(code: String, name: String): Long {
		val response = mockMvc.perform(
			post("/api/battle-rules/format-clauses")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "code": "$code",
					  "name": "$name",
					  "clauseType": "TEAM",
					  "description": "接口测试条款。",
					  "enabled": true,
					  "sortOrder": 777
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response
			.contentAsString
		return idAt(response, "$.id")
	}

	private fun createStatusRule(code: String, name: String): Long {
		val response = mockMvc.perform(
			post("/api/battle-rules/status-rules")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "code": "$code",
					  "name": "$name",
					  "statusKind": "VOLATILE",
					  "effectPolicy": "volatile-confusion",
					  "minTurns": 1,
					  "maxTurns": 4,
					  "description": "接口测试状态规则。",
					  "enabled": true,
					  "sortOrder": 888
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response
			.contentAsString
		return idAt(response, "$.id")
	}

	private fun firstId(path: String): Long {
		val response = mockMvc.perform(get(path).param("page", "0").param("size", "1"))
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString
		return idAt(response, "$.rows[0].id")
	}

	private fun formatJson(code: String, name: String, battleMode: String): String =
		"""
		{
		  "code": "$code",
		  "name": "$name",
		  "description": "接口测试赛制。",
		  "battleMode": "$battleMode",
		  "playerCount": 2,
		  "teamSize": 6,
		  "activeParticipantCount": 1,
		  "defaultLevel": 50,
		  "allowCustomRules": true,
		  "enabled": true,
		  "sortOrder": 999
		}
		""".trimIndent()

	private fun sandboxTurnJson(): String =
		"""
		{
		  "formatCode": "official-double",
		  "randomSeed": 0,
		  "sides": [
		    {
		      "sideId": "side-a",
		      "activeActorIds": ["a-1", "a-2"],
		      "participants": [
		        {
		          "actorId": "a-1",
		          "creatureId": 1,
		          "level": 50,
		          "skillIds": [1],
		          "itemId": 10
		        },
		        {
		          "actorId": "a-2",
		          "creatureId": 2,
		          "level": 50,
		          "skillIds": [1],
		          "itemId": 11
		        }
		      ]
		    },
		    {
		      "sideId": "side-b",
		      "activeActorIds": ["b-1", "b-2"],
		      "participants": [
		        {
		          "actorId": "b-1",
		          "creatureId": 3,
		          "level": 50,
		          "skillIds": [1],
		          "itemId": 12
		        },
		        {
		          "actorId": "b-2",
		          "creatureId": 4,
		          "level": 50,
		          "skillIds": [1],
		          "itemId": 13
		        }
		      ]
		    }
		  ],
		  "actions": [
		    {
		      "type": "USE_SKILL",
		      "actorId": "a-1",
		      "skillId": 1,
		      "targetActorId": "b-1"
		    }
		  ]
		}
		""".trimIndent()

	private fun idAt(json: String, path: String): Long =
		JsonPath.read<Number>(json, path).toLong()

	private fun String.jsonStringLiteral(): String =
		replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")

	private fun nextCode(prefix: String): String =
		"$prefix-${nextCodeSuffix.getAndIncrement()}"

	private companion object {
		private val nextCodeSuffix = AtomicLong(System.nanoTime())
	}
}
