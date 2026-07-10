package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒复盘校验响应。
 *
 * 校验接口面向已经保存的复盘请求和响应 JSON：先确认响应仍能被当前沙盒页面安全读取，再用原始请求重新调用
 * 当前运行时入口，比较重放响应是否与保存响应完全一致。这样生产排障时可以区分“JSON 结构坏了”和“当前规则
 * 已经跑不出当时结果”这两类问题。
 */
@Schema(description = "战斗沙盒复盘校验响应。")
@Immutable
interface BattleSandboxReplayValidationResponse {
	@get:Schema(type = "string", description = "复盘记录 ID。", example = "865732440461672401")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "复盘标题。")
	val title: String
	@get:Schema(description = "赛制稳定 code。", example = "standard-single")
	val formatCode: String
	@get:Schema(description = "保存时的最新回合序号。", example = "3")
	val turnNumber: Int
	@get:Schema(description = "保存时该回合是否完成结算。", example = "true")
	val resolved: Boolean
	@get:Schema(description = "复盘 JSON 是否通过当前结构校验。", example = "true")
	val valid: Boolean
	@get:Schema(description = "复盘 JSON 中累计事件数量。", example = "8")
	val eventCount: Int
	@get:Schema(description = "复盘 JSON 中已结算回合数量。", example = "3")
	val turnCount: Int
	@get:Schema(description = "复盘 JSON 中规则命中摘要数量。", example = "4")
	val ruleHitCount: Int
	@get:Schema(description = "复盘 JSON 中出现的规则族 code。")
	val ruleHitFamilyCodes: List<String>
	@get:Schema(description = "是否已经使用原始请求执行确定性重放。", example = "true")
	val deterministicReplayChecked: Boolean
	@get:Schema(description = "确定性重放结果是否与保存响应完全一致。", example = "true")
	val deterministicReplayMatched: Boolean
	@get:Schema(description = "不阻止导入但值得人工关注的问题。")
	val warnings: List<String>
	@get:Schema(description = "导致复盘不可安全导入或续算的问题。")
	val violations: List<String>
}
