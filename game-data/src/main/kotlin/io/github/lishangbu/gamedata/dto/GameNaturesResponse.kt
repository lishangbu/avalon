package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性格资料响应。
 */
@Schema(name = "GameNaturesResponse", description = "性格资料响应。")
data class GameNaturesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("increased_stat_id")
	@field:Schema(description = "提升数值项 ID")
	val increasedStatId: Long?,
	@get:JsonProperty("decreased_stat_id")
	@field:Schema(description = "降低数值项 ID")
	val decreasedStatId: Long?,
	@get:JsonProperty("likes_flavor_id")
	@field:Schema(description = "偏好口味 ID")
	val likesFlavorId: Long?,
	@get:JsonProperty("hates_flavor_id")
	@field:Schema(description = "厌恶口味 ID")
	val hatesFlavorId: Long?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameNaturesResponse =
			GameNaturesResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				increasedStatId = record.longField("increased_stat_id"),
				decreasedStatId = record.longField("decreased_stat_id"),
				likesFlavorId = record.longField("likes_flavor_id"),
				hatesFlavorId = record.longField("hates_flavor_id"),
				enabled = record.booleanField("enabled")
			)
	}
}
