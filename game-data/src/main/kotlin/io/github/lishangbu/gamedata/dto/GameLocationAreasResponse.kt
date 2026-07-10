package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 地点区域响应。
 */
@Schema(name = "GameLocationAreasResponse", description = "地点区域响应。")
@Immutable
interface GameLocationAreasResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("location_id")
	@get:Schema(description = "地点 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val locationId: Long?
	@get:JsonProperty("game_index")
	@get:Schema(description = "索引")
	val gameIndex: Int?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
