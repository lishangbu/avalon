package io.github.lishangbu.battlesession.model

/** 区分运行中、引擎自然完成与外部显式终止三种会话生命周期状态。 */
enum class BattleSessionStatus {
	ACTIVE,
	COMPLETED,
	TERMINATED,
}
