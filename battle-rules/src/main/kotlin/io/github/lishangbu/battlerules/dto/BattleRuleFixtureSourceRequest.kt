package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗规则 Fixture 公开来源维护请求。
 */
@Schema(description = "战斗规则 Fixture 公开来源维护请求。")
data class BattleRuleFixtureSourceRequest(
	@field:Schema(description = "Fixture ID。", example = "1")
	var fixtureId: Long = 0,
	@field:Schema(description = "公开来源 HTTPS 地址。")
	var sourceUrl: String = "",
	@field:Schema(description = "来源简短名称。", nullable = true)
	var sourceLabel: String? = null,
	@field:Schema(description = "来源说明。", nullable = true)
	var sourceNote: String? = null,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
