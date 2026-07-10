package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗场上效果规则维护响应。
 */
@Schema(description = "战斗场上效果规则维护响应。")
@Immutable
interface BattleFieldRuleResponse {
	@get:Schema(type = "string", description = "场上效果主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "场上效果稳定 code。", example = "reflect")
	val code: String
	@get:Schema(description = "场上效果简体中文名称。", example = "反射壁")
	val name: String
	@get:Schema(description = "效果范围。", example = "SIDE")
	val effectScope: String
	@get:Schema(description = "引擎效果策略编码。", example = "side-reflect")
	val effectPolicy: String
	@get:Schema(description = "最少持续回合。", example = "5", nullable = true)
	val minTurns: Int?
	@get:Schema(description = "最多持续回合。", example = "8", nullable = true)
	val maxTurns: Int?
	@get:Schema(description = "可叠加层数上限。", example = "3", nullable = true)
	val maxLayers: Int?
	@get:Schema(description = "场上效果说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
