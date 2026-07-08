package io.github.lishangbu.battlerules.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxReplaySummaryResponse
import io.github.lishangbu.battlerules.entity.BattleSandboxReplay
import io.github.lishangbu.battlerules.entity.formatCode
import io.github.lishangbu.battlerules.entity.id
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
 * Service 不复算战斗，也不尝试“修复”旧快照；它只校验当前响应结构已经足够回到沙盒页面，然后把响应按 JSON 文本
 * 保存下来。后续如果要做严格 replay 校验，应新增独立校验接口，而不是让保存动作变成一次隐式重放。
 */
@Service
class BattleSandboxReplayService(
	private val repository: BattleSandboxReplayRepository,
	private val sqlClient: KSqlClient,
) {
	private val objectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()

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

	@Transactional
	fun create(request: BattleSandboxReplayRequest): BattleSandboxReplayResponse {
		val responseJson = request.responseJson.requiredText("responseJson", maxLength = 2_000_000)
		val snapshot = parseReplaySnapshot(responseJson)
		val title = request.title.requiredText("title", maxLength = 120)
		val formatCode = request.formatCode.requiredSlugCode("formatCode")
		val saved = repository.save(
			BattleSandboxReplay {
				this.title = title
				this.formatCode = formatCode
				turnNumber = snapshot.turnNumber
				resolved = snapshot.resolved
				resultSummary = snapshot.resultSummary
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

	private fun parseReplaySnapshot(responseJson: String): ReplaySnapshotMetadata {
		val root = try {
			objectMapper.readTree(responseJson)
		} catch (_: Exception) {
			invalidReplay("responseJson", "responseJson 不是有效的沙盒响应 JSON")
		}

		val turnNumberNode = root.path("turnNumber")
		if (!turnNumberNode.isNumber || turnNumberNode.asInt() < 0) {
			invalidReplay("responseJson", "复盘响应缺少有效的 turnNumber")
		}
		val resolvedNode = root.path("resolved")
		if (!resolvedNode.isBoolean) {
			invalidReplay("responseJson", "复盘响应缺少 resolved")
		}
		val sidesNode = root.path("sides")
		val stateNode = root.path("state")
		val stateSidesNode = stateNode.path("sides")
		val stateTurnNumberNode = stateNode.path("turnNumber")
		if (!sidesNode.isArray || sidesNode.isEmpty || !stateNode.isObject || !stateSidesNode.isArray || stateSidesNode.isEmpty) {
			invalidReplay("responseJson", "复盘快照必须包含双方状态")
		}
		if (!stateTurnNumberNode.isNumber || stateTurnNumberNode.asInt() < 0) {
			invalidReplay("responseJson", "状态回合序号不能小于 0")
		}

		return ReplaySnapshotMetadata(
			turnNumber = turnNumberNode.asInt(),
			resolved = resolvedNode.asBoolean(),
			resultSummary = resultSummary(root.path("result")),
		)
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
			responseJson = responseJson,
		)

	private data class ReplaySnapshotMetadata(
		val turnNumber: Int,
		val resolved: Boolean,
		val resultSummary: String?,
	)

	private fun invalidReplay(fieldName: String, message: String): Nothing =
		throw ApiException(
			status = HttpStatus.BAD_REQUEST,
			code = ApiErrorCode.VALIDATION_INVALID,
			message = message,
			field = fieldName,
		)
}
