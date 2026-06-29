package io.github.lishangbu.battlerules.dto

/**
 * 战斗规则覆盖报告。
 *
 * 覆盖报告服务于管理端观察当前引擎实现进度，不表示数据库中的可维护业务资料。
 */
data class BattleRuleCoverageResponse(
	val summary: BattleRuleCoverageSummaryResponse,
	val targetSummary: BattleRuleCoverageTargetSummaryResponse,
	val matrix: List<BattleRuleCoverageMatrixRowResponse>,
	val checks: List<BattleRuleCoverageCheckResponse>,
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
 * 最终战斗系统规则目标汇总。
 *
 * 这里按可复用规则行为统计最终目标，不直接等同于当前覆盖报表的登记项数量。覆盖报表是已上线实现清单；
 * 目标汇总用于管理端跟踪离“现代主系列规则正确”的完整系统还剩多少行为族。
 */
data class BattleRuleCoverageTargetSummaryResponse(
	val targetRuleCount: Int,
	val coveredRuleCount: Int,
	val remainingRuleCount: Int,
	val implementationPercent: Int,
	val coverageItemCount: Int,
	val basis: String,
)

/**
 * 按规则分类聚合的覆盖矩阵行。
 */
data class BattleRuleCoverageMatrixRowResponse(
	val category: String,
	val totalCount: Int,
	val implementedCount: Int,
	val partialCount: Int,
	val plannedCount: Int,
	val fixtureCount: Int,
	val referenceCount: Int,
	val implementationPercent: Int,
)

/**
 * 覆盖报告完整性检查结果。
 */
data class BattleRuleCoverageCheckResponse(
	val code: String,
	val name: String,
	val status: String,
	val message: String,
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
