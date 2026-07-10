package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 属性克制关系写入请求。
 */
@Schema(name = "GameElementDamageRelationsRequest", description = "属性克制关系写入请求。")
data class GameElementDamageRelationsRequest(
	@param:JsonProperty("source_element_id")
	@get:JsonProperty("source_element_id")
	@field:Schema(description = "来源属性 ID")
	val sourceElementId: Long? = null,
	@param:JsonProperty("target_element_id")
	@get:JsonProperty("target_element_id")
	@field:Schema(description = "目标属性 ID")
	val targetElementId: Long? = null,
	@param:JsonProperty("relation_type")
	@get:JsonProperty("relation_type")
	@field:Schema(description = "关系类型")
	val relationType: String? = null,
)
