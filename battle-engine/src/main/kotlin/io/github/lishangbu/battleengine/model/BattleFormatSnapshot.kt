package io.github.lishangbu.battleengine.model

/**
 * 战斗开始时冻结的对战格式快照。
 *
 * 该快照来自规则资料模块，但引擎只接收已经组装好的不可变值，不直接读取数据库。
 * `activeParticipantsPerSide` 决定每方同时在场的成员数量；第一阶段要求单打时该值为 1。
 * `maxTurns` 用于后续实现回合上限和裁定逻辑，目前仅保存在状态里，不主动结束战斗。
 */
data class BattleFormatSnapshot(
	val code: String,
	val mode: BattleMode,
	val activeParticipantsPerSide: Int,
	val maxTurns: Int? = null,
) {
	init {
		require(code.isNotBlank()) { "format code must not be blank" }
		require(activeParticipantsPerSide > 0) { "activeParticipantsPerSide must be positive" }
		require(maxTurns == null || maxTurns > 0) { "maxTurns must be positive when present" }
	}
}
