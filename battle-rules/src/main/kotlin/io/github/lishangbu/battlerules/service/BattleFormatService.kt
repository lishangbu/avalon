package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleFormatRequest
import io.github.lishangbu.battlerules.dto.BattleFormatResponse
import io.github.lishangbu.battlerules.entity.BattleFormat
import io.github.lishangbu.battlerules.entity.activeParticipantCount
import io.github.lishangbu.battlerules.entity.allowCustomRules
import io.github.lishangbu.battlerules.entity.battleMode
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.defaultLevel
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.playerCount
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.teamSize
import io.github.lishangbu.battlerules.repository.BattleFormatRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗赛制维护服务。
 *
 * 赛制是后续战斗引擎选择规则包的第一层入口，因此这里对 code、站位模式、队伍规模和等级拉平做明确校验。
 * Service 只维护赛制主表，不顺手维护条款、限制或特殊机制绑定，避免一次写入跨越多个规则聚合。
 */
@Service
class BattleFormatService(
	private val repository: BattleFormatRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按 code 或名称分页查询赛制。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleFormatResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleFormat::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 按主键读取赛制详情。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleFormatResponse =
		formatByIdOrNotFound(id).toResponse()

	/**
	 * 创建赛制。
	 */
	@Transactional
	fun create(request: BattleFormatRequest): BattleFormatResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleFormat {
				this.code = code
				name = normalized.name
				description = normalized.description
				battleMode = normalized.battleMode
				playerCount = normalized.playerCount
				teamSize = normalized.teamSize
				activeParticipantCount = normalized.activeParticipantCount
				defaultLevel = normalized.defaultLevel
				allowCustomRules = normalized.allowCustomRules
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新赛制基础字段。
	 */
	@Transactional
	fun update(id: Long, request: BattleFormatRequest): BattleFormatResponse {
		formatByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleFormat {
				this.id = id
				this.code = code
				name = normalized.name
				description = normalized.description
				battleMode = normalized.battleMode
				playerCount = normalized.playerCount
				teamSize = normalized.teamSize
				activeParticipantCount = normalized.activeParticipantCount
				defaultLevel = normalized.defaultLevel
				allowCustomRules = normalized.allowCustomRules
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除赛制。
	 *
	 * 赛制被条款、限制或特殊机制绑定引用时，数据库外键和全局异常处理会把删除失败转换成稳定 409 响应。
	 */
	@Transactional
	fun delete(id: Long) {
		formatByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun formatByIdOrNotFound(id: Long): BattleFormat =
		repository.findNullable(id) ?: notFound("id", "战斗赛制不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleFormat::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "赛制 code 已存在: $code")
		}
	}

	private fun BattleFormatRequest.normalized(): BattleFormatRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			description = optionalText(description, "description", 600),
			battleMode = requiredUpperText(battleMode, "battleMode", 32),
			playerCount = requiredIntRange(playerCount, "playerCount", 2, 4),
			teamSize = requiredIntRange(teamSize, "teamSize", 1, 12),
			activeParticipantCount = requiredIntRange(activeParticipantCount, "activeParticipantCount", 1, 4),
			defaultLevel = optionalIntRange(defaultLevel, "defaultLevel", 1, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleFormat.toResponse(): BattleFormatResponse =
		BattleFormatResponse(
			id = id,
			code = code,
			name = name,
			description = description,
			battleMode = battleMode,
			playerCount = playerCount,
			teamSize = teamSize,
			activeParticipantCount = activeParticipantCount,
			defaultLevel = defaultLevel,
			allowCustomRules = allowCustomRules,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
