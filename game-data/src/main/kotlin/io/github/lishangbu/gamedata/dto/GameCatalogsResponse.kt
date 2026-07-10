package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 图鉴目录响应。
 */
@Schema(name = "GameCatalogsResponse", description = "图鉴目录响应。")
@Immutable
interface GameCatalogsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("region_id")
	@get:Schema(description = "地区 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val regionId: Long?
	@get:JsonProperty("main_series")
	@get:Schema(description = "主体资料")
	val mainSeries: Boolean?
	@get:JsonProperty("description")
	@get:Schema(description = "说明")
	val description: String?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
