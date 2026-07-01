package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus

/**
 * 状态写入成功后的即时治愈道具结算器。
 *
 * 这个类刻意只做一件事：状态已经被写入运行态并产生可观察事件后，检查目标当前有效携带道具是否声明了对应的
 * 治愈效果；如果命中效果，就清除刚写入的状态、按效果配置决定是否消费道具，并追加清除事件。它不参与“状态
 * 能否写入”的任何判定，所以属性免疫、场地免疫、替身、特性免疫、道具免疫和已有状态阻止仍由
 * [BattleMajorStatusEffects] 或 [BattleVolatileStatusEffects] 在随机数消费前完成。
 *
 * 这种拆法比给每一种异常状态挂独立处理器更小：治愈道具的可观察规则完全相同，差异只在主要异常状态和临时
 * 状态对应的运行态字段、道具效果类型和事件类型不同。把两条路径放在同一个 helper 里，可以稳定保留
 * “状态写入事件 -> 道具治愈清除事件”的 replay 顺序，同时避免主状态结算器继续承担道具消费细节。
 */
internal class BattleStatusCureEffects {
	/**
	 * 处理成功获得主要异常状态后的即时治愈携带道具。
	 *
	 * 调用方必须保证 [BattleEvent.StatusApplied] 已经追加完成；本函数只读取最新运行态中的目标成员，因此能处理
	 * 状态写入时顺手更新的睡眠回合、剧毒计数等附属字段。没有目标、目标没有主要异常状态、或没有匹配道具效果
	 * 时直接返回原状态，不追加任何“未触发”事件，保持事件流只记录真实发生的规则结果。
	 *
	 * 触发成功时先通过 `clearMajorStatus` 清除主要异常状态及其附属计数，再根据
	 * [BattleItemEffect.MajorStatusCure.consumesItem] 决定是否消费携带道具，最后追加 [BattleEvent.StatusCleared]。
	 * 事件顺序必须保持在状态写入之后，避免 replay 或 UI 把道具治愈误解为状态免疫。
	 */
	fun applyMajorStatusCureItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		val status = participant.majorStatus ?: return state
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.MajorStatusCure>()
			.firstOrNull { status in it.statuses }
			?: return state
		val cured = if (effect.consumesItem) {
			participant.clearMajorStatus().consumeHeldItem()
		} else {
			participant.clearMajorStatus()
		}
		return state
			.replaceParticipant(cured)
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
	}

	/**
	 * 处理成功获得临时状态后的即时治愈携带道具。
	 *
	 * 调用方必须已经写入临时状态并追加 [BattleEvent.VolatileStatusApplied]；因此本函数不会遮蔽薄雾场地、替身、
	 * 特性免疫、道具免疫或已有混乱等前置阻止语义。这里读取的是目标当前道具效果快照，资料层想让某个道具治愈
	 * 混乱、畏缩、束缚或其它临时状态时，只需要把对应枚举放进 [BattleItemEffect.VolatileStatusCure.statuses]。
	 *
	 * 触发成功时先清除指定临时状态，确保混乱/回复封锁/挑衅/定身法/无理取闹/束缚各自的持续字段同步归零；再按
	 * [BattleItemEffect.VolatileStatusCure.consumesItem] 消费携带道具；最后追加
	 * [BattleEvent.VolatileStatusCleared]，让事件流稳定呈现“临时状态写入 -> 道具治愈”的顺序。
	 */
	fun applyVolatileStatusCureItem(
		state: BattleState,
		actorId: String,
		status: BattleVolatileStatus,
	): BattleState {
		val participant = state.participant(actorId) ?: return state
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.VolatileStatusCure>()
			.firstOrNull { status in it.statuses }
			?: return state
		val cured = if (effect.consumesItem) {
			participant.clearVolatileStatus(status).consumeHeldItem()
		} else {
			participant.clearVolatileStatus(status)
		}
		return state
			.replaceParticipant(cured)
			.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
	}
}
