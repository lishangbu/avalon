package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import kotlin.math.floor

/**
 * 普通伤害公式中的天气和场地修正集合。
 *
 * 天气/场地会同时影响不同公式位置：雪景和沙暴修改防御侧能力值，晴天/下雨和青草场地修改最终伤害倍率。
 * 这两类修正不能简单合并成一个“环境倍率”，因为能力值修正会先参与基础伤害整数除法，最终倍率则在基础伤害
 * 之后统一叠乘。集中在本类中可以让调用点显式表达取整位置，同时避免 [BattleDamageCalculator] 被环境分支淹没。
 *
 * 本类只处理“普通伤害公式中的环境数值修正”。场地带来的睡眠免疫、先制技能阻挡，天气带来的回合末伤害、
 * 回复和持续时间，都属于战斗状态机其它阶段，不应放进伤害公式。
 */
internal class BattleDamageEnvironmentModifiers {
	/**
	 * 计算天气对防御侧物防的修正。
	 *
	 * 现代雪景会让冰属性成员的物防按 1.5 倍参与普通伤害公式。该修正在能力阶级之后应用；
	 * 如果规则快照缺少冰属性 ID，则不应用该修正，避免引擎猜测资料库编号。
	 */
	fun physicalDefenseAfterWeather(request: BattleDamageRequest, stagedDefense: Int): Int =
		if (request.environment.weather == BattleWeather.SNOW && request.defender.hasElement(request.rules.elementId("ice"))) {
			floor(stagedDefense * WEATHER_DEFENSE_BOOST_MULTIPLIER).toInt().coerceAtLeast(1)
		} else {
			stagedDefense
		}

	/**
	 * 计算天气对防御侧特防的修正。
	 *
	 * 现代沙暴会让岩属性成员的特防按 1.5 倍参与普通伤害公式。该修正在能力阶级之后应用；
	 * 如果规则快照缺少岩属性 ID，则不应用该修正。
	 */
	fun specialDefenseAfterWeather(request: BattleDamageRequest, stagedSpecialDefense: Int): Int =
		if (request.environment.weather == BattleWeather.SANDSTORM && request.defender.hasElement(request.rules.elementId("rock"))) {
			floor(stagedSpecialDefense * WEATHER_DEFENSE_BOOST_MULTIPLIER).toInt().coerceAtLeast(1)
		} else {
			stagedSpecialDefense
		}

	/**
	 * 计算天气对火/水属性普通伤害的倍率。
	 *
	 * 元素 ID 来自规则快照，避免引擎硬编码资料库编号。若快照缺少对应元素 ID，天气不会修改伤害。
	 * 晴天和下雨对火/水伤害的互相增强/削弱发生在最终倍率阶段；雪景和沙暴对防御侧能力的修正在上面的
	 * 能力值函数中处理，因为它们会改变基础伤害整数公式的输入，不能与最终倍率混在一起。
	 */
	fun weatherDamageMultiplier(request: BattleDamageRequest): Double =
		when (request.environment.weather) {
			BattleWeather.SUN -> when (request.skill.effectiveElementId(request.environment.weather)) {
				request.rules.elementId("fire") -> 1.5
				request.rules.elementId("water") -> 0.5
				else -> 1.0
			}
			BattleWeather.RAIN -> when (request.skill.effectiveElementId(request.environment.weather)) {
				request.rules.elementId("water") -> 1.5
				request.rules.elementId("fire") -> 0.5
				else -> 1.0
			}
			BattleWeather.NONE,
			BattleWeather.SANDSTORM,
			BattleWeather.SNOW -> 1.0
		}

	/**
	 * 计算场地对普通伤害的倍率。
	 *
	 * 现代场地的伤害侧效果按“攻击方是否接地”和“目标是否接地”分开判断：青草、电气、精神场地会增强接地使用者
	 * 发出的对应属性技能；薄雾场地会削弱命中接地目标的龙属性技能；青草场地还会削弱明确带震动标签的地面技能。
	 * 场地带来的状态免疫、先制阻挡和回合末回复不属于伤害公式。
	 */
	fun terrainDamageMultiplier(request: BattleDamageRequest): Double {
		val skillElementId = request.skill.effectiveElementId(request.environment.weather)
		return when (request.environment.terrain) {
			BattleTerrain.GRASSY -> {
				val grassBoost = if (
					request.attacker.grounded &&
					skillElementId == request.rules.elementId("grass")
				) {
					TERRAIN_SAME_ELEMENT_DAMAGE_MULTIPLIER
				} else {
					1.0
				}
				val groundMoveReduction = if (request.defender.grounded && request.skill.weakenedByGrassyTerrain) {
					GRASSY_TERRAIN_GROUND_MOVE_MULTIPLIER
				} else {
					1.0
				}
				grassBoost * groundMoveReduction
			}
			BattleTerrain.ELECTRIC ->
				if (request.attacker.grounded && skillElementId == request.rules.elementId("electric")) {
					TERRAIN_SAME_ELEMENT_DAMAGE_MULTIPLIER
				} else {
					1.0
				}
			BattleTerrain.PSYCHIC ->
				if (request.attacker.grounded && skillElementId == request.rules.elementId("psychic")) {
					TERRAIN_SAME_ELEMENT_DAMAGE_MULTIPLIER
				} else {
					1.0
				}
			BattleTerrain.MISTY ->
				if (request.defender.grounded && skillElementId == request.rules.elementId("dragon")) {
					MISTY_TERRAIN_DRAGON_DAMAGE_MULTIPLIER
				} else {
					1.0
				}
			BattleTerrain.NONE -> 1.0
		}
	}

	private fun BattleParticipant.hasElement(elementId: Long?): Boolean =
		elementId != null && elementId in elementIds

	private companion object {
		private const val TERRAIN_SAME_ELEMENT_DAMAGE_MULTIPLIER = 1.3
		private const val GRASSY_TERRAIN_GROUND_MOVE_MULTIPLIER = 0.5
		private const val MISTY_TERRAIN_DRAGON_DAMAGE_MULTIPLIER = 0.5
		private const val WEATHER_DEFENSE_BOOST_MULTIPLIER = 1.5
	}
}
