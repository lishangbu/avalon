package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.notFound
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 战斗核心资料运行时读取器。
 *
 * 本类只读取纯资料侧的稳定事实：核心属性 ID、成员属性集合和成员基础能力值。它不读取赛制限制，也不读取技能、
 * 特性、道具规则。这样能力值公式或资料字段扩展时，不会牵动技能规则 SQL；反过来新增技能子表也不会污染成员画像
 * 的读取逻辑。
 *
 * 成员最终能力值由基础能力、等级和 [BattleParticipantStatConfig] 共同计算。该读取器仍然只返回最终画像，
 * 不把个体值、努力值或性格概念泄漏给 battle-engine；这样纯引擎在回合结算时只处理确定事实，不需要反向理解
 * 请求 DTO 或资料库字段。
 */
@Component
class BattleCoreRuntimeLookup(
	private val jdbcTemplate: JdbcTemplate,
) {
	/**
	 * 读取引擎基础规则需要识别的核心属性 ID。
	 *
	 * 这里按稳定 code 而不是硬编码资料 ID 装配快照，保持纯引擎不依赖资料库编号。查询范围覆盖现代主系列 18 个
	 * 常规属性，因为伤害、天气、主要异常状态、属性限定特性和属性伤害提升道具都可能需要把资料 policy 翻译成
	 * 引擎可执行的属性 ID。
	 */
	fun coreElementIds(): Map<String, Long> =
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

	/**
	 * 读取当前资料库中的属性克制表。
	 *
	 * 资料表同时保存 `*_to` 和 `*_from` 两类关系，二者表达的是同一张表的两个方向。战斗引擎计算伤害时只需要
	 * “攻击属性打到防御属性”的方向，因此这里只读取 `double_damage_to`、`half_damage_to` 和 `no_damage_to`。
	 * 未出现的组合由 [ElementEffectivenessChart] 按中性倍率 1.0 处理。
	 */
	fun elementChart(): ElementEffectivenessChart {
		val multiplierBySource = jdbcTemplate.query(
			"""
			select r.source_element_id, r.target_element_id, r.relation_type
			from game_element_damage_relation r
			join game_element source_element on source_element.id = r.source_element_id
			join game_element target_element on target_element.id = r.target_element_id
			where source_element.enabled = true
				and target_element.enabled = true
				and r.relation_type in ('double_damage_to', 'half_damage_to', 'no_damage_to')
			order by r.source_element_id, r.target_element_id, r.id
			""".trimIndent(),
		) { rs, _ ->
			ElementDamageRelation(
				sourceElementId = rs.getLong("source_element_id"),
				targetElementId = rs.getLong("target_element_id"),
				multiplier = rs.getString("relation_type").toElementDamageMultiplier(),
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
		// 体重必须在进入引擎前固定到快照中；后续低踢、打草结、重磅冲撞、高温重压类规则都只读该快照值。
		val creatureWeight = jdbcTemplate.query(
			"""
			select weight
			from game_creature
			where id = ? and enabled = true
			""".trimIndent(),
			{ rs, _ -> rs.getInt("weight") },
			creatureId,
		).singleOrNull() ?: notFound("creatureId", "成员体重资料不存在: $creatureId")
		if (creatureWeight <= 0) {
			notFound("creatureId", "成员体重资料无效: $creatureId")
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

	private data class ElementDamageRelation(
		val sourceElementId: Long,
		val targetElementId: Long,
		val multiplier: Double,
	)
}
