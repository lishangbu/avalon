package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗特殊机制维护响应。
 */
@Schema(description = "战斗特殊机制维护响应。")
@Immutable
interface BattleSpecialMechanicResponse {
	@get:Schema(type = "string", description = "特殊机制主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "特殊机制稳定 code。", example = "temporary-type-boost")
	val code: String
	@get:Schema(description = "特殊机制简体中文名称。", example = "临时属性强化")
	val name: String
	@get:Schema(description = "特殊机制说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
