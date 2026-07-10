package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 技能资料响应。
 */
@Schema(name = "GameSkillResponse", description = "技能资料响应。")
@Immutable
interface GameSkillResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("element_id")
	@get:Schema(description = "属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val elementId: Long?
	@get:JsonProperty("damage_class_id")
	@get:Schema(description = "分类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val damageClassId: Long?
	@get:JsonProperty("accuracy")
	@get:Schema(description = "命中")
	val accuracy: Int?
	@get:JsonProperty("power")
	@get:Schema(description = "威力")
	val power: Int?
	@get:JsonProperty("pp")
	@get:Schema(description = "PP")
	val pp: Int?
	@get:JsonProperty("priority")
	@get:Schema(description = "优先级")
	val priority: Int?
	@get:JsonProperty("effect_chance")
	@get:Schema(description = "效果概率")
	val effectChance: Int?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
