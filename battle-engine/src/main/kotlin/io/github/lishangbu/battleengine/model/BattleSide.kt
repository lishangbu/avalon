package io.github.lishangbu.battleengine.model

/**
 * 战斗中的一方。
 *
 * 一方包含队伍成员和当前上场成员 ID。第一阶段只支持单打，因此 `activeActorIds` 在执行前会被格式校验限制为 1。
 * 双打扩展时，该集合会承载同一方多个站位的参与者，目标选择和范围技能也会基于这里扩展。
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
	 * 替换成员运行态。
	 */
	fun replaceParticipant(participant: BattleParticipant): BattleSide =
		copy(participants = participants.map { current -> if (current.actorId == participant.actorId) participant else current })
}
