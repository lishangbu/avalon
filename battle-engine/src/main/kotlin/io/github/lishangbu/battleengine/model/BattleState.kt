package io.github.lishangbu.battleengine.model

import io.github.lishangbu.battleengine.canBattle

/**
 * 战斗运行态。
 *
 * 状态是不可变数据结构：每次回合结算都会返回新的 `BattleState`，并追加事件流。
 * 这让测试可以直接比较中间状态，也让未来的 replay 能够从初始状态、行动序列和随机序列稳定复现。
 *
 * `BattleInitialState` 会在启动前校验双方、成员 ID 和上场席位；这里重复保留同一组队伍骨架不变量，是为了保护
 * 启动后的状态复制、沙盒恢复和 replay 复算。运行态允许 HP、PP、异常状态和场地效果变化，但不允许在一场战斗
 * 中途改变双方数量、制造全局重复 actorId，或让单打/双打的当前上场数量与赛制定义脱节。
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
		require(sides.size == 2) { "exactly two sides are required" }
		require(sides.map { it.sideId }.toSet().size == sides.size) { "side ids must be unique" }
		val actorIds = sides.flatMap { side -> side.participants.map { it.actorId } }
		require(actorIds.toSet().size == actorIds.size) { "actor ids must be unique across all sides" }
		require(sides.all { it.activeActorIds.size == format.activeParticipantsPerSide }) {
			"active participant count must match format"
		}
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
	fun replaceParticipant(participant: BattleParticipant): BattleState {
		val previous = participant(participant.actorId)
		val consumedItem = previous?.itemId != null &&
			participant.itemId == null &&
			participant.lastConsumedItemId == previous.itemId
		val effectiveParticipant = if (consumedItem && participant.lastConsumedItemTurn == null) {
			val nextConsumptionOrder = sides.flatMap { it.participants }
				.mapNotNull { it.lastConsumedItemOrder }
				.maxOrNull()?.plus(1) ?: 1
			participant.copy(
				lastConsumedItemTurn = turnNumber,
				lastConsumedItemOrder = nextConsumptionOrder,
				lastConsumedItemAvailableForPickup = true,
			)
		} else {
			participant
		}
		val replaced = copy(sides = sides.map { side ->
			if (side.participant(effectiveParticipant.actorId) == null) side else side.replaceParticipant(effectiveParticipant)
		})
		if (!consumedItem || !replaced.isActive(effectiveParticipant.actorId)) return replaced
		val side = replaced.sideOf(effectiveParticipant.actorId) ?: return replaced
		val donor = side.activeParticipants()
			.filter { it.actorId != effectiveParticipant.actorId && it.canBattle() && it.itemId != null }
			.filter { candidate ->
				candidate.abilityEffects.any { it is BattleAbilityEffect.AllyItemConsumptionTransfer } &&
					candidate.abilityEffects.none { it is BattleAbilityEffect.HeldItemRemovalImmunity }
			}
			.minByOrNull { it.actorId } ?: return replaced
		val donatedItemId = requireNotNull(donor.itemId)
		val recipient = effectiveParticipant.copy(
			itemId = donatedItemId,
			itemEffects = donor.itemEffects,
			itemLostSinceEntering = false,
			choiceLockedSkillId = null,
		)
		val emptiedDonor = donor.copy(
			itemId = null,
			itemEffects = emptyList(),
			itemLostSinceEntering = true,
			choiceLockedSkillId = null,
		)
		return replaced.copy(
			sides = replaced.sides.map { currentSide ->
				if (currentSide.sideId != side.sideId) currentSide else {
					currentSide.replaceParticipant(recipient).replaceParticipant(emptiedDonor)
				}
			},
			events = replaced.events + BattleEvent.HeldItemTransferred(
				replaced.turnNumber,
				donor.actorId,
				recipient.actorId,
				donatedItemId,
			),
		)
	}

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
	 * 在指定一侧新增防护效果。
	 *
	 * 返回 null 表示目标侧不存在，或该侧已经有同种防护。状态对象只表达不可变数据是否发生变化；“同种防护已经
	 * 存在时技能为何失败”由技能效果 resolver 追加事件，保持模型层不依赖具体技能文案。
	 */
	fun addSideProtection(sideId: String, protection: BattleSideProtection): BattleState? {
		var changed = false
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				side.addProtection(protection)?.also { changed = true } ?: side
			}
		}
		return if (changed) copy(sides = nextSides) else null
	}

	/**
	 * 判断指定成员所属侧是否存在某个一侧防护。
	 */
	fun sideHasProtection(actorId: String, kind: BattleSideProtectionKind): Boolean =
		sideOf(actorId)?.hasProtection(kind) == true

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
	 * 从指定一侧批量移除全部入场陷阱，并返回移除前真实存在的陷阱。
	 *
	 * 清场技能需要一次移除隐形岩、撒菱、毒菱和黏黏网，同时保留移除前的顺序用于事件流。这里返回完整
	 * [BattleSideEntryHazard]，而不是只返回 kind，是为了让测试和未来 replay 能确认“多层撒菱也被整组清掉”，
	 * 但事件层仍可以按需要只展示种类。
	 */
	fun clearSideEntryHazards(sideId: String): BattleSideEntryHazardRemoval? {
		var removedHazards = emptyList<BattleSideEntryHazard>()
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else if (side.entryHazards.isEmpty()) {
				side
			} else {
				removedHazards = side.entryHazards
				requireNotNull(side.clearEntryHazards())
			}
		}
		return removedHazards
			.takeIf { it.isNotEmpty() }
			?.let { BattleSideEntryHazardRemoval(copy(sides = nextSides), it) }
	}

	/**
	 * 从指定一侧批量移除伤害减免屏障，并返回被移除的屏障种类。
	 *
	 * 屏障击破技能需要在伤害计算前删除目标侧屏障，否则本次伤害会被刚刚被打碎的屏障错误减半。领域状态层只负责
	 * “这侧有哪些 kind 被删除”这一事实，不判断技能是否命中、目标是否免疫或是否被保护；那些仍由技能结算流程在
	 * 调用本函数前完成。返回 null 表示目标侧不存在或没有任何匹配屏障，调用方不应追加移除事件。
	 */
	fun removeSideDamageReductions(
		sideId: String,
		kinds: Set<BattleSideDamageReductionKind>,
	): BattleSideDamageReductionRemoval? {
		var removedKinds = emptyList<BattleSideDamageReductionKind>()
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				val currentRemovedKinds = side.damageReductions
					.filter { it.kind in kinds }
					.map { it.kind }
				if (currentRemovedKinds.isEmpty()) {
					side
				} else {
					removedKinds = currentRemovedKinds
					requireNotNull(side.removeDamageReductions(kinds))
				}
			}
		}
		return removedKinds
			.takeIf { it.isNotEmpty() }
			?.let { BattleSideDamageReductionRemoval(copy(sides = nextSides), it) }
	}

	/**
	 * 从指定一侧批量移除非伤害型防护，并返回被移除的防护种类。
	 *
	 * 白雾、神秘守护这类状态和光墙/反射壁同样挂在 side 上，但读取入口不同：前者阻止异常或能力下降，后者参与
	 * 伤害公式。清除浓雾类技能需要同时清掉两类 side 状态，因此这里提供与 [removeSideDamageReductions] 对称的
	 * 删除入口，避免调用方直接复制 [BattleSide] 内部列表。
	 */
	fun removeSideProtections(
		sideId: String,
		kinds: Set<BattleSideProtectionKind>,
	): BattleSideProtectionRemoval? {
		var removedKinds = emptyList<BattleSideProtectionKind>()
		val nextSides = sides.map { side ->
			if (side.sideId != sideId) {
				side
			} else {
				val currentRemovedKinds = side.protections
					.filter { it.kind in kinds }
					.map { it.kind }
				if (currentRemovedKinds.isEmpty()) {
					side
				} else {
					removedKinds = currentRemovedKinds
					requireNotNull(side.removeProtections(kinds))
				}
			}
		}
		return removedKinds
			.takeIf { it.isNotEmpty() }
			?.let { BattleSideProtectionRemoval(copy(sides = nextSides), it) }
	}

	/**
	 * 替换当前上场成员。
	 */
	fun switchActive(previousActorId: String, nextActorId: String): BattleState =
		copy(sides = sides.map { side ->
			if (side.participant(previousActorId) == null) side else side.switchActive(previousActorId, nextActorId)
		}).clearAccuracyLocksTargeting(previousActorId)

	/**
	 * 清除所有指向指定目标的命中锁定。
	 *
	 * Lock-On / Mind Reader 的效果绑定到“当时被锁定的成员”，不是绑定到站位槽。目标主动替换或被强制换下后，
	 * 后续换入成员不应继承旧目标的必中效果。现代规则允许多个使用者同时锁定同一目标，因此这里的“所有来源”
	 * 只用于目标离场时批量终止这些来源各自持有的运行态。
	 */
	fun clearAccuracyLocksTargeting(targetActorId: String): BattleState =
		sides
			.flatMap { it.participants }
			.fold(this) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (latest.accuracyLockTargetActorId == targetActorId) {
					current.replaceParticipant(
						latest.copy(
							accuracyLockTargetActorId = null,
							accuracyLockTurnsRemaining = 0,
						),
					)
				} else {
					current
				}
			}

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
