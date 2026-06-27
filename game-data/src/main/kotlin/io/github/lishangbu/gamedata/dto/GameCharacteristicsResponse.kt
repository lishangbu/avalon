package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 个体特征响应。
 */
@Schema(name = "GameCharacteristicsResponse", description = "个体特征响应。")
data class GameCharacteristicsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("highest_stat_id")
	@field:Schema(description = "最高数值项 ID")
	val highestStatId: Long?,
	@get:JsonProperty("gene_modulo")
	@field:Schema(description = "模数")
	val geneModulo: Int?,
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCharacteristicsResponse =
			GameCharacteristicsResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				highestStatId = record.longField("highest_stat_id"),
				geneModulo = record.intField("gene_modulo"),
				description = record.stringField("description"),
				enabled = record.booleanField("enabled")
			)
	}
}
