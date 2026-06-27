package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 进化链节点写入请求。
 */
@Schema(name = "GameEvolutionNodesRequest", description = "进化链节点写入请求。")
data class GameEvolutionNodesRequest(
	@param:JsonProperty("chain_id")
	@get:JsonProperty("chain_id")
	@field:Schema(description = "进化链 ID")
	val chainId: Long? = null,
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("parent_species_id")
	@get:JsonProperty("parent_species_id")
	@field:Schema(description = "父级种类 ID")
	val parentSpeciesId: Long? = null,
	@param:JsonProperty("baby")
	@get:JsonProperty("baby")
	@field:Schema(description = "幼体")
	val baby: Boolean? = null,
	@param:JsonProperty("node_order")
	@get:JsonProperty("node_order")
	@field:Schema(description = "节点顺序")
	val nodeOrder: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"chain_id" to chainId,
		"species_id" to speciesId,
		"parent_species_id" to parentSpeciesId,
		"baby" to baby,
		"node_order" to nodeOrder,
		)
}
