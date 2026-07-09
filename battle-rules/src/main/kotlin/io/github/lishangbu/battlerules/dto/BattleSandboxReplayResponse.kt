package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * 战斗沙盒复盘详情响应。
 */
@Schema(description = "战斗沙盒复盘详情响应。")
data class BattleSandboxReplayResponse(
	@field:Schema(description = "复盘记录 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "复盘标题。")
	val title: String,
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	val formatCode: String,
	@field:Schema(description = "保存时的最新回合序号。", example = "3")
	val turnNumber: Int,
	@field:Schema(description = "保存时该回合是否完成结算。", example = "true")
	val resolved: Boolean,
	@field:Schema(description = "战斗结果摘要；未结束时为空。", nullable = true)
	val resultSummary: String?,
	@field:Schema(description = "保存时间。")
	val savedAt: Instant,
	@field:Schema(description = "产生该响应的沙盒回合请求 JSON 文本；旧记录可能为空。", nullable = true)
	val requestJson: String?,
	@field:Schema(description = "可直接导入战斗沙盒继续查看或续算的响应 JSON 文本。")
	val responseJson: String,
)

/**
 * 战斗沙盒复盘列表响应。
 */
@Schema(description = "战斗沙盒复盘列表响应。")
data class BattleSandboxReplaySummaryResponse(
	@field:Schema(description = "复盘记录 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "复盘标题。")
	val title: String,
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	val formatCode: String,
	@field:Schema(description = "保存时的最新回合序号。", example = "3")
	val turnNumber: Int,
	@field:Schema(description = "保存时该回合是否完成结算。", example = "true")
	val resolved: Boolean,
	@field:Schema(description = "战斗结果摘要；未结束时为空。", nullable = true)
	val resultSummary: String?,
	@field:Schema(description = "保存时间。")
	val savedAt: Instant,
)
