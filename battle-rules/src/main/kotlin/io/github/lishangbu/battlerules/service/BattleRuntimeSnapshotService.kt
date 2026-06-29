package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.BattleActionValidator
import io.github.lishangbu.battleengine.BattleActionViolation
import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.BattlePreparationValidator
import io.github.lishangbu.battleengine.BattlePreparationViolation
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.MAX_BATTLE_SKILL_SLOTS
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationResponse
import io.github.lishangbu.battlerules.dto.BattleActionViolationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationParticipantRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationSideRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationViolationResponse
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
import io.github.lishangbu.common.web.requiredText
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import java.sql.ResultSet
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_INDIVIDUAL_VALUE = 31
private const val DEFAULT_EFFORT_VALUE = 0

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
	private val jdbcTemplate: JdbcTemplate,
) {
	private val preparationValidator = BattlePreparationValidator()
	private val actionValidator = BattleActionValidator()
	private val battleEngine = BattleEngine()

	/**
	 * 按赛制 code 装配运行时快照。
	 */
	@Transactional(readOnly = true)
	fun getByFormatCode(formatCode: String): BattleRuntimeSnapshot {
		val format = formatByCode(formatCode)
		val restrictions = enabledRestrictions(format.id)
		val clauseCodes = enabledClauseCodes(format.id)
		val elementIds = coreElementIds()
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(),
			rules = BattleRuleSnapshot(
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

	/**
	 * 按赛制 code 校验准备阶段队伍。
	 *
	 * 该方法先装配运行时规则快照，再把请求中的轻量队伍资料转换成引擎初始快照，最后调用纯引擎校验器。
	 * 它不查询成员、技能、特性或道具资料表；ID 是否存在应由上层构建队伍时保证，当前校验只判断赛制是否合法。
	 */
	@Transactional(readOnly = true)
	fun validatePreparation(request: BattlePreparationValidationRequest): BattlePreparationValidationResponse {
		val normalized = request.normalized()
		val runtime = getByFormatCode(normalized.formatCode)
		val elementIds = coreElementIds()
		val initialState = BattleInitialState(
			format = runtime.format,
			rules = runtime.rules,
			sides = normalized.sides.map { it.toBattleSide(elementIds) },
		)
		val violations = preparationValidator.validate(initialState)
		return BattlePreparationValidationResponse(
			valid = violations.isEmpty(),
			violations = violations.map { it.toResponse() },
		)
	}

	/**
	 * 按赛制 code 校验首回合行动提交。
	 *
	 * 该接口和准备阶段校验共享同一套轻量队伍请求。服务会先装配运行时规则快照，再启动一份纯内存战斗状态，
	 * 最后调用 battle-engine 的行动校验器。它不执行回合结算、不消费随机数，也不把校验结果写入数据库。
	 */
	@Transactional(readOnly = true)
	fun validateActions(request: BattleActionValidationRequest): BattleActionValidationResponse {
		val normalized = request.normalized()
		val runtime = getByFormatCode(normalized.formatCode)
		val elementIds = coreElementIds()
		val initialState = BattleInitialState(
			format = runtime.format,
			rules = runtime.rules,
			sides = normalized.sides.map { it.toBattleSide(elementIds) },
		)
		val state = battleEngine.start(initialState)
		val violations = actionValidator.validate(state, normalized.actions.map { it.toBattleAction() })
		return BattleActionValidationResponse(
			valid = violations.isEmpty(),
			violations = violations.map { it.toResponse() },
		)
	}

	/**
	 * 按基础技能 ID 装配战斗引擎可消费的技能槽。
	 *
	 * 装配分为两层：
	 * - 基础事实来自 `game_skill`：名称、属性、伤害分类、威力、命中、PP 和优先度。
	 * - 战斗规则来自 `battle_skill_rule` 及其子表：多段命中、保护交互、天气命中覆盖、天气威力倍率、
	 *   状态附加、能力阶级变化、技能 HP 效果和全场环境效果。
	 *
	 * 如果某个基础技能暂时没有显式技能规则，仍会使用引擎默认行为构建技能槽。这个默认只发生在运行时适配层，
	 * 不是通用 CRUD 或原始 JSON fallback；它确保准备校验和早期战斗用例可以消费完整基础资料，同时让需要特殊规则的
	 * 技能逐步由独立规则表覆盖。
	 */
	@Transactional(readOnly = true)
	fun skillSlotsBySkillIds(skillIds: List<Long>): List<BattleSkillSlot> {
		if (skillIds.isEmpty()) {
			invalidValue("skillIds", "skillIds 不能为空")
		}
		return skillIds.map { skillId ->
			if (skillId <= 0) {
				invalidValue("skillIds", "skillIds 只能包含正数 ID")
			}
			skillSlotBySkillId(skillId)
		}
	}

	/**
	 * 按基础成员资料和等级装配战斗引擎需要的能力值与属性集合。
	 *
	 * 第一版请求 DTO 尚未携带个体值、努力值和性格，因此这里采用现代对战常用的中性默认：
	 * - 个体值使用 31。
	 * - 努力值使用 0。
	 * - 性格修正使用 1.0。
	 *
	 * 这样得到的数值仍然来自三范式资料表和标准能力公式，不再使用占位 1。后续 DTO 增加个体/努力/性格后，
	 * 只需要扩展该装配函数的输入，不需要改纯引擎模型。
	 */
	@Transactional(readOnly = true)
	fun creatureRuntimeProfile(creatureId: Long, level: Int): BattleCreatureRuntimeProfile {
		if (creatureId <= 0) {
			invalidValue("creatureId", "creatureId 必须大于 0")
		}
		if (level !in 1..100) {
			invalidValue("level", "level 必须在 1 到 100 之间")
		}
		val elementIds = jdbcTemplate.query(
			"""
			select element_id
			from game_creature_element
			where creature_id = ?
			order by slot_order, id
			""".trimIndent(),
			{ rs, _ -> rs.getLong("element_id") },
			creatureId,
		)
		if (elementIds.isEmpty()) {
			notFound("creatureId", "成员属性资料不存在: $creatureId")
		}
		val baseStats = jdbcTemplate.query(
			"""
			select s.code, cs.base_value
			from game_creature_stat cs
			join game_stat s on s.id = cs.stat_id
			where cs.creature_id = ?
			""".trimIndent(),
			{ rs, _ -> rs.getString("code") to rs.getInt("base_value") },
			creatureId,
		).toMap()
		if (baseStats.isEmpty()) {
			notFound("creatureId", "成员能力资料不存在: $creatureId")
		}
		return BattleCreatureRuntimeProfile(
			maxHp = baseStats.requiredBaseStat("hp").toRuntimeHp(level),
			attack = baseStats.requiredBaseStat("attack").toRuntimeBattleStat(level),
			defense = baseStats.requiredBaseStat("defense").toRuntimeBattleStat(level),
			specialAttack = baseStats.requiredBaseStat("special-attack").toRuntimeBattleStat(level),
			specialDefense = baseStats.requiredBaseStat("special-defense").toRuntimeBattleStat(level),
			speed = baseStats.requiredBaseStat("speed").toRuntimeBattleStat(level),
			elementIds = elementIds.toSet(),
		)
	}

	/**
	 * 按基础特性 ID 装配战斗引擎可消费的结构化特性效果。
	 *
	 * 这里显式识别 `battle_ability_rule.effect_policy` 中已经有引擎模型承载的策略：
	 * - 低体力时强化草/火/水属性伤害。
	 * - 受到接触类攻击后概率让攻击方麻痹。
	 * - 出场时降低当前对手上场成员攻击阶级。
	 * - 出场时设置现代普通天气。
	 * - 出场时设置现代普通场地。
	 * - 指定天气下修改速度倍率。
	 * - 指定场地下修改速度倍率。
	 * - 指定天气下回合末按最大 HP 固定比例回复。
	 * - 满 HP 承受致命直接伤害时保留 1 HP。
	 * - 间接伤害免疫。
	 * - 技能反作用伤害免疫。
	 * - 被技能击中要害免疫。
	 * - 无视对手伤害公式能力阶级变化。
	 * - 无视对手命中/闪避阶级变化。
	 * - 使用技能时无视目标侧防守特性效果。
	 * - 免疫其它成员使用的声音类技能。
	 * - 阻止对手先制技能影响己方。
	 *
	 * `ground-immunity` 会影响成员是否接地，由 `groundedByAbilityId` 单独装配；它不是伤害或状态 hook，
	 * 因此不塞进 `BattleAbilityEffect` 列表。暂未有引擎结构的策略保持不输出效果，避免用字符串在纯引擎里硬解析。
	 */
	@Transactional(readOnly = true)
	fun abilityEffectsByAbilityId(abilityId: Long?): List<BattleAbilityEffect> {
		if (abilityId == null) {
			return emptyList()
		}
		if (abilityId <= 0) {
			invalidValue("abilityId", "abilityId 必须大于 0")
		}
		val elementIds = coreElementIds()
		return enabledAbilityPolicies(abilityId).mapNotNull { it.toBattleAbilityEffect(elementIds) }
	}

	/**
	 * 判断特性是否让成员在现代规则中不被视为接地。
	 *
	 * 这会影响地面属性免疫、场地是否作用、部分粉末/电属性规则等多个 hook。当前资料用独立 policy 表达，
	 * 装配时转换成成员快照上的稳定布尔值，让引擎后续所有接地判断共享同一个事实。
	 */
	@Transactional(readOnly = true)
	fun groundedByAbilityId(abilityId: Long?): Boolean {
		if (abilityId == null) {
			return true
		}
		if (abilityId <= 0) {
			invalidValue("abilityId", "abilityId 必须大于 0")
		}
		return "ground-immunity" !in enabledAbilityPolicies(abilityId)
	}

	/**
	 * 按基础道具 ID 装配战斗引擎可消费的结构化携带道具效果。
	 *
	 * 当前接入引擎已有模型覆盖的两类策略：回合末按最大 HP 比例回复，以及造成伤害时增伤并按最大 HP 反伤。
	 * 低体力树果会映射为一次性回复；造成伤害后回复道具会映射为按实际伤害量的比例回复；讲究类速度道具会
	 * 映射为速度倍率和技能选择锁定；指定属性伤害提升道具会映射为匹配技能属性时的稳定威力倍率；
	 * 物理/特殊分类强化道具会映射为对应分类的威力倍率；抗性树果会映射为本体受到指定属性伤害时的一次性减伤；
	 * 效果绝佳伤害提升道具会映射为最终伤害倍率；
	 * 获得主要异常状态或临时状态后即时解除的道具会映射为状态治愈效果；天气、场地和屏障延长类道具会映射为
	 * 成功设置对应持续效果时的回合覆盖；满 HP 保命道具会映射为一次性致命伤害保留 1 HP。
	 */
	@Transactional(readOnly = true)
	fun itemEffectsByItemId(itemId: Long?): List<BattleItemEffect> {
		if (itemId == null) {
			return emptyList()
		}
		if (itemId <= 0) {
			invalidValue("itemId", "itemId 必须大于 0")
		}
		val elementIds = coreElementIds()
		return enabledItemPolicies(itemId).mapNotNull { it.toBattleItemEffect(elementIds) }
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

	/**
	 * 读取引擎基础规则需要识别的核心属性 ID。
	 *
	 * 这里按稳定 code 而不是硬编码资料 ID 装配快照，保持纯引擎不依赖资料库编号。查询范围覆盖现代主系列 18 个
	 * 常规属性，因为伤害、天气、主要异常状态、属性限定特性和属性伤害提升道具都可能需要把资料 policy 翻译成
	 * 引擎可执行的属性 ID。
	 */
	private fun coreElementIds(): Map<String, Long> =
		jdbcTemplate.query(
			"""
			select code, id
			from game_element
			where code in (
				'normal', 'fighting', 'flying', 'poison', 'ground', 'rock',
				'bug', 'ghost', 'steel', 'fire', 'water', 'grass',
				'electric', 'psychic', 'ice', 'dragon', 'dark', 'fairy'
			)
			""".trimIndent(),
		) { rs, _ -> rs.getString("code") to rs.getLong("id") }.toMap()

	private fun Map<String, Long>.requiredElementId(code: String): Long =
		this[code] ?: error("核心属性资料缺失: $code")

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

	private fun BattlePreparationValidationRequest.normalized(): BattlePreparationValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
		)

	private fun BattleActionValidationRequest.normalized(): BattleActionValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
			actions = actions.takeIf { it.isNotEmpty() } ?: invalidValue("actions", "actions 不能为空"),
		)

	private fun BattlePreparationSideRequest.toBattleSide(elementIds: Map<String, Long>): BattleSide {
		val normalizedSideId = sideId.requiredText("sideId", maxLength = 80)
		if (activeActorIds.isEmpty()) {
			invalidValue("activeActorIds", "activeActorIds 不能为空")
		}
		if (participants.isEmpty()) {
			invalidValue("participants", "participants 不能为空")
		}
		return BattleSide(
			sideId = normalizedSideId,
			activeActorIds = activeActorIds.map { it.requiredText("activeActorIds", maxLength = 80) },
			participants = participants.map { it.toBattleParticipant(elementIds) },
		)
	}

	private fun BattlePreparationParticipantRequest.toBattleParticipant(elementIds: Map<String, Long>): BattleParticipant {
		val normalizedActorId = actorId.requiredText("actorId", maxLength = 80)
		if (creatureId <= 0) {
			invalidValue("creatureId", "creatureId 必须大于 0")
		}
		if (level !in 1..100) {
			invalidValue("level", "level 必须在 1 到 100 之间")
		}
		if (skillIds.isEmpty()) {
			invalidValue("skillIds", "skillIds 不能为空")
		}
		if (skillIds.size > MAX_BATTLE_SKILL_SLOTS) {
			invalidValue("skillIds", "skillIds 最多只能包含 $MAX_BATTLE_SKILL_SLOTS 个技能")
		}
		if (skillIds.toSet().size != skillIds.size) {
			invalidValue("skillIds", "skillIds 不能包含重复技能")
		}
		abilityId?.takeIf { it <= 0 }?.let {
			invalidValue("abilityId", "abilityId 必须大于 0")
		}
		itemId?.takeIf { it <= 0 }?.let {
			invalidValue("itemId", "itemId 必须大于 0")
		}
		val abilityPolicies = abilityId?.let(::enabledAbilityPolicies).orEmpty()
		val itemPolicies = itemId?.let(::enabledItemPolicies).orEmpty()
		val profile = creatureRuntimeProfile(creatureId, level)
		return BattleParticipant(
			actorId = normalizedActorId,
			creatureId = creatureId,
			level = level,
			maxHp = profile.maxHp,
			currentHp = profile.maxHp,
			attack = profile.attack,
			defense = profile.defense,
			specialAttack = profile.specialAttack,
			specialDefense = profile.specialDefense,
			speed = profile.speed,
			elementIds = profile.elementIds,
			skillSlots = skillSlotsBySkillIds(skillIds),
			abilityId = abilityId,
			itemId = itemId,
			grounded = "ground-immunity" !in abilityPolicies,
			abilityEffects = abilityPolicies.mapNotNull { it.toBattleAbilityEffect(elementIds) },
			itemEffects = itemPolicies.mapNotNull { it.toBattleItemEffect(elementIds) },
		)
	}

	private fun BattleActionRequest.toBattleAction(): BattleAction {
		val normalizedType = type.requiredText("type", maxLength = 40).uppercase()
		val normalizedActorId = actorId.requiredText("actorId", maxLength = 80)
		val normalizedTargetActorId = targetActorId.requiredText("targetActorId", maxLength = 80)
		return when (normalizedType) {
			"USE_SKILL" -> BattleAction.UseSkill(
				actorId = normalizedActorId,
				skillId = skillId?.takeIf { it > 0 } ?: invalidValue("skillId", "skillId 必须大于 0"),
				targetActorId = normalizedTargetActorId,
			)
			"SWITCH_PARTICIPANT" -> BattleAction.SwitchParticipant(
				actorId = normalizedActorId,
				targetActorId = normalizedTargetActorId,
			)
			else -> invalidValue("type", "type 只支持 USE_SKILL 或 SWITCH_PARTICIPANT")
		}
	}

	private fun enabledAbilityPolicies(abilityId: Long): List<String> =
		jdbcTemplate.query(
			"""
			select effect_policy
			from battle_ability_rule
			where ability_id = ? and enabled = true
			order by trigger_order, sort_order, id
			""".trimIndent(),
			{ rs, _ -> rs.getString("effect_policy") },
			abilityId,
		)

	private fun enabledItemPolicies(itemId: Long): List<String> =
		jdbcTemplate.query(
			"""
			select effect_policy
			from battle_item_rule
			where item_id = ? and enabled = true
			order by trigger_order, sort_order, id
			""".trimIndent(),
			{ rs, _ -> rs.getString("effect_policy") },
			itemId,
		)

	private fun skillSlotBySkillId(skillId: Long): BattleSkillSlot {
		val row = jdbcTemplate.query(
			"""
			select
				s.id as skill_id,
				s.name as skill_name,
				s.element_id,
				dc.code as damage_class_code,
				s.power,
				s.accuracy,
				s.pp,
				s.priority,
				r.id as rule_id,
				r.effect_policy,
				r.target_policy,
				r.min_hits,
				r.max_hits,
				r.critical_hit_stage,
				r.makes_contact,
				r.affected_by_protect,
				r.protects_user,
				r.thaws_user_before_move,
				r.sound_based,
				r.powder_based,
				r.punch_based,
				r.slicing_based,
				r.weakened_by_grassy_terrain,
				r.charges_before_use,
				r.recharges_after_use,
				r.lock_move_turns_min,
				r.lock_move_turns_max,
				r.confuses_user_after_lock,
				r.force_target_switch
			from game_skill s
			join game_skill_damage_class dc on dc.id = s.damage_class_id
			left join battle_skill_rule r on r.skill_id = s.id and r.enabled = true
			where s.id = ?
			""".trimIndent(),
			{ rs, _ -> rs.toSkillRuntimeRow() },
			skillId,
		).singleOrNull() ?: invalidValue("skillIds", "技能不存在: $skillId")

		return BattleSkillSlot(
			skillId = row.skillId,
			name = row.name,
			elementId = row.elementId,
			damageClass = row.damageClassCode.toBattleDamageClass(),
			power = row.power,
			fixedDamage = row.effectPolicy.toBattleFixedDamage(),
			proportionalDamage = row.effectPolicy.toBattleProportionalDamage(),
			hpDerivedDamage = row.effectPolicy.toBattleHpDerivedDamage(),
			accuracy = row.accuracy,
			targetScope = row.targetPolicy.toBattleSkillTargetScope(),
			minHits = row.minHits ?: 1,
			maxHits = row.maxHits ?: 1,
			makesContact = row.makesContact ?: false,
			criticalHitStage = row.criticalHitStage ?: 0,
			affectedByProtect = row.affectedByProtect ?: true,
			protectsUser = row.protectsUser ?: false,
			thawsUserBeforeMove = row.thawsUserBeforeMove ?: false,
			soundBased = row.soundBased ?: false,
			powderBased = row.powderBased ?: false,
			punchBased = row.punchBased ?: false,
			slicingBased = row.slicingBased ?: false,
			weakenedByGrassyTerrain = row.weakenedByGrassyTerrain ?: false,
			chargesBeforeUse = row.chargesBeforeUse ?: false,
			chargeSkippedByWeathers = chargeSkippedByWeathers(row.ruleId),
			rechargesAfterUse = row.rechargesAfterUse ?: false,
			accuracyOverridesByWeather = weatherAccuracyOverrides(row.ruleId),
			powerMultipliersByWeather = weatherPowerMultipliers(row.ruleId),
			elementOverridesByWeather = weatherElementOverrides(row.ruleId),
			lockMoveTurnsMin = row.lockMoveTurnsMin ?: 1,
			lockMoveTurnsMax = row.lockMoveTurnsMax ?: 1,
			confusesUserAfterLock = row.confusesUserAfterLock ?: false,
			forceTargetSwitch = row.forceTargetSwitch ?: false,
			priority = row.priority,
			remainingPp = row.pp,
			maxPp = row.pp,
			statusApplications = statusApplications(row.ruleId),
			volatileStatusApplications = volatileStatusApplications(row.ruleId),
			statStageEffects = statStageEffects(row.ruleId),
			statStageOperations = statStageOperations(row.ruleId),
			sideConditionApplications = sideConditionApplications(row.ruleId),
			sideSpeedModifierApplications = sideSpeedModifierApplications(row.ruleId),
			sideEntryHazardApplications = sideEntryHazardApplications(row.ruleId),
			fieldSpeedOrderApplications = fieldSpeedOrderApplications(row.ruleId),
			hpEffects = row.effectPolicy.toBattleSkillHpEffects(),
			environmentEffects = row.effectPolicy.toBattleSkillEnvironmentEffects(),
		)
	}

	private fun ResultSet.toSkillRuntimeRow(): SkillRuntimeRow =
		SkillRuntimeRow(
			skillId = getLong("skill_id"),
			name = getString("skill_name"),
			elementId = getLong("element_id"),
			damageClassCode = getString("damage_class_code"),
			power = nullableInt("power")?.takeIf { it > 0 },
			accuracy = nullableInt("accuracy")?.takeIf { it > 0 },
			pp = getInt("pp").coerceAtLeast(0),
			priority = getInt("priority"),
			ruleId = nullableLong("rule_id"),
			effectPolicy = getString("effect_policy"),
			targetPolicy = getString("target_policy"),
			minHits = nullableInt("min_hits"),
			maxHits = nullableInt("max_hits"),
			criticalHitStage = nullableInt("critical_hit_stage"),
			makesContact = nullableBoolean("makes_contact"),
			affectedByProtect = nullableBoolean("affected_by_protect"),
			protectsUser = nullableBoolean("protects_user"),
			thawsUserBeforeMove = nullableBoolean("thaws_user_before_move"),
			soundBased = nullableBoolean("sound_based"),
			powderBased = nullableBoolean("powder_based"),
			punchBased = nullableBoolean("punch_based"),
			slicingBased = nullableBoolean("slicing_based"),
			weakenedByGrassyTerrain = nullableBoolean("weakened_by_grassy_terrain"),
			chargesBeforeUse = nullableBoolean("charges_before_use"),
			rechargesAfterUse = nullableBoolean("recharges_after_use"),
			lockMoveTurnsMin = nullableInt("lock_move_turns_min"),
			lockMoveTurnsMax = nullableInt("lock_move_turns_max"),
			confusesUserAfterLock = nullableBoolean("confuses_user_after_lock"),
			forceTargetSwitch = nullableBoolean("force_target_switch"),
		)

	private fun weatherAccuracyOverrides(ruleId: Long?): Map<BattleWeather, Int?> {
		if (ruleId == null) {
			return emptyMap()
		}
		return jdbcTemplate.query(
			"""
			select w.code as weather_code, o.accuracy_percent
			from battle_skill_weather_accuracy_override o
			join battle_weather_rule w on w.id = o.weather_rule_id
			where o.skill_rule_id = ? and o.enabled = true and w.enabled = true
			order by o.sort_order, o.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("weather_code").toBattleWeather() to rs.nullableInt("accuracy_percent") },
			ruleId,
		).toMap()
	}

	private fun weatherPowerMultipliers(ruleId: Long?): Map<BattleWeather, Double> {
		if (ruleId == null) {
			return emptyMap()
		}
		return jdbcTemplate.query(
			"""
			select w.code as weather_code, m.power_multiplier
			from battle_skill_weather_power_modifier m
			join battle_weather_rule w on w.id = m.weather_rule_id
			where m.skill_rule_id = ? and m.enabled = true and w.enabled = true
			order by m.sort_order, m.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("weather_code").toBattleWeather() to rs.getDouble("power_multiplier") },
			ruleId,
		).toMap()
	}

	private fun weatherElementOverrides(ruleId: Long?): Map<BattleWeather, Long> {
		if (ruleId == null) {
			return emptyMap()
		}
		return jdbcTemplate.query(
			"""
			select w.code as weather_code, o.target_element_id
			from battle_skill_weather_element_override o
			join battle_weather_rule w on w.id = o.weather_rule_id
			join game_element e on e.id = o.target_element_id
			where o.skill_rule_id = ? and o.enabled = true and w.enabled = true and e.enabled = true
			order by o.sort_order, o.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("weather_code").toBattleWeather() to rs.getLong("target_element_id") },
			ruleId,
		).toMap()
	}

	private fun chargeSkippedByWeathers(ruleId: Long?): Set<BattleWeather> {
		if (ruleId == null) {
			return emptySet()
		}
		return jdbcTemplate.query(
			"""
			select w.code as weather_code
			from battle_skill_charge_skip_weather s
			join battle_weather_rule w on w.id = s.weather_rule_id
			where s.skill_rule_id = ? and s.enabled = true and w.enabled = true
			order by s.sort_order, s.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("weather_code").toBattleWeather() },
			ruleId,
		).toSet()
	}

	private fun statusApplications(ruleId: Long?): List<BattleStatusApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select sr.code as status_code, e.target_scope, e.chance_percent
			from battle_skill_status_effect e
			join battle_status_rule sr on sr.id = e.status_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and sr.enabled = true
				and sr.status_kind = 'MAJOR'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val target = rs.getString("target_scope").toBattleEffectTarget() ?: return@query null
				BattleStatusApplication(
					status = rs.getString("status_code").toBattleMajorStatus(),
					target = target,
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun volatileStatusApplications(ruleId: Long?): List<BattleVolatileStatusApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select sr.code as status_code, e.target_scope, e.chance_percent
			from battle_skill_status_effect e
			join battle_status_rule sr on sr.id = e.status_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and sr.enabled = true
				and sr.status_kind = 'VOLATILE'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val target = rs.getString("target_scope").toBattleEffectTarget() ?: return@query null
				BattleVolatileStatusApplication(
					status = rs.getString("status_code").toBattleVolatileStatus(),
					target = target,
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun statStageEffects(ruleId: Long?): List<BattleStatStageEffect> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select st.code as stat_code, e.target_scope, e.stage_delta, e.chance_percent
			from battle_skill_stat_stage_effect e
			join game_stat st on st.id = e.stat_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val target = rs.getString("target_scope").toBattleEffectTarget() ?: return@query null
				BattleStatStageEffect(
					stat = rs.getString("stat_code").toBattleStat(),
					target = target,
					stageDelta = rs.getInt("stage_delta"),
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun statStageOperations(ruleId: Long?): List<BattleStatStageOperation> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select
				st.code as stat_code,
				e.operation_kind,
				e.target_scope,
				e.source_scope,
				e.chance_percent
			from battle_skill_stat_stage_operation e
			join game_stat st on st.id = e.stat_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val source = rs.getString("source_scope")?.toBattleStatStageOperationTarget()
				BattleStatStageOperation(
					kind = rs.getString("operation_kind").toBattleStatStageOperationKind(),
					stat = rs.getString("stat_code").toBattleStat(),
					target = rs.getString("target_scope").toBattleStatStageOperationTarget(),
					source = source,
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		)
	}

	private fun sideConditionApplications(ruleId: Long?): List<BattleSideConditionApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select
				fr.effect_policy as field_effect_policy,
				fr.min_turns,
				e.target_side,
				e.chance_percent,
				w.code as required_weather_code
			from battle_skill_field_effect e
			join battle_field_rule fr on fr.id = e.field_rule_id
			left join battle_weather_rule w on w.id = e.required_weather_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and fr.enabled = true
				and fr.effect_scope = 'SIDE'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val reductionKind = rs.getString("field_effect_policy").toBattleSideDamageReductionKind() ?: return@query null
				BattleSideConditionApplication(
					targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
					damageReduction = BattleSideDamageReduction(
						kind = reductionKind,
						turnsRemaining = rs.nullableInt("min_turns"),
					),
					chancePercent = rs.getInt("chance_percent"),
					requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun sideSpeedModifierApplications(ruleId: Long?): List<BattleSideSpeedModifierApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select
				fr.effect_policy as field_effect_policy,
				fr.min_turns,
				e.target_side,
				e.chance_percent,
				w.code as required_weather_code
			from battle_skill_field_effect e
			join battle_field_rule fr on fr.id = e.field_rule_id
			left join battle_weather_rule w on w.id = e.required_weather_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and fr.enabled = true
				and fr.effect_scope = 'SIDE'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val modifierKind = rs.getString("field_effect_policy").toBattleSideSpeedModifierKind() ?: return@query null
				BattleSideSpeedModifierApplication(
					targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
					speedModifier = BattleSideSpeedModifier(
						kind = modifierKind,
						turnsRemaining = rs.nullableInt("min_turns"),
					),
					chancePercent = rs.getInt("chance_percent"),
					requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun sideEntryHazardApplications(ruleId: Long?): List<BattleSideEntryHazardApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select
				fr.effect_policy as field_effect_policy,
				fr.max_layers,
				e.target_side,
				e.chance_percent,
				w.code as required_weather_code
			from battle_skill_field_effect e
			join battle_field_rule fr on fr.id = e.field_rule_id
			left join battle_weather_rule w on w.id = e.required_weather_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and fr.enabled = true
				and fr.effect_scope = 'SIDE'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val hazardKind = rs.getString("field_effect_policy").toBattleSideEntryHazardKind() ?: return@query null
				BattleSideEntryHazardApplication(
					targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
					hazard = BattleSideEntryHazard(
						kind = hazardKind,
						maxLayers = rs.nullableInt("max_layers") ?: hazardKind.defaultMaxLayers,
					),
					chancePercent = rs.getInt("chance_percent"),
					requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun fieldSpeedOrderApplications(ruleId: Long?): List<BattleFieldSpeedOrderApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select
				fr.effect_policy as field_effect_policy,
				fr.min_turns,
				e.chance_percent,
				w.code as required_weather_code
			from battle_skill_global_field_effect e
			join battle_field_rule fr on fr.id = e.field_rule_id
			left join battle_weather_rule w on w.id = e.required_weather_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and fr.enabled = true
				and fr.effect_scope = 'FIELD'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val speedOrderKind = rs.getString("field_effect_policy").toBattleFieldSpeedOrderKind() ?: return@query null
				BattleFieldSpeedOrderApplication(
					speedOrderEffect = BattleFieldSpeedOrderEffect(
						kind = speedOrderKind,
						turnsRemaining = rs.nullableInt("min_turns"),
					),
					chancePercent = rs.getInt("chance_percent"),
					requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun String?.toBattleSkillTargetScope(): BattleSkillTargetScope =
		when (this) {
			"all-opponents" -> BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS
			"all-adjacent-participants" -> BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS
			else -> BattleSkillTargetScope.SELECTED_TARGET
		}

	private fun String?.toBattleSkillHpEffects(): List<BattleSkillHpEffect> =
		when (this) {
			"drain-half-damage" -> listOf(
				BattleSkillHpEffect.DrainDamage(
					numerator = 1,
					denominator = 2,
				),
			)
			"drain-three-quarter-damage" -> listOf(
				BattleSkillHpEffect.DrainDamage(
					numerator = 3,
					denominator = 4,
				),
			)
			"recoil-third-damage" -> listOf(
				BattleSkillHpEffect.RecoilByDamageDealt(
					numerator = 1,
					denominator = 3,
				),
			)
			"self-heal-half-max-hp" -> listOf(
				BattleSkillHpEffect.SelfHealMaxHpFraction(
					numerator = 1,
					denominator = 2,
				),
			)
			"weather-self-heal-max-hp" -> listOf(
				BattleSkillHpEffect.SelfHealMaxHpByWeather(
					defaultFraction = BattleSkillHpEffect.HpFraction(1, 2),
					weatherFractions = mapOf(
						BattleWeather.SUN to BattleSkillHpEffect.HpFraction(2, 3),
						BattleWeather.RAIN to BattleSkillHpEffect.HpFraction(1, 4),
						BattleWeather.SANDSTORM to BattleSkillHpEffect.HpFraction(1, 4),
						BattleWeather.SNOW to BattleSkillHpEffect.HpFraction(1, 4),
					),
				),
			)
			"create-substitute-quarter-max-hp" -> listOf(
				BattleSkillHpEffect.CreateSubstitute(
					numerator = 1,
					denominator = 4,
				),
			)
			else -> emptyList()
		}

	private fun String?.toBattleFixedDamage(): BattleFixedDamage? =
		when (this) {
			"fixed-damage-20" -> BattleFixedDamage.FixedAmount(20)
			"fixed-damage-40" -> BattleFixedDamage.FixedAmount(40)
			"user-level-fixed-damage" -> BattleFixedDamage.UserLevel
			else -> null
		}

	private fun String?.toBattleProportionalDamage(): BattleProportionalDamage? =
		when (this) {
			"target-current-hp-half-damage" -> BattleProportionalDamage.TargetCurrentHpFraction(
				numerator = 1,
				denominator = 2,
			)
			else -> null
		}

	private fun String?.toBattleHpDerivedDamage(): BattleHpDerivedDamage? =
		when (this) {
			"target-hp-minus-user-hp-damage" -> BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp
			"user-current-hp-sacrifice-damage" -> BattleHpDerivedDamage.UserCurrentHpAndUserFaints
			else -> null
		}

	private fun String?.toBattleSkillEnvironmentEffects(): List<BattleSkillEnvironmentEffect> =
		when (this) {
			"set-terrain-electric" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.ELECTRIC))
			"set-terrain-grassy" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.GRASSY))
			"set-terrain-misty" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.MISTY))
			"set-terrain-psychic" -> listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.PSYCHIC))
			"set-weather-rain" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN))
			"set-weather-sandstorm" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.SANDSTORM))
			"set-weather-snow" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.SNOW))
			"set-weather-sun" -> listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.SUN))
			else -> emptyList()
		}

	private fun String.toBattleDamageClass(): BattleDamageClass =
		when (this) {
			"physical" -> BattleDamageClass.PHYSICAL
			"special" -> BattleDamageClass.SPECIAL
			"status" -> BattleDamageClass.STATUS
			else -> invalidValue("damageClass", "不支持的技能伤害分类: $this")
		}

	private fun String.toBattleWeather(): BattleWeather =
		when (this) {
			"harsh-sunlight" -> BattleWeather.SUN
			"rain" -> BattleWeather.RAIN
			"sandstorm" -> BattleWeather.SANDSTORM
			"snow" -> BattleWeather.SNOW
			else -> invalidValue("weatherRuleId", "不支持的天气规则: $this")
		}

	private fun String.toBattleSideConditionTarget(): BattleSideConditionTarget =
		when (this) {
			"USER_SIDE" -> BattleSideConditionTarget.USER_SIDE
			"TARGET_SIDE" -> BattleSideConditionTarget.TARGET_SIDE
			else -> invalidValue("targetSide", "不支持的场上效果作用侧: $this")
		}

	private fun String.toBattleSideDamageReductionKind(): BattleSideDamageReductionKind? =
		when (this) {
			"side-reflect" -> BattleSideDamageReductionKind.PHYSICAL
			"side-light-screen" -> BattleSideDamageReductionKind.SPECIAL
			"side-aurora-veil" -> BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE
			else -> null
		}

	private fun String.toBattleSideSpeedModifierKind(): BattleSideSpeedModifierKind? =
		when (this) {
			"side-tailwind" -> BattleSideSpeedModifierKind.TAILWIND
			else -> null
		}

	private fun String.toBattleSideEntryHazardKind(): BattleSideEntryHazardKind? =
		when (this) {
			"hazard-stealth-rock" -> BattleSideEntryHazardKind.STEALTH_ROCK
			"hazard-spikes" -> BattleSideEntryHazardKind.SPIKES
			"hazard-toxic-spikes" -> BattleSideEntryHazardKind.TOXIC_SPIKES
			"hazard-sticky-web" -> BattleSideEntryHazardKind.STICKY_WEB
			else -> null
		}

	private fun String.toBattleFieldSpeedOrderKind(): BattleFieldSpeedOrderKind? =
		when (this) {
			"field-trick-room" -> BattleFieldSpeedOrderKind.TRICK_ROOM
			else -> null
		}

	private fun String.toBattleEffectTarget(): BattleEffectTarget? =
		when (this) {
			"USER" -> BattleEffectTarget.USER
			"TARGET" -> BattleEffectTarget.TARGET
			// 技能执行器已经按实际命中的目标逐个调用附加效果；资料中的全体对手在这里映射为
			// “当前实际目标”，避免范围技能在效果层再次展开后重复结算。
			"ALL_OPPONENTS" -> BattleEffectTarget.TARGET
			else -> null
		}

	private fun String.toBattleStatStageOperationKind(): BattleStatStageOperationKind =
		when (this) {
			"CLEAR" -> BattleStatStageOperationKind.CLEAR
			"COPY" -> BattleStatStageOperationKind.COPY
			"SWAP" -> BattleStatStageOperationKind.SWAP
			"INVERT" -> BattleStatStageOperationKind.INVERT
			else -> invalidValue("operationKind", "不支持的能力阶级操作类型: $this")
		}

	private fun String.toBattleStatStageOperationTarget(): BattleStatStageOperationTarget =
		when (this) {
			"USER" -> BattleStatStageOperationTarget.USER
			"TARGET" -> BattleStatStageOperationTarget.TARGET
			"ALL_ACTIVE" -> BattleStatStageOperationTarget.ALL_ACTIVE
			else -> invalidValue("targetScope", "不支持的能力阶级操作目标: $this")
		}

	private fun String.toBattleMajorStatus(): BattleMajorStatus =
		when (this) {
			"burn" -> BattleMajorStatus.BURN
			"paralysis" -> BattleMajorStatus.PARALYSIS
			"poison" -> BattleMajorStatus.POISON
			"bad-poison" -> BattleMajorStatus.BAD_POISON
			"sleep" -> BattleMajorStatus.SLEEP
			"freeze" -> BattleMajorStatus.FREEZE
			else -> invalidValue("statusRuleId", "不支持的主要异常状态: $this")
		}

	private fun String.toBattleVolatileStatus(): BattleVolatileStatus =
		when (this) {
			"confusion" -> BattleVolatileStatus.CONFUSION
			"flinch" -> BattleVolatileStatus.FLINCH
			"heal-block" -> BattleVolatileStatus.HEAL_BLOCK
			"taunt" -> BattleVolatileStatus.TAUNT
			"disable" -> BattleVolatileStatus.DISABLE
			"torment" -> BattleVolatileStatus.TORMENT
			else -> invalidValue("statusRuleId", "不支持的临时状态: $this")
		}

	private fun String.toBattleStat(): BattleStat =
		when (this) {
			"attack" -> BattleStat.ATTACK
			"defense" -> BattleStat.DEFENSE
			"special-attack" -> BattleStat.SPECIAL_ATTACK
			"special-defense" -> BattleStat.SPECIAL_DEFENSE
			"speed" -> BattleStat.SPEED
			"accuracy" -> BattleStat.ACCURACY
			"evasion" -> BattleStat.EVASION
			else -> invalidValue("statId", "不支持的战斗能力项: $this")
		}

	private fun String.toBattleAbilityEffect(elementIds: Map<String, Long>): BattleAbilityEffect? =
		when (this) {
			"low-hp-grass-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
				elementId = elementIds.requiredElementId("grass"),
			)
			"low-hp-fire-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
				elementId = elementIds.requiredElementId("fire"),
			)
			"low-hp-water-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
				elementId = elementIds.requiredElementId("water"),
			)
			"low-hp-bug-boost" -> BattleAbilityEffect.LowHpElementDamageBoost(
				elementId = elementIds.requiredElementId("bug"),
			)
			"element-dragon-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
				elementIds = setOf(elementIds.requiredElementId("dragon")),
				multiplier = 1.5,
			)
			"element-rock-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
				elementIds = setOf(elementIds.requiredElementId("rock")),
				multiplier = 1.5,
			)
			"element-steel-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
				elementIds = setOf(elementIds.requiredElementId("steel")),
				multiplier = 1.5,
			)
			"element-electric-damage-boost" -> BattleAbilityEffect.ElementSkillDamageBoost(
				elementIds = setOf(elementIds.requiredElementId("electric")),
				multiplier = 1.3,
			)
			"weather-sandstorm-rock-ground-steel-damage-boost" -> BattleAbilityEffect.WeatherElementDamageBoost(
				weather = BattleWeather.SANDSTORM,
				elementIds = setOf(
					elementIds.requiredElementId("rock"),
					elementIds.requiredElementId("ground"),
					elementIds.requiredElementId("steel"),
				),
			)
			"punch-based-skill-damage-boost" -> BattleAbilityEffect.PunchBasedSkillDamageBoost()
			"slicing-based-skill-damage-boost" -> BattleAbilityEffect.SlicingBasedSkillDamageBoost()
			"contact-based-skill-damage-boost" -> BattleAbilityEffect.ContactBasedSkillDamageBoost()
			"sound-based-skill-damage-boost" -> BattleAbilityEffect.SoundBasedSkillDamageBoost()
			"sound-based-skill-damage-reduction" -> BattleAbilityEffect.SoundBasedSkillDamageReduction()
			"super-effective-damage-reduction" -> BattleAbilityEffect.SuperEffectiveDamageReduction()
			"full-hp-damage-reduction" -> BattleAbilityEffect.FullHpDamageReduction()
			"special-damage-reduction" -> BattleAbilityEffect.DamageClassDamageReduction(
				damageClasses = setOf(BattleDamageClass.SPECIAL),
			)
			"defense-stat-double" -> BattleAbilityEffect.DefendingStatMultiplier(
				stat = BattleStat.DEFENSE,
				multiplier = 2.0,
			)
			"grassy-terrain-defense-stat-boost" -> BattleAbilityEffect.DefendingStatMultiplier(
				stat = BattleStat.DEFENSE,
				multiplier = 1.5,
				requiredTerrain = BattleTerrain.GRASSY,
			)
			"attack-stat-double" -> BattleAbilityEffect.AttackingStatMultiplier(
				stat = BattleStat.ATTACK,
				multiplier = 2.0,
			)
			"major-status-attack-stat-boost-ignore-burn-drop" -> BattleAbilityEffect.AttackingStatMultiplier(
				stat = BattleStat.ATTACK,
				multiplier = 1.5,
				requiresMajorStatus = true,
				ignoresBurnAttackReduction = true,
			)
			"same-element-bonus-double" -> BattleAbilityEffect.SameElementBonusOverride(
				multiplier = 2.0,
			)
			// 现代接触反制类特性按 30% 附加主要异常状态；当前种子里只有麻痹变体。
			"contact-paralysis" -> BattleAbilityEffect.ContactStatusOnAttacker(
				status = BattleMajorStatus.PARALYSIS,
				chancePercent = 30,
			)
			"switch-in-opponents-attack-down" -> BattleAbilityEffect.SwitchInStatStageChange(
				stat = BattleStat.ATTACK,
				stageDelta = -1,
			)
			"switch-in-weather-rain" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.RAIN)
			"switch-in-weather-sandstorm" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SANDSTORM)
			"switch-in-weather-snow" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SNOW)
			"switch-in-weather-sun" -> BattleAbilityEffect.SwitchInWeatherChange(BattleWeather.SUN)
			"switch-in-terrain-electric" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.ELECTRIC)
			"switch-in-terrain-grassy" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.GRASSY)
			"switch-in-terrain-misty" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.MISTY)
			"switch-in-terrain-psychic" -> BattleAbilityEffect.SwitchInTerrainChange(BattleTerrain.PSYCHIC)
			"weather-speed-rain" -> BattleAbilityEffect.WeatherSpeedMultiplier(
				weather = BattleWeather.RAIN,
				multiplier = 2.0,
			)
			"weather-speed-sandstorm" -> BattleAbilityEffect.WeatherSpeedMultiplier(
				weather = BattleWeather.SANDSTORM,
				multiplier = 2.0,
			)
			"weather-speed-snow" -> BattleAbilityEffect.WeatherSpeedMultiplier(
				weather = BattleWeather.SNOW,
				multiplier = 2.0,
			)
			"weather-speed-sun" -> BattleAbilityEffect.WeatherSpeedMultiplier(
				weather = BattleWeather.SUN,
				multiplier = 2.0,
			)
			"terrain-speed-electric" -> BattleAbilityEffect.TerrainSpeedMultiplier(
				terrain = BattleTerrain.ELECTRIC,
				multiplier = 2.0,
			)
			"weather-heal-rain" -> BattleAbilityEffect.WeatherEndTurnHeal(
				weathers = setOf(BattleWeather.RAIN),
				healDenominator = 16,
			)
			"weather-heal-snow" -> BattleAbilityEffect.WeatherEndTurnHeal(
				weathers = setOf(BattleWeather.SNOW),
				healDenominator = 16,
			)
			"critical-hit-immunity" -> BattleAbilityEffect.CriticalHitImmunity
			"full-hp-fatal-damage-survival" -> BattleAbilityEffect.SurviveFatalDamageAtFullHp()
			"indirect-damage-immunity" -> BattleAbilityEffect.IndirectDamageImmunity
			"weather-damage-immunity-sandstorm" -> BattleAbilityEffect.WeatherDamageImmunity(
				weathers = setOf(BattleWeather.SANDSTORM),
			)
			"skill-recoil-damage-immunity" -> BattleAbilityEffect.SkillRecoilDamageImmunity
			"ignore-opponent-accuracy-stat-stages" -> BattleAbilityEffect.IgnoreOpponentAccuracyStatStages
			"ignore-opponent-damage-stat-stages" -> BattleAbilityEffect.IgnoreOpponentDamageStatStages
			"ignore-target-ability-effects" -> BattleAbilityEffect.IgnoreTargetAbilityEffects
			"sound-based-skill-immunity" -> BattleAbilityEffect.SoundBasedSkillImmunity
			"side-priority-move-immunity" -> BattleAbilityEffect.PriorityMoveImmunityForSide()
			"status-skill-priority-boost" -> BattleAbilityEffect.StatusSkillPriorityBoost()
			"element-electric-absorb-heal" -> BattleAbilityEffect.ElementSkillAbsorbHeal(
				elementId = elementIds.requiredElementId("electric"),
			)
			"element-water-absorb-heal" -> BattleAbilityEffect.ElementSkillAbsorbHeal(
				elementId = elementIds.requiredElementId("water"),
			)
			"element-ground-absorb-heal" -> BattleAbilityEffect.ElementSkillAbsorbHeal(
				elementId = elementIds.requiredElementId("ground"),
			)
			"element-electric-absorb-speed-up" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
				elementId = elementIds.requiredElementId("electric"),
				stat = BattleStat.SPEED,
				stageDelta = 1,
			)
			"element-grass-absorb-attack-up" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
				elementId = elementIds.requiredElementId("grass"),
				stat = BattleStat.ATTACK,
				stageDelta = 1,
			)
			"element-fire-absorb-defense-up-two" -> BattleAbilityEffect.ElementSkillAbsorbStatStage(
				elementId = elementIds.requiredElementId("fire"),
				stat = BattleStat.DEFENSE,
				stageDelta = 2,
			)
			// 接地免疫会写入 BattleParticipant.grounded，不作为独立效果返回。
			"ground-immunity" -> null
			else -> null
		}

	private fun String.toBattleItemEffect(elementIds: Map<String, Long>): BattleItemEffect? =
		when (this) {
			"leftovers-heal" -> BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)
			"life-orb-boost-and-recoil" -> BattleItemEffect.DamageBoostWithRecoil(
				multiplier = 1.3,
				recoilDenominator = 10,
			)
			"damage-dealt-heal-eighth" -> BattleItemEffect.DamageDealtHeal(healDenominator = 8)
			"damage-class-power-boost-physical" -> BattleItemEffect.DamageClassPowerBoost(
				damageClasses = setOf(BattleDamageClass.PHYSICAL),
				multiplier = 1.1,
			)
			"damage-class-power-boost-special" -> BattleItemEffect.DamageClassPowerBoost(
				damageClasses = setOf(BattleDamageClass.SPECIAL),
				multiplier = 1.1,
			)
			"super-effective-damage-boost" -> BattleItemEffect.SuperEffectiveDamageBoost(multiplier = 1.2)
			"element-damage-boost-normal" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("normal"),
				multiplier = 1.2,
			)
			"element-damage-boost-fighting" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("fighting"),
				multiplier = 1.2,
			)
			"element-damage-boost-flying" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("flying"),
				multiplier = 1.2,
			)
			"element-damage-boost-poison" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("poison"),
				multiplier = 1.2,
			)
			"element-damage-boost-ground" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("ground"),
				multiplier = 1.2,
			)
			"element-damage-boost-rock" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("rock"),
				multiplier = 1.2,
			)
			"element-damage-boost-bug" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("bug"),
				multiplier = 1.2,
			)
			"element-damage-boost-ghost" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("ghost"),
				multiplier = 1.2,
			)
			"element-damage-boost-steel" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("steel"),
				multiplier = 1.2,
			)
			"element-damage-boost-fire" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("fire"),
				multiplier = 1.2,
			)
			"element-damage-boost-water" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("water"),
				multiplier = 1.2,
			)
			"element-damage-boost-grass" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("grass"),
				multiplier = 1.2,
			)
			"element-damage-boost-electric" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("electric"),
				multiplier = 1.2,
			)
			"element-damage-boost-psychic" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("psychic"),
				multiplier = 1.2,
			)
			"element-damage-boost-ice" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("ice"),
				multiplier = 1.2,
			)
			"element-damage-boost-dragon" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("dragon"),
				multiplier = 1.2,
			)
			"element-damage-boost-dark" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("dark"),
				multiplier = 1.2,
			)
			"element-damage-boost-fairy" -> BattleItemEffect.ElementDamageBoost(
				elementId = elementIds.requiredElementId("fairy"),
				multiplier = 1.2,
			)
			"element-damage-reduction-normal" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("normal"),
				multiplier = 0.5,
				requiresSuperEffective = false,
			)
			"element-damage-reduction-fighting" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("fighting"),
				multiplier = 0.5,
			)
			"element-damage-reduction-flying" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("flying"),
				multiplier = 0.5,
			)
			"element-damage-reduction-poison" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("poison"),
				multiplier = 0.5,
			)
			"element-damage-reduction-ground" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("ground"),
				multiplier = 0.5,
			)
			"element-damage-reduction-rock" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("rock"),
				multiplier = 0.5,
			)
			"element-damage-reduction-bug" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("bug"),
				multiplier = 0.5,
			)
			"element-damage-reduction-ghost" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("ghost"),
				multiplier = 0.5,
			)
			"element-damage-reduction-steel" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("steel"),
				multiplier = 0.5,
			)
			"element-damage-reduction-fire" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("fire"),
				multiplier = 0.5,
			)
			"element-damage-reduction-water" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("water"),
				multiplier = 0.5,
			)
			"element-damage-reduction-grass" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("grass"),
				multiplier = 0.5,
			)
			"element-damage-reduction-electric" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("electric"),
				multiplier = 0.5,
			)
			"element-damage-reduction-psychic" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("psychic"),
				multiplier = 0.5,
			)
			"element-damage-reduction-ice" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("ice"),
				multiplier = 0.5,
			)
			"element-damage-reduction-dragon" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("dragon"),
				multiplier = 0.5,
			)
			"element-damage-reduction-dark" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("dark"),
				multiplier = 0.5,
			)
			"element-damage-reduction-fairy" -> BattleItemEffect.ElementDamageReduction(
				elementId = elementIds.requiredElementId("fairy"),
				multiplier = 0.5,
			)
			"small-berry-heal" -> BattleItemEffect.LowHpHeal(fixedHealAmount = 10)
			"medium-berry-heal" -> BattleItemEffect.LowHpHeal(healDenominator = 4)
			"choice-speed-lock" -> BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)
			"major-status-cure-paralysis" -> BattleItemEffect.MajorStatusCure(
				statuses = setOf(BattleMajorStatus.PARALYSIS),
			)
			"major-status-cure-sleep" -> BattleItemEffect.MajorStatusCure(
				statuses = setOf(BattleMajorStatus.SLEEP),
			)
			"major-status-cure-poison" -> BattleItemEffect.MajorStatusCure(
				statuses = setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
			)
			"major-status-cure-burn" -> BattleItemEffect.MajorStatusCure(
				statuses = setOf(BattleMajorStatus.BURN),
			)
			"major-status-cure-freeze" -> BattleItemEffect.MajorStatusCure(
				statuses = setOf(BattleMajorStatus.FREEZE),
			)
			"major-status-cure-all" -> BattleItemEffect.MajorStatusCure(
				statuses = setOf(
					BattleMajorStatus.BURN,
					BattleMajorStatus.PARALYSIS,
					BattleMajorStatus.POISON,
					BattleMajorStatus.BAD_POISON,
					BattleMajorStatus.SLEEP,
					BattleMajorStatus.FREEZE,
				),
			)
			"volatile-status-cure-confusion" -> BattleItemEffect.VolatileStatusCure(
				statuses = setOf(BattleVolatileStatus.CONFUSION),
			)
			"charge-skip-once" -> BattleItemEffect.ChargeSkipOnce()
			"consumable-full-hp-fatal-damage-survival" -> BattleItemEffect.SurviveFatalDamageAtFullHp()
			"side-condition-duration-screen" -> BattleItemEffect.SideDamageReductionDurationExtension(
				kinds = setOf(
					BattleSideDamageReductionKind.PHYSICAL,
					BattleSideDamageReductionKind.SPECIAL,
					BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE,
				),
				turnsRemaining = 8,
			)
			"weather-duration-rain" -> BattleItemEffect.WeatherDurationExtension(
				weathers = setOf(BattleWeather.RAIN),
				turnsRemaining = 8,
			)
			"weather-duration-sandstorm" -> BattleItemEffect.WeatherDurationExtension(
				weathers = setOf(BattleWeather.SANDSTORM),
				turnsRemaining = 8,
			)
			"weather-duration-snow" -> BattleItemEffect.WeatherDurationExtension(
				weathers = setOf(BattleWeather.SNOW),
				turnsRemaining = 8,
			)
			"weather-duration-sun" -> BattleItemEffect.WeatherDurationExtension(
				weathers = setOf(BattleWeather.SUN),
				turnsRemaining = 8,
			)
			"terrain-duration-all" -> BattleItemEffect.TerrainDurationExtension(
				terrains = setOf(
					BattleTerrain.ELECTRIC,
					BattleTerrain.GRASSY,
					BattleTerrain.MISTY,
					BattleTerrain.PSYCHIC,
				),
				turnsRemaining = 8,
			)
			else -> null
		}

	private fun Map<String, Int>.requiredBaseStat(code: String): Int =
		this[code] ?: notFound("creatureId", "成员基础能力缺失: $code")

	private fun Int.toRuntimeHp(level: Int): Int =
		(((2 * this + DEFAULT_INDIVIDUAL_VALUE + DEFAULT_EFFORT_VALUE / 4) * level) / 100) + level + 10

	private fun Int.toRuntimeBattleStat(level: Int): Int =
		(((2 * this + DEFAULT_INDIVIDUAL_VALUE + DEFAULT_EFFORT_VALUE / 4) * level) / 100) + 5

	private fun ResultSet.nullableInt(column: String): Int? {
		val value = getInt(column)
		return if (wasNull()) null else value
	}

	private fun ResultSet.nullableLong(column: String): Long? {
		val value = getLong(column)
		return if (wasNull()) null else value
	}

	private fun ResultSet.nullableBoolean(column: String): Boolean? {
		val value = getBoolean(column)
		return if (wasNull()) null else value
	}

	private fun BattlePreparationViolation.toResponse(): BattlePreparationViolationResponse =
		BattlePreparationViolationResponse(
			code = code,
			sideId = sideId,
			actorId = actorId,
			resourceId = resourceId,
			message = message,
		)

	private fun BattleActionViolation.toResponse(): BattleActionViolationResponse =
		BattleActionViolationResponse(
			code = code,
			actorId = actorId,
			targetActorId = targetActorId,
			resourceId = resourceId,
			message = message,
		)
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

data class BattleCreatureRuntimeProfile(
	val maxHp: Int,
	val attack: Int,
	val defense: Int,
	val specialAttack: Int,
	val specialDefense: Int,
	val speed: Int,
	val elementIds: Set<Long>,
)

private data class SkillRuntimeRow(
	val skillId: Long,
	val name: String,
	val elementId: Long,
	val damageClassCode: String,
	val power: Int?,
	val accuracy: Int?,
	val pp: Int,
	val priority: Int,
	val ruleId: Long?,
	val effectPolicy: String?,
	val targetPolicy: String?,
	val minHits: Int?,
	val maxHits: Int?,
	val criticalHitStage: Int?,
	val makesContact: Boolean?,
	val affectedByProtect: Boolean?,
	val protectsUser: Boolean?,
	val thawsUserBeforeMove: Boolean?,
	val soundBased: Boolean?,
	val powderBased: Boolean?,
	val punchBased: Boolean?,
	val slicingBased: Boolean?,
	val weakenedByGrassyTerrain: Boolean?,
	val chargesBeforeUse: Boolean?,
	val rechargesAfterUse: Boolean?,
	val lockMoveTurnsMin: Int?,
	val lockMoveTurnsMax: Int?,
	val confusesUserAfterLock: Boolean?,
	val forceTargetSwitch: Boolean?,
)
