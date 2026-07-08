package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒复盘保存请求。
 */
@Schema(description = "战斗沙盒复盘保存请求。")
data class BattleSandboxReplayRequest(
	@field:Schema(description = "复盘标题。", example = "标准单打第 3 回合异常排查")
	var title: String = "",
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	var formatCode: String = "",
	@field:Schema(description = "沙盒回合结算响应 JSON 文本。")
	var responseJson: String = "",
)
