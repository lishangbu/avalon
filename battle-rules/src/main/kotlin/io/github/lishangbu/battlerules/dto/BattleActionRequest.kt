package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.ToStringSerializer

/**
 * 战斗行动请求。
 */
@Schema(description = "战斗行动请求。")
data class BattleActionRequest(
	@field:Schema(description = "行动类型：USE_SKILL 或 SWITCH_PARTICIPANT。", example = "USE_SKILL")
	var type: String = "",
	@field:Schema(description = "行动成员 actorId。", example = "side-a-1")
	var actorId: String = "",
	@field:Schema(description = "技能资料 ID；使用技能时必填。", nullable = true, example = "1")
	@field:JsonSerialize(using = ToStringSerializer::class)
	var skillId: Long? = null,
	@field:Schema(description = "目标成员 actorId 或替换目标 actorId。", example = "side-b-1")
	var targetActorId: String = "",
)
