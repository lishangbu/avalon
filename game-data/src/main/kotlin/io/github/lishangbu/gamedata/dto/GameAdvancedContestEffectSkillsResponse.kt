package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 高级评价效果技能响应。
 */
@Schema(name = "GameAdvancedContestEffectSkillsResponse", description = "高级评价效果技能响应。")
@Immutable
interface GameAdvancedContestEffectSkillsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("advanced_contest_effect_id")
	@get:Schema(description = "高级评价效果 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val advancedContestEffectId: Long?
	@get:JsonProperty("skill_id")
	@get:Schema(description = "技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long?
}
