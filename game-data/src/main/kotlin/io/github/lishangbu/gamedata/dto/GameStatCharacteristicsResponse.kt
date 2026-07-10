package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 数值项特征响应。
 */
@Schema(name = "GameStatCharacteristicsResponse", description = "数值项特征响应。")
@Immutable
interface GameStatCharacteristicsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("stat_id")
	@get:Schema(description = "数值项 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val statId: Long?
	@get:JsonProperty("characteristic_id")
	@get:Schema(description = "特征 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val characteristicId: Long?
}
