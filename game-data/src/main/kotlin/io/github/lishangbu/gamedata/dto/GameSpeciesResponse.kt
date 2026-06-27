package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类资料响应。
 */
@Schema(name = "GameSpeciesResponse", description = "种类资料响应。")
data class GameSpeciesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("color_id")
	@field:Schema(description = "颜色 ID")
	val colorId: Long?,
	@get:JsonProperty("shape_id")
	@field:Schema(description = "形态 ID")
	val shapeId: Long?,
	@get:JsonProperty("habitat_id")
	@field:Schema(description = "栖息地 ID")
	val habitatId: Long?,
	@get:JsonProperty("gender_rate")
	@field:Schema(description = "性别比例")
	val genderRate: Int?,
	@get:JsonProperty("capture_rate")
	@field:Schema(description = "捕获率")
	val captureRate: Int?,
	@get:JsonProperty("base_happiness")
	@field:Schema(description = "初始亲和度")
	val baseHappiness: Int?,
	@get:JsonProperty("hatch_counter")
	@field:Schema(description = "孵化计数")
	val hatchCounter: Int?,
	@get:JsonProperty("baby")
	@field:Schema(description = "幼体")
	val baby: Boolean?,
	@get:JsonProperty("legendary")
	@field:Schema(description = "传说级")
	val legendary: Boolean?,
	@get:JsonProperty("mythical")
	@field:Schema(description = "幻级")
	val mythical: Boolean?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSpeciesResponse =
			GameSpeciesResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				colorId = record.longField("color_id"),
				shapeId = record.longField("shape_id"),
				habitatId = record.longField("habitat_id"),
				genderRate = record.intField("gender_rate"),
				captureRate = record.intField("capture_rate"),
				baseHappiness = record.intField("base_happiness"),
				hatchCounter = record.intField("hatch_counter"),
				baby = record.booleanField("baby"),
				legendary = record.booleanField("legendary"),
				mythical = record.booleanField("mythical"),
				enabled = record.booleanField("enabled")
			)
	}
}
