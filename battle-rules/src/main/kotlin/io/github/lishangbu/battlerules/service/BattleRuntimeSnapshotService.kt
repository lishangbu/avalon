package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battlerules.entity.BattleFormat
import io.github.lishangbu.battlerules.entity.BattleFormatClause
import io.github.lishangbu.battlerules.entity.BattleFormatClauseBinding
import io.github.lishangbu.battlerules.entity.BattleFormatRestriction
import io.github.lishangbu.battlerules.entity.activeParticipantCount
import io.github.lishangbu.battlerules.entity.battleMode
import io.github.lishangbu.battlerules.entity.clauseId
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.defaultLevel
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.formatId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.operandNumber
import io.github.lishangbu.battlerules.entity.operandText
import io.github.lishangbu.battlerules.entity.playerCount
import io.github.lishangbu.battlerules.entity.restrictionOperator
import io.github.lishangbu.battlerules.entity.restrictionType
import io.github.lishangbu.battlerules.entity.teamSize
import io.github.lishangbu.common.web.notFound
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗运行时规则快照装配服务。
 *
 * 该服务是 battle-rules 管理资料和 battle-engine 纯领域模型之间的适配层。它只读取已经启用的赛制条款
 * 和限制记录，并把它们转换成引擎可直接消费的结构化快照；不执行队伍校验，也不启动战斗。
 *
 * 当前解释的资料约定：
 * - `LEVEL/MAX` 的 `operandNumber` 转换为成员等级上限。
 * - `CREATURE/SKILL/ABILITY/ITEM` + `BAN` 转换为对应禁用 ID 集合。
 * - `species-unique` 条款转换为队伍内种类唯一。
 * - `item-unique` 条款转换为队伍内道具唯一。
 *
 * 未识别的条款或限制会被忽略，便于资料先行维护；真正需要引擎执行的新规则应在这里显式补充映射。
 */
@Service
class BattleRuntimeSnapshotService(
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按赛制 code 装配运行时快照。
	 */
	@Transactional(readOnly = true)
	fun getByFormatCode(formatCode: String): BattleRuntimeSnapshot {
		val format = formatByCode(formatCode)
		val restrictions = enabledRestrictions(format.id)
		val clauseCodes = enabledClauseCodes(format.id)
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(),
			rules = BattleRuleSnapshot(
				maxParticipantLevel = restrictions
					.filter { it.restrictionType == "LEVEL" && it.restrictionOperator == "MAX" }
					.mapNotNull { it.operandNumber }
					.minOrNull(),
				bannedCreatureIds = restrictions.bannedIds("CREATURE"),
				bannedSkillIds = restrictions.bannedIds("SKILL"),
				bannedAbilityIds = restrictions.bannedIds("ABILITY"),
				bannedItemIds = restrictions.bannedIds("ITEM"),
				uniqueCreatureRequired = "species-unique" in clauseCodes,
				uniqueItemRequired = "item-unique" in clauseCodes,
			),
		)
	}

	private fun formatByCode(formatCode: String): BattleFormat =
		sqlClient.executeQuery(BattleFormat::class) {
			where(table.code eq formatCode)
			select(table)
		}.singleOrNull() ?: notFound("formatCode", "战斗赛制不存在: $formatCode")

	private fun enabledRestrictions(formatId: Long): List<BattleFormatRestriction> =
		sqlClient.executeQuery(BattleFormatRestriction::class) {
			where(table.formatId eq formatId)
			where(table.enabled eq true)
			select(table)
		}

	private fun enabledClauseCodes(formatId: Long): Set<String> {
		val clauseIds = sqlClient.executeQuery(BattleFormatClauseBinding::class) {
			where(table.formatId eq formatId)
			select(table.clauseId)
		}
		if (clauseIds.isEmpty()) {
			return emptySet()
		}
		return sqlClient.executeQuery(BattleFormatClause::class) {
			where(table.id valueIn clauseIds)
			where(table.enabled eq true)
			select(table.code)
		}.toSet()
	}

	private fun BattleFormat.toEngineFormatSnapshot(): BattleFormatSnapshot =
		BattleFormatSnapshot(
			code = code,
			mode = BattleMode.valueOf(battleMode),
			activeParticipantsPerSide = activeParticipantCount,
			playerCount = playerCount,
			teamSize = teamSize,
			defaultLevel = defaultLevel,
		)

	private fun List<BattleFormatRestriction>.bannedIds(type: String): Set<Long> =
		filter { it.restrictionType == type && it.restrictionOperator == "BAN" }
			.flatMap { restriction ->
				buildList {
					restriction.operandNumber?.takeIf { it > 0 }?.let { add(it.toLong()) }
					restriction.operandText
						?.split(',', ';', ' ', '\n', '\t')
						.orEmpty()
						.mapNotNull { it.trim().toLongOrNull() }
						.filter { it > 0 }
						.forEach(::add)
				}
			}
			.toSet()
}

/**
 * battle-rules 装配出的引擎运行时快照。
 *
 * `format` 描述站位、队伍规模和等级拉平；`rules` 描述准备阶段限制和战斗内基础规则。
 */
data class BattleRuntimeSnapshot(
	val format: BattleFormatSnapshot,
	val rules: BattleRuleSnapshot,
)
