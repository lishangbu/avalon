package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
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
import org.springframework.stereotype.Component

/**
 * 战斗赛制运行时读取器。
 *
 * 本类只处理赛制维度的资料：`battle_format` 基础字段、启用条款、启用限制，以及这些资料到
 * [BattleRuleSnapshot] 的转换。技能、成员能力、特性和道具都不应该放进这里，因为它们的变化原因不同；
 * 赛制规则变更时只需要审阅本文件，运行时队伍资料变更则由其它读取器负责。
 *
 * 这里使用 Jimmer 查询实体表，是因为赛制/限制已经有完善实体模型和字段扩展属性。返回值依旧是纯引擎快照，
 * 不把 Jimmer 实体泄漏给 [BattleRuntimeSnapshotService] 之外的调用链。
 */
@Component
class BattleFormatRuntimeLookup(
	private val sqlClient: KSqlClient,
) {
	fun runtimeSnapshotByFormatCode(
		formatCode: String,
		elementIds: Map<String, Long>,
		elementChart: ElementEffectivenessChart,
	): BattleRuntimeSnapshot {
		val format = formatByCode(formatCode)
		val restrictions = enabledRestrictions(format.id)
		val clauseCodes = enabledClauseCodes(format.id)
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(),
			rules = BattleRuleSnapshot(
				elementChart = elementChart,
				darkElementId = elementIds.requiredElementId("dark"),
				electricElementId = elementIds.requiredElementId("electric"),
				fireElementId = elementIds.requiredElementId("fire"),
				grassElementId = elementIds.requiredElementId("grass"),
				groundElementId = elementIds.requiredElementId("ground"),
				iceElementId = elementIds.requiredElementId("ice"),
				poisonElementId = elementIds.requiredElementId("poison"),
				rockElementId = elementIds.requiredElementId("rock"),
				steelElementId = elementIds.requiredElementId("steel"),
				waterElementId = elementIds.requiredElementId("water"),
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
						?.split(',', ';', ' ', '\n', '	')
						.orEmpty()
						.mapNotNull { it.trim().toLongOrNull() }
						.filter { it > 0 }
						.forEach(::add)
				}
			}
			.toSet()
}
