package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制限制维护响应。
 */
@Schema(description = "战斗赛制限制维护响应。")
@Immutable
interface BattleFormatRestrictionResponse {
	@get:Schema(type = "string", description = "限制主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "赛制 ID。", example = "3")
	@JsonConverter(LongToStringConverter::class)
	val formatId: Long
	@get:Schema(description = "限制稳定 code。", example = "level-cap-50")
	val code: String
	@get:Schema(description = "限制简体中文名称。", example = "等级上限")
	val name: String
	@get:Schema(description = "限制类型。", example = "LEVEL")
	val restrictionType: String
	@get:Schema(description = "限制判定方式。", example = "MAX")
	val restrictionOperator: String
	@get:Schema(description = "文本操作数。", nullable = true)
	val operandText: String?
	@get:Schema(description = "数值操作数。", example = "50", nullable = true)
	val operandNumber: Int?
	@get:Schema(description = "限制说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
