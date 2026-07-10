package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 个体特征写入请求。
 */
@Schema(name = "GameCharacteristicsRequest", description = "个体特征写入请求。")
data class GameCharacteristicsRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("highest_stat_id")
	@get:JsonProperty("highest_stat_id")
	@field:Schema(description = "最高数值项 ID")
	val highestStatId: Long? = null,
	@param:JsonProperty("gene_modulo")
	@get:JsonProperty("gene_modulo")
	@field:Schema(description = "模数")
	val geneModulo: Int? = null,
	@param:JsonProperty("description")
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
)
