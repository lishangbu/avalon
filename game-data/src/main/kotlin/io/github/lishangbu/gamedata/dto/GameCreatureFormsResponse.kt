package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵形态响应。
 */
@Schema(name = "GameCreatureFormsResponse", description = "精灵形态响应。")
@Immutable
interface GameCreatureFormsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("form_name")
	@get:Schema(description = "形态名")
	val formName: String?
	@get:JsonProperty("sort_order")
	@get:Schema(description = "排序")
	val sortOrder: Int?
	@get:JsonProperty("form_order")
	@get:Schema(description = "形态排序")
	val formOrder: Int?
	@get:JsonProperty("battle_only")
	@get:Schema(description = "仅战斗")
	val battleOnly: Boolean?
	@get:JsonProperty("default_form")
	@get:Schema(description = "默认形态")
	val defaultForm: Boolean?
	@get:JsonProperty("enhanced_form")
	@get:Schema(description = "强化形态")
	val enhancedForm: Boolean?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
