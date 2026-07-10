package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗准备阶段校验违规项。
 */
@Schema(description = "战斗准备阶段校验违规项。")
@Immutable
interface BattlePreparationViolationResponse {
	@get:Schema(description = "稳定违规 code。", example = "level-too-high")
	val code: String
	@get:Schema(description = "队伍侧 ID。", example = "side-a")
	val sideId: String
	@get:Schema(description = "成员 actorId。", example = "side-a-1")
	val actorId: String
	@get:Schema(type = "string", description = "触发规则的资料 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val resourceId: Long
	@get:Schema(description = "简体中文说明。")
	val message: String
}
