package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 技能自身造成的 HP 与关联状态效果。
 *
 * 本类处理的都是“技能已经被判定为成功命中或成功使用之后”的附加结果：自爆式代价、造成伤害后的吸取/反作用、
 * 造成伤害后的休整，以及火属性伤害命中后的解冻。变化技能成功后的自我回复和替身由
 * [BattleStatusSkillHpEffects] 处理，因为那条路径不读取本次伤害量，也不进入伤害后倒下/低体力道具顺序。
 *
 * 本类不做命中、保护、属性免疫、接触特性、低体力道具或倒下胜负结算；这些阶段必须继续由 [BattleEngine]
 * 编排，原因是它们和技能 HP 效果之间有严格的现代规则顺序。这里保留的 helper 都和“实际造成伤害之后”或
 * “伤害技能命中后”直接相关，不需要独立的事件总线或规则注册表。
 */
internal class BattleSkillHpEffects {
	/**
	 * 写入技能自身代价造成的使用者倒下。
	 *
	 * 该 helper 只服务“命中后使用者以当前 HP 作为代价倒下”的直接伤害规则。它不读取目标实际损失 HP，也不检查
	 * 反作用伤害免疫；后续倒下事件和胜负判定仍交给调用方统一处理，避免这里重复判胜或改变伤害后阶段顺序。
	 */
	fun applySelfSacrificeDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val amount = actor.currentHp
		return state
			.replaceParticipant(actor.receiveDamage(amount))
			.appendEvent(
				BattleEvent.SkillSelfSacrificeDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = amount,
				),
			)
	}

	/**
	 * 处理造成伤害后的技能 HP 效果。
	 *
	 * 当前支持两类直接绑定在技能上的 HP 后效：按本次实际伤害吸取回复使用者，以及按本次实际伤害让使用者承受
	 * 反作用伤害。调用方负责保证该函数位于伤害事件之后、目标低体力道具和接触类反制之前；这样事件流能直接表达
	 * “本段真实扣掉了多少 HP，随后技能自身基于该数值造成了什么”。如果本段没有造成实际伤害，或使用者已经无法
	 * 战斗，则不产生技能 HP 后效事件。
	 */
	fun applyPostDamageSkillHpEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0) {
			return state
		}
		return skill.hpEffects
			.fold(state) { current, effect ->
				when (effect) {
					is BattleSkillHpEffect.DrainDamage -> applySkillDrainDamage(
						state = current,
						actorId = actorId,
						skill = skill,
						damageAmount = damageAmount,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.RecoilByDamageDealt -> applySkillRecoilDamage(
						state = current,
						actorId = actorId,
						skill = skill,
						damageAmount = damageAmount,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					else -> current
				}
			}
	}

	/**
	 * 处理成功造成实际伤害后的技能休整写入。
	 *
	 * 休整只由真正扣掉目标 HP 的技能触发；未命中、保护、属性无效、目标已经没有可扣除 HP 等情况都不会写入。
	 * 调用方应把写入放在技能自身 HP 后效之后、目标低体力道具和接触类反制之前。由于休整只影响未来行动，这个
	 * 顺序不会改变当前回合其它后效，只是让事件流更贴近“技能自身效果先完成”的阅读顺序。
	 */
	fun applyRechargeAfterDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (!skill.rechargesAfterUse || damageAmount <= 0) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.rechargeTurnsRemaining > 0) {
			return state
		}
		val recharging = actor.startRecharge()
		return state
			.replaceParticipant(recharging)
			.appendEvent(
				BattleEvent.RechargeStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					turnsRemainingAfterCurrent = recharging.rechargeTurnsRemaining,
				),
			)
	}

	/**
	 * 处理火属性伤害命中后解除目标冰冻。
	 *
	 * 现代规则中，冰冻目标被火属性伤害技能命中会解冻。这里要求目标在该段伤害后仍可战斗，避免对已经倒下的
	 * 成员追加无意义的状态解除事件；特定“冰冻中也可使用并解除自身冰冻”的技能会通过后续技能标签接入，而不是
	 * 混入这个“目标被火属性伤害命中”的分支。
	 */
	fun clearFreezeAfterFireDamage(
		state: BattleState,
		damagedTarget: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState {
		if (
			skill.effectiveElementId(state.environment.weather) != state.rules.fireElementId ||
			damagedTarget.majorStatus != BattleMajorStatus.FREEZE ||
			!damagedTarget.canBattle()
		) {
			return state
		}
		return state
			.replaceParticipant(damagedTarget.clearMajorStatus())
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = damagedTarget.actorId,
					status = BattleMajorStatus.FREEZE,
				),
			)
	}

	/**
	 * 按本次实际伤害回复使用者。
	 *
	 * 吸取回复使用向下取整的比例口径，并在最后按当前缺失 HP 夹取；它不负责处理污泥浆、回复封锁、吸取强化等
	 * 额外规则，那些会以新的明确效果或 hook 接入，避免这里出现难以复盘的隐式分支。
	 */
	private fun applySkillDrainDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canReceiveHealing()) {
			return state
		}
		val healAmount = fractionAmount(damageAmount, numerator, denominator)
			.coerceAtMost(actor.maxHp - actor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.heal(healAmount))
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 按本次实际伤害让使用者承受技能反作用伤害。
	 *
	 * 反作用伤害使用现代公开实现中“按目标实际损失 HP 计算，四舍五入到最近整数，最少 1 点”的规则。这里不会
	 * 重新读取公式伤害，也不会因为目标已经倒下而跳过；只要本段确实让目标损失 HP，使用者仍会承受对应自损。
	 */
	private fun applySkillRecoilDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasIndirectDamageImmunity() || actor.hasSkillRecoilDamageImmunity()) {
			return state
		}
		val recoilAmount = roundedHalfUpFractionAmount(damageAmount, numerator, denominator)
			.coerceAtMost(actor.currentHp)
		if (recoilAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.receiveDamage(recoilAmount))
			.appendEvent(
				BattleEvent.SkillRecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = recoilAmount,
					sourceDamageAmount = damageAmount,
				),
			)
	}

}
