package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能场地属性覆盖维护响应。
 */
@Schema(description = "技能场地属性覆盖维护响应。")
@Immutable
interface BattleSkillTerrainElementOverrideResponse {
	@get:Schema(type = "string", description = "技能场地属性覆盖主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "技能规则 ID。", example = "100000")
	@JsonConverter(LongToStringConverter::class)
	val skillRuleId: Long
	@get:Schema(type = "string", description = "场地规则 ID。", example = "4")
	@JsonConverter(LongToStringConverter::class)
	val terrainRuleId: Long
	@get:Schema(type = "string", description = "目标属性 ID，引用游戏属性资料。", example = "14")
	@JsonConverter(LongToStringConverter::class)
	val targetElementId: Long
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
