package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性格战斗风格偏好写入请求。
 */
@Schema(name = "GameNatureBattleStylePreferencesRequest", description = "性格战斗风格偏好写入请求。")
data class GameNatureBattleStylePreferencesRequest(
	@param:JsonProperty("nature_id")
	@get:JsonProperty("nature_id")
	@field:Schema(description = "性格 ID")
	val natureId: Long? = null,
	@param:JsonProperty("battle_style_id")
	@get:JsonProperty("battle_style_id")
	@field:Schema(description = "战斗风格 ID")
	val battleStyleId: Long? = null,
	@param:JsonProperty("low_hp_preference")
	@get:JsonProperty("low_hp_preference")
	@field:Schema(description = "低体力偏好")
	val lowHpPreference: Int? = null,
	@param:JsonProperty("high_hp_preference")
	@get:JsonProperty("high_hp_preference")
	@field:Schema(description = "高体力偏好")
	val highHpPreference: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"nature_id" to natureId,
		"battle_style_id" to battleStyleId,
		"low_hp_preference" to lowHpPreference,
		"high_hp_preference" to highHpPreference,
		)
}
