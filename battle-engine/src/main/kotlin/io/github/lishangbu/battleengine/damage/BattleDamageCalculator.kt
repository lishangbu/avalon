package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import kotlin.math.floor

/**
 * 现代普通伤害公式计算器。
 *
 * 该实现覆盖第一阶段 MVP 需要的基础公式：等级、威力、攻击/防御、随机浮动、击中要害、
 * 范围目标修正、属性一致加成和属性克制。它暂不处理护盾和其它复杂倍率；这些倍率会在后续 hook
 * 管线中以结构化 modifier 继续追加，并由对照 fixture 分别验证。
 *
 * 取整规则按主系列常见公开公式建模：基础伤害部分在整数除法中逐步截断，最终倍率组合后向下取整。
 * 如果属性克制倍率为 0，最终伤害为 0；否则普通命中造成的最小伤害为 1。
 */
class BattleDamageCalculator(
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
) {
	/**
	 * 计算一次物理或特殊技能的普通伤害。
	 *
	 * @throws IllegalArgumentException 当技能为变化技能、缺少威力或防御能力无效时抛出。
	 */
	fun calculate(request: BattleDamageRequest): BattleDamageResult {
		require(request.skill.damageClass != BattleDamageClass.STATUS) { "status skill does not use standard damage formula" }
		val power = effectivePower(request)
		val attackingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> physicalAttackAfterBurn(request)
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.attacker.specialAttack,
				attackingStage(request.attacker.statStage(BattleStat.SPECIAL_ATTACK), request.criticalHit),
			)
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		val defendingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> statStageModifiers.modifiedBattleStat(
				request.defender.defense,
				defendingStage(request.defender.statStage(BattleStat.DEFENSE), request.criticalHit),
			).let { physicalDefenseAfterWeather(request, it) }
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.defender.specialDefense,
				defendingStage(request.defender.statStage(BattleStat.SPECIAL_DEFENSE), request.criticalHit),
			).let { specialDefenseAfterWeather(request, it) }
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		require(defendingStat > 0) { "defending stat must be positive" }

		val levelFactor = (2 * request.attacker.level) / 5 + 2
		val baseDamage = (((levelFactor * power * attackingStat) / defendingStat) / 50) + 2
		val sameElementBonus = if (request.skill.elementId in request.attacker.elementIds) 1.5 else 1.0
		val effectiveness = request.rules.elementChart.multiplier(request.skill.elementId, request.defender.elementIds)
		val criticalHitMultiplier = if (request.criticalHit) 1.5 else 1.0
		val weatherMultiplier = weatherDamageMultiplier(request)
		val terrainMultiplier = terrainDamageMultiplier(request)
		val abilityMultiplier = abilityDamageMultiplier(request)
		val itemMultiplier = itemDamageMultiplier(request)
		val combined = baseDamage * request.targetMultiplier * (request.randomPercent / 100.0) * sameElementBonus *
			effectiveness * criticalHitMultiplier * weatherMultiplier * terrainMultiplier * abilityMultiplier * itemMultiplier
		val amount = if (effectiveness == 0.0) 0 else floor(combined).toInt().coerceAtLeast(1)
		return BattleDamageResult(
			amount = amount,
			baseDamage = baseDamage,
			targetMultiplier = request.targetMultiplier,
			sameElementBonus = sameElementBonus,
			effectiveness = effectiveness,
			criticalHitMultiplier = criticalHitMultiplier,
			weatherMultiplier = weatherMultiplier,
			terrainMultiplier = terrainMultiplier,
			abilityMultiplier = abilityMultiplier,
			itemMultiplier = itemMultiplier,
		)
	}

	/**
	 * 计算物理攻击侧数值。
	 *
	 * 第一批把灼伤减半作为普通物理伤害的固定修正接入，不处理特性或道具绕过灼伤减半的例外。
	 */
	private fun physicalAttackAfterBurn(request: BattleDamageRequest): Int {
		val stagedAttack = statStageModifiers.modifiedBattleStat(
			request.attacker.attack,
			attackingStage(request.attacker.statStage(BattleStat.ATTACK), request.criticalHit),
		)
		return if (request.attacker.majorStatus == BattleMajorStatus.BURN) {
			(stagedAttack / 2).coerceAtLeast(1)
		} else {
			stagedAttack
		}
	}

	/**
	 * 计算进入普通伤害公式的有效威力。
	 *
	 * 天气球、日光束类技能会在特定天气下改变威力。这里把资料层给出的倍率应用在基础威力上并向下取整；
	 * 取整后至少为 1，避免极端自定义倍率产生无效威力。
	 */
	private fun effectivePower(request: BattleDamageRequest): Int {
		val basePower = requireNotNull(request.skill.power) { "damaging skill must define power" }
		val multiplier = request.skill.powerMultipliersByWeather[request.environment.weather] ?: 1.0
		return floor(basePower * multiplier).toInt().coerceAtLeast(1)
	}

	/**
	 * 计算击中要害时参与攻击侧公式的能力阶级。
	 *
	 * 现代规则下，击中要害会忽略攻击方不利的攻击/特攻阶级，但不会忽略有利阶级，也不会忽略灼伤本身的物理减半。
	 */
	private fun attackingStage(stage: Int, criticalHit: Boolean): Int =
		if (criticalHit && stage < 0) 0 else stage

	/**
	 * 计算击中要害时参与防御侧公式的能力阶级。
	 *
	 * 现代规则下，击中要害会忽略防御方有利的防御/特防阶级，但不会忽略防御方不利阶级。
	 */
	private fun defendingStage(stage: Int, criticalHit: Boolean): Int =
		if (criticalHit && stage > 0) 0 else stage

	/**
	 * 计算天气对防御侧物防的修正。
	 *
	 * 现代雪景会让冰属性成员的物防按 1.5 倍参与普通伤害公式。该修正在能力阶级之后应用；
	 * 如果规则快照缺少冰属性 ID，则不应用该修正，避免引擎猜测资料库编号。
	 */
	private fun physicalDefenseAfterWeather(request: BattleDamageRequest, stagedDefense: Int): Int =
		if (request.environment.weather == BattleWeather.SNOW && request.defender.hasElement(request.rules.iceElementId)) {
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
	private fun specialDefenseAfterWeather(request: BattleDamageRequest, stagedSpecialDefense: Int): Int =
		if (request.environment.weather == BattleWeather.SANDSTORM && request.defender.hasElement(request.rules.rockElementId)) {
			floor(stagedSpecialDefense * WEATHER_DEFENSE_BOOST_MULTIPLIER).toInt().coerceAtLeast(1)
		} else {
			stagedSpecialDefense
		}

	/**
	 * 计算天气对火/水属性普通伤害的倍率。
	 *
	 * 元素 ID 来自规则快照，避免引擎硬编码资料库编号。若快照缺少对应元素 ID，天气不会修改伤害。
	 * 第一批实现晴天和下雨对火/水伤害的互相增强/削弱；天气对防御侧能力的修正在独立函数中处理。
	 */
	private fun weatherDamageMultiplier(request: BattleDamageRequest): Double =
		when (request.environment.weather) {
			BattleWeather.SUN -> when (request.skill.elementId) {
				request.rules.fireElementId -> 1.5
				request.rules.waterElementId -> 0.5
				else -> 1.0
			}
			BattleWeather.RAIN -> when (request.skill.elementId) {
				request.rules.waterElementId -> 1.5
				request.rules.fireElementId -> 0.5
				else -> 1.0
			}
			BattleWeather.NONE,
			BattleWeather.SANDSTORM,
			BattleWeather.SNOW -> 1.0
		}

	/**
	 * 计算场地对普通伤害的倍率。
	 *
	 * 现代青草场地有两个伤害侧效果：接地成员使用草属性技能时伤害按 1.3 倍计算；地震、重踏、震级等带有
	 * 明确震动标签的技能命中接地目标时伤害减半。其它场地的状态免疫、先制阻挡和回合末回复不属于伤害公式。
	 */
	private fun terrainDamageMultiplier(request: BattleDamageRequest): Double =
		when (request.environment.terrain) {
			BattleTerrain.GRASSY -> {
				val grassBoost = if (
					request.attacker.grounded &&
					request.rules.grassElementId != null &&
					request.skill.elementId == request.rules.grassElementId
				) {
					GRASSY_TERRAIN_GRASS_DAMAGE_MULTIPLIER
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
			BattleTerrain.NONE,
			BattleTerrain.ELECTRIC,
			BattleTerrain.MISTY,
			BattleTerrain.PSYCHIC -> 1.0
		}

	/**
	 * 计算攻击方特性带来的伤害倍率。
	 *
	 * 第一批只支持低体力指定属性增伤。触发条件按当前 HP 与最大 HP 比例判断，满足阈值且技能属性匹配时叠乘倍率。
	 */
	private fun abilityDamageMultiplier(request: BattleDamageRequest): Double =
		request.attacker.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.LowHpElementDamageBoost -> {
					val hpAtOrBelowThreshold =
						request.attacker.currentHp * effect.hpThresholdDenominator <=
							request.attacker.maxHp * effect.hpThresholdNumerator
					if (hpAtOrBelowThreshold && request.skill.elementId == effect.elementId) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				}
				is BattleAbilityEffect.ContactStatusOnAttacker -> multiplier
				is BattleAbilityEffect.MajorStatusImmunity -> multiplier
				is BattleAbilityEffect.VolatileStatusImmunity -> multiplier
				is BattleAbilityEffect.WeatherDamageImmunity -> multiplier
				is BattleAbilityEffect.WeatherSpeedMultiplier -> multiplier
			}
		}

	/**
	 * 计算攻击方携带道具带来的伤害倍率。
	 *
	 * 第一批只支持造成伤害时直接提升倍率的道具。反伤本身由状态机在伤害事件之后处理，避免计算器修改战斗状态。
	 */
	private fun itemDamageMultiplier(request: BattleDamageRequest): Double =
		request.attacker.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.DamageBoostWithRecoil -> multiplier * effect.multiplier
				is BattleItemEffect.ChoiceSkillLock -> multiplier
				is BattleItemEffect.HeldEndTurnHeal -> multiplier
				is BattleItemEffect.LowHpHeal -> multiplier
				is BattleItemEffect.MajorStatusImmunity -> multiplier
				is BattleItemEffect.VolatileStatusImmunity -> multiplier
				is BattleItemEffect.WeatherDamageImmunity -> multiplier
			}
		}

	private fun BattleParticipant.hasElement(elementId: Long?): Boolean =
		elementId != null && elementId in elementIds

	private companion object {
		private const val GRASSY_TERRAIN_GRASS_DAMAGE_MULTIPLIER = 1.3
		private const val GRASSY_TERRAIN_GROUND_MOVE_MULTIPLIER = 0.5
		private const val WEATHER_DEFENSE_BOOST_MULTIPLIER = 1.5
	}
}
