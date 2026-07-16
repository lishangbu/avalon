package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.gamedata.entity.GameCreature
import io.github.lishangbu.gamedata.entity.GameCreatureElement
import io.github.lishangbu.gamedata.entity.GameCreatureStat
import io.github.lishangbu.gamedata.entity.GameElement
import io.github.lishangbu.gamedata.entity.GameElementDamageRelations
import io.github.lishangbu.gamedata.entity.GameEvolutionNodes
import io.github.lishangbu.gamedata.entity.GameStat
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.elementId
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.relationType
import io.github.lishangbu.gamedata.entity.parentSpeciesId
import io.github.lishangbu.gamedata.entity.slotOrder
import io.github.lishangbu.gamedata.entity.sourceElementId
import io.github.lishangbu.gamedata.entity.targetElementId
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Component

/**
 * 战斗核心资料运行时读取器。
 *
 * 本类只读取纯资料侧的稳定事实：核心属性 ID、成员属性集合和成员基础能力值。它不读取赛制限制，也不读取技能、
 * 特性、道具规则。这样能力值公式或资料字段扩展时，不会牵动技能规则查询；反过来新增技能子表也不会污染成员画像
 * 的读取逻辑。
 *
 * 成员最终能力值由基础能力、等级和 [BattleParticipantStatConfig] 共同计算。该读取器仍然只返回最终画像，
 * 不把个体值、努力值或性格概念泄漏给 battle-engine；这样纯引擎在回合结算时只处理确定事实，不需要反向理解
 * 请求 DTO 或资料库字段。
 */
@Component
class BattleCoreRuntimeLookup(
	private val sqlClient: KSqlClient,
) {
	/**
	 * 读取引擎基础规则需要识别的核心属性 ID。
	 *
	 * 这里按稳定 code 而不是硬编码资料 ID 装配快照，保持纯引擎不依赖资料库编号。查询范围覆盖现代主系列 18 个
	 * 常规属性，因为伤害、天气、主要异常状态、属性限定特性和属性伤害提升道具都可能需要把资料 policy 翻译成
	 * 引擎可执行的属性 ID。
	 */
	fun coreElementIds(): Map<String, Long> =
		sqlClient.executeQuery(GameElement::class) {
			where(table.code valueIn CORE_ELEMENT_CODES)
			select(table)
		}.associate { element -> element.code to element.id }

	/**
	 * 读取当前资料库中的属性克制表。
	 *
	 * 资料表同时保存 `*_to` 和 `*_from` 两类关系，二者表达的是同一张表的两个方向。战斗引擎计算伤害时只需要
	 * “攻击属性打到防御属性”的方向，因此这里只读取 `double_damage_to`、`half_damage_to` 和 `no_damage_to`。
	 * 未出现的组合由 [ElementEffectivenessChart] 按中性倍率 1.0 处理。
	 */
	fun elementChart(): ElementEffectivenessChart {
		val enabledElementIds = sqlClient.executeQuery(GameElement::class) {
			where(table.enabled eq true)
			select(table.id)
		}
		val multiplierBySource = sqlClient.executeQuery(GameElementDamageRelations::class) {
			where(table.sourceElementId valueIn enabledElementIds)
			where(table.targetElementId valueIn enabledElementIds)
			where(table.relationType valueIn DAMAGE_RELATION_TYPES)
			orderBy(table.sourceElementId, table.targetElementId, table.id)
			select(table)
		}.map { relation ->
			ElementDamageRelation(
				sourceElementId = relation.sourceElementId,
				targetElementId = relation.targetElementId,
				multiplier = relation.relationType.toElementDamageMultiplier(),
			)
		}.groupBy { it.sourceElementId }
			.mapValues { (_, relations) ->
				relations.associate { relation -> relation.targetElementId to relation.multiplier }
			}

		return ElementEffectivenessChart(multiplierBySource)
	}

	/**
	 * 按基础成员资料和等级读取引擎需要的能力值与属性集合。
	 *
	 * 返回值可以直接放入 [io.github.lishangbu.battleengine.model.BattleParticipant]。如果成员缺属性或缺基础能力，
	 * 这里直接抛出结构化 API 异常，避免引擎拿到半成品画像后在更深处出现难定位的计算错误。
	 */
	fun creatureRuntimeProfile(
		creatureId: Long,
		level: Int,
		statConfig: BattleParticipantStatConfig = BattleParticipantStatConfig.DEFAULT,
	): BattleCreatureRuntimeProfile {
		if (creatureId <= 0) {
			invalidValue("creatureId", "creatureId 必须大于 0")
		}
		if (level !in 1..100) {
			invalidValue("level", "level 必须在 1 到 100 之间")
		}
		val elementIds = sqlClient.executeQuery(GameCreatureElement::class) {
			where(table.creatureId eq creatureId)
			orderBy(table.slotOrder, table.id)
			select(table.elementId)
		}
		if (elementIds.isEmpty()) {
			notFound("creatureId", "成员属性资料不存在: $creatureId")
		}
		// 体重必须在进入引擎前固定到快照中；后续低踢、打草结、重磅冲撞、高温重压类规则都只读该快照值。
		val creature = sqlClient.findById(GameCreature::class, creatureId)
			?.takeIf { creature -> creature.enabled == true }
			?: notFound("creatureId", "成员体重资料不存在: $creatureId")
		val creatureWeight = creature.weight ?: notFound("creatureId", "成员体重资料不存在: $creatureId")
		if (creatureWeight <= 0) {
			notFound("creatureId", "成员体重资料无效: $creatureId")
		}
		val creatureStats = sqlClient.executeQuery(GameCreatureStat::class) {
			where(table.creatureId eq creatureId)
			select(table)
		}
		if (creatureStats.isEmpty()) {
			notFound("creatureId", "成员能力资料不存在: $creatureId")
		}
		val statsById = sqlClient.findMapByIds(GameStat::class, creatureStats.map { stat -> stat.statId })
		val baseStats = creatureStats.mapNotNull { creatureStat ->
			statsById[creatureStat.statId]?.code?.let { code -> code to creatureStat.baseValue }
		}.toMap()
		val canEvolve = sqlClient.executeQuery(GameEvolutionNodes::class) {
			where(table.parentSpeciesId eq creature.speciesId)
			select(table.id)
		}.isNotEmpty()
		return BattleCreatureRuntimeProfile(
			maxHp = baseStats.requiredBaseStat("hp").toRuntimeHp(level, statConfig, "hp"),
			attack = baseStats.requiredBaseStat("attack").toRuntimeBattleStat(level, statConfig, "attack"),
			defense = baseStats.requiredBaseStat("defense").toRuntimeBattleStat(level, statConfig, "defense"),
			specialAttack = baseStats.requiredBaseStat("special-attack")
				.toRuntimeBattleStat(level, statConfig, "special-attack"),
			specialDefense = baseStats.requiredBaseStat("special-defense")
				.toRuntimeBattleStat(level, statConfig, "special-defense"),
			speed = baseStats.requiredBaseStat("speed").toRuntimeBattleStat(level, statConfig, "speed"),
			weight = creatureWeight,
			elementIds = elementIds.toSet(),
			canEvolve = canEvolve,
		)
	}

	private fun Map<String, Int>.requiredBaseStat(code: String): Int =
		this[code] ?: notFound("creatureId", "成员基础能力缺失: $code")

	private fun Int.toRuntimeHp(
		level: Int,
		statConfig: BattleParticipantStatConfig,
		statCode: String,
	): Int =
		(((2 * this + statConfig.individualValue(statCode) + statConfig.effortValue(statCode) / 4) * level) / 100) +
			level + 10

	private fun Int.toRuntimeBattleStat(
		level: Int,
		statConfig: BattleParticipantStatConfig,
		statCode: String,
	): Int {
		val neutralValue =
			(((2 * this + statConfig.individualValue(statCode) + statConfig.effortValue(statCode) / 4) * level) / 100) + 5
		return statConfig.applyNature(statCode, neutralValue)
	}

	private fun String.toElementDamageMultiplier(): Double =
		when (this) {
			"double_damage_to" -> 2.0
			"half_damage_to" -> 0.5
			"no_damage_to" -> 0.0
			else -> invalidValue("relationType", "不支持的属性克制关系: $this")
		}

	/**
	 * 从资料库读取到的一条属性克制关系。
	 *
	 * 运行时快照装配会把多条关系折叠成 [io.github.lishangbu.battleengine.model.ElementEffectivenessChart]，引擎只读取
	 * 最终克制倍率表。这个内部行模型只存在于查询层，用来避免把数据库里的 `relation_type` 文本继续传入纯引擎。
	 */
	private data class ElementDamageRelation(
		val sourceElementId: Long,
		val targetElementId: Long,
		val multiplier: Double,
	)

	companion object {
		private val CORE_ELEMENT_CODES = setOf(
			"normal", "fighting", "flying", "poison", "ground", "rock",
			"bug", "ghost", "steel", "fire", "water", "grass",
			"electric", "psychic", "ice", "dragon", "dark", "fairy",
		)
		private val DAMAGE_RELATION_TYPES = setOf("double_damage_to", "half_damage_to", "no_damage_to")
	}
}
