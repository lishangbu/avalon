package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵资料响应。
 */
@Schema(name = "GameCreatureResponse", description = "精灵资料响应。")
data class GameCreatureResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("height")
	@field:Schema(description = "高度")
	val height: Int?,
	@get:JsonProperty("weight")
	@field:Schema(description = "重量")
	val weight: Int?,
	@get:JsonProperty("base_experience")
	@field:Schema(description = "基础经验")
	val baseExperience: Int?,
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int?,
	@get:JsonProperty("default_form")
	@field:Schema(description = "默认形态")
	val defaultForm: Boolean?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureResponse =
			GameCreatureResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				speciesId = record.longField("species_id"),
				height = record.intField("height"),
				weight = record.intField("weight"),
				baseExperience = record.intField("base_experience"),
				sortOrder = record.intField("sort_order"),
				defaultForm = record.booleanField("default_form"),
				enabled = record.booleanField("enabled")
			)
	}
}
