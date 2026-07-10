package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 数值项技能影响响应。
 */
@Schema(name = "GameStatSkillEffectsResponse", description = "数值项技能影响响应。")
@Immutable
interface GameStatSkillEffectsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("stat_id")
	@get:Schema(description = "数值项 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val statId: Long?
	@get:JsonProperty("skill_id")
	@get:Schema(description = "技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long?
	@get:JsonProperty("change_value")
	@get:Schema(description = "变化值")
	val changeValue: Int?
	@get:JsonProperty("effect_type")
	@get:Schema(description = "影响类型")
	val effectType: String?
}
