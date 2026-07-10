package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗状态规则维护响应。
 */
@Schema(description = "战斗状态规则维护响应。")
@Immutable
interface BattleStatusRuleResponse {
	@get:Schema(type = "string", description = "状态规则主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "状态规则稳定 code。", example = "burn")
	val code: String
	@get:Schema(description = "状态规则简体中文名称。", example = "灼伤")
	val name: String
	@get:Schema(description = "状态类型。", example = "MAJOR")
	val statusKind: String
	@get:Schema(description = "引擎效果策略编码。", example = "major-burn")
	val effectPolicy: String
	@get:Schema(description = "最少持续回合。", example = "1", nullable = true)
	val minTurns: Int?
	@get:Schema(description = "最多持续回合。", example = "3", nullable = true)
	val maxTurns: Int?
	@get:Schema(description = "状态规则说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
