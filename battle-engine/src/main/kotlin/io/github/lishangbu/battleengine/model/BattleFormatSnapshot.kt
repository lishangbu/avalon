package io.github.lishangbu.battleengine.model

/**
 * 战斗开始时冻结的对战格式快照。
 *
 * 该快照来自规则资料模块，但引擎只接收已经组装好的不可变值，不直接读取数据库。
 * `activeParticipantsPerSide` 必须与 `mode` 的现代站位定义一致；`teamSize` 用于限制单方实际带入战斗的成员数量；
 * `maxTurns` 用于格式级回合上限裁定，达到上限且没有其它胜负结果时按平局结束。
 *
 * 注意：资料管理层可以同时维护“登记人数”和“队伍预览后选择人数”。纯引擎看不到队伍预览流程，只消费已经选好
 * 的参战队伍，因此这里的 `teamSize` 必须是运行态参战人数，而不是后台赛制主表上的登记人数。
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
