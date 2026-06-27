package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 进化链节点响应。
 */
@Schema(name = "GameEvolutionNodesResponse", description = "进化链节点响应。")
data class GameEvolutionNodesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("chain_id")
	@field:Schema(description = "进化链 ID")
	val chainId: Long?,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("parent_species_id")
	@field:Schema(description = "父级种类 ID")
	val parentSpeciesId: Long?,
	@get:JsonProperty("baby")
	@field:Schema(description = "幼体")
	val baby: Boolean?,
	@get:JsonProperty("node_order")
	@field:Schema(description = "节点顺序")
	val nodeOrder: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameEvolutionNodesResponse =
			GameEvolutionNodesResponse(
				id = record.id,
				chainId = record.longField("chain_id"),
				speciesId = record.longField("species_id"),
				parentSpeciesId = record.longField("parent_species_id"),
				baby = record.booleanField("baby"),
				nodeOrder = record.intField("node_order")
			)
	}
}
