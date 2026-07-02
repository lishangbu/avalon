package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 变化技能成功后的 HP 效果结算器。
 *
 * 变化技能不会进入普通伤害公式，也没有“本次造成的实际伤害量”。因此它们的自我回复、目标回复、天气/场地变量
 * 回复和建立替身不应该塞进伤害后 HP 效果类里，否则读代码时会误以为这些效果也参与吸取/反作用/低体力道具/
 * 倒下判定那条顺序。
 *
 * 本类只在变化技能已经通过目标、保护、命中等前置判定并成功后运行。满 HP、回复封锁、无法战斗、已存在替身或
 * HP 不足以支付替身费用时保持状态不变且不产生事件；如果未来要表达“技能失败”事件，应在技能宣告或失败阶段
 * 明确建模，而不是让 HP 写入 helper 同时承担失败语义。
 */
internal class BattleStatusSkillHpEffects {
	/**
	 * 处理变化技能成功后的 HP 回复和替身效果。
	 *
	 * 效果按技能槽中的 [BattleSkillSlot.hpEffects] 顺序执行。自我回复、天气变量回复、目标回复和场地变量目标
	 * 回复最终汇入同一个最大 HP 比例 helper；替身单独处理 HP 支付和替身 HP 写入。其它 HP 效果属于伤害技能后效，
	 * 在这里直接跳过。
	 */
	fun apply(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.hpEffects
			.fold(state) { current, effect ->
				when (effect) {
					is BattleSkillHpEffect.SelfHealMaxHpFraction -> applyHealMaxHpFraction(
						state = current,
						healedActorId = actorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.SelfHealMaxHpByWeather -> {
						val fraction = effect.weatherFractions[current.environment.weather] ?: effect.defaultFraction
						applyHealMaxHpFraction(
							state = current,
							healedActorId = actorId,
							skill = skill,
							numerator = fraction.numerator,
							denominator = fraction.denominator,
						)
					}
					is BattleSkillHpEffect.TargetHealMaxHpFraction -> applyHealMaxHpFraction(
						state = current,
						healedActorId = targetActorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.TargetHealMaxHpByTerrain -> {
						val fraction = effect.terrainFractions[current.environment.terrain] ?: effect.defaultFraction
						applyHealMaxHpFraction(
							state = current,
							healedActorId = targetActorId,
							skill = skill,
							numerator = fraction.numerator,
							denominator = fraction.denominator,
						)
					}
					is BattleSkillHpEffect.CreateSubstitute -> applyCreateSubstitute(
						state = current,
						actorId = actorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					else -> current
				}
			}

	/**
	 * 按最大 HP 比例回复指定成员。
	 *
	 * 自我回复、天气变量回复、目标回复和场地变量目标回复最终都汇入这里，确保满 HP 跳过、缺失 HP 夹取、回复封锁
	 * 和事件写入规则完全一致。这里不消费随机数，也不判断技能是否应该失败；调用方已经确认变化技能成功。
	 */
	private fun applyHealMaxHpFraction(
		state: BattleState,
		healedActorId: String,
		skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val healedActor = state.participant(healedActorId) ?: return state
		if (!healedActor.canReceiveHealing()) {
			return state
		}
		val healAmount = fractionAmount(healedActor.maxHp, numerator, denominator)
			.coerceAtMost(healedActor.maxHp - healedActor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(healedActor.heal(healAmount))
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = healedActor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 支付使用者最大 HP 的固定比例来建立替身。
	 *
	 * 现代替身要求使用者当前 HP 必须严格大于费用，且不能已经拥有替身。失败时技能已经完成使用和 PP 消耗，但不
	 * 产生额外事件；成功时本体扣除费用、替身获得同等 HP，并用专用事件记录该运行态事实。
	 */
	private fun applyCreateSubstitute(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasSubstitute()) {
			return state
		}
		val hpCost = fractionAmount(actor.maxHp, numerator, denominator)
			.coerceAtMost(actor.maxHp - 1)
		if (actor.currentHp <= hpCost) {
			return state
		}
		val substituted = actor.startSubstitute(hpCost)
		return state
			.replaceParticipant(substituted)
			.appendEvent(
				BattleEvent.SubstituteStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					hpCost = hpCost,
					substituteHp = substituted.substituteHp,
				),
			)
	}
}
