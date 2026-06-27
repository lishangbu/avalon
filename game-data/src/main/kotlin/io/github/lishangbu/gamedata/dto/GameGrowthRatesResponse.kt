package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 成长速率响应。
 */
@Schema(name = "GameGrowthRatesResponse", description = "成长速率响应。")
data class GameGrowthRatesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("formula")
	@field:Schema(description = "公式")
	val formula: String?,
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameGrowthRatesResponse =
			GameGrowthRatesResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				formula = record.stringField("formula"),
				description = record.stringField("description"),
				enabled = record.booleanField("enabled")
			)
	}
}
