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
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.notFound
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Component
import java.util.Locale

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
		clauseCodes.validateSupportedFormatClauses()
		restrictions.validateSupportedFormatRestrictions()
		val battleTeamSize = restrictions.battleTeamSize(format)
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(battleTeamSize),
			rules = BattleRuleSnapshot(
				elementChart = elementChart,
				elementIds = elementIds.requiredBattleRuleElementIds(),
				maxParticipantLevel = restrictions.minimumPositiveOperand("LEVEL", "MAX"),
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

	private fun BattleFormat.toEngineFormatSnapshot(battleTeamSize: Int): BattleFormatSnapshot =
		BattleFormatSnapshot(
			code = code,
			mode = toEngineBattleMode(),
			activeParticipantsPerSide = activeParticipantCount,
			playerCount = playerCount,
			teamSize = battleTeamSize,
			defaultLevel = defaultLevel,
		)

	/**
	 * 把资料表中的文本模式转换为引擎枚举。
	 *
	 * `battle_format.battle_mode` 是管理员可维护的资料字段，不是编译期常量；生产环境里一旦被误改，直接调用
	 * `BattleMode.valueOf` 会把错误资料泄漏成普通运行时异常。这里在读取快照时先转成稳定的 API 字段错误，
	 * 让管理端可以明确看到是哪条赛制资料需要修正。
	 */
	private fun BattleFormat.toEngineBattleMode(): BattleMode =
		BattleMode.entries.firstOrNull { it.name == battleMode.uppercase(Locale.ROOT) }
			?: invalidValue("battleMode", "不支持的战斗模式: $battleMode")

	/**
	 * 读取数值型赛制限制的最小正数操作数。
	 *
	 * 赛制限制是后台可维护资料，`LEVEL/MAX` 这类限制如果没有 `operandNumber`，运行时继续忽略会让“等级上限缺失”
	 * 伪装成“没有等级上限”。这里对匹配到的限制逐条校验正数，再按现代官方规则取最严格的最小值。没有匹配限制
	 * 时仍返回 null，表示该赛制确实没有配置这类规则。
	 */
	private fun List<BattleFormatRestriction>.minimumPositiveOperand(type: String, operator: String): Int? =
		filter { it.restrictionType == type && it.restrictionOperator == operator }
			.map { it.positiveOperandNumber("赛制限制 ${it.code}") }
			.minOrNull()

	/**
	 * 解析进入 battle-engine 的实际参战队伍人数。
	 *
	 * `battle_format.team_size` 表示赛制登记人数，例如官方双打通常登记 6 名；`TEAM/SELECT_COUNT` 才表示队伍预览后
	 * 真正带入一场战斗的成员数量，例如从 6 名中选择 4 名。纯引擎只关心“这场战斗最多能换上多少成员”，所以运行态
	 * 快照优先使用 `SELECT_COUNT`，没有配置时才退回登记人数。这里同时校验 `TEAM/LIMIT_COUNT` 必须和赛制主表一致，
	 * 避免后台把同一事实维护出两套互相冲突的数值。
	 */
	private fun List<BattleFormatRestriction>.battleTeamSize(format: BattleFormat): Int {
		val registrationLimit = singlePositiveOperand("TEAM", "LIMIT_COUNT", "队伍登记人数")
		if (registrationLimit != null && registrationLimit != format.teamSize) {
			invalidValue("operandNumber", "队伍登记人数限制必须等于赛制队伍人数: $registrationLimit != ${format.teamSize}")
		}
		val selectedTeamSize = singlePositiveOperand("TEAM", "SELECT_COUNT", "队伍出战人数")
		if (selectedTeamSize != null) {
			if (selectedTeamSize > format.teamSize) {
				invalidValue("operandNumber", "队伍出战人数不能超过登记人数: $selectedTeamSize > ${format.teamSize}")
			}
			if (selectedTeamSize < format.activeParticipantCount) {
				invalidValue(
					"operandNumber",
					"队伍出战人数不能少于当前上场人数: $selectedTeamSize < ${format.activeParticipantCount}",
				)
			}
		}
		return selectedTeamSize ?: registrationLimit ?: format.teamSize
	}

	/**
	 * 读取只能出现一个语义值的正数操作数。
	 *
	 * 等级上限和禁用清单可以自然合并；队伍登记人数、出战人数这类赛制骨架值却不能同时维护出两个不同数字。生产
	 * 运行态如果取最小值或最后一条，后台会以为多条限制都生效，实际引擎只执行其中一个值。因此这里允许重复相同
	 * 数值，但拒绝同一语义下的冲突配置。
	 */
	private fun List<BattleFormatRestriction>.singlePositiveOperand(
		type: String,
		operator: String,
		label: String,
	): Int? {
		val values = filter { it.restrictionType == type && it.restrictionOperator == operator }
			.map { it.positiveOperandNumber("${label}限制 ${it.code}") }
			.distinct()
		if (values.size > 1) {
			invalidValue("operandNumber", "${label}不能配置多个不同操作数: ${values.joinToString()}")
		}
		return values.singleOrNull()
	}

	private fun List<BattleFormatRestriction>.bannedIds(type: String): Set<Long> =
		filter { it.restrictionType == type && it.restrictionOperator == "BAN" }
			.flatMap { restriction ->
				buildList {
					restriction.operandNumber?.let {
						add(restriction.positiveOperandNumber("禁用限制 ${restriction.code}").toLong())
					}
					restriction.operandText
						?.split(',', ';', ' ', '\n', '	')
						.orEmpty()
						.map { it.trim() }
						.filter { it.isNotEmpty() }
						.map { restriction.positiveOperandTextId(it) }
						.forEach(::add)
				}
			}
			.toSet()

	/**
	 * 校验并读取数值操作数。
	 *
	 * 禁用清单和等级上限都只接受正整数 ID/数值；0、负数或空值通常意味着 CSV/后台表单误填。如果这里继续跳过，
	 * 队伍准备校验会把本应禁止的资料放行，问题会延迟到真实对战或赛事裁定阶段才暴露。
	 */
	private fun BattleFormatRestriction.positiveOperandNumber(context: String): Int {
		val value = operandNumber ?: invalidValue("operandNumber", "$context 必须配置正数操作数")
		if (value <= 0) {
			invalidValue("operandNumber", "$context 操作数必须大于 0: $value")
		}
		return value
	}

	/**
	 * 校验并读取文本禁用清单中的单个 ID。
	 *
	 * `operandText` 允许用逗号、分号、空白或换行维护多个 ID；拆分后的每个 token 都必须能解析为正整数。遇到坏
	 * token 立刻失败，可以把错误定位到具体限制 code 和具体文本片段，而不是在运行时悄悄少禁一条资料。
	 */
	private fun BattleFormatRestriction.positiveOperandTextId(token: String): Long {
		val value = token.toLongOrNull()
			?: invalidValue("operandText", "禁用限制 $code 包含非数字操作数: $token")
		if (value <= 0) {
			invalidValue("operandText", "禁用限制 $code 操作数必须大于 0: $token")
		}
		return value
	}

	/**
	 * 校验启用中的赛制条款已经被运行时明确承载。
	 *
	 * 有些条款不会直接变成 [BattleRuleSnapshot] 字段：`level-flattened` 由赛制默认等级和等级限制共同表达，
	 * `team-preview` 属于开战前选队流程。但它们仍必须列在白名单里，表示运行态读取器知道该条款的归属。未来新增
	 * 条款如果没同步接入这里，就不能被静默当成“已生效规则”。
	 */
	private fun Set<String>.validateSupportedFormatClauses() {
		val unsupported = firstOrNull { it !in SUPPORTED_FORMAT_CLAUSE_CODES }
		if (unsupported != null) {
			invalidValue("clauseCode", "不支持的赛制条款: $unsupported")
		}
	}

	/**
	 * 校验启用中的赛制限制已经能转换为引擎规则或赛制快照字段。
	 *
	 * 限制表是管理端可维护资料，不能让未知 `restrictionType/restrictionOperator` 静默跳过；否则生产环境会出现
	 * “后台启用了限制，但战斗引擎没有执行”的隐性错配。这里把当前已承载的组合集中列出，新增限制时必须同步补
	 * 运行态映射或明确说明它由哪一个赛制字段承载。
	 */
	private fun List<BattleFormatRestriction>.validateSupportedFormatRestrictions() {
		val unsupported = firstOrNull { restriction ->
			restriction.restrictionType to restriction.restrictionOperator !in SUPPORTED_FORMAT_RESTRICTIONS
		}
		if (unsupported != null) {
			invalidValue(
				"restrictionOperator",
				"不支持的赛制限制: ${unsupported.code} (${unsupported.restrictionType}/${unsupported.restrictionOperator})",
			)
		}
	}

	/**
	 * 冻结引擎规则会直接按 code 读取的核心属性 ID。
	 *
	 * [BattleCoreRuntimeLookup.coreElementIds] 会读取现代主系列全部常规属性；这里仍显式列出当前引擎规则真正使用的
	 * code，是为了让资料缺失在赛制快照装配时立即失败，而不是等到天气、异常状态或入场陷阱结算时才悄悄失效。
	 */
	private fun Map<String, Long>.requiredBattleRuleElementIds(): Map<String, Long> =
		listOf(
			"dark",
			"electric",
			"fire",
			"grass",
			"ground",
			"ice",
			"poison",
			"rock",
			"steel",
			"water",
		).associateWith { code -> requiredElementId(code) }

	private companion object {
		private val SUPPORTED_FORMAT_CLAUSE_CODES = setOf(
			"species-unique",
			"item-unique",
			"level-flattened",
			"team-preview",
		)
		private val SUPPORTED_FORMAT_RESTRICTIONS = setOf(
			"LEVEL" to "MAX",
			"TEAM" to "LIMIT_COUNT",
			"TEAM" to "SELECT_COUNT",
			"CREATURE" to "BAN",
			"SKILL" to "BAN",
			"ABILITY" to "BAN",
			"ITEM" to "BAN",
		)
	}
}
