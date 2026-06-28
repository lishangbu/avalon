package io.github.lishangbu.battleengine.model

/**
 * 战斗引擎启动输入。
 *
 * 该对象是一次战斗的完整初始快照，包含格式、规则和双方队伍。它应由上层应用在战斗开始前组装完成，
 * 引擎不会在启动时查询数据库或远程服务。
 *
 * 引擎当前以两方对战为边界，每方当前上场成员数量必须等于格式要求，所有成员 ID 必须在整场战斗内唯一。
 * 若格式限制了队伍登记成员数量，初始快照也会在这里拒绝超额队伍。
 */
data class BattleInitialState(
	val format: BattleFormatSnapshot,
	val rules: BattleRuleSnapshot,
	val environment: BattleEnvironment = BattleEnvironment(),
	val sides: List<BattleSide>,
) {
	init {
		require(sides.size == 2) { "exactly two sides are required" }
		require(sides.map { it.sideId }.toSet().size == sides.size) { "side ids must be unique" }
		val actorIds = sides.flatMap { side -> side.participants.map { it.actorId } }
		require(actorIds.toSet().size == actorIds.size) { "actor ids must be unique across all sides" }
		require(sides.all { it.activeActorIds.size == format.activeParticipantsPerSide }) {
			"active participant count must match format"
		}
		format.teamSize?.let { teamSize ->
			require(sides.all { it.participants.size <= teamSize }) { "participant count must not exceed teamSize" }
		}
	}
}
