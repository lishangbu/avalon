package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗特性规则维护响应。
 */
@Schema(description = "战斗特性规则维护响应。")
@Immutable
interface BattleAbilityRuleResponse {
	@get:Schema(type = "string", description = "特性规则主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "特性 ID，引用基础游戏资料。", example = "65")
	@JsonConverter(LongToStringConverter::class)
	val abilityId: Long
	@get:Schema(description = "触发时机。", example = "BEFORE_DAMAGE")
	val triggerTiming: String
	@get:Schema(description = "效果策略编码。", example = "low-hp-grass-boost")
	val effectPolicy: String
	@get:Schema(description = "同一触发时机内的结算顺序。", example = "100")
	val triggerOrder: Int
	@get:Schema(description = "特性规则说明。", nullable = true)
	val description: String?
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
