package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 回合末扣血结果的统一收口器。
 *
 * 异常状态、束缚和天气伤害虽然来源不同，但一旦进入“回合末扣血”阶段，它们共享同一个可观察顺序：
 * 1. 先把扣血后的成员写回状态。
 * 2. 追加本次扣血对应的事件。
 * 3. 允许该小阶段追加紧跟扣血事件的阶段内事件，例如束缚自然解除。
 * 4. 执行低体力一次性回复道具。
 * 5. 基于道具处理后的最新成员状态判断倒下和胜负。
 *
 * 这个类只保存上述收口顺序，不计算任何伤害，也不决定某个规则是否应该触发。这样 [BattleEndTurnEffects] 和
 * [BattleBindingEffects] 可以共享同一条后续流程，而不是各自复制一份低体力道具和倒下判定。
 *
 * @property lowHpItemHealing 统一的低体力回复道具入口，由主组件接入 [BattlePostDamageEffects]。
 */
internal class BattleEndTurnDamageResultEffects(
	private val lowHpItemHealing: (BattleState, String, BattleRandom?) -> BattleState,
) {
	/**
	 * 写入一次回合末扣血结果，并完成低体力回复道具、倒下和胜负收口。
	 *
	 * [afterEvent] 只用于同一小阶段内必须紧跟扣血事件追加的事件，不能在里面跳到其它回合末阶段。这样扣血来源
	 * 可以表达自己的局部事件顺序，同时不会把整个回合末大顺序隐藏在回调里。
	 */
	fun apply(
		state: BattleState,
		damaged: BattleParticipant,
		event: BattleEvent,
		random: BattleRandom,
		afterEvent: (BattleState) -> BattleState = { it },
	): BattleState {
		val afterDamage = afterEvent(state.replaceParticipant(damaged).appendEvent(event))
		val afterLowHpItem = lowHpItemHealing(afterDamage, damaged.actorId, random)
		val latestAfterItem = afterLowHpItem.participant(damaged.actorId) ?: damaged
		return afterLowHpItem.handleFaintAndResult(latestAfterItem)
	}
}
