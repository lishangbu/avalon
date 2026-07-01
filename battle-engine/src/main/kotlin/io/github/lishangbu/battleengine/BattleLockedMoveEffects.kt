package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 锁招类技能的持续回合与结束结算器。
 *
 * 锁招类技能的规则不属于普通伤害公式，也不属于行动前状态本身。它横跨三个时点：
 * - 首次成功使用时，决定总持续回合并记录未来强制行动次数。
 * - 后续强制行动成功时，递减剩余次数并在结束时清理锁招。
 * - 后续强制行动被行动前状态、目标缺失、命中失败、保护、场地或属性免疫打断时，立即结束锁招。
 *
 * 这些时点都由 [BattleEngine] 的显式阶段状态机决定；本类只处理锁招运行态字段、锁招事件和结束后可能产生的
 * 疲劳混乱。它不选择行动、不扣 PP、不解析目标，也不决定技能是否命中。这样主流程仍能清楚表达“什么情况下
 * 调用锁招结束”，而锁招自身的字段维护不会继续挤在主引擎里。
 *
 * @property volatileStatusEffects 锁招结束后的疲劳混乱复用统一临时状态结算器，确保已有混乱、薄雾场地、特性/道具免疫和
 * 状态治愈道具仍按普通临时状态写入规则处理。
 */
internal class BattleLockedMoveEffects(
	private val volatileStatusEffects: BattleVolatileStatusEffects,
) {
	/**
	 * 在技能成功执行后推进锁招状态。
	 *
	 * 首次成功使用锁招技能时，成员进入“未来回合必须继续使用同一技能”的状态；后续锁定回合成功执行时，只递减
	 * 未来剩余次数，不再次扣 PP。剩余次数耗尽后，如果技能声明会疲劳混乱，则立即给使用者附加混乱。若使用者
	 * 已经倒下，则清除残留锁招字段，避免倒下成员在后续强制换入前保留无意义的锁定状态。
	 */
	fun updateAfterSuccessfulUse(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return if (actor.lockedMoveTurnsRemaining > 0) state.replaceParticipant(actor.clearLockedMove()) else state
		}
		if (actor.lockedMoveTurnsRemaining > 0) {
			return advanceAfterSuccessfulUse(state, actor, skill, random)
		}
		if (skill.lockMoveTurnsMax <= 1) {
			return state
		}
		val totalTurns = determineTotalTurns(skill, random)
		val turnsRemainingAfterCurrent = totalTurns - 1
		if (turnsRemainingAfterCurrent <= 0) {
			return state
		}
		return state
			.replaceParticipant(
				actor.startLockedMove(
					skillId = skill.skillId,
					targetActorId = targetActorId,
					turnsRemainingAfterCurrent = turnsRemainingAfterCurrent,
					confusesOnEnd = skill.confusesUserAfterLock,
				),
			)
			.appendEvent(
				BattleEvent.LockedMoveStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					totalTurns = totalTurns,
					turnsRemainingAfterCurrent = turnsRemainingAfterCurrent,
				),
			)
	}

	/**
	 * 处理中断锁招的失败分支。
	 *
	 * 现代规则中，锁招后续回合如果被行动前状态阻止、找不到目标、未命中、被保护/场地/属性免疫挡下，会退出锁招。
	 * 若中断正好发生在本应结束并疲劳的最后一次强制行动上，仍按公开说明附加疲劳混乱。函数不会自行判断中断
	 * 原因是否成立；调用方只在已经确认本次强制行动失败时调用它。
	 */
	fun endAfterDisruption(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (actor.lockedMoveTurnsRemaining <= 0) {
			return state
		}
		val shouldConfuse = actor.lockedMoveConfusesOnEnd && actor.lockedMoveTurnsRemaining == 1 && actor.canBattle()
		val cleared = actor.clearLockedMove()
		val ended = state
			.replaceParticipant(cleared)
			.appendEvent(
				BattleEvent.LockedMoveEnded(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					confusesUser = shouldConfuse,
				),
			)
		return if (shouldConfuse) {
			applyFatigueConfusion(
				state = ended,
				actorId = actor.actorId,
				recipient = cleared,
				skill = skill,
				random = random,
			)
		} else {
			ended
		}
	}

	/**
	 * 决定锁招技能本次会持续的总回合数。
	 *
	 * 公开成熟实现会在首次成功使用时决定 2 或 3 回合等持续时间，并把当前回合计入总数。固定持续时间不消费
	 * 随机数，避免普通单回合技能或测试用例因无意义随机消费破坏 replay 脚本。
	 */
	private fun determineTotalTurns(skill: BattleSkillSlot, random: BattleRandom): Int {
		if (skill.lockMoveTurnsMin == skill.lockMoveTurnsMax) {
			return skill.lockMoveTurnsMin
		}
		return skill.lockMoveTurnsMin +
			random.nextInt(skill.lockMoveTurnsMax - skill.lockMoveTurnsMin + 1, "locked move duration for ${skill.skillId}")
	}

	/**
	 * 消耗一次已经存在的锁招强制行动。
	 *
	 * 锁招的剩余次数只表示未来还会被强制行动几次，因此每次后续成功发动后递减。若递减后仍大于 0，只记录
	 * [BattleEvent.LockedMoveAdvanced]；若正好结束，则清除锁招并按技能配置处理疲劳混乱。
	 */
	private fun advanceAfterSuccessfulUse(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val updated = actor.consumeLockedMoveTurn()
		val afterConsume = state.replaceParticipant(updated)
		return if (actor.lockedMoveTurnsRemaining > 1) {
			afterConsume.appendEvent(
				BattleEvent.LockedMoveAdvanced(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					turnsRemainingAfterCurrent = updated.lockedMoveTurnsRemaining,
				),
			)
		} else {
			val shouldConfuse = actor.lockedMoveConfusesOnEnd && updated.canBattle()
			val ended = afterConsume.appendEvent(
				BattleEvent.LockedMoveEnded(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					confusesUser = shouldConfuse,
				),
			)
			if (shouldConfuse) {
				applyFatigueConfusion(
					state = ended,
					actorId = actor.actorId,
					recipient = updated,
					skill = skill,
					random = random,
				)
			} else {
				ended
			}
		}
	}

	/**
	 * 给锁招结束后的使用者附加疲劳混乱。
	 *
	 * 这里不直接写入混乱字段，而是走 [BattleVolatileStatusEffects.applyVolatileStatus]。这样如果使用者已经混乱、被薄雾
	 * 场地保护、拥有临时状态免疫或携带可治愈混乱的道具，事件顺序和普通技能附加混乱保持一致。
	 */
	private fun applyFatigueConfusion(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		volatileStatusEffects.applyVolatileStatus(
			state = state,
			actorId = actorId,
			recipient = recipient,
			status = BattleVolatileStatus.CONFUSION,
			random = random,
			randomReason = "locked move confusion duration for ${skill.skillId}",
		)
}
