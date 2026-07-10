package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗场地规则维护响应。
 */
@Schema(description = "战斗场地规则维护响应。")
@Immutable
interface BattleTerrainRuleResponse {
	@get:Schema(type = "string", description = "场地规则主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "场地规则稳定 code。", example = "electric-terrain")
	val code: String
	@get:Schema(description = "场地规则简体中文名称。", example = "电气场地")
	val name: String
	@get:Schema(description = "引擎效果策略编码。", example = "terrain-electric")
	val effectPolicy: String
	@get:Schema(description = "默认持续回合。", example = "5", nullable = true)
	val defaultDurationTurns: Int?
	@get:Schema(description = "场地规则说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
