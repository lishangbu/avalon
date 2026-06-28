package io.github.lishangbu.battlerules.dto

/**
 * 战斗规则覆盖报告。
 *
 * 覆盖报告服务于管理端观察当前引擎实现进度，不表示数据库中的可维护业务资料。
 */
data class BattleRuleCoverageResponse(
	val summary: BattleRuleCoverageSummaryResponse,
	val items: List<BattleRuleCoverageItemResponse>,
)

/**
 * 战斗规则覆盖汇总。
 */
data class BattleRuleCoverageSummaryResponse(
	val totalCount: Int,
	val implementedCount: Int,
	val partialCount: Int,
	val plannedCount: Int,
	val fixtureCount: Int,
	val implementationPercent: Int,
)

/**
 * 单个规则点的覆盖情况。
 *
 * `status` 使用稳定英文枚举值，前端负责翻译展示；`referenceUrls` 保存公开规则或成熟实现来源。
 */
data class BattleRuleCoverageItemResponse(
	val code: String,
	val name: String,
	val category: String,
	val status: String,
	val fixtureNames: List<String>,
	val referenceUrls: List<String>,
	val note: String,
)
