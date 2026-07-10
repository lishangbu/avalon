package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 性格战斗风格偏好响应。
 */
@Schema(name = "GameNatureBattleStylePreferencesResponse", description = "性格战斗风格偏好响应。")
@Immutable
interface GameNatureBattleStylePreferencesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("nature_id")
	@get:Schema(description = "性格 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val natureId: Long?
	@get:JsonProperty("battle_style_id")
	@get:Schema(description = "战斗风格 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val battleStyleId: Long?
	@get:JsonProperty("low_hp_preference")
	@get:Schema(description = "低体力偏好")
	val lowHpPreference: Int?
	@get:JsonProperty("high_hp_preference")
	@get:Schema(description = "高体力偏好")
	val highHpPreference: Int?
}
