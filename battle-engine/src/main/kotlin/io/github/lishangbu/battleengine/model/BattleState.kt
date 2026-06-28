package io.github.lishangbu.battleengine.model

/**
 * 战斗运行态。
 *
 * 状态是不可变数据结构：每次回合结算都会返回新的 `BattleState`，并追加事件流。
 * 这让测试可以直接比较中间状态，也让未来的 replay 能够从初始状态、行动序列和随机序列稳定复现。
 */
data class BattleState(
	val format: BattleFormatSnapshot,
	val rules: BattleRuleSnapshot,
	val sides: List<BattleSide>,
	val turnNumber: Int,
	val events: List<BattleEvent>,
	val result: BattleResult? = null,
) {
	init {
		require(turnNumber >= 0) { "turnNumber must not be negative" }
	}

	/**
	 * 查找任意一方的成员。
	 */
	fun participant(actorId: String): BattleParticipant? =
		sides.asSequence().mapNotNull { it.participant(actorId) }.firstOrNull()

	/**
	 * 查找成员所属方。
	 */
	fun sideOf(actorId: String): BattleSide? =
		sides.firstOrNull { side -> side.participant(actorId) != null }

	/**
	 * 替换成员状态。
	 */
	fun replaceParticipant(participant: BattleParticipant): BattleState =
		copy(sides = sides.map { side ->
			if (side.participant(participant.actorId) == null) side else side.replaceParticipant(participant)
		})

	/**
	 * 追加事件。
	 */
	fun appendEvent(event: BattleEvent): BattleState =
		copy(events = events + event)

	/**
	 * 追加一组事件。
	 */
	fun appendEvents(newEvents: List<BattleEvent>): BattleState =
		copy(events = events + newEvents)
}
