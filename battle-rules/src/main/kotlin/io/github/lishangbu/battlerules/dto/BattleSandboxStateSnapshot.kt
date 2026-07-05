package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒连续回合状态快照。
 *
 * 该 DTO 是管理端和后端之间的无状态续算边界：响应返回当前快照，下一次请求原样带回即可继续结算。
 * 快照只保存跨回合会变化的运行态，不保存规则快照、技能完整定义或数据库实体；这些事实仍由后端按
 * `formatCode` 和当前资料表重新装配，避免前端持有过期规则。
 */
@Schema(description = "战斗沙盒连续回合状态快照。")
data class BattleSandboxStateSnapshot(
	@field:Schema(description = "当前已完成的回合序号。", example = "1")
	var turnNumber: Int = 0,
	@field:Schema(description = "已确认的战斗结果；未结束时为空。", nullable = true)
	var result: BattleSandboxTurnResponse.Result? = null,
	@field:Schema(description = "全场环境运行态。")
	var environment: Environment = Environment(),
	@field:Schema(description = "双方运行态。")
	var sides: List<Side> = emptyList(),
	@field:Schema(description = "累计事件流。")
	var events: List<BattleSandboxTurnResponse.Event> = emptyList(),
	@field:Schema(description = "已经成功结算的回合记录，用于导出和复查沙盒复盘材料。")
	var turns: List<TurnRecord> = emptyList(),
) {
	@Schema(name = "BattleSandboxStateTurnRecord", description = "已结算回合的复盘片段。")
	data class TurnRecord(
		@field:Schema(description = "已结算回合序号。", example = "1")
		var turnNumber: Int = 0,
		@field:Schema(description = "该回合提交并通过校验的行动。")
		var actions: List<BattleActionRequest> = emptyList(),
		@field:Schema(description = "该回合随机消费记录。")
		var randomTrace: List<BattleSandboxTurnResponse.RandomTrace> = emptyList(),
		@field:Schema(description = "该回合新增事件片段，不包含战斗启动事件和历史回合事件。")
		var events: List<BattleSandboxTurnResponse.Event> = emptyList(),
	)

	@Schema(name = "BattleSandboxStateEnvironment", description = "全场环境运行态。")
	data class Environment(
		@field:Schema(description = "天气枚举名。", example = "RAIN")
		var weather: String = "NONE",
		@field:Schema(description = "天气剩余回合；永久或不计时效果为空。", nullable = true)
		var weatherTurnsRemaining: Int? = null,
		@field:Schema(description = "场地枚举名。", example = "GRASSY")
		var terrain: String = "NONE",
		@field:Schema(description = "场地剩余回合；永久或不计时效果为空。", nullable = true)
		var terrainTurnsRemaining: Int? = null,
		@field:Schema(description = "全场速度顺序效果；不存在时为空。", nullable = true)
		var fieldSpeedOrderEffect: FieldSpeedOrderEffect? = null,
	)

	@Schema(name = "BattleSandboxStateFieldSpeedOrderEffect", description = "全场速度顺序效果快照。")
	data class FieldSpeedOrderEffect(
		@field:Schema(description = "效果种类枚举名。", example = "TRICK_ROOM")
		var kind: String = "",
		@field:Schema(description = "剩余回合；永久或不计时效果为空。", nullable = true)
		var turnsRemaining: Int? = null,
	)

	@Schema(name = "BattleSandboxStateSide", description = "一方运行态快照。")
	data class Side(
		@field:Schema(description = "队伍侧 ID。", example = "side-a")
		var sideId: String = "",
		@field:Schema(description = "当前上场成员 actorId。")
		var activeActorIds: List<String> = emptyList(),
		@field:Schema(description = "成员运行态。")
		var participants: List<Participant> = emptyList(),
		@field:Schema(description = "一侧伤害减免屏障。")
		var damageReductions: List<DamageReduction> = emptyList(),
		@field:Schema(description = "一侧速度修正。")
		var speedModifiers: List<SpeedModifier> = emptyList(),
		@field:Schema(description = "一侧入场陷阱。")
		var entryHazards: List<EntryHazard> = emptyList(),
	)

	@Schema(name = "BattleSandboxStateParticipant", description = "成员运行态快照。")
	data class Participant(
		@field:Schema(description = "战斗内成员 ID。", example = "side-a-1")
		var actorId: String = "",
		@field:Schema(description = "当前 HP。", example = "100")
		var currentHp: Int = 0,
		@field:Schema(description = "当前属性 ID 集合。")
		var elementIds: List<Long> = emptyList(),
		@field:Schema(description = "当前是否接地。", example = "true")
		var grounded: Boolean = true,
		@field:Schema(description = "主要异常状态枚举名；无异常时为空。", nullable = true)
		var majorStatus: String? = null,
		@field:Schema(description = "能力阶级变化。")
		var statStages: Map<String, Int> = emptyMap(),
		@field:Schema(description = "技能槽 PP 运行态。")
		var skillSlots: List<SkillSlot> = emptyList(),
		@field:Schema(description = "本次上场后的技能行动尝试次数。", example = "0")
		var activeSkillActionCount: Int = 0,
		@field:Schema(description = "临时体重减轻量。", example = "0")
		var weightReduction: Int = 0,
		@field:Schema(description = "连续保护计数。", example = "0")
		var protectionChain: Int = 0,
		@field:Schema(description = "本回合挺住来源技能 ID；没有挺住姿态时为空。", nullable = true)
		var fatalDamageEndureSkillId: Long? = null,
		@field:Schema(description = "剧毒计数。", example = "0")
		var badPoisonCounter: Int = 0,
		@field:Schema(description = "睡眠剩余阻止行动次数。", example = "0")
		var sleepTurnsRemaining: Int = 0,
		@field:Schema(description = "蓄力技能 ID；未蓄力时为空。", nullable = true)
		var chargingSkillId: Long? = null,
		@field:Schema(description = "蓄力目标 actorId；未蓄力时为空。", nullable = true)
		var chargingTargetActorId: String? = null,
		@field:Schema(description = "蓄力剩余回合。", example = "0")
		var chargingTurnsRemaining: Int = 0,
		@field:Schema(description = "休整剩余回合。", example = "0")
		var rechargeTurnsRemaining: Int = 0,
		@field:Schema(description = "本回合是否畏缩。", example = "false")
		var flinched: Boolean = false,
		@field:Schema(description = "混乱剩余回合。", example = "0")
		var confusionTurnsRemaining: Int = 0,
		@field:Schema(description = "回复封锁剩余回合。", example = "0")
		var healBlockTurnsRemaining: Int = 0,
		@field:Schema(description = "挑衅剩余回合。", example = "0")
		var tauntTurnsRemaining: Int = 0,
		@field:Schema(description = "被定身技能 ID；未定身时为空。", nullable = true)
		var disabledSkillId: Long? = null,
		@field:Schema(description = "定身剩余回合。", example = "0")
		var disabledSkillTurnsRemaining: Int = 0,
		@field:Schema(description = "是否处于无理取闹状态。", example = "false")
		var tormented: Boolean = false,
		@field:Schema(description = "束缚来源 actorId；未束缚时为空。", nullable = true)
		var boundByActorId: String? = null,
		@field:Schema(description = "束缚剩余回合。", example = "0")
		var bindingTurnsRemaining: Int = 0,
		@field:Schema(description = "寄生种子来源侧 ID；未被寄生时为空。", nullable = true)
		var leechSeedSourceSideId: String? = null,
		@field:Schema(description = "寄生种子来源侧上场席位索引；未被寄生时为空。", nullable = true)
		var leechSeedSourceActiveIndex: Int? = null,
		@field:Schema(description = "上一次成功使用的技能 ID；没有时为空。", nullable = true)
		var lastSuccessfulSkillId: Long? = null,
		@field:Schema(description = "命中锁定目标 actorId；未锁定时为空。", nullable = true)
		var accuracyLockTargetActorId: String? = null,
		@field:Schema(description = "命中锁定剩余回合末递减次数。", example = "0")
		var accuracyLockTurnsRemaining: Int = 0,
		@field:Schema(description = "锁招技能 ID；未锁招时为空。", nullable = true)
		var lockedMoveSkillId: Long? = null,
		@field:Schema(description = "锁招目标 actorId；未锁招时为空。", nullable = true)
		var lockedMoveTargetActorId: String? = null,
		@field:Schema(description = "锁招剩余回合。", example = "0")
		var lockedMoveTurnsRemaining: Int = 0,
		@field:Schema(description = "锁招结束后是否混乱。", example = "false")
		var lockedMoveConfusesOnEnd: Boolean = false,
		@field:Schema(description = "讲究类道具锁定技能 ID；未锁定时为空。", nullable = true)
		var choiceLockedSkillId: Long? = null,
		@field:Schema(description = "替身剩余 HP。", example = "0")
		var substituteHp: Int = 0,
	)

	@Schema(name = "BattleSandboxStateSkillSlot", description = "技能槽 PP 快照。")
	data class SkillSlot(
		@field:Schema(description = "技能资料 ID。", example = "1")
		var skillId: Long = 0,
		@field:Schema(description = "剩余 PP。", example = "34")
		var remainingPp: Int = 0,
	)

	@Schema(name = "BattleSandboxStateDamageReduction", description = "一侧伤害减免屏障快照。")
	data class DamageReduction(
		@field:Schema(description = "屏障种类枚举名。", example = "PHYSICAL")
		var kind: String = "",
		@field:Schema(description = "剩余回合；永久或不计时效果为空。", nullable = true)
		var turnsRemaining: Int? = null,
	)

	@Schema(name = "BattleSandboxStateSpeedModifier", description = "一侧速度修正快照。")
	data class SpeedModifier(
		@field:Schema(description = "速度修正种类枚举名。", example = "TAILWIND")
		var kind: String = "",
		@field:Schema(description = "速度倍率。", example = "2.0")
		var multiplier: Double = 1.0,
		@field:Schema(description = "剩余回合；永久或不计时效果为空。", nullable = true)
		var turnsRemaining: Int? = null,
	)

	@Schema(name = "BattleSandboxStateEntryHazard", description = "一侧入场陷阱快照。")
	data class EntryHazard(
		@field:Schema(description = "陷阱种类枚举名。", example = "SPIKES")
		var kind: String = "",
		@field:Schema(description = "当前层数。", example = "1")
		var layers: Int = 1,
		@field:Schema(description = "最大层数。", example = "3")
		var maxLayers: Int = 1,
	)
}
