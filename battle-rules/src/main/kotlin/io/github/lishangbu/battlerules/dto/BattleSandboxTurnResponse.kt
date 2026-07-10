package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒回合结算响应。
 *
 * `resolved=false` 表示行动提交没有通过规则校验，此时 `violations` 会给出原因，`sides` 和 `events`
 * 反映当前快照状态。`resolved=true` 表示已经完整结算一回合，事件流、随机 trace 和 [state] 可用于调试规则顺序
 * 以及继续结算下一回合。
 */
@Schema(description = "战斗沙盒回合结算响应。")
data class BattleSandboxTurnResponse(
	@field:Schema(description = "是否完成回合结算。", example = "true")
	val resolved: Boolean,
	@field:Schema(description = "当前回合序号。", example = "1")
	val turnNumber: Int,
	@field:Schema(description = "战斗结果；未结束时为空。", nullable = true)
	val result: Result?,
	@field:Schema(description = "双方运行态摘要。")
	val sides: List<Side>,
	@field:Schema(description = "战斗事件日志，按发生顺序排列。")
	val events: List<Event>,
	@field:Schema(description = "行动校验违规项；仅在 resolved=false 时非空。")
	val violations: List<BattleActionViolationResponse>,
	@field:Schema(description = "本次回合结算命中的规则摘要。")
	val ruleHits: List<RuleHitSummary>,
	@field:Schema(description = "本回合随机消费 trace。")
	val randomTrace: List<RandomTrace>,
	@field:Schema(description = "可直接带入下一次请求的连续回合状态快照。")
	val state: BattleSandboxStateSnapshot,
) {
	/**
	 * 战斗结束后的结果摘要。
	 *
	 * 沙盒响应只需要告诉前端胜利方和结束原因；完整的倒下、替换失败或回合结束事件仍保留在事件流中。这样列表区可以快速
	 * 展示结果，而排障时又能回到结构化事件查看具体触发顺序。
	 */
	@Schema(name = "BattleSandboxTurnResult", description = "战斗结果摘要。")
	data class Result(
		@field:Schema(description = "获胜方 ID；平局或无胜方时为空。", nullable = true, example = "side-a")
		var winningSideId: String? = null,
		@field:Schema(description = "结果原因。", example = "all-opponents-fainted")
		var reason: String = "",
	)

	/**
	 * 回合响应中用于页面摘要的一方状态。
	 *
	 * 与连续 [BattleSandboxStateSnapshot.Side] 不同，这里只面向本次响应展示：当前上场成员和成员摘要足够驱动管理页表格。
	 * 下一回合续算必须使用 [BattleSandboxTurnResponse.state]，不能从这个摘要反推完整运行态。
	 */
	@Schema(name = "BattleSandboxTurnSide", description = "一方运行态摘要。")
	data class Side(
		@field:Schema(description = "队伍侧 ID。", example = "side-a")
		val sideId: String = "",
		@field:Schema(description = "当前上场成员 actorId。")
		val activeActorIds: List<String> = emptyList(),
		@field:Schema(description = "成员运行态摘要。")
		val participants: List<BattleSandboxTurnParticipant> = emptyList(),
	)

	/**
	 * 回合响应中的结构化事件展示项。
	 *
	 * [type] 保留引擎事件类型，方便测试和排障精确定位；[typeLabel] 与 [message] 提供中文展示；[payload] 保存该事件
	 * 的原始结构化字段，供前端详情抽屉或日志导出使用。事件只描述已经发生的事实，不应被下一回合请求当作规则输入。
	 */
	@Schema(name = "BattleSandboxTurnEvent", description = "战斗事件日志。")
	data class Event(
		@field:Schema(description = "事件类型。", example = "SkillUsed")
		var type: String = "",
		@field:Schema(description = "事件类型的简短中文名称，用于管理页表格、筛选和日志摘要直接展示。", example = "使用技能")
		var typeLabel: String = "",
		@field:Schema(description = "事件发生回合。", example = "1")
		var turnNumber: Int = 0,
		@field:Schema(description = "简短中文说明。")
		var message: String = "",
		@field:Schema(description = "事件结构化字段。")
		var payload: Map<String, Any?> = emptyMap(),
	)

	/**
	 * 本次沙盒响应命中的规则摘要。
	 *
	 * 规则族对应 312 条规则覆盖账本中的 12 个稳定分组；规则项来自本回合新增事件、行动违规或随机 trace。
	 * 它只服务管理页排障和覆盖报告串联，不参与下一回合状态恢复，也不替代事件流里的完整结构化事实。
	 */
	@Schema(name = "BattleSandboxRuleHitSummary", description = "沙盒规则命中摘要。")
	data class RuleHitSummary(
		@field:Schema(description = "规则族编码。", example = "damage-formula-stat-element-rounding")
		val familyCode: String = "",
		@field:Schema(description = "规则族中文名称。", example = "伤害公式、能力与属性")
		val familyName: String = "",
		@field:Schema(description = "规则项编码。", example = "DamageApplied")
		val itemCode: String = "",
		@field:Schema(description = "规则项中文名称。", example = "造成伤害")
		val itemName: String = "",
		@field:Schema(description = "本次响应中的触发次数。", example = "1")
		val triggerCount: Int = 0,
	)

	/**
	 * 本回合随机数消费记录。
	 *
	 * 每条记录保留消费顺序、上界、原因和实际值，用于复盘命中、同速、段数、异常追加等随机分支。它不参与下一回合规则计算，
	 * 只帮助确认同一输入和同一随机脚本能够得到完全可重复的事件流。
	 */
	@Schema(name = "BattleSandboxTurnRandomTrace", description = "随机消费记录。")
	data class RandomTrace(
		@field:Schema(description = "本回合内消费顺序。", example = "1")
		var sequence: Int = 0,
		@field:Schema(description = "随机上界，合法值范围为 [0, bound)。", example = "100")
		var bound: Int = 0,
		@field:Schema(description = "消费原因。", example = "accuracy")
		var reason: String = "",
		@field:Schema(description = "实际随机值。", example = "42")
		var value: Int = 0,
	)
}
