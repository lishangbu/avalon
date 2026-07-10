package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleEffectTarget

/**
 * 技能状态效果和状态规则合并后的最小运行时行。
 *
 * `statusKind` 决定该行进入主要异常还是临时状态模型；目标作用域在创建该对象前已经校验并转换，避免后续拆分时
 * 对同一数据库值作出不同解释。
 */
internal data class StatusEffectRuntimeRow(
	val statusCode: String,
	val statusKind: String,
	val target: BattleEffectTarget,
	val chancePercent: Int,
)
