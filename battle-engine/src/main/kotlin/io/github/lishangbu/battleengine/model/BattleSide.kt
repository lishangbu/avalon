package io.github.lishangbu.battleengine.model

import io.github.lishangbu.battleengine.canBattle
import io.github.lishangbu.battleengine.leaveBattlefield

/**
 * 战斗中的一方。
 *
 * 一方包含队伍成员、当前上场成员 ID，以及只属于这一侧的场上状态。`activeActorIds` 的数量由
 * `BattleFormatSnapshot` 校验：单打为 1，双打为 2。目标选择、范围技能和防守方屏障会基于这些上场席位继续扩展。
 */
data class BattleSide(
	val sideId: String,
	val activeActorIds: List<String>,
	val participants: List<BattleParticipant>,
	val damageReductions: List<BattleSideDamageReduction> = emptyList(),
	val speedModifiers: List<BattleSideSpeedModifier> = emptyList(),
	val entryHazards: List<BattleSideEntryHazard> = emptyList(),
	val protections: List<BattleSideProtection> = emptyList(),
) {
	init {
		require(sideId.isNotBlank()) { "sideId must not be blank" }
		require(participants.isNotEmpty()) { "participants must not be empty" }
		require(participants.map { it.actorId }.toSet().size == participants.size) { "actor ids must be unique inside a side" }
		require(activeActorIds.isNotEmpty()) { "activeActorIds must not be empty" }
		require(activeActorIds.toSet().size == activeActorIds.size) { "activeActorIds must not contain duplicates" }
		require(activeActorIds.all { activeId -> participants.any { it.actorId == activeId } }) {
			"activeActorIds must reference participants on the same side"
		}
		require(damageReductions.map { it.kind }.toSet().size == damageReductions.size) {
			"damage reductions must not contain duplicate kinds"
		}
		require(speedModifiers.map { it.kind }.toSet().size == speedModifiers.size) {
			"speed modifiers must not contain duplicate kinds"
		}
		require(entryHazards.map { it.kind }.toSet().size == entryHazards.size) {
			"entry hazards must not contain duplicate kinds"
		}
		require(protections.map { it.kind }.toSet().size == protections.size) {
			"side protections must not contain duplicate kinds"
		}
	}

	/**
	 * 判断这一方是否仍有可战斗成员。
	 */
	fun hasRemainingParticipant(): Boolean =
		participants.any { it.canBattle() }

	/**
	 * 查找成员。
	 */
	fun participant(actorId: String): BattleParticipant? =
		participants.firstOrNull { it.actorId == actorId }

	/**
	 * 判断成员是否位于当前上场席位。
	 */
	fun isActive(actorId: String): Boolean =
		actorId in activeActorIds

	/**
	 * 返回当前上场成员快照。
	 */
	fun activeParticipants(): List<BattleParticipant> =
		activeActorIds.mapNotNull(::participant)

	/**
	 * 替换成员运行态。
	 */
	fun replaceParticipant(participant: BattleParticipant): BattleSide =
		copy(participants = participants.map { current -> if (current.actorId == participant.actorId) participant else current })

	/**
	 * 在这一侧新增一个防守方伤害减免屏障。
	 *
	 * 现代规则中，同一种屏障已经存在时再次使用通常不会刷新持续时间；这里返回 null 表示本次没有写入新状态。
	 * 其它种类的屏障可以共存，但单次伤害只会由引擎选中一个适用屏障参与倍率计算。
	 */
	fun addDamageReduction(reduction: BattleSideDamageReduction): BattleSide? {
		if (damageReductions.any { it.kind == reduction.kind }) {
			return null
		}
		return copy(damageReductions = damageReductions + reduction)
	}

	/**
	 * 在这一侧新增一个速度结算修正。
	 *
	 * 现代规则中，同一种一侧速度效果已经存在时再次使用通常不会刷新持续时间；这里返回 null 表示本次没有写入新状态。
	 * 不同种类的速度效果未来可以共存，行动排序时会按结构化倍率相乘，调用方不需要了解资料表 code。
	 */
	fun addSpeedModifier(modifier: BattleSideSpeedModifier): BattleSide? {
		if (speedModifiers.any { it.kind == modifier.kind }) {
			return null
		}
		return copy(speedModifiers = speedModifiers + modifier)
	}

	/**
	 * 在这一侧新增一个防护效果。
	 *
	 * 防护效果不刷新同种已存在状态：白雾或神秘守护已经生效时再次使用会被上层记录为技能失败，而不是悄悄重置
	 * 持续回合。这里仍只表达“能否写入状态”这一件事，不决定失败文案。
	 */
	fun addProtection(protection: BattleSideProtection): BattleSide? {
		if (protections.any { it.kind == protection.kind }) {
			return null
		}
		return copy(protections = protections + protection)
	}

	/**
	 * 判断这一侧是否拥有指定防护。
	 */
	fun hasProtection(kind: BattleSideProtectionKind): Boolean =
		protections.any { it.kind == kind }

	/**
	 * 在这一侧新增或叠加入场陷阱。
	 *
	 * 不存在同种陷阱时写入第一层；存在同种陷阱时只在该陷阱允许叠层且尚未达到最大层数时递增层数。返回 null
	 * 表示本次技能没有改变一侧场上状态，调用方不应产生层数变化事件。入场陷阱没有回合持续时间，因此不参与
	 * [advanceSideConditionDurations]。
	 */
	fun addEntryHazard(hazard: BattleSideEntryHazard): BattleSideEntryHazardAddResult? {
		val current = entryHazards.firstOrNull { it.kind == hazard.kind }
		val nextHazard = if (current == null) hazard else current.addLayer() ?: return null
		val nextHazards = if (current == null) {
			entryHazards + nextHazard
		} else {
			entryHazards.map { existing -> if (existing.kind == nextHazard.kind) nextHazard else existing }
		}
		return BattleSideEntryHazardAddResult(
			side = copy(entryHazards = nextHazards),
			hazard = nextHazard,
		)
	}

	/**
	 * 从这一侧移除指定入场陷阱。
	 *
	 * 当前用于毒菱被接地毒属性成员换入吸收。若陷阱不存在，返回 null 表示状态未变化，调用方也不应产生移除事件。
	 */
	fun removeEntryHazard(kind: BattleSideEntryHazardKind): BattleSide? {
		if (entryHazards.none { it.kind == kind }) {
			return null
		}
		return copy(entryHazards = entryHazards.filterNot { it.kind == kind })
	}

	/**
	 * 从这一侧移除全部入场陷阱。
	 *
	 * 高速旋转、晶光转转这类“命中后清场”规则会一次清除使用者一侧所有陷阱和层数。模型层只表达状态变化本身，
	 * 不判断技能是否命中、是否造成伤害或是否应该产生日志；这些仍由技能效果 resolver 在更高层根据阶段顺序决定。
	 */
	fun clearEntryHazards(): BattleSide? =
		if (entryHazards.isEmpty()) {
			null
		} else {
			copy(entryHazards = emptyList())
		}

	/**
	 * 从这一侧移除指定种类的伤害减免屏障。
	 *
	 * 击破屏障类技能会一次性清除目标侧物理屏障、特殊屏障和全伤害屏障。这里接收集合而不是单个 kind，是为了让
	 * 调用方在同一个不可变复制中完成批量删除，并能从返回值判断本次技能是否真的改变了目标侧场地状态。若目标侧
	 * 没有任何匹配屏障，返回 null，调用方也不应产生“屏障被清除”的 replay 事件。
	 */
	fun removeDamageReductions(kinds: Set<BattleSideDamageReductionKind>): BattleSide? {
		val remaining = damageReductions.filterNot { it.kind in kinds }
		if (remaining.size == damageReductions.size) {
			return null
		}
		return copy(damageReductions = remaining)
	}

	/**
	 * 从这一侧移除指定种类的非伤害型防护。
	 *
	 * 清除浓雾类技能会移除目标侧白雾和神秘守护；它们不参与伤害倍率，因此不能复用伤害减免屏障的删除函数。
	 * 返回 null 表示这一侧没有匹配防护，调用方也不应产生“防护被清除”的 replay 事件。
	 */
	fun removeProtections(kinds: Set<BattleSideProtectionKind>): BattleSide? {
		val remaining = protections.filterNot { it.kind in kinds }
		if (remaining.size == protections.size) {
			return null
		}
		return copy(protections = remaining)
	}

	/**
	 * 推进这一侧的回合型场上状态。
	 *
	 * 当前包含伤害减免屏障和速度修正。持续回合为空的状态保持不变；剩余 1 回合的状态在完整回合末移除。
	 * 入场陷阱没有回合持续时间，不在这里递减。
	 */
	fun advanceSideConditionDurations(): BattleSide =
		copy(
			damageReductions = damageReductions.mapNotNull { it.advanceTurn() },
			speedModifiers = speedModifiers.mapNotNull { it.advanceTurn() },
			protections = protections.mapNotNull { it.advanceTurn() },
		)

	/**
	 * 替换一个上场席位。
	 *
	 * `previousActorId` 必须当前在场；`nextActorId` 必须属于同一方、未在场并且仍可战斗。该函数只修改
	 * 上场席位，并清理离场成员的能力阶级和回合内连续保护计数。HP、PP 和主要异常状态保留在成员快照上。
	 */
	fun switchActive(previousActorId: String, nextActorId: String): BattleSide {
		require(isActive(previousActorId)) { "switch actor must be active: $previousActorId" }
		require(!isActive(nextActorId)) { "switch target must not already be active: $nextActorId" }
		val previous = requireNotNull(participant(previousActorId)) { "switch actor must belong to the same side: $previousActorId" }
		val next = requireNotNull(participant(nextActorId)) { "switch target must belong to the same side: $nextActorId" }
		require(next.canBattle()) { "switch target must be able to battle: $nextActorId" }
		return copy(
			activeActorIds = activeActorIds.map { current -> if (current == previousActorId) nextActorId else current },
			participants = participants.map { current ->
				if (current.actorId == previous.actorId) previous.leaveBattlefield() else current
			},
		)
	}
}
