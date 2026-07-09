package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒复盘校验响应。
 *
 * 校验接口面向已经保存的复盘 JSON，返回它是否还能被当前沙盒页面和后端规则命中契约安全读取。它不会尝试重放
 * 整场战斗，因为当前复盘只保存响应快照，没有保存原始队伍请求中的特性、道具、能力值配置等完整输入。
 */
@Schema(description = "战斗沙盒复盘校验响应。")
data class BattleSandboxReplayValidationResponse(
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
	@field:Schema(description = "复盘 JSON 是否通过当前结构校验。", example = "true")
	val valid: Boolean,
	@field:Schema(description = "复盘 JSON 中累计事件数量。", example = "8")
	val eventCount: Int,
	@field:Schema(description = "复盘 JSON 中已结算回合数量。", example = "3")
	val turnCount: Int,
	@field:Schema(description = "复盘 JSON 中规则命中摘要数量。", example = "4")
	val ruleHitCount: Int,
	@field:Schema(description = "复盘 JSON 中出现的规则族 code。")
	val ruleHitFamilyCodes: List<String>,
	@field:Schema(description = "不阻止导入但值得人工关注的问题。")
	val warnings: List<String>,
	@field:Schema(description = "导致复盘不可安全导入或续算的问题。")
	val violations: List<String>,
)
