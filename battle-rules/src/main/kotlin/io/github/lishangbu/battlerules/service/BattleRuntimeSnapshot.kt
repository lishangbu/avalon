package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot

/**
 * battle-rules 装配出的引擎运行时快照。
 *
 * 这是管理资料进入纯战斗引擎前的冻结边界：`format` 描述站位、队伍规模和等级拉平，
 * `rules` 描述准备阶段限制和战斗内基础规则。该类型不暴露 Jimmer 实体，也不保存数据库行，
 * 调用方拿到后只能按引擎模型读取，不能再依赖 battle-rules 的持久化结构。
 */
data class BattleRuntimeSnapshot(
	val format: BattleFormatSnapshot,
	val rules: BattleRuleSnapshot,
)
