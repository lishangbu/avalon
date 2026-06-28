package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleWeather
import kotlin.math.floor

/**
 * 现代普通伤害公式计算器。
 *
 * 该实现覆盖第一阶段 MVP 需要的基础公式：等级、威力、攻击/防御、随机浮动、属性一致加成和属性克制。
 * 它暂不处理击中要害、场地伤害修正、范围技能、护盾和其它倍率；这些倍率会在后续 hook
 * 管线中以结构化 modifier 追加，并由对照 fixture 分别验证。
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
		val power = requireNotNull(request.skill.power) { "damaging skill must define power" }
		val attackingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> physicalAttackAfterBurn(request)
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.attacker.specialAttack,
				request.attacker.statStage(BattleStat.SPECIAL_ATTACK),
			)
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		val defendingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> statStageModifiers.modifiedBattleStat(
				request.defender.defense,
				request.defender.statStage(BattleStat.DEFENSE),
			)
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.defender.specialDefense,
				request.defender.statStage(BattleStat.SPECIAL_DEFENSE),
			)
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		require(defendingStat > 0) { "defending stat must be positive" }

		val levelFactor = (2 * request.attacker.level) / 5 + 2
		val baseDamage = (((levelFactor * power * attackingStat) / defendingStat) / 50) + 2
		val sameElementBonus = if (request.skill.elementId in request.attacker.elementIds) 1.5 else 1.0
		val effectiveness = request.rules.elementChart.multiplier(request.skill.elementId, request.defender.elementIds)
		val weatherMultiplier = weatherDamageMultiplier(request)
		val abilityMultiplier = abilityDamageMultiplier(request)
		val itemMultiplier = itemDamageMultiplier(request)
		val combined = baseDamage * (request.randomPercent / 100.0) * sameElementBonus *
			effectiveness * weatherMultiplier * abilityMultiplier * itemMultiplier
		val amount = if (effectiveness == 0.0) 0 else floor(combined).toInt().coerceAtLeast(1)
		return BattleDamageResult(
			amount = amount,
			baseDamage = baseDamage,
			sameElementBonus = sameElementBonus,
			effectiveness = effectiveness,
			weatherMultiplier = weatherMultiplier,
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
			request.attacker.statStage(BattleStat.ATTACK),
		)
		return if (request.attacker.majorStatus == BattleMajorStatus.BURN) {
			(stagedAttack / 2).coerceAtLeast(1)
		} else {
			stagedAttack
		}
	}

	/**
	 * 计算天气对火/水属性普通伤害的倍率。
	 *
	 * 元素 ID 来自规则快照，避免引擎硬编码资料库编号。若快照缺少对应元素 ID，天气不会修改伤害。
	 * 第一批实现晴天和下雨对火/水伤害的互相增强/削弱；沙暴、雪景和特定技能例外会通过后续 fixture 接入。
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
				is BattleItemEffect.HeldEndTurnHeal -> multiplier
			}
		}
}
