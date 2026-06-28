package io.github.lishangbu.battleengine.model

/**
 * 战斗开始时冻结的对战格式快照。
 *
 * 该快照来自规则资料模块，但引擎只接收已经组装好的不可变值，不直接读取数据库。
 * `activeParticipantsPerSide` 必须与 `mode` 的现代站位定义一致；`teamSize` 用于限制单方登记成员数量；
 * `maxTurns` 用于格式级回合上限裁定，达到上限且没有其它胜负结果时按平局结束。
 */
data class BattleFormatSnapshot(
	val code: String,
	val mode: BattleMode,
	val activeParticipantsPerSide: Int,
	val playerCount: Int = 2,
	val teamSize: Int? = null,
	val defaultLevel: Int? = null,
	val maxTurns: Int? = null,
) {
	init {
		require(code.isNotBlank()) { "format code must not be blank" }
		require(playerCount in 2..4) { "playerCount must be between 2 and 4" }
		require(activeParticipantsPerSide > 0) { "activeParticipantsPerSide must be positive" }
		require(activeParticipantsPerSide == mode.activeParticipantsPerSide) {
			"activeParticipantsPerSide must match battle mode"
		}
		require(teamSize == null || teamSize > 0) { "teamSize must be positive when present" }
		require(defaultLevel == null || defaultLevel in 1..100) { "defaultLevel must be between 1 and 100 when present" }
		require(maxTurns == null || maxTurns > 0) { "maxTurns must be positive when present" }
	}
}
