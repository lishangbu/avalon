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
	@field:Schema(description = "本回合随机消费 trace。")
	val randomTrace: List<RandomTrace>,
	@field:Schema(description = "可直接带入下一次请求的连续回合状态快照。")
	val state: BattleSandboxStateSnapshot,
) {
	@Schema(name = "BattleSandboxTurnResult", description = "战斗结果摘要。")
	data class Result(
		@field:Schema(description = "获胜方 ID；平局或无胜方时为空。", nullable = true, example = "side-a")
		var winningSideId: String? = null,
		@field:Schema(description = "结果原因。", example = "all-opponents-fainted")
		var reason: String = "",
	)

	@Schema(name = "BattleSandboxTurnSide", description = "一方运行态摘要。")
	data class Side(
		@field:Schema(description = "队伍侧 ID。", example = "side-a")
		val sideId: String = "",
		@field:Schema(description = "当前上场成员 actorId。")
		val activeActorIds: List<String> = emptyList(),
		@field:Schema(description = "成员运行态摘要。")
		val participants: List<Participant> = emptyList(),
	)

	@Schema(name = "BattleSandboxTurnParticipant", description = "成员运行态摘要。")
	data class Participant(
		@field:Schema(description = "战斗内成员 ID。", example = "side-a-1")
		val actorId: String = "",
		@field:Schema(description = "精灵资料 ID。", example = "1")
		val creatureId: Long = 0,
		@field:Schema(description = "是否当前上场。", example = "true")
		val active: Boolean = false,
		@field:Schema(description = "等级。", example = "50")
		val level: Int = 0,
		@field:Schema(description = "当前 HP。", example = "100")
		val currentHp: Int = 0,
		@field:Schema(description = "最大 HP。", example = "120")
		val maxHp: Int = 0,
		@field:Schema(description = "主要异常状态；无异常时为空。", nullable = true, example = "BURN")
		val majorStatus: String? = null,
		@field:Schema(description = "能力阶级变化。")
		val statStages: Map<String, Int> = emptyMap(),
		@field:Schema(description = "技能槽运行态。")
		val skillSlots: List<SkillSlot> = emptyList(),
	)

	@Schema(name = "BattleSandboxTurnSkillSlot", description = "技能槽运行态。")
	data class SkillSlot(
		@field:Schema(description = "技能资料 ID。", example = "33")
		val skillId: Long = 0,
		@field:Schema(description = "技能名称。", example = "撞击")
		val name: String = "",
		@field:Schema(description = "剩余 PP。", example = "34")
		val remainingPp: Int = 0,
		@field:Schema(description = "最大 PP。", example = "35")
		val maxPp: Int = 0,
	)

	@Schema(name = "BattleSandboxTurnEvent", description = "战斗事件日志。")
	data class Event(
		@field:Schema(description = "事件类型。", example = "SkillUsed")
		var type: String = "",
		@field:Schema(description = "事件发生回合。", example = "1")
		var turnNumber: Int = 0,
		@field:Schema(description = "简短中文说明。")
		var message: String = "",
		@field:Schema(description = "事件结构化字段。")
		var payload: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "BattleSandboxTurnRandomTrace", description = "随机消费记录。")
	data class RandomTrace(
		@field:Schema(description = "本回合内消费顺序。", example = "1")
		val sequence: Int = 0,
		@field:Schema(description = "随机上界，合法值范围为 [0, bound)。", example = "100")
		val bound: Int = 0,
		@field:Schema(description = "消费原因。", example = "accuracy")
		val reason: String = "",
		@field:Schema(description = "实际随机值。", example = "42")
		val value: Int = 0,
	)
}
