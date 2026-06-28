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
	val environment: BattleEnvironment,
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
	 * 判断成员是否当前上场。
	 */
	fun isActive(actorId: String): Boolean =
		sideOf(actorId)?.isActive(actorId) == true

	/**
	 * 替换成员状态。
	 */
	fun replaceParticipant(participant: BattleParticipant): BattleState =
		copy(sides = sides.map { side ->
			if (side.participant(participant.actorId) == null) side else side.replaceParticipant(participant)
		})

	/**
	 * 在指定一侧新增防守方伤害减免屏障。
	 *
	 * 返回 null 表示目标侧不存在，或该侧已经有同种屏障。调用方据此决定是否产生事件；状态对象自身不猜测
	 * “技能失败”文案，保持领域模型只表达确定的状态变更。
	 */
	fun addSideDamageReduction(sideId: String, reduction: BattleSideDamageReduction): BattleState? {
		var changed = false
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				side.addDamageReduction(reduction)?.also { changed = true } ?: side
			}
		}
		return if (changed) copy(sides = nextSides) else null
	}

	/**
	 * 替换当前上场成员。
	 */
	fun switchActive(previousActorId: String, nextActorId: String): BattleState =
		copy(sides = sides.map { side ->
			if (side.participant(previousActorId) == null) side else side.switchActive(previousActorId, nextActorId)
		})

	/**
	 * 推进所有一侧场上状态的持续回合。
	 */
	fun advanceSideConditionDurations(): BattleState =
		copy(sides = sides.map { it.advanceSideConditionDurations() })

	/**
	 * 解析目标槽位当前成员。
	 *
	 * 若 `targetActorId` 已经不在场，返回其所属方当前可战斗的第一个上场成员。单打第一版用该行为表达
	 * “攻击目标槽位而非固定成员”的换人后目标重定向。
	 */
	fun activeTargetFor(targetActorId: String): BattleParticipant? {
		val side = sideOf(targetActorId) ?: return null
		if (side.isActive(targetActorId)) {
			return side.participant(targetActorId)
		}
		return side.activeParticipants().firstOrNull { it.canBattle() }
	}

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
