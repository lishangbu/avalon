package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 迁移区域种类响应。
 */
@Schema(name = "GameTransferAreaSpeciesResponse", description = "迁移区域种类响应。")
@Immutable
interface GameTransferAreaSpeciesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("area_id")
	@get:Schema(description = "区域 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val areaId: Long?
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("base_score")
	@get:Schema(description = "基础分")
	val baseScore: Int?
	@get:JsonProperty("rate")
	@get:Schema(description = "概率")
	val rate: Int?
}
