package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 生物形态响应。
 */
@Schema(name = "GameCreatureFormsResponse", description = "生物形态响应。")
data class GameCreatureFormsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "生物 ID")
	val creatureId: Long?,
	@get:JsonProperty("form_name")
	@field:Schema(description = "形态名")
	val formName: String?,
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int?,
	@get:JsonProperty("form_order")
	@field:Schema(description = "形态排序")
	val formOrder: Int?,
	@get:JsonProperty("battle_only")
	@field:Schema(description = "仅战斗")
	val battleOnly: Boolean?,
	@get:JsonProperty("default_form")
	@field:Schema(description = "默认形态")
	val defaultForm: Boolean?,
	@get:JsonProperty("enhanced_form")
	@field:Schema(description = "强化形态")
	val enhancedForm: Boolean?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureFormsResponse =
			GameCreatureFormsResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				creatureId = record.longField("creature_id"),
				formName = record.stringField("form_name"),
				sortOrder = record.intField("sort_order"),
				formOrder = record.intField("form_order"),
				battleOnly = record.booleanField("battle_only"),
				defaultForm = record.booleanField("default_form"),
				enhancedForm = record.booleanField("enhanced_form"),
				enabled = record.booleanField("enabled")
			)
	}
}
