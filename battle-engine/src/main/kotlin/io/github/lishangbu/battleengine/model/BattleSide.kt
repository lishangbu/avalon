package io.github.lishangbu.battleengine.model

/**
 * 战斗中的一方。
 *
 * 一方包含队伍成员和当前上场成员 ID。`activeActorIds` 的数量由 `BattleFormatSnapshot` 校验：
 * 单打为 1，双打为 2。目标选择和范围技能会基于这些上场席位继续扩展。
 */
data class BattleSide(
	val sideId: String,
	val activeActorIds: List<String>,
	val participants: List<BattleParticipant>,
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
