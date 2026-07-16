package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 混乱状态的行动前结算器。
 *
 * 混乱是行动前状态里最特殊的一支：它既可能只是递减持续计数并继续行动，也可能阻止原技能、按固定 40 威力
 * 物理公式造成自伤、触发低体力回复道具，最后还要检查成员是否倒下。把它从 [BattleBeforeMoveEffects] 拆出后，
 * 行动前主流程可以继续保持“睡眠、冰冻、畏缩、混乱、回复封锁、挑衅、定身法、无理取闹、麻痹”的顺序说明；
 * 这里则完整保存混乱自身的随机消费和伤害事件顺序。
 *
 * 本类不调用标准技能伤害管线。现代规则中的混乱自伤只使用攻击方自己的攻击、防御、等级、能力阶级和 85..100
 * 随机浮动；它不套用属性一致、属性克制、要害、技能威力修正、道具威力修正或大多数特性修正。独立公式比伪造
 * 一个普通技能更短，也更不容易误吃后续伤害管线新增的 modifier。
 *
 * @property statStageModifiers 共享的能力阶级计算器，保证混乱自伤和普通能力阶级倍率使用同一份数学实现。
 * @property lowHpItemHealing 统一的伤害后低体力道具入口，让混乱自伤和普通伤害保持一致的道具消费顺序。
 */
internal class BattleConfusionEffects(
	private val statStageModifiers: BattleStatStageModifiers,
	private val lowHpItemHealing: (state: BattleState, actorId: String, random: BattleRandom?) -> BattleState,
) {
	/**
	 * 处理混乱的行动前计数、解除、自伤和行动阻止。
	 *
	 * 混乱保存的是内部计数，而不是“还会自伤判定几次”。每次行动前先递减计数；递减到 0 时表示成员恢复清醒，
	 * 立即追加 [BattleEvent.VolatileStatusCleared] 并继续执行原技能，不消费混乱自伤概率随机数。递减后仍大于
	 * 0 时，才消费 33% 自伤判定；若未自伤，原技能继续执行；若自伤，再消费一次 85..100 伤害浮动、追加技能阻止
	 * 事件和混乱伤害事件，并跳过原技能的 PP、命中、伤害与附加效果流程。
	 */
	fun resolveBeforeMove(
		state: BattleState,
		actor: BattleParticipant,
		random: BattleRandom,
	): BattleBeforeMoveResult {
		val turnsRemainingBefore = actor.confusionTurnsRemaining
		val decremented = actor.decrementConfusionBeforeMove()
		val afterDecrement = state.replaceParticipant(decremented)
		if (decremented.confusionTurnsRemaining == 0) {
			return BattleBeforeMoveResult.continues(
				afterDecrement.appendEvent(
					BattleEvent.VolatileStatusCleared(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						status = BattleVolatileStatus.CONFUSION,
					),
				),
			)
		}
		if (!chanceSucceeds(CONFUSION_SELF_DAMAGE_CHANCE_PERCENT, random, "confusion self-hit chance for ${actor.actorId}")) {
			return BattleBeforeMoveResult.continues(afterDecrement)
		}
		val randomPercent = 85 + random.nextInt(16, "confusion damage random for ${actor.actorId}")
		val damage = confusionSelfDamage(decremented, randomPercent)
		val blockedState = afterDecrement.appendEvent(
			BattleEvent.SkillPrevented(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				reason = SkillPreventionReason.VOLATILE_STATUS,
				status = BattleVolatileStatus.CONFUSION,
			),
		)
		if (decremented.hasIndirectDamageImmunity()) {
			return BattleBeforeMoveResult.blocked(blockedState)
		}
		val damaged = decremented.receiveDamage(damage)
		val afterDamage = blockedState
			.replaceParticipant(damaged)
			.appendEvent(
				BattleEvent.ConfusionDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = damage,
					randomPercent = randomPercent,
					turnsRemainingBefore = turnsRemainingBefore,
				),
			)
		val afterLowHpItem = lowHpItemHealing(afterDamage, damaged.actorId, random)
		val latest = afterLowHpItem.participant(damaged.actorId) ?: damaged
		return BattleBeforeMoveResult.blocked(afterLowHpItem.handleFaintAndResult(latest))
	}

	/**
	 * 计算混乱自伤。
	 *
	 * 公式等价于 40 威力物理攻击打自己：攻击和防御读取当前能力阶级后的数值，等级使用行动者等级，随机数使用
	 * 85..100 的百分比浮动，最终至少造成 1 点伤害。这里显式要求防御大于 0，因为运行态防御字段如果已经被外部
	 * 资料构造为 0，继续计算会把错误隐藏成除零或异常伤害。
	 */
	private fun confusionSelfDamage(actor: BattleParticipant, randomPercent: Int): Int {
		val attack = statStageModifiers.modifiedBattleStat(actor.attack, actor.statStage(BattleStat.ATTACK))
		val defense = statStageModifiers.modifiedBattleStat(actor.defense, actor.statStage(BattleStat.DEFENSE))
		require(defense > 0) { "confusion defending stat must be positive" }
		val levelFactor = (2 * actor.level) / 5 + 2
		val baseDamage = (((levelFactor * CONFUSION_BASE_POWER * attack) / defense) / 50) + 2
		return floor(baseDamage * (randomPercent / 100.0)).toInt().coerceAtLeast(1)
	}

	private companion object {
		private const val CONFUSION_BASE_POWER = 40
		private const val CONFUSION_SELF_DAMAGE_CHANCE_PERCENT = 33
	}
}
