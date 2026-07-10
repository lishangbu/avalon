package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 树果口味强度写入请求。
 */
@Schema(name = "GameBerryFlavorPotenciesRequest", description = "树果口味强度写入请求。")
data class GameBerryFlavorPotenciesRequest(
	@param:JsonProperty("berry_id")
	@get:JsonProperty("berry_id")
	@field:Schema(description = "树果 ID")
	val berryId: Long? = null,
	@param:JsonProperty("flavor_id")
	@get:JsonProperty("flavor_id")
	@field:Schema(description = "口味 ID")
	val flavorId: Long? = null,
	@param:JsonProperty("potency")
	@get:JsonProperty("potency")
	@field:Schema(description = "强度")
	val potency: Int? = null
)
