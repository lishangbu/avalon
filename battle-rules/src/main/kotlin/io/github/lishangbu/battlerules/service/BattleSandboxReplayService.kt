package io.github.lishangbu.battlerules.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxReplaySummaryResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayValidationResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnRequest
import io.github.lishangbu.battlerules.entity.BattleSandboxReplay
import io.github.lishangbu.battlerules.entity.formatCode
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.requestJson
import io.github.lishangbu.battlerules.entity.resolved
import io.github.lishangbu.battlerules.entity.resultSummary
import io.github.lishangbu.battlerules.entity.responseJson
import io.github.lishangbu.battlerules.entity.savedAt
import io.github.lishangbu.battlerules.entity.title
import io.github.lishangbu.battlerules.entity.turnNumber
import io.github.lishangbu.battlerules.repository.BattleSandboxReplayRepository
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 战斗沙盒复盘保存与读取服务。
 *
 * Service 保存产生复盘的一对请求/响应 JSON。保存时先执行结构校验和确定性重放校验，读取时仍只返回保存事实；
 * 校验接口可以在生产排障时重新执行同一请求，确认当前规则资料和引擎实现是否还能跑出当时保存的响应。
 */
@Service
class BattleSandboxReplayService(
	private val repository: BattleSandboxReplayRepository,
	private val sqlClient: KSqlClient,
	private val runtimeSnapshotService: BattleRuntimeSnapshotService,
) {
	private val objectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()
	private val ruleHitMapper = BattleSandboxRuleHitMapper()

	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleSandboxReplaySummaryResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleSandboxReplay::class) {
			search.pattern?.let { pattern ->
				where(or(table.title ilike pattern, table.formatCode ilike pattern))
			}
			orderBy(table.savedAt.desc(), table.id.desc())
			select(table)
		}.fetchPage(page, size).mapRows { it.toSummaryResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleSandboxReplayResponse =
		replayByIdOrNotFound(id).toDetailResponse()

	@Transactional(readOnly = true)
	fun validate(id: Long): BattleSandboxReplayValidationResponse {
		val replay = replayByIdOrNotFound(id)
		val validation = validateReplaySnapshot(replay.responseJson, replay.requestJson)
		return BattleSandboxReplayValidationResponse(
			id = replay.id,
			title = replay.title,
			formatCode = replay.formatCode,
			turnNumber = replay.turnNumber,
			resolved = replay.resolved,
			valid = validation.violations.isEmpty(),
			eventCount = validation.eventCount,
			turnCount = validation.turnCount,
			ruleHitCount = validation.ruleHitCount,
			ruleHitFamilyCodes = validation.ruleHitFamilyCodes,
			deterministicReplayChecked = validation.deterministicReplayChecked,
			deterministicReplayMatched = validation.deterministicReplayMatched,
			warnings = validation.warnings,
			violations = validation.violations,
		)
	}

	@Transactional
	fun create(request: BattleSandboxReplayRequest): BattleSandboxReplayResponse {
		val requestJson = request.requestJson.requiredText("requestJson", maxLength = 2_000_000)
		val responseJson = request.responseJson.requiredText("responseJson", maxLength = 2_000_000)
		val validation = validateReplaySnapshot(responseJson, requestJson)
		if (validation.violations.isNotEmpty()) {
			invalidReplay("responseJson", validation.violations.first())
		}
		val snapshot = validation.metadata ?: invalidReplay("responseJson", "复盘响应缺少有效元数据")
		val title = request.title.requiredText("title", maxLength = 120)
		val formatCode = request.formatCode.requiredSlugCode("formatCode")
		val saved = repository.save(
			BattleSandboxReplay {
				this.title = title
				this.formatCode = formatCode
				turnNumber = snapshot.turnNumber
				resolved = snapshot.resolved
				resultSummary = snapshot.resultSummary
				this.requestJson = requestJson
				this.responseJson = responseJson
				savedAt = Instant.now()
			},
			SaveMode.INSERT_ONLY,
		)
		return saved.toDetailResponse()
	}

	@Transactional
	fun delete(id: Long) {
		replayByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun replayByIdOrNotFound(id: Long): BattleSandboxReplay =
		repository.findNullable(id) ?: notFound("id", "战斗沙盒复盘不存在: $id")

	private fun validateReplaySnapshot(responseJson: String, requestJson: String?): ReplaySnapshotValidation {
		val root = try {
			objectMapper.readTree(responseJson)
		} catch (_: Exception) {
			return ReplaySnapshotValidation(
				metadata = null,
				eventCount = 0,
				turnCount = 0,
				ruleHitCount = 0,
				ruleHitFamilyCodes = emptyList(),
				deterministicReplayChecked = false,
				deterministicReplayMatched = false,
				warnings = emptyList(),
				violations = listOf("responseJson 不是有效的沙盒响应 JSON"),
			)
		}
		val violations = mutableListOf<String>()
		val warnings = mutableListOf<String>()

		val turnNumberNode = root.path("turnNumber")
		val turnNumber = turnNumberNode.takeIf { it.isNumber }?.asInt()
		if (!turnNumberNode.isNumber || turnNumberNode.asInt() < 0) {
			violations += "复盘响应缺少有效的 turnNumber"
		}
		val resolvedNode = root.path("resolved")
		if (!resolvedNode.isBoolean) {
			violations += "复盘响应缺少 resolved"
		}
		val sidesNode = root.path("sides")
		val eventsNode = root.path("events")
		val violationsNode = root.path("violations")
		val ruleHitsNode = root.path("ruleHits")
		val randomTraceNode = root.path("randomTrace")
		val stateNode = root.path("state")
		val stateSidesNode = stateNode.path("sides")
		val stateTurnNumberNode = stateNode.path("turnNumber")
		val stateEventsNode = stateNode.path("events")
		val turnsNode = stateNode.path("turns")
		if (!sidesNode.isArray || sidesNode.isEmpty || !stateNode.isObject || !stateSidesNode.isArray || stateSidesNode.isEmpty) {
			violations += "复盘快照必须包含双方状态"
		}
		if (!stateTurnNumberNode.isNumber || stateTurnNumberNode.asInt() < 0) {
			violations += "状态回合序号不能小于 0"
		} else if (turnNumber != null && stateTurnNumberNode.asInt() != turnNumber) {
			violations += "响应回合序号必须与状态回合序号一致"
		}
		if (!eventsNode.isArray) violations += "复盘响应缺少 events"
		if (!violationsNode.isArray) violations += "复盘响应缺少 violations"
		if (!ruleHitsNode.isArray) violations += "复盘响应缺少 ruleHits"
		if (!randomTraceNode.isArray) violations += "复盘响应缺少 randomTrace"
		if (!stateEventsNode.isArray) violations += "状态快照缺少累计事件流"
		if (!turnsNode.isArray) violations += "状态快照缺少已结算回合"
		if (eventsNode.isArray && stateEventsNode.isArray && eventsNode.size() != stateEventsNode.size()) {
			violations += "响应事件流必须与状态累计事件流一致"
		}
		validateTurnRecords(turnsNode, stateEventsNode, turnNumber, violations)
		val ruleHitFamilyCodes = validateRuleHits(
			ruleHitsNode = ruleHitsNode,
			turnsNode = turnsNode,
			violationsNode = violationsNode,
			randomTraceNode = randomTraceNode,
			violations = violations,
			warnings = warnings,
		)
		val deterministicReplay = if (violations.isEmpty()) {
			validateDeterministicReplay(root, requestJson, violations, warnings)
		} else {
			ReplayDeterminism(checked = false, matched = false)
		}

		return ReplaySnapshotValidation(
			metadata = if (turnNumber != null && turnNumber >= 0 && resolvedNode.isBoolean) {
				ReplaySnapshotMetadata(
					turnNumber = turnNumber,
					resolved = resolvedNode.asBoolean(),
					resultSummary = resultSummary(root.path("result")),
				)
			} else {
				null
			},
			eventCount = eventsNode.takeIf { it.isArray }?.size() ?: 0,
			turnCount = turnsNode.takeIf { it.isArray }?.size() ?: 0,
			ruleHitCount = ruleHitsNode.takeIf { it.isArray }?.size() ?: 0,
			ruleHitFamilyCodes = ruleHitFamilyCodes,
			deterministicReplayChecked = deterministicReplay.checked,
			deterministicReplayMatched = deterministicReplay.matched,
			warnings = warnings,
			violations = violations,
		)
	}

	private fun validateDeterministicReplay(
		savedResponse: JsonNode,
		requestJson: String?,
		violations: MutableList<String>,
		warnings: MutableList<String>,
	): ReplayDeterminism {
		if (requestJson.isNullOrBlank()) {
			violations += "复盘缺少原始请求，无法确定性重放"
			return ReplayDeterminism(checked = false, matched = false)
		}
		val request = try {
			objectMapper.readValue(requestJson, BattleSandboxTurnRequest::class.java)
		} catch (_: Exception) {
			violations += "requestJson 不是有效的沙盒请求 JSON"
			return ReplayDeterminism(checked = false, matched = false)
		}
		val replayedResponse = try {
			runtimeSnapshotService.resolveSandboxTurn(request)
		} catch (error: ApiException) {
			violations += "确定性重放失败：${error.message}"
			return ReplayDeterminism(checked = true, matched = false)
		} catch (_: Exception) {
			violations += "确定性重放失败：当前规则运行时无法完成该请求"
			return ReplayDeterminism(checked = true, matched = false)
		}
		val replayedJson = objectMapper.readTree(objectMapper.writeValueAsString(replayedResponse))
		val mismatch = firstJsonDifference(savedResponse, replayedJson)
		if (mismatch == null) {
			return ReplayDeterminism(checked = true, matched = true)
		}
		val difference = "：$mismatch"
		violations += "确定性重放结果与保存响应不一致$difference"
		warnings += "首个差异$difference"
		return ReplayDeterminism(checked = true, matched = false)
	}

	private fun validateTurnRecords(
		turnsNode: com.fasterxml.jackson.databind.JsonNode,
		stateEventsNode: com.fasterxml.jackson.databind.JsonNode,
		turnNumber: Int?,
		violations: MutableList<String>,
	) {
		if (!turnsNode.isArray) {
			return
		}
		if (turnNumber != null && turnsNode.arrayElements().map { it.path("turnNumber").asInt(-1) } != (1..turnNumber).toList()) {
			violations += "状态回合记录必须从 1 连续到当前回合"
		}
		turnsNode.arrayElements().forEach { turn ->
			val recordTurnNumber = turn.path("turnNumber").takeIf { it.isNumber }?.asInt()
			if (recordTurnNumber == null || recordTurnNumber <= 0) {
				violations += "状态回合记录包含非法 turnNumber"
			}
			val actionsNode = turn.path("actions")
			val turnRandomTraceNode = turn.path("randomTrace")
			val turnEventsNode = turn.path("events")
			if (!actionsNode.isArray || actionsNode.isEmpty) violations += "状态回合记录必须包含已提交行动"
			if (!turnRandomTraceNode.isArray) violations += "状态回合记录缺少随机轨迹"
			if (!turnEventsNode.isArray) violations += "状态回合记录缺少事件片段"
			if (turnEventsNode.isArray && recordTurnNumber != null) {
				if (turnEventsNode.arrayElements().any { it.path("turnNumber").asInt(-1) != recordTurnNumber }) {
					violations += "状态回合记录只能包含自身回合事件"
				}
				if (stateEventsNode.isArray) {
					val accumulatedEventCount = stateEventsNode.arrayElements()
						.count { it.path("turnNumber").asInt(-1) == recordTurnNumber }
					if (accumulatedEventCount != turnEventsNode.size()) {
						violations += "状态回合事件片段必须与累计事件流一致"
					}
				}
			}
			if (turnRandomTraceNode.isArray) {
				val sequences = turnRandomTraceNode.arrayElements().map { it.path("sequence").asInt(-1) }
				if (sequences != (1..turnRandomTraceNode.size()).toList()) {
					violations += "状态回合随机轨迹序号必须从 1 连续递增"
				}
			}
		}
	}

	private fun validateRuleHits(
		ruleHitsNode: com.fasterxml.jackson.databind.JsonNode,
		turnsNode: com.fasterxml.jackson.databind.JsonNode,
		violationsNode: com.fasterxml.jackson.databind.JsonNode,
		randomTraceNode: com.fasterxml.jackson.databind.JsonNode,
		violations: MutableList<String>,
		warnings: MutableList<String>,
	): List<String> {
		if (!ruleHitsNode.isArray) {
			return emptyList()
		}
		val knownFamilies = ruleHitMapper.ruleHitFamilyCodes().toSet()
		val ruleHits = ruleHitsNode.arrayElements()
		val familyCodes = ruleHits.mapNotNull { it.path("familyCode").takeIf { node -> node.isTextual }?.asText() }.sorted().distinct()
		ruleHits.forEach { hit ->
			val familyCode = hit.path("familyCode").takeIf { it.isTextual }?.asText()
			val itemCode = hit.path("itemCode").takeIf { it.isTextual }?.asText()
			if (familyCode.isNullOrBlank()) violations += "规则命中缺少 familyCode"
			if (!familyCode.isNullOrBlank() && familyCode !in knownFamilies) violations += "规则命中包含未知规则族: $familyCode"
			if (itemCode.isNullOrBlank()) violations += "规则命中缺少 itemCode"
			if (!hit.path("triggerCount").isNumber || hit.path("triggerCount").asInt() <= 0) {
				violations += "规则命中 triggerCount 必须大于 0"
			}
		}
		val hitPairs = ruleHits.mapNotNull { hit ->
			val familyCode = hit.path("familyCode").takeIf { it.isTextual }?.asText()
			val itemCode = hit.path("itemCode").takeIf { it.isTextual }?.asText()
			if (familyCode.isNullOrBlank() || itemCode.isNullOrBlank()) null else familyCode to itemCode
		}.toSet()
		validateLatestTurnEventHits(turnsNode, hitPairs, warnings)
		if (violationsNode.isArray && !violationsNode.isEmpty && hitPairs.none { it.first == "turn-flow-action-ordering" }) {
			warnings += "行动违规响应应包含回合流程规则命中"
		}
		if (randomTraceNode.isArray && !randomTraceNode.isEmpty && hitPairs.none { it.first == "random-replay-public-reference" }) {
			warnings += "包含随机轨迹的响应应包含随机规则命中"
		}
		return familyCodes
	}

	private fun validateLatestTurnEventHits(
		turnsNode: com.fasterxml.jackson.databind.JsonNode,
		hitPairs: Set<Pair<String, String>>,
		warnings: MutableList<String>,
	) {
		val latestTurn = turnsNode.takeIf { it.isArray && !it.isEmpty }?.lastOrNull() ?: return
		val latestEvents = latestTurn.path("events").takeIf { it.isArray } ?: return
		latestEvents.arrayElements().forEach { event ->
			val type = event.path("type").takeIf { it.isTextual }?.asText() ?: return@forEach
			val familyCode = ruleHitMapper.familyCodeForEventType(type) ?: return@forEach
			if (familyCode to type !in hitPairs) {
				warnings += "最新回合事件 $type 未出现在规则命中摘要中"
			}
		}
	}

	private fun resultSummary(resultNode: com.fasterxml.jackson.databind.JsonNode): String? {
		if (!resultNode.isObject) {
			return null
		}
		return listOfNotNull(
			resultNode.path("winningSideId").takeIf { it.isTextual }?.asText()?.takeIf(String::isNotBlank)?.let { "胜方 $it" },
			resultNode.path("reason").takeIf { it.isTextual }?.asText()?.takeIf(String::isNotBlank),
		).joinToString("，").ifBlank { null }
	}

	private fun firstJsonDifference(left: JsonNode, right: JsonNode, path: String = "$"): String? {
		if (left.nodeType != right.nodeType) {
			return "$path 类型不同：保存=${left.nodeType}，重放=${right.nodeType}"
		}
		if (left.isValueNode) {
			// 浏览器 JSON.stringify 会把 1.0 写回 1；确定性校验只关心 JSON 数值语义，不关心客户端序列化格式。
			return if (left == right || left.numberEquals(right)) {
				null
			} else {
				"$path 值不同：保存=${left.shortJsonValue()}，重放=${right.shortJsonValue()}"
			}
		}
		if (left.isArray) {
			if (left.size() != right.size()) {
				return "$path 数组长度不同：保存=${left.size()}，重放=${right.size()}"
			}
			for (index in 0 until left.size()) {
				firstJsonDifference(left[index], right[index], "$path[$index]")?.let { return it }
			}
			return null
		}
		val leftFields = left.fieldNames().asSequence().toSet()
		val rightFields = right.fieldNames().asSequence().toSet()
		leftFields.minus(rightFields).minOrNull()?.let { return "$path 缺少重放字段 $it" }
		rightFields.minus(leftFields).minOrNull()?.let { return "$path 多出重放字段 $it" }
		return leftFields.sorted().firstNotNullOfOrNull { fieldName ->
			firstJsonDifference(left.path(fieldName), right.path(fieldName), "$path.$fieldName")
		}
	}

	private fun JsonNode.shortJsonValue(): String =
		toString().let { value -> if (value.length <= 80) value else "${value.take(77)}..." }

	private fun JsonNode.numberEquals(other: JsonNode): Boolean =
		isNumber && other.isNumber && decimalValue().compareTo(other.decimalValue()) == 0

	private fun BattleSandboxReplay.toSummaryResponse(): BattleSandboxReplaySummaryResponse =
		BattleSandboxReplaySummaryResponse(
			id = id,
			title = title,
			formatCode = formatCode,
			turnNumber = turnNumber,
			resolved = resolved,
			resultSummary = resultSummary,
			savedAt = savedAt,
		)

	private fun BattleSandboxReplay.toDetailResponse(): BattleSandboxReplayResponse =
		BattleSandboxReplayResponse(
			id = id,
			title = title,
			formatCode = formatCode,
			turnNumber = turnNumber,
			resolved = resolved,
			resultSummary = resultSummary,
			savedAt = savedAt,
			requestJson = requestJson,
			responseJson = responseJson,
		)

	/**
	 * 从响应 JSON 中提取的列表展示元数据。
	 *
	 * 这些字段会写入复盘表的普通列，用来支撑分页列表和搜索；完整响应仍然以 `responseJson` 保存，避免把
	 * 临时沙盒快照拆成新的战斗资料表。
	 */
	private data class ReplaySnapshotMetadata(
		val turnNumber: Int,
		val resolved: Boolean,
		val resultSummary: String?,
	)

	/**
	 * 单次复盘 JSON 结构校验结果。
	 *
	 * 保存接口会把 [violations] 转换成 400；校验接口则原样返回这些问题，让管理页可以在不破坏记录的情况下提示
	 * 哪些旧复盘或手工改坏的复盘已经不能安全导入。
	 */
	private data class ReplaySnapshotValidation(
		val metadata: ReplaySnapshotMetadata?,
		val eventCount: Int,
		val turnCount: Int,
		val ruleHitCount: Int,
		val ruleHitFamilyCodes: List<String>,
		val deterministicReplayChecked: Boolean,
		val deterministicReplayMatched: Boolean,
		val warnings: List<String>,
		val violations: List<String>,
	)

	/**
	 * 确定性重放结果摘要。
	 *
	 * `checked=false` 表示缺少可解析请求，`checked=true && matched=false` 表示请求已经重新执行但输出不同或运行失败。
	 */
	private data class ReplayDeterminism(
		val checked: Boolean,
		val matched: Boolean,
	)

	private fun com.fasterxml.jackson.databind.JsonNode.arrayElements(): List<com.fasterxml.jackson.databind.JsonNode> =
		elements().asSequence().toList()

	private fun com.fasterxml.jackson.databind.JsonNode.lastOrNull(): com.fasterxml.jackson.databind.JsonNode? =
		arrayElements().lastOrNull()

	private fun invalidReplay(fieldName: String, message: String): Nothing =
		throw ApiException(
			status = HttpStatus.BAD_REQUEST,
			code = ApiErrorCode.VALIDATION_INVALID,
			message = message,
			field = fieldName,
		)
}
