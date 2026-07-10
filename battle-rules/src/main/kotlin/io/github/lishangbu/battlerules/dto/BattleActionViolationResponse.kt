package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗行动校验违规项。
 */
@Schema(description = "战斗行动校验违规项。")
@Immutable
interface BattleActionViolationResponse {
	@get:Schema(description = "稳定违规 code。", example = "skill-not-found")
	val code: String
	@get:Schema(description = "行动成员 actorId。", example = "side-a-1")
	val actorId: String
	@get:Schema(description = "目标成员 actorId。", nullable = true, example = "side-b-1")
	val targetActorId: String?
	@get:Schema(type = "string", description = "触发规则的资料 ID。", nullable = true, example = "1")
	@JsonConverter(LongToStringConverter::class)
	val resourceId: Long?
	@get:Schema(description = "简体中文说明。")
	val message: String
}
