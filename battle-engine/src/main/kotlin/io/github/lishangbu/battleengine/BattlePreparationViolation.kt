package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSide

/**
 * 一条准备阶段规则违规。
 *
 * `code` 用于调用方稳定匹配错误类型；`sideId` 和 `actorId` 定位到队伍与成员；`resourceId` 记录触发规则的资料
 * ID，例如禁用技能 ID、重复道具 ID 或成员种类 ID。`message` 是面向管理端的简体中文说明。
 */
data class BattlePreparationViolation(
	val code: String,
	val sideId: String,
	val actorId: String,
	val resourceId: Long,
	val message: String,
)
