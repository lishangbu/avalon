package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 种类详情响应。
 */
@Schema(name = "GameSpeciesDetailsResponse", description = "种类详情响应。")
@Immutable
interface GameSpeciesDetailsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("growth_rate_id")
	@get:Schema(description = "成长速率 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val growthRateId: Long?
	@get:JsonProperty("evolves_from_species_id")
	@get:Schema(description = "进化来源种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val evolvesFromSpeciesId: Long?
	@get:JsonProperty("evolution_chain_id")
	@get:Schema(description = "进化链 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val evolutionChainId: Long?
	@get:JsonProperty("sort_order")
	@get:Schema(description = "排序")
	val sortOrder: Int?
	@get:JsonProperty("gender_differences")
	@get:Schema(description = "性别差异")
	val genderDifferences: Boolean?
	@get:JsonProperty("forms_switchable")
	@get:Schema(description = "形态可切换")
	val formsSwitchable: Boolean?
	@get:JsonProperty("genus")
	@get:Schema(description = "分类")
	val genus: String?
	@get:JsonProperty("flavor_text")
	@get:Schema(description = "风味说明")
	val flavorText: String?
}
