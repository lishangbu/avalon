package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * 战斗规则 Fixture 测试运行结果维护响应。
 */
@Schema(description = "战斗规则 Fixture 测试运行结果维护响应。")
data class BattleRuleTestRunResponse(
	@field:Schema(description = "测试运行主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "测试运行稳定 code。")
	val runCode: String,
	@field:Schema(description = "Fixture ID。")
	val fixtureId: Long,
	@field:Schema(description = "运行状态。")
	val runStatus: String,
	@field:Schema(description = "执行器名称。")
	val executor: String,
	@field:Schema(description = "执行命令摘要。", nullable = true)
	val command: String?,
	@field:Schema(description = "后端提交或构建号。", nullable = true)
	val engineCommit: String?,
	@field:Schema(description = "开始时间。")
	val startedAt: OffsetDateTime,
	@field:Schema(description = "结束时间。", nullable = true)
	val finishedAt: OffsetDateTime?,
	@field:Schema(description = "耗时毫秒数。", nullable = true)
	val durationMs: Long?,
	@field:Schema(description = "断言数量。", nullable = true)
	val assertionCount: Int?,
	@field:Schema(description = "失败摘要。", nullable = true)
	val failureMessage: String?,
	@field:Schema(description = "展示排序。")
	val sortOrder: Int,
)
