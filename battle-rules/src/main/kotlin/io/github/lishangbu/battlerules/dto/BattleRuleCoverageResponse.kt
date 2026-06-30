package io.github.lishangbu.battlerules.dto

import java.time.OffsetDateTime

/**
 * 战斗规则覆盖报告。
 *
 * 覆盖报告服务于管理端观察当前引擎实现进度，不表示数据库中的可维护业务资料。
 */
data class BattleRuleCoverageResponse(
	val summary: BattleRuleCoverageSummaryResponse,
	val targetSummary: BattleRuleCoverageTargetSummaryResponse,
	val fixtureSummary: BattleRuleCoverageFixtureSummaryResponse,
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
 * 公开对照 fixture 与最近运行结果汇总。
 */
data class BattleRuleCoverageFixtureSummaryResponse(
	val runtimeAvailable: Boolean,
	val fixtureReferenceCount: Int,
	val matchedFixtureCount: Int,
	val missingFixtureCount: Int,
	val latestPassedCount: Int,
	val latestFailedCount: Int,
	val latestRunningCount: Int,
	val withoutRunCount: Int,
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
 * `status` 使用稳定英文枚举值，前端负责翻译展示；公开来源保留在对应测试 fixture 附近。
 */
data class BattleRuleCoverageItemResponse(
	val code: String,
	val name: String,
	val category: String,
	val status: String,
	val fixtureNames: List<String>,
	val fixtures: List<BattleRuleCoverageFixtureResponse>,
	val note: String,
)

/**
 * 覆盖项绑定的公开对照 fixture 和最近一次运行结果。
 */
data class BattleRuleCoverageFixtureResponse(
	val code: String,
	val fixtureId: Long?,
	val name: String?,
	val enabled: Boolean?,
	val latestRunCode: String?,
	val latestRunStatus: String?,
	val latestRunStartedAt: OffsetDateTime?,
	val missing: Boolean,
)
