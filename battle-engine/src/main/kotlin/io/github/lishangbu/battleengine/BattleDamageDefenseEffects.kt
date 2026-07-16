package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.pow

/**
 * 目标在伤害写入 HP 前触发的防守型效果。
 *
 * 本类只处理“本次伤害已经计算出基础数值，但目标 HP 尚未被扣除”这一小段窗口：一次性属性减伤携带道具，以及
 * 满 HP 承受致命伤害时保留 1 点或指定 HP 的特性/道具。它不运行完整伤害公式、不判断替身、不触发低体力道具、
 * 不处理接触反制，也不做倒下判定；这些阶段仍由 [BattleEngine] 按固定顺序编排。这样可以避免防守侧道具/特性
 * 逻辑分散在普通伤害、直接伤害和未来其它伤害入口中。
 */
internal class BattleDamageDefenseEffects {
	/**
	 * 处理防守方一次性属性减伤携带道具。
	 *
	 * 该函数与伤害计算器使用同一个 [BattleItemEffect.ElementDamageReduction.matches] 条件，因此“是否减伤”和
	 * “是否消费道具”不会分叉。调用点必须已经排除了替身挡住本体的场景；如果未来增加穿透替身、紧张感或道具禁用等
	 * 规则，应在进入这里前把是否允许触发表达成明确的结构化状态，而不是在函数中读取技能名称或道具名称。
	 */
	fun heldItemDamageReduction(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		skillElementId: Long,
		effectiveness: Double,
	): BattleHeldItemDamageReduction? {
		if (skill.typelessDamage) {
			return null
		}
		val itemId = target.itemId ?: return null
		val effect = target.itemEffects
			.filterIsInstance<BattleItemEffect.ElementDamageReduction>()
			.firstOrNull { it.matches(skillElementId, effectiveness) }
			?: return null
		val reductionMultiplier = effect.multiplier.pow(target.heldBerryEffectMultiplier())
		val updatedTarget = if (effect.consumesItem) target.consumeHeldItem() else target
		return BattleHeldItemDamageReduction(
			target = updatedTarget,
			event = BattleEvent.DamageReducedByItem(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = target.actorId,
				skillId = skill.skillId,
				itemId = itemId,
				elementId = skillElementId,
				multiplier = reductionMultiplier,
				consumed = effect.consumesItem,
			),
		)
	}

	/**
	 * 在伤害写入目标 HP 前套用“满 HP 致命伤害保留 HP”的特性和道具规则。
	 *
	 * 这类规则必须发生在低体力道具、接触特性和倒下判定之前；否则一次本应保留 HP 的攻击会先把目标写成倒下。
	 * 特性优先于道具，避免同一成员同时拥有两种来源时错误消耗携带道具。若本次技能声明无视目标防守特性，只跳过
	 * 特性来源的保命，道具来源仍按普通携带道具规则结算。
	 */
	fun fatalDamageSurvival(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		ignoreTargetAbilityEffects: Boolean,
		random: BattleRandom,
	): BattleFatalDamageSurvivalResult {
		if (
			damageAmount <= 0 ||
			!target.canBattle() ||
			damageAmount < target.currentHp
		) {
			return BattleFatalDamageSurvivalResult(target = target, damageAmount = damageAmount)
		}
		val abilityEffect = if (ignoreTargetAbilityEffects || target.currentHp != target.maxHp) {
			null
		} else {
			target.abilityEffects
				.filterIsInstance<BattleAbilityEffect.SurviveFatalDamageAtFullHp>()
				.firstOrNull()
		}
		if (abilityEffect != null) {
			return target.toFatalDamageSurvivalResult(
				state = state,
				actor = actor,
				skill = skill,
				damageAmount = damageAmount,
				remainingHp = abilityEffect.remainingHp,
				source = BattleFatalDamageSurvivalSource.ABILITY,
				sourceId = target.abilityId,
				consumed = false,
			)
		}
		val itemId = target.itemId
		val itemEffect = if (target.currentHp == target.maxHp) target.itemEffects
			.filterIsInstance<BattleItemEffect.SurviveFatalDamageAtFullHp>()
			.firstOrNull() else null
		if (itemId != null && itemEffect != null) {
			val updatedTarget = if (itemEffect.consumesItem) target.consumeHeldItem() else target
			return updatedTarget.toFatalDamageSurvivalResult(
				state = state,
				actor = actor,
				skill = skill,
				damageAmount = damageAmount,
				remainingHp = itemEffect.remainingHp,
				source = BattleFatalDamageSurvivalSource.ITEM,
				sourceId = itemId,
				consumed = itemEffect.consumesItem,
			)
		}
		val randomItemEffect = target.itemEffects
			.filterIsInstance<BattleItemEffect.RandomFatalDamageSurvival>()
			.firstOrNull()
		if (itemId != null && randomItemEffect != null && chanceSucceeds(
				randomItemEffect.chancePercent,
				random,
				"fatal damage survival for ${target.actorId}",
			)) {
			return target.toFatalDamageSurvivalResult(
				state = state,
				actor = actor,
				skill = skill,
				damageAmount = damageAmount,
				remainingHp = randomItemEffect.remainingHp,
				source = BattleFatalDamageSurvivalSource.ITEM,
				sourceId = itemId,
				consumed = false,
			)
		}
		return BattleFatalDamageSurvivalResult(target = target, damageAmount = damageAmount)
	}

	/**
	 * 把保命规则转换成后续可直接写入 HP 的伤害结果。
	 *
	 * 这里不直接修改目标 HP，而是返回调整后的目标快照、实际应扣伤害和事件。调用方随后统一执行 `receiveDamage`，
	 * 可以保证普通伤害和直接伤害的事件顺序一致，也能让保命事件紧跟本次伤害事件之后出现。
	 */
	private fun BattleParticipant.toFatalDamageSurvivalResult(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		remainingHp: Int,
		source: BattleFatalDamageSurvivalSource,
		sourceId: Long?,
		consumed: Boolean,
	): BattleFatalDamageSurvivalResult {
		val adjustedDamage = (currentHp - remainingHp).coerceAtLeast(0)
		return BattleFatalDamageSurvivalResult(
			target = this,
			damageAmount = adjustedDamage,
			event = BattleEvent.FatalDamageSurvived(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = actorId,
				skillId = skill.skillId,
				source = source,
				sourceId = sourceId,
				consumed = consumed,
				incomingDamage = damageAmount,
				preventedDamage = damageAmount - adjustedDamage,
			),
		)
	}
}
