package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制条款绑定维护响应。
 */
@Schema(description = "战斗赛制条款绑定维护响应。")
@Immutable
interface BattleFormatClauseBindingResponse {
	@get:Schema(type = "string", description = "绑定主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "赛制 ID。", example = "3")
	@JsonConverter(LongToStringConverter::class)
	val formatId: Long
	@get:Schema(type = "string", description = "条款 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val clauseId: Long
	@get:Schema(description = "是否为强制条款。", example = "true")
	val required: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
