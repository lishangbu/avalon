package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.BattleActionViolation

/** 表示完整回合中的行动组合违反共享战斗行动约束。 */
class InvalidTurnActionsException(
	val violations: List<BattleActionViolation>,
) : IllegalArgumentException("turn command contains invalid action combination: ${violations.joinToString { it.code }}")
