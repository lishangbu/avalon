package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 属性克制关系响应。
 */
@Schema(name = "GameElementDamageRelationsResponse", description = "属性克制关系响应。")
data class GameElementDamageRelationsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("source_element_id")
	@field:Schema(description = "来源属性 ID")
	val sourceElementId: Long?,
	@get:JsonProperty("target_element_id")
	@field:Schema(description = "目标属性 ID")
	val targetElementId: Long?,
	@get:JsonProperty("relation_type")
	@field:Schema(description = "关系类型")
	val relationType: String?,
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameElementDamageRelationsResponse =
			GameElementDamageRelationsResponse(
				id = record.id,
				sourceElementId = record.longField("source_element_id"),
				targetElementId = record.longField("target_element_id"),
				relationType = record.stringField("relation_type")
			)
	}
}
