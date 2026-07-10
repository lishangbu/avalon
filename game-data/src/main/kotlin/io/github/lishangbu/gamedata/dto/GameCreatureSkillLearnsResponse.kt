package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵技能学习响应。
 */
@Schema(name = "GameCreatureSkillLearnsResponse", description = "精灵技能学习响应。")
@Immutable
interface GameCreatureSkillLearnsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("skill_id")
	@get:Schema(description = "技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long?
	@get:JsonProperty("learn_method_id")
	@get:Schema(description = "学习方式 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val learnMethodId: Long?
	@get:JsonProperty("level_learned_at")
	@get:Schema(description = "习得等级")
	val levelLearnedAt: Int?
}
