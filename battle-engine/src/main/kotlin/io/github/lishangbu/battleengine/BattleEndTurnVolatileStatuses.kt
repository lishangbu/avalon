package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus

/**
 * 回合末临时状态清理与持续回合推进。
 *
 * 这个 resolver 的范围刻意很窄：它不处理异常状态伤害、天气伤害、场地回复、携带道具回复或回合上限，只处理
 * 那些“附着在成员运行态上，并且需要在回合边界清理或递减”的临时标记。这样主状态机仍然清楚地掌握完整回合
 * 末阶段顺序，而临时状态自身的机械推进可以从庞大的 [BattleEngine] 中移出。
 *
 * 当前包含三类职责：
 * - 保护类技能的连续成功计数：只有本回合成功保护的成员保留计数，其它成员在回合末重置。
 * - 畏缩等不跨回合保留的临时标记：如果本回合没有在行动前消费，回合末静默清理。
 * - 回复封锁、挑衅、定身法这类按回合末递减的临时状态：递减到 0 时追加解除事件。
 *
 * 这里不使用通用 `Map<BattleVolatileStatus, Int>`，而是继续调用 [BattleParticipant] 上的专用方法。原因是这些
 * 状态的生命周期语义并不完全一致：混乱在行动前递减，畏缩只持续本回合，束缚有来源成员和伤害阶段，挑衅/定身
 * 法/回复封锁才是单纯的回合末计数。用一个通用容器强行统一，短期看似瘦字段，长期会把规则差异藏到字符串或
 * 特殊分支里。
 */
internal class BattleEndTurnVolatileStatuses {
	/**
	 * 重置未在本回合成功保护的成员连续保护计数。
	 *
	 * 保护递减概率只取决于连续成功保护次数。成员使用其它技能、替换、无法行动或没有提交行动时，都应该在回合
	 * 末失去连续计数；只有 [successfulProtectionActorIds] 中的成员把计数保留到下一回合。函数遍历全队成员而
	 * 不只遍历当前上场成员，是为了让未来替换、强制换入或双打场景里离场成员也不会错误保留计数。
	 */
	fun resetProtectionChains(state: BattleState, successfulProtectionActorIds: Set<String>): BattleState =
		state.sides
			.flatMap { it.participants }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (latest.actorId in successfulProtectionActorIds || latest.protectionChain == 0) {
					current
				} else {
					current.replaceParticipant(latest.resetProtectionChain())
				}
			}

	/**
	 * 清理回合结束时不会跨回合保留的临时状态。
	 *
	 * 目前只有畏缩需要该阶段兜底：如果目标已经行动后才被附加畏缩，它不会阻止任何行动，也不应该进入下一回合。
	 * 这里不追加解除事件，因为公开规则上“回合末畏缩自然消失”不是一个需要 replay 对齐的可见动作；真正阻止
	 * 行动的畏缩会在行动前阶段产生 [BattleEvent.SkillPrevented]。
	 */
	fun clearEndTurnOnlyStatuses(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				val cleared = latest.clearEndTurnVolatileStatuses()
				if (cleared == latest) current else current.replaceParticipant(cleared)
			}

	/**
	 * 推进跨回合临时状态的持续回合。
	 *
	 * 畏缩已经由 [clearEndTurnOnlyStatuses] 静默清理；混乱按行动前计数，不在回合末递减；束缚还需要处理来源成员
	 * 是否仍在场和回合末伤害，因此留给主回合末伤害阶段。这里推进的是回复封锁、挑衅和定身法：每个回合末减少
	 * 1，归零时追加 [BattleEvent.VolatileStatusCleared]，让 replay 能看到状态自然结束的时间点。
	 */
	fun advanceEndTurnDurations(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				val afterHealBlock = advanceOneDuration(
					state = current,
					participant = latest,
					status = BattleVolatileStatus.HEAL_BLOCK,
					turnsRemaining = latest.healBlockTurnsRemaining,
					decrement = BattleParticipant::decrementHealBlockEndTurn,
				)
				val latestAfterHealBlock = afterHealBlock.participant(participant.actorId) ?: return@fold afterHealBlock
				val afterTaunt = advanceOneDuration(
					state = afterHealBlock,
					participant = latestAfterHealBlock,
					status = BattleVolatileStatus.TAUNT,
					turnsRemaining = latestAfterHealBlock.tauntTurnsRemaining,
					decrement = BattleParticipant::decrementTauntEndTurn,
				)
				val latestAfterTaunt = afterTaunt.participant(participant.actorId) ?: return@fold afterTaunt
				val afterDisable = advanceOneDuration(
					state = afterTaunt,
					participant = latestAfterTaunt,
					status = BattleVolatileStatus.DISABLE,
					turnsRemaining = latestAfterTaunt.disabledSkillTurnsRemaining,
					decrement = BattleParticipant::decrementDisableEndTurn,
				)
				val latestAfterDisable = afterDisable.participant(participant.actorId) ?: return@fold afterDisable
				advanceAccuracyLockDuration(afterDisable, latestAfterDisable)
			}

	/**
	 * 推进命中锁定的持续时间。
	 *
	 * 命中锁定不是 [BattleVolatileStatus] 枚举的一员，因为它不由通用临时状态附加表维护，也没有解除文案；但它和
	 * 回复封锁、挑衅、定身法一样按完整回合末递减。效果建立时保存 2 个回合末单位，当前回合末递减为 1，下一回合
	 * 结束时清除，从而表达“到下回合结束前有效”。
	 */
	private fun advanceAccuracyLockDuration(state: BattleState, participant: BattleParticipant): BattleState {
		if (participant.accuracyLockTurnsRemaining <= 0) {
			return state
		}
		return state.replaceParticipant(participant.decrementAccuracyLockEndTurn())
	}

	/**
	 * 推进一个“回合末递减、归零可见”的临时状态计数。
	 *
	 * 这个 helper 只抽取三种状态共有的机械行为：计数大于 0 时递减，递减前为 1 时追加解除事件。具体字段如何
	 * 清零仍由 [BattleParticipant] 的专用方法维护，避免这里通过反射或状态枚举去读写成员内部字段。
	 */
	private fun advanceOneDuration(
		state: BattleState,
		participant: BattleParticipant,
		status: BattleVolatileStatus,
		turnsRemaining: Int,
		decrement: BattleParticipant.() -> BattleParticipant,
	): BattleState {
		if (turnsRemaining <= 0) {
			return state
		}
		val updated = participant.decrement()
		val advanced = state.replaceParticipant(updated)
		return if (turnsRemaining == 1) {
			advanced.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
		} else {
			advanced
		}
	}
}
