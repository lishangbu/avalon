package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗规则 Fixture 维护请求。
 */
@Schema(description = "战斗规则 Fixture 维护请求。")
data class BattleRuleFixtureRequest(
	@field:Schema(description = "Fixture 稳定 code。", example = "burn-halves-physical-attacking-stat-before-damage")
	var code: String = "",
	@field:Schema(description = "Fixture 简体中文名称。", example = "灼伤物理攻击减半")
	var name: String = "",
	@field:Schema(description = "规则分类。", example = "STATUS")
	var category: String = "",
	@field:Schema(description = "Fixture 类型。", example = "FORMULA")
	var fixtureType: String = "",
	@field:Schema(description = "赛制 code，公式级 Fixture 可为空。", nullable = true)
	var formatCode: String? = null,
	@field:Schema(description = "场景说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "输入摘要。")
	var inputSummary: String = "",
	@field:Schema(description = "期望摘要。")
	var expectedSummary: String = "",
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
