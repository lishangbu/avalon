package io.github.lishangbu.battleengine.model

/**
 * 战斗站位模式。
 *
 * 枚举只表达现代规则下的站位形态，不携带历史版本或外部平台语义。每个模式都给出单方同时上场成员数量，
 * 让格式快照可以在战斗开始前校验“规则声明”和“实际队伍站位”是否一致。
 */
enum class BattleMode {
	SINGLE,
	DOUBLE;

	/**
	 * 单方同时上场成员数量。
	 *
	 * 单打为 1，双打为 2。后续若支持其它现代变体，应在这里明确建模，而不是让调用方随意传数字。
	 */
	val activeParticipantsPerSide: Int
		get() = when (this) {
			SINGLE -> 1
			DOUBLE -> 2
		}
}
