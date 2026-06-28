package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗规则 Fixture 维护响应。
 */
@Schema(description = "战斗规则 Fixture 维护响应。")
data class BattleRuleFixtureResponse(
	@field:Schema(description = "Fixture 主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "Fixture 稳定 code。")
	val code: String,
	@field:Schema(description = "Fixture 简体中文名称。")
	val name: String,
	@field:Schema(description = "规则分类。")
	val category: String,
	@field:Schema(description = "Fixture 类型。")
	val fixtureType: String,
	@field:Schema(description = "赛制 code。", nullable = true)
	val formatCode: String?,
	@field:Schema(description = "场景说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "输入摘要。")
	val inputSummary: String,
	@field:Schema(description = "期望摘要。")
	val expectedSummary: String,
	@field:Schema(description = "是否启用。")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。")
	val sortOrder: Int,
)
