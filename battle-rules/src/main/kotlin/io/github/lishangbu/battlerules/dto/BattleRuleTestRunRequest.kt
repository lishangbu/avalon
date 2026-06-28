package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * 战斗规则 Fixture 测试运行结果维护请求。
 */
@Schema(description = "战斗规则 Fixture 测试运行结果维护请求。")
data class BattleRuleTestRunRequest(
	@field:Schema(description = "测试运行稳定 code。", example = "local-fixture-run-20260628")
	var runCode: String = "",
	@field:Schema(description = "Fixture ID。", example = "1")
	var fixtureId: Long = 0,
	@field:Schema(description = "运行状态。", example = "PASSED")
	var runStatus: String = "",
	@field:Schema(description = "执行器名称。", example = "gradle")
	var executor: String = "",
	@field:Schema(description = "执行命令摘要。", nullable = true)
	var command: String? = null,
	@field:Schema(description = "后端提交或构建号。", nullable = true)
	var engineCommit: String? = null,
	@field:Schema(description = "开始时间；为空时由服务端写入当前时间。", nullable = true)
	var startedAt: OffsetDateTime? = null,
	@field:Schema(description = "结束时间。", nullable = true)
	var finishedAt: OffsetDateTime? = null,
	@field:Schema(description = "耗时毫秒数。", nullable = true)
	var durationMs: Long? = null,
	@field:Schema(description = "断言数量。", nullable = true)
	var assertionCount: Int? = null,
	@field:Schema(description = "失败摘要。", nullable = true)
	var failureMessage: String? = null,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
