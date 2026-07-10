package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制特殊机制绑定维护响应。
 */
@Schema(description = "战斗赛制特殊机制绑定维护响应。")
@Immutable
interface BattleFormatSpecialMechanicResponse {
	@get:Schema(type = "string", description = "绑定主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "赛制 ID。", example = "3")
	@JsonConverter(LongToStringConverter::class)
	val formatId: Long
	@get:Schema(type = "string", description = "特殊机制 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val mechanicId: Long
	@get:Schema(description = "该赛制是否启用该特殊机制。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
