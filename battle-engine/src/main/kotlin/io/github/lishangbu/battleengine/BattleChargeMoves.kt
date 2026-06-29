package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 负责蓄力技能的等待、跳过、释放和中断状态。
 *
 * 蓄力技能的第一回合只写入运行态，第二回合释放时才进入命中、保护、属性和伤害流程。本类只处理这段运行态与
 * 事件流，不决定技能是否能命中，也不处理锁招、异常状态或伤害；这些仍由 [BattleEngine] 的主结算链负责。
 */
internal class BattleChargeMoves {
	/**
	 * 判断技能在当前天气下是否仍需要等待蓄力回合。
	 *
	 * `chargesBeforeUse` 只说明技能存在蓄力流程；是否能被天气跳过由运行时快照中的
	 * `chargeSkippedByWeathers` 精确声明，避免把晴天加速误套到所有蓄力技能上。
	 */
	fun requiresChargeBeforeUse(skill: BattleSkillSlot, weather: BattleWeather): Boolean =
		skill.chargesBeforeUse && weather !in skill.chargeSkippedByWeathers

	/**
	 * 尝试用携带道具跳过本次蓄力等待。
	 *
	 * 该函数只服务首次提交的蓄力技能：技能已经宣告并消耗 PP，但还没有进入命中和伤害流程。若行动者携带
	 * [BattleItemEffect.ChargeSkipOnce]，会按道具效果声明消费道具、追加可复盘事件，并返回继续结算用的新状态。
	 * 若没有可用道具，返回 null，让调用方写入常规蓄力等待状态。
	 */
	fun skipWithHeldItem(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState? {
		val actor = state.participant(actorId) ?: return null
		val itemId = actor.itemId ?: return null
		val effect = actor.itemEffects.filterIsInstance<BattleItemEffect.ChargeSkipOnce>().firstOrNull() ?: return null
		val updatedActor = if (effect.consumesItem) actor.consumeHeldItem() else actor
		return state
			.replaceParticipant(updatedActor)
			.appendEvent(
				BattleEvent.SkillChargeSkippedByItem(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					itemId = itemId,
					consumed = effect.consumesItem,
				),
			)
	}

	/**
	 * 首次使用蓄力技能时写入等待释放状态。
	 *
	 * 这里发生在 PP 已消耗和 `SkillUsed` 已记录之后，但早于命中、保护、属性和伤害流程；也就是说第一回合只是
	 * 宣告并进入蓄力，真正的攻击会由下一次自动生成的技能行动释放。
	 */
	fun startCharge(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val charging = actor.startChargingSkill(skill.skillId, targetActorId)
		return state
			.replaceParticipant(charging)
			.appendEvent(
				BattleEvent.SkillChargeStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					turnsRemainingBeforeUse = charging.chargingTurnsRemaining,
				),
			)
	}

	/**
	 * 释放已蓄力技能。
	 *
	 * 释放只清理蓄力计数并追加事件；PP、命中、保护和伤害仍由后续统一技能流程处理。
	 */
	fun releaseChargedSkill(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		targetActorId: String,
	): BattleState {
		if (actor.chargingTurnsRemaining <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.consumeChargingTurn())
			.appendEvent(
				BattleEvent.SkillChargeReleased(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
				),
			)
	}

	/**
	 * 处理蓄力释放前被行动前状态阻止的情况。
	 *
	 * 如果成员在第二回合因为睡眠、冰冻、麻痹、休整或临时状态无法行动，本次蓄力会结束，不会在后续回合继续
	 * 反复尝试释放同一个技能。
	 */
	fun endAfterDisruption(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (actor.chargingTurnsRemaining <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.clearChargingSkill())
			.appendEvent(
				BattleEvent.SkillChargeInterrupted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
				),
			)
	}
}
