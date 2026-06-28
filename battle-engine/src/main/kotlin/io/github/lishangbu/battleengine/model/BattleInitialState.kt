package io.github.lishangbu.battleengine.model

/**
 * 战斗引擎启动输入。
 *
 * 该对象是一次战斗的完整初始快照，包含格式、规则和双方队伍。它应由上层应用在战斗开始前组装完成，
 * 引擎不会在启动时查询数据库或远程服务。
 *
 * 第一阶段只接受两方对战，并要求每方当前上场成员数量等于格式要求。
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
		require(sides.all { it.activeActorIds.size == format.activeParticipantsPerSide }) {
			"active participant count must match format"
		}
		require(format.mode == BattleMode.SINGLE) { "only SINGLE mode is implemented in the first engine batch" }
	}
}
