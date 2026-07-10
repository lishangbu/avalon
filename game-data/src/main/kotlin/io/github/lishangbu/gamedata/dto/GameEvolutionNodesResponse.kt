package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 进化链节点响应。
 */
@Schema(name = "GameEvolutionNodesResponse", description = "进化链节点响应。")
@Immutable
interface GameEvolutionNodesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("chain_id")
	@get:Schema(description = "进化链 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val chainId: Long?
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("parent_species_id")
	@get:Schema(description = "父级种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val parentSpeciesId: Long?
	@get:JsonProperty("baby")
	@get:Schema(description = "幼体")
	val baby: Boolean?
	@get:JsonProperty("node_order")
	@get:Schema(description = "节点顺序")
	val nodeOrder: Int?
}
