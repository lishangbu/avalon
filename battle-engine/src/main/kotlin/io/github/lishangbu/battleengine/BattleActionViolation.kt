package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 一条回合行动提交违规。
 *
 * `code` 是稳定机器码；`actorId` 定位提交行动的成员；`targetActorId` 在目标或替换目标相关问题中提供；
 * `resourceId` 用于技能、锁定技能等数值资料；`message` 是面向管理端或调试报告的简体中文说明。
 */
data class BattleActionViolation(
	val code: String,
	val actorId: String,
	val targetActorId: String? = null,
	val resourceId: Long? = null,
	val message: String,
)
