package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 伤害后接触特性与携带道具结算器。
 *
 * 本类的边界是“HP 已经因为某个阶段发生变化之后”。它不计算普通伤害、不处理属性倍率、不做击中要害、不判定
 * 免死，也不追加倒下事件；这些仍由主伤害流程和 [BattleState.handleFaintsAndResult] 负责。这里集中处理的是
 * 多个伤害入口都会复用的后续效果：
 * - 目标受到接触技能后，按目标特性概率给攻击方附加主要异常状态。
 * - 目标受到伤害后，触发低体力一次性回复道具。
 * - 使用者造成伤害后，触发生命宝珠类固定反伤。
 * - 整次技能造成伤害后，触发按实际伤害量回复的携带道具。
 *
 * 这些效果容易被误拆成“普通伤害一套、混乱自伤一套、回合末伤害一套、入场陷阱一套”。集中在这里后，各阶段
 * 仍然可以通过明确的方法选择自己需要的 hook，而真正的道具消费、回复封锁、间接伤害免疫和事件顺序只有一份。
 *
 * @property majorStatusEffects 接触特性附加主要异常状态时复用的主要异常状态结算器。
 * @property skillIgnoresTargetAbilityEffects 判断本次技能是否无视目标侧防守特性；它同时服务伤害和接触特性，
 * 因此从主引擎以回调形式传入，避免本类复制目标侧判定。
 */
internal class BattlePostDamageEffects(
	private val majorStatusEffects: BattleMajorStatusEffects,
	private val skillIgnoresTargetAbilityEffects: (BattleState, String, String) -> Boolean,
) {
	/**
	 * 处理目标方“受到接触技能后影响攻击方”的特性效果。
	 *
	 * 当前覆盖概率附加主要异常状态。该 hook 应在伤害事件之后、攻击方反伤和倒下判定之前执行，这样可以表达“攻击
	 * 方命中并接触目标后被麻痹/灼伤/中毒”等常见场景。函数会先检查技能是否接触、目标是否仍存在、攻击方是否
	 * 无视目标特性；概率失败时只消费随机数，不追加任何事件。
	 */
	fun applyContactAbilityEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		if (!skill.makesContact) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		if (skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) {
			return state
		}
		return target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ContactStatusOnAttacker>()
			.fold(state) { current, effect ->
				val actor = current.participant(actorId) ?: return@fold current
				if (!actor.canBattle() || actor.majorStatus != null) {
					current
				} else if (!chanceSucceeds(effect.chancePercent, random, "contact status for $targetActorId")) {
					current
				} else {
					majorStatusEffects.applyMajorStatus(
						state = current,
						actorId = targetActorId,
						recipient = actor,
						status = effect.status,
						random = random,
						randomReason = "contact sleep duration for $targetActorId",
					)
				}
			}
	}

	/**
	 * 处理造成单次伤害后的携带道具效果。
	 *
	 * 当前 hook 覆盖生命宝珠类道具：成功造成伤害后按使用者最大 HP 固定比例反伤。贝壳之铃类道具需要读取整次
	 * 技能的总实际伤害，因此由多段命中循环之后的 [applyPostMoveDamageDealtHealingItem] 单独处理。变化类技能
	 * 或没有造成实际 HP 损失时直接短路，避免“0 伤害也反伤”的错误事件。
	 */
	fun applyPostDamageItemEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		return state.participant(actorId)
			?.itemEffects
			.orEmpty()
			.fold(state) { current, effect ->
				when (effect) {
					is BattleItemEffect.DamageBoostWithRecoil -> applyDamageBoostRecoilItem(current, actorId, effect)
					else -> current
				}
			}
	}

	/**
	 * 处理整次技能结束后的“按造成伤害回复”携带道具效果。
	 *
	 * 公开规则中贝壳之铃类道具按本次技能总实际伤害回复，而不是每一段命中各自回复。因此该 hook 放在多段循环
	 * 之后，只读取主伤害流程汇总出的普通伤害和替身伤害总量。该道具不被消费，也不改变锁招、反伤或目标侧触发
	 * 流程；如果使用者倒下、满 HP、被回复封锁或没有实际造成伤害，就保持状态不变。
	 */
	fun applyPostMoveDamageDealtHealingItem(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		return state.participant(actorId)
			?.itemEffects
			.orEmpty()
			.fold(state) { current, effect ->
				when (effect) {
					is BattleItemEffect.DamageDealtHeal -> applyDamageDealtHealingItem(
						state = current,
						actorId = actorId,
						damageAmount = damageAmount,
						effect = effect,
					)
					else -> current
				}
			}
	}

	/**
	 * 处理低体力一次性回复类携带道具。
	 *
	 * 现代规则中，这类道具在持有者受到伤害后，如果 HP 降到触发线及以下且仍未倒下，会立刻回复并被消费。该方法
	 * 被普通伤害、混乱自伤、入场陷阱、异常状态伤害和天气伤害共同调用；它不关心伤害来源，只读取最新成员快照
	 * 判断道具是否触发。主动使用道具、紧张感等更复杂来源应在资料快照中表达为道具是否有效，而不是在调用方传
	 * 自由文本开关。
	 */
	fun applyLowHpHealingItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		if (!participant.canBattle() || participant.currentHp == participant.maxHp || participant.healingBlocked()) {
			return state
		}
		val effect = participant.itemEffects.filterIsInstance<BattleItemEffect.LowHpHeal>().firstOrNull() ?: return state
		if (!effect.shouldTrigger(participant.currentHp, participant.maxHp)) {
			return state
		}
		val healAmount = effect.healAmount(participant.maxHp)
			.coerceAtMost(participant.maxHp - participant.currentHp)
		if (healAmount <= 0) {
			return state
		}
		val healed = participant.heal(healAmount).consumeHeldItem()
		return state
			.replaceParticipant(healed)
			.appendEvent(
				BattleEvent.HealingApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 处理生命宝珠类道具造成的最大 HP 比例反伤。
	 *
	 * 生命宝珠类现代主系列规则不是“按造成伤害反伤”，而是“按使用者最大 HP 反伤”；这个函数故意只读取成员快照
	 * 中的 `maxHp`，避免伤害随机浮动、属性倍率或屏障倍率改变反伤数值。反伤造成的倒下不在这里追加倒下事件，
	 * 调用方会把攻击方作为候选交给统一倒下收口。
	 */
	private fun applyDamageBoostRecoilItem(
		state: BattleState,
		actorId: String,
		effect: BattleItemEffect.DamageBoostWithRecoil,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasIndirectDamageImmunity()) {
			return state
		}
		val recoil = (actor.maxHp / effect.recoilDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.currentHp)
		return state
			.replaceParticipant(actor.receiveDamage(recoil))
			.appendEvent(
				BattleEvent.RecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = recoil,
				),
			)
	}

	/**
	 * 处理按实际造成伤害量回复的携带道具。
	 *
	 * 回复量使用本次实际 HP 损失向下取整，最少 1 点，并夹取到使用者缺失 HP。调用方已经保证 `damageAmount > 0`，
	 * 因此本函数只需要处理使用者倒下、满 HP 或被回复封锁的情况。回复事件使用普通 [BattleEvent.HealingApplied]，
	 * 与其它非天气/非场地回复保持一致。
	 */
	private fun applyDamageDealtHealingItem(
		state: BattleState,
		actorId: String,
		damageAmount: Int,
		effect: BattleItemEffect.DamageDealtHeal,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.currentHp == actor.maxHp || actor.healingBlocked()) {
			return state
		}
		val healAmount = (damageAmount / effect.healDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.maxHp - actor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.heal(healAmount))
			.appendEvent(
				BattleEvent.HealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = healAmount,
				),
			)
	}
}
