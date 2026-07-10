package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 技能评价组合响应。
 */
@Schema(name = "GameSkillContestCombosResponse", description = "技能评价组合响应。")
@Immutable
interface GameSkillContestCombosResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("skill_id")
	@get:Schema(description = "技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long?
	@get:JsonProperty("combo_type")
	@get:Schema(description = "组合类型")
	val comboType: String?
	@get:JsonProperty("relation_type")
	@get:Schema(description = "关系类型")
	val relationType: String?
	@get:JsonProperty("related_skill_id")
	@get:Schema(description = "关联技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val relatedSkillId: Long?
}
