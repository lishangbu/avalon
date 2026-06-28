package io.github.lishangbu.battleengine.damage

/**
 * 一次普通伤害计算结果。
 *
 * `amount` 是最终要扣除的 HP；`baseDamage` 是套用随机、属性一致和属性克制前的基础伤害；
 * `targetMultiplier`、防守方侧屏障倍率、`sameElementBonus`、`effectiveness`、`criticalHitMultiplier`
 * 和天气/场地/特性/道具倍率保留中间值，供事件流和对照测试定位公式差异。
 */
data class BattleDamageResult(
	val amount: Int,
	val baseDamage: Int,
	val targetMultiplier: Double,
	val sideDamageReductionMultiplier: Double,
	val sameElementBonus: Double,
	val effectiveness: Double,
	val criticalHitMultiplier: Double,
	val weatherMultiplier: Double,
	val terrainMultiplier: Double,
	val abilityMultiplier: Double,
	val itemMultiplier: Double,
)
