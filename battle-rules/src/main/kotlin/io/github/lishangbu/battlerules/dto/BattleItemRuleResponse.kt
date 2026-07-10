package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗道具规则维护响应。
 */
@Schema(description = "战斗道具规则维护响应。")
@Immutable
interface BattleItemRuleResponse {
	@get:Schema(type = "string", description = "道具规则主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "道具 ID，引用基础游戏资料。", example = "211")
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long
	@get:Schema(description = "触发时机。", example = "HELD_END_TURN")
	val triggerTiming: String
	@get:Schema(description = "效果策略编码。", example = "leftovers-heal")
	val effectPolicy: String
	@get:Schema(description = "触发后是否消耗该道具。", example = "false")
	val consumable: Boolean
	@get:Schema(description = "同一触发时机内的结算顺序。", example = "100")
	val triggerOrder: Int
	@get:Schema(description = "道具规则说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
