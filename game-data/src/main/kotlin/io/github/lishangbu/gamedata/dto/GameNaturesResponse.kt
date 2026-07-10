package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 性格资料响应。
 */
@Schema(name = "GameNaturesResponse", description = "性格资料响应。")
@Immutable
interface GameNaturesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("increased_stat_id")
	@get:Schema(description = "提升数值项 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val increasedStatId: Long?
	@get:JsonProperty("decreased_stat_id")
	@get:Schema(description = "降低数值项 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val decreasedStatId: Long?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
