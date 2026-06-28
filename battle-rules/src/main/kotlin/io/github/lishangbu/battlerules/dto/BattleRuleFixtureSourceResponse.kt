package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗规则 Fixture 公开来源维护响应。
 */
@Schema(description = "战斗规则 Fixture 公开来源维护响应。")
data class BattleRuleFixtureSourceResponse(
	@field:Schema(description = "Fixture 来源主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "Fixture ID。")
	val fixtureId: Long,
	@field:Schema(description = "公开来源 HTTPS 地址。")
	val sourceUrl: String,
	@field:Schema(description = "来源简短名称。", nullable = true)
	val sourceLabel: String?,
	@field:Schema(description = "来源说明。", nullable = true)
	val sourceNote: String?,
	@field:Schema(description = "展示排序。")
	val sortOrder: Int,
)
