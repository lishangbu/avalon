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
	/**
	 * 一个已完成回合的最小复盘记录。
	 *
	 * 连续沙盒请求会把完整 [BattleSandboxStateSnapshot] 带回后端继续结算，而 [TurnRecord] 只负责给管理端展示和导出
	 * “这一回合提交了什么、消费了哪些随机数、产生了哪些新增事件”。它不保存规则快照或成员完整资料，避免前端历史记录
	 * 反过来成为下一回合规则事实源。
	 */
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

	/**
	 * 全场环境在连续沙盒中的可续算状态。
	 *
	 * 天气、场地和全场速度顺序是跨回合持续的环境事实，必须随快照一起返回，下一回合才能正确递减持续时间并影响
	 * 行动排序、伤害倍率、状态免疫和回合末回复。该 DTO 只保存枚举名和剩余回合，不复制规则说明或管理端中文文案，
	 * 因为这些展示信息应继续从后端规则资料读取。
	 */
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

	/**
	 * 全场速度顺序效果的轻量快照。
	 *
	 * 目前用于表达戏法空间这类会反转同优先度内速度比较方向的规则。它独立成嵌套 DTO，是为了让环境快照能够清楚地区分
	 * “没有速度顺序效果”和“有效果但剩余回合为空的永久/不计时效果”，避免前端用空字符串推断规则状态。
	 */
	@Schema(name = "BattleSandboxStateFieldSpeedOrderEffect", description = "全场速度顺序效果快照。")
	data class FieldSpeedOrderEffect(
		@field:Schema(description = "效果种类枚举名。", example = "TRICK_ROOM")
		var kind: String = "",
		@field:Schema(description = "剩余回合；永久或不计时效果为空。", nullable = true)
		var turnsRemaining: Int? = null,
	)

	/**
	 * 一方队伍在连续沙盒中的运行态边界。
	 *
	 * [activeActorIds] 决定当前可行动席位，[participants] 保存同一方全部成员的跨回合状态；一侧屏障、速度修正和入场
	 * 陷阱也归属在这里，因为它们随队伍侧而不是某个成员移动。该结构不承载赛制限制或队伍合法性，准备校验仍由独立接口处理。
	 */
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

	/**
	 * 单个成员在连续沙盒中的可变战斗状态。
	 *
	 * 这里保存 HP、主要异常、临时状态计数、技能 PP、锁招/蓄力/讲究锁定、替身和命中锁定等会跨行动或跨回合影响规则的
	 * 事实。它不是资料详情 DTO：名称、图像、能力值来源、技能完整规则和特性/道具 policy 都会在下一次请求时由后端根据
	 * 规则资料重新装配，避免管理端快照覆盖生产规则。
	 */
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

	/**
	 * 成员技能槽在连续沙盒中的 PP 状态。
	 *
	 * 技能槽只需要保存技能资料 ID 和剩余 PP；最大 PP、技能名称、属性、命中率和所有效果规则都从资料库重新读取。这样既能
	 * 让连续回合正确消费 PP，也不会让前端提交的旧技能规则污染后端运行时快照。
	 */
	@Schema(name = "BattleSandboxStateSkillSlot", description = "技能槽 PP 快照。")
	data class SkillSlot(
		@field:Schema(description = "技能资料 ID。", example = "1")
		var skillId: Long = 0,
		@field:Schema(description = "剩余 PP。", example = "34")
		var remainingPp: Int = 0,
	)

	/**
	 * 一侧伤害减免屏障的持续状态。
	 *
	 * 屏障挂在队伍侧上，会影响后续多个目标受到的物理或特殊伤害；它不属于具体成员，也不会随成员替换消失。
	 * [turnsRemaining] 为空时表示该效果由规则声明为永久或本沙盒暂不计时，结算器不能把空值当作已经过期。
	 */
	@Schema(name = "BattleSandboxStateDamageReduction", description = "一侧伤害减免屏障快照。")
	data class DamageReduction(
		@field:Schema(description = "屏障种类枚举名。", example = "PHYSICAL")
		var kind: String = "",
		@field:Schema(description = "剩余回合；永久或不计时效果为空。", nullable = true)
		var turnsRemaining: Int? = null,
	)

	/**
	 * 一侧速度修正效果的持续状态。
	 *
	 * 该 DTO 用于顺风等队伍侧速度倍率。倍率和剩余回合必须随快照续传，因为它们会影响下一回合的行动排序；技能来源、
	 * 中文说明和是否可被清除等管理信息不在这里保存，避免把展示资料混入运行态。
	 */
	@Schema(name = "BattleSandboxStateSpeedModifier", description = "一侧速度修正快照。")
	data class SpeedModifier(
		@field:Schema(description = "速度修正种类枚举名。", example = "TAILWIND")
		var kind: String = "",
		@field:Schema(description = "速度倍率。", example = "2.0")
		var multiplier: Double = 1.0,
		@field:Schema(description = "剩余回合；永久或不计时效果为空。", nullable = true)
		var turnsRemaining: Int? = null,
	)

	/**
	 * 一侧入场陷阱的层数状态。
	 *
	 * 入场陷阱在队伍侧累计层数，并在对方成员换入时触发伤害、状态或能力阶级变化。快照保存 [kind]、当前 [layers] 和
	 * [maxLayers]，使连续沙盒可以正确叠层和封顶；具体伤害公式、免疫判断和清除规则仍由后端引擎根据结构化规则执行。
	 */
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
