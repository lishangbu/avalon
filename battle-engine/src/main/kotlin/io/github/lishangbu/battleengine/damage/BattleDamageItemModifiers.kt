package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.boosterEnergyMultiplier
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat

/**
 * 普通伤害公式中的道具修正集合。
 *
 * 道具规则同样需要区分“有效威力阶段”和“最终伤害倍率阶段”。例如属性强化道具会先修改技能威力并参与后续
 * 基础伤害整数取整，而生命宝珠类稳定增伤、效果绝佳强化和抗性果减伤则属于最终伤害倍率。把两者放在本类里，
 * 可以让计算器在调用点清楚标出取整位置，而不是把所有道具分支塞进公式主流程。
 *
 * 本类只返回倍率，不负责道具消费、反伤或事件。一次性抗性道具是否被消耗、增伤道具是否造成反伤，都依赖
 * 这次攻击是否实际命中并造成相应事件，应由状态机在伤害应用阶段处理。
 */
internal class BattleDamageItemModifiers {
	/** 计算限定种族道具对攻击侧能力值的倍率。 */
	fun attackingStatAfterItem(request: BattleDamageRequest, stat: BattleStat, value: Int): Int {
		val multiplier = request.attacker.itemEffects.fold(1.0) { current, effect ->
			if (effect is BattleItemEffect.CreatureStatMultiplier &&
				request.attacker.creatureId in effect.creatureIds && stat in effect.stats
			) current * effect.multiplier else current
		}
		return (value * multiplier * request.attacker.boosterEnergyMultiplier(stat)).toInt().coerceAtLeast(1)
	}

	/** 计算防守方携带道具对伤害公式防守能力值的倍率。 */
	fun defendingStatAfterItem(request: BattleDamageRequest, stat: BattleStat, value: Int): Int {
		val multiplier = request.defender.itemEffects.fold(1.0) { current, effect ->
			when {
				effect is BattleItemEffect.DefendingStatMultiplier && stat in effect.stats -> current * effect.multiplier
				effect is BattleItemEffect.EvolvableDefendingStatMultiplier && request.defender.canEvolve && stat in effect.stats ->
					current * effect.multiplier
				effect is BattleItemEffect.CreatureStatMultiplier && request.defender.creatureId in effect.creatureIds && stat in effect.stats -> current * effect.multiplier
				else -> current
			}
		}
		return (value * multiplier * request.defender.boosterEnergyMultiplier(stat)).toInt().coerceAtLeast(1)
	}

	/**
	 * 计算攻击方携带道具贡献到技能有效威力阶段的倍率。
	 *
	 * 该阶段覆盖传统属性强化道具以及按物理/特殊分类提升威力的道具。它与 [damageMultiplier] 分离，是为了遵守
	 * 现代公开公式中“威力修正”和“最终伤害修正”的不同取整位置。
	 */
	fun powerMultiplier(request: BattleDamageRequest): Double =
		request.attacker.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.DamageClassPowerBoost -> if (request.skill.damageClass in effect.damageClasses) {
					multiplier * effect.multiplier
				} else {
					multiplier
				}
				is BattleItemEffect.PunchBasedSkillPowerBoost ->
					if (request.skill.punchBased) multiplier * effect.multiplier else multiplier
				is BattleItemEffect.ElementDamageBoost ->
					if (!request.skill.typelessDamage && request.skillElementId == effect.elementId) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				is BattleItemEffect.ConsumableElementDamageBoost ->
					if (!request.skill.typelessDamage && request.skillElementId == effect.elementId) {
						multiplier * effect.multiplier
					} else multiplier
				is BattleItemEffect.CreatureElementDamageBoost ->
					if (request.attacker.creatureId in effect.creatureIds && request.skillElementId in effect.elementIds) {
						multiplier * effect.multiplier
					} else multiplier
				is BattleItemEffect.CreatureDamageBoost ->
					if (request.attacker.creatureId in effect.creatureIds) multiplier * effect.multiplier else multiplier
				else -> multiplier
			}
		}

	/**
	 * 计算攻击方和防守方携带道具共同带来的最终伤害倍率。
	 *
	 * 这里只读取“伤害公式中的稳定倍率”。攻击方增伤道具和防守方抗性道具都会进入同一最终倍率链；带反伤的增伤
	 * 道具只在此处贡献倍率，反伤本身由状态机在伤害事件之后处理。防守方一次性减伤道具是否消费，同样由状态机
	 * 根据同一结构化效果在伤害写入前处理，计算器保持纯函数。
	 */
	fun damageMultiplier(request: BattleDamageRequest): Double =
		attackerDamageMultiplier(request) * defenderDamageMultiplier(request)

	/**
	 * 计算攻击方携带道具贡献的最终伤害倍率。
	 */
	private fun attackerDamageMultiplier(request: BattleDamageRequest): Double =
		request.attacker.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.DamageBoostWithRecoil -> multiplier * effect.multiplier
				is BattleItemEffect.ConsecutiveSkillDamageBoost -> {
					val repeats = (request.attacker.consecutiveSuccessfulSkillUses - 1).coerceAtLeast(0)
					multiplier * (1.0 + (repeats * effect.boostPerRepeat).coerceAtMost(effect.maximumBoost))
				}
				is BattleItemEffect.SuperEffectiveDamageBoost -> if (
					request.typeEffectiveness > 1.0
				) {
					multiplier * effect.multiplier
				} else {
					multiplier
				}
				else -> multiplier
			}
		}

	/**
	 * 计算防守方携带道具贡献的伤害倍率。
	 *
	 * 目前只支持本体受击时触发的指定属性减伤道具。替身挡住本体时，外层状态机会把
	 * `allowDefenderItemDamageReduction` 置为 false，因此本函数即使看到防守方携带抗性道具也会保持中性。
	 */
	private fun defenderDamageMultiplier(request: BattleDamageRequest): Double =
		if (!request.allowDefenderItemDamageReduction) {
			1.0
		} else {
			val effectiveness = request.typeEffectiveness
			request.defender.itemEffects.fold(1.0) { multiplier, effect ->
				when (effect) {
					is BattleItemEffect.ElementDamageReduction -> if (
						!request.skill.typelessDamage &&
						effect.matches(request.skillElementId, effectiveness)
					) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
					else -> multiplier
				}
			}
		}
}
