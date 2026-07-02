package io.github.lishangbu.battleengine.model

import io.github.lishangbu.battleengine.canBattle

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
	 * 在指定一侧新增速度结算修正。
	 *
	 * 返回 null 表示目标侧不存在，或该侧已经有同种速度修正。调用方据此决定是否产生事件；状态对象自身不推断
	 * 技能失败原因，保持这里仅表达不可变状态变更。
	 */
	fun addSideSpeedModifier(sideId: String, modifier: BattleSideSpeedModifier): BattleState? {
		var changed = false
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				side.addSpeedModifier(modifier)?.also { changed = true } ?: side
			}
		}
		return if (changed) copy(sides = nextSides) else null
	}

	/**
	 * 在指定一侧新增或叠加入场陷阱。
	 *
	 * 返回 null 表示目标侧不存在、同种不可叠层陷阱已存在，或可叠层陷阱已经达到最大层数。调用方据此决定是否
	 * 产生层数变化事件；状态对象自身不理解技能成功或失败的叙事，只表达不可变状态是否发生了变化。
	 */
	fun addSideEntryHazard(sideId: String, hazard: BattleSideEntryHazard): BattleSideEntryHazardStateChange? {
		var changedHazard: BattleSideEntryHazard? = null
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				val result = side.addEntryHazard(hazard)
				if (result == null) {
					side
				} else {
					changedHazard = result.hazard
					result.side
				}
			}
		}
		return changedHazard?.let { BattleSideEntryHazardStateChange(copy(sides = nextSides), it) }
	}

	/**
	 * 从指定一侧移除入场陷阱。
	 *
	 * 当前用于毒菱吸收。返回 null 表示目标侧不存在或该陷阱本来就不存在，调用方不应产生移除事件。
	 */
	fun removeSideEntryHazard(sideId: String, kind: BattleSideEntryHazardKind): BattleState? {
		var changed = false
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				side.removeEntryHazard(kind)?.also { changed = true } ?: side
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
	 * 若 `targetActorId` 已经不在场，返回其所属方当前可战斗的第一个上场成员。引擎用该行为表达
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
	 *
	 * [BattleState] 是不可变快照，所以追加事件必须返回一个新实例，不能原地修改 [events]。这里不用
	 * `events + event`，而是用 `buildList(events.size + 1)` 预分配目标容量：语义仍然是复制旧事件再追加一个
	 * 新事实，但可以减少 Kotlin 在长 replay 事件流下反复扩容或创建中间集合的开销。
	 *
	 * 这个优化只改变集合构造方式，不改变事件顺序、事件对象、状态复制边界，也不把事件流改成可变结构。调用方
	 * 仍然可以把返回的新状态视为完整的、可比较的战斗事实快照。
	 */
	fun appendEvent(event: BattleEvent): BattleState =
		copy(
			events = buildList(events.size + 1) {
				addAll(events)
				add(event)
			},
		)

	/**
	 * 追加一组事件。
	 *
	 * 空列表直接返回当前状态，避免无意义复制。非空时同样预分配 `旧事件数量 + 新事件数量` 的容量，并按传入顺序
	 * 追加。战斗 replay 依赖事件顺序完全稳定，因此这里不会排序、去重、合并事件，也不会因为事件类型相同而压缩。
	 */
	fun appendEvents(newEvents: List<BattleEvent>): BattleState =
		if (newEvents.isEmpty()) {
			this
		} else {
			copy(
				events = buildList(events.size + newEvents.size) {
					addAll(events)
					addAll(newEvents)
				},
			)
		}
}
