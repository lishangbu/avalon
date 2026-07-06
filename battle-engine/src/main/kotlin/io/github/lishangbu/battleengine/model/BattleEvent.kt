package io.github.lishangbu.battleengine.model

/**
 * 战斗事件流中的一条事实。
 *
 * 引擎所有可观察结果都通过事件表达：开始、回合、使用技能、命中、伤害、倒下和结束。
 * 事件是复盘和对照测试的事实来源；外部系统不应只依赖最终 HP，因为触发顺序错误也可能得到相同终局数值。
 */
sealed interface BattleEvent {
	val turnNumber: Int

	// 生命周期与换人事件：描述战斗、回合和上场席位如何变化。
	/**
	 * 战斗已经从冻结初始快照进入运行态。
	 *
	 * 该事件只在 [io.github.lishangbu.battleengine.BattleEngine.start] 产出一次，记录赛制 code 和双方 id，便于
	 * replay 在不反查数据库的情况下确认初始上下文。它不表示任何出场特性、天气、场地或入场陷阱已经结算；这些
	 * 启动后的副作用会按实际顺序继续追加独立事件。
	 */
	data class BattleStarted(
		override val turnNumber: Int,
		val formatCode: String,
		val sideIds: List<String>,
	) : BattleEvent

	/**
	 * 一个新的战斗回合开始结算。
	 *
	 * 事件在行动排序、替换、行动前状态和技能处理之前写入，作为 replay 中每回合事件片段的锚点。它不承载天气、
	 * 场地或持续时间递减语义；那些回合末效果必须在对应阶段独立产生事件，避免仅凭回合号推断副作用。
	 */
	data class TurnStarted(
		override val turnNumber: Int,
	) : BattleEvent

	/**
	 * 一个上场席位发生替换。
	 *
	 * `forced=false` 表示主动替换；`forced=true` 表示原上场成员已经无法战斗，需要由同一方后备成员补位。
	 * 事件只记录席位变化，不暗含入场特性、状态清除或道具触发；这些副作用会用后续事件单独表达。
	 */
	data class ParticipantSwitched(
		override val turnNumber: Int,
		val sideId: String,
		val previousActorId: String,
		val nextActorId: String,
		val forced: Boolean,
	) : BattleEvent

	/**
	 * 技能命中后的强制替换效果选中了一个后备成员。
	 *
	 * 该事件只表达来源技能和随机选中的后备成员；真正的上场席位变化仍由随后的 [ParticipantSwitched] 记录。
	 * 两条事件分开后，replay 可以同时恢复“为什么换人”和“席位现在是谁”，也能在入场陷阱或出场特性继续触发时
	 * 保持事件顺序清楚。
	 */
	data class TargetForcedSwitchSelected(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val nextActorId: String,
	) : BattleEvent

	/**
	 * 成员无法执行主动替换。
	 *
	 * 替换阻止都发生在替换阶段，不消费技能 PP，也不会推进锁招、休整、蓄力或束缚计数。`reason` 说明阻止
	 * 来源；`skillId` 仅在锁招或蓄力等技能绑定状态下有值；`sourceActorId` 和 `turnsRemainingBefore`
	 * 仅在束缚等需要解释来源和剩余回合的状态下有值。合并成一个事件后，replay 仍能读到完整事实，同时避免
	 * 每新增一种替换限制都扩展一个新的事件类型。
	 */
	data class SwitchPrevented(
		override val turnNumber: Int,
		val actorId: String,
		val reason: SwitchPreventionReason,
		val skillId: Long? = null,
		val sourceActorId: String? = null,
		val turnsRemainingBefore: Int? = null,
	) : BattleEvent

	/**
	 * 成员已经宣布并进入一次技能使用流程。
	 *
	 * 该事件发生在 PP 消耗和命中前 gate 之前，用来表达“行动者本回合确实尝试使用了这个技能”。它不保证技能
	 * 最终命中、造成伤害或产生附加效果；后续可能紧跟未命中、属性免疫、保护、失败、状态阻挡或伤害事件。
	 * [targetActorId] 是本次使用流程的主目标或被规则解析后的代表目标，范围技能仍会在逐目标阶段拆出多个结果。
	 */
	data class SkillUsed(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val skillName: String,
	) : BattleEvent

	/**
	 * 成员对指定目标建立了命中锁定。
	 *
	 * 该事件表达的是“使用者下回合结束前对这个目标的下一次命中判定会跳过命中骰”。它不表示目标被束缚或站位被锁定；
	 * 目标换下后效果会消失，保护、属性免疫和一击必杀等级失败等命中前 gate 也仍然优先于该效果。
	 */
	data class AccuracyLockStarted(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 成员建立了在场期间的要害等级加成。
	 *
	 * 聚气类效果不修改具体技能槽，而是写入成员运行态；后续任何普通伤害技能都会把该加成与技能自身要害等级相加。
	 * 该状态随成员离场清除，因此事件只记录开始事实，不记录固定持续回合。
	 */
	data class CriticalHitStageBoostStarted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val stageBonus: Int,
	) : BattleEvent

	/**
	 * 技能在命中判定阶段未命中指定目标。
	 *
	 * [accuracyRoll] 是本次消费的 1..100 命中随机值，保存在事件里是为了让 replay 和公开规则对照测试可以校验
	 * 随机消费顺序。该事件只覆盖命中率/闪避率/天气修正后的未命中；属性免疫、保护、精神场地阻挡或一击必杀
	 * 等级失败会使用其它更具体事件，不应被归并为 miss。
	 */
	data class SkillMissed(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val accuracyRoll: Int,
	) : BattleEvent

	/**
	 * 技能在已经使用、消耗 PP 并通过前置命中流程后，因为自身规则条件不满足而失败。
	 *
	 * 该事件用于表达“不能继续进入伤害或附加效果”的稳定事实，例如目标当前 HP 不高于使用者当前 HP 时，
	 * HP 差值伤害技能不会退回普通伤害公式。`reason` 使用面向规则的稳定代码，方便 replay 和对照测试断言。
	 */
	data class SkillFailed(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val reason: String,
	) : BattleEvent

	/**
	 * 技能成功扣减了目标某个技能槽的剩余 PP。
	 *
	 * 怨恨读取目标最近一次成功使用的技能，并把那个技能槽的 PP 至多扣到 0。事件同时记录来源技能和被扣减技能，
	 * 避免 replay 只能从最终技能槽反推状态变化；`amount` 是实际扣减值，不是规则声明的最大扣减值。
	 */
	data class SkillPpReduced(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val reducedSkillId: Long,
		val amount: Int,
		val previousRemainingPp: Int,
		val currentRemainingPp: Int,
	) : BattleEvent

	/**
	 * 成员成功建立本回合保护屏障。
	 *
	 * 该事件只表达“保护状态已经生效”，不表达技能命中目标或造成效果。保护屏障是回合内临时状态，
	 * 由引擎上下文持有，回合结束后自动失效。
	 */
	data class ProtectionStarted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 保护类技能因连续使用概率递减而失败。
	 *
	 * 技能已经使用且 PP 已经消耗，但本回合不会建立保护屏障，也不会阻挡后续技能。该事件不表示命中失败；
	 * 它发生在保护类技能自身的成功率判定阶段。
	 */
	data class ProtectionFailed(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 成员成功进入本回合致命技能伤害保留 1 HP 的姿态。
	 *
	 * 挺住类行动和守住共用连续保护成功率，但成功后的战斗含义不同：它不会阻挡命中，也不会阻止异常、能力变化或
	 * 间接回合末伤害；只有技能伤害即将把使用者 HP 扣到 0 时，伤害写入层才会把本次伤害夹到至少剩余 1 HP。
	 * 因此 replay 需要独立事件来表达“姿态建立”，实际保命仍由后续 [FatalDamageSurvived] 记录。
	 */
	data class FatalDamageEndureStarted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

		/**
		 * 技能被目标本回合的保护屏障阻挡。
		 *
		 * 这里的保护屏障包含成员自身的守住类保护，也包含目标所在侧本回合的临时一侧防护。行动者已经使用技能并消耗
		 * PP 后才会产生该事件；被阻挡后不再进行命中判定、伤害计算或附加效果结算。
		 */
	data class SkillBlockedByProtection(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 技能被当前场地规则阻挡。
	 *
	 * 当前用于精神场地阻止针对接地对手的先制技能。技能已经使用且 PP 已消耗，但不会继续进入命中、
	 * 伤害或附加效果流程。
	 */
	data class SkillBlockedByTerrain(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val terrain: BattleTerrain,
	) : BattleEvent

	/**
	 * 技能被目标侧当前上场成员的特性阻挡。
	 *
	 * 当前用于阻止对手先制技能影响特性拥有者或同侧伙伴。技能已经使用且 PP 已消耗，但不会继续进入命中、
	 * 伤害或附加效果流程。事件记录特性拥有者和特性 ID，不记录本地化特性名称。
	 */
	data class SkillBlockedByAbility(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val abilityHolderActorId: String,
		val abilityId: Long?,
	) : BattleEvent

	/**
	 * 技能被目标属性天然免疫。
	 *
	 * 当前用于草属性目标免疫粉末类技能，以及恶属性目标免疫由特性提升先制的对手变化技能。技能已经使用且
	 * PP 已消耗，但不会继续进入命中、伤害或附加效果流程。
	 */
	data class SkillBlockedByElement(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val elementId: Long,
	) : BattleEvent

	/**
	 * 技能被目标特性吸收。
	 *
	 * 当前用于指定属性技能命中目标后，被目标特性阻止继续结算，并可能转换为 HP 回复或能力阶级提升。技能已经
	 * 使用且 PP 已消耗；`healAmount` 是夹取后的实际回复量，满 HP 或非回复型吸收时为 0。事件记录属性 ID 和
	 * 特性 ID，不记录本地化名称；能力阶级提升使用独立 [StatStageChanged] 事件记录。
	 */
	data class SkillAbsorbedByAbility(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val abilityHolderActorId: String,
		val abilityId: Long?,
		val elementId: Long,
		val healAmount: Int,
	) : BattleEvent

	/**
	 * 成员当前属性集合发生变化。
	 *
	 * 该事件用于表达燃尽、电光双击等技能成功造成伤害后移除使用者自身属性的事实。事件只记录属性 ID 集合的前后
	 * 快照和来源技能 ID，不记录本地化属性名；外部展示层可以按规则快照自行翻译。允许 `newElementIds` 为空，
	 * 因为现代规则中成员可以在这类效果后暂时变成无属性。
	 */
	data class ParticipantElementsChanged(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val previousElementIds: Set<Long>,
		val newElementIds: Set<Long>,
	) : BattleEvent

	// 技能宣告、锁招和行动前阻止事件：描述一次行动为什么能继续或为什么提前停止。
	/**
	 * 多段技能本次使用的实际命中段数已经确定。
	 *
	 * 该事件只在段数大于 1 时产生。随后每一段伤害仍使用独立的 [DamageApplied] 事件记录，目标提前倒下时
	 * 事件中的 `hitCount` 表示原本抽到的段数，不表示最终实际造成了多少段伤害。
	 */
	data class MultiHitCountDetermined(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val hitCount: Int,
	) : BattleEvent

	/**
	 * 成员开始进入锁招状态。
	 *
	 * `totalTurns` 包含当前首次使用回合；`turnsRemainingAfterCurrent` 表示未来还会被强制继续行动几次。
	 */
	data class LockedMoveStarted(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val totalTurns: Int,
		val turnsRemainingAfterCurrent: Int,
	) : BattleEvent

	/**
	 * 锁招状态消耗了一次未来强制行动。
	 */
	data class LockedMoveAdvanced(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val turnsRemainingAfterCurrent: Int,
	) : BattleEvent

	/**
	 * 锁招状态在本次行动后结束。
	 */
	data class LockedMoveEnded(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val confusesUser: Boolean,
	) : BattleEvent

	/**
	 * 行动者无法执行本次技能行动。
	 *
	 * 该事件统一表达行动前阻止：睡眠、冰冻、麻痹、休整、回复封锁、挑衅、定身法、无理取闹、畏缩和混乱等
	 * 都不会消耗本次提交技能的 PP，也不会继续进入 `SkillUsed`、命中、伤害或附加效果流程。`reason`
	 * 给出稳定规则原因；`skillId` 只在阻止和某个技能绑定时出现；`previousSkillId` 只用于无理取闹；
	 * `status` 只用于畏缩、混乱等临时状态；`turnsRemainingBefore` 用于可复盘地解释倒计时消费。
	 */
	data class SkillPrevented(
		override val turnNumber: Int,
		val actorId: String,
		val reason: SkillPreventionReason,
		val skillId: Long? = null,
		val previousSkillId: Long? = null,
		val status: BattleVolatileStatus? = null,
		val turnsRemainingBefore: Int? = null,
	) : BattleEvent

	/**
	 * 目标最近使用过的技能被定身法禁用。
	 *
	 * `disabledSkillId` 单独记录在事件上，而不是让 replay 去反查目标当时的运行态，是为了避免后续技能槽 PP
	 * 变化、离场清理或资料名称变化影响历史事件解释。定身法本身作为临时状态仍会追加
	 * [VolatileStatusApplied]，该事件补足“具体禁用哪个技能”的事实。
	 */
	data class SkillDisabled(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val disabledSkillId: Long,
		val turnsRemaining: Int,
	) : BattleEvent

	/**
	 * 束缚类临时状态在回合末造成伤害。
	 *
	 * 现代规则中束缚伤害属于间接伤害，来源成员仍在场且目标仍可战斗时才结算。该事件单独记录来源成员和
	 * 剩余回合，避免把它混入主要异常状态的 [ResidualDamageApplied]。
	 */
	data class BindingDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val sourceActorId: String,
		val amount: Int,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	/**
	 * 目标被成功种下寄生种子。
	 *
	 * 事件同时记录使用者当时所在侧和上场席位索引，因为后续回合末回复并不绑定原使用者 actorId，而是绑定这个
	 * 站位。这样 replay 可以解释原使用者换下后，为什么同一站位的新上场成员仍然获得回复。
	 */
	data class LeechSeedPlanted(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val sourceSideId: String,
		val sourceActiveIndex: Int,
	) : BattleEvent

	/**
	 * 寄生种子在回合末对目标造成间接伤害。
	 *
	 * 该伤害属于目标身上的持续状态，按目标最大 HP 的 1/8 取整且至少 1 点；它与主要异常、束缚、天气伤害分开
	 * 记录，避免 replay 端把不同来源的固定比例扣血混在同一种事件里。
	 */
	data class LeechSeedDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val sourceSideId: String,
		val sourceActiveIndex: Int,
		val amount: Int,
	) : BattleEvent

	/**
	 * 成员身上的寄生种子状态被技能清除。
	 *
	 * 寄生种子不是普通 [BattleVolatileStatus] 枚举，因为它绑定的是来源站位而不是单纯持续回合；因此清除事件也
	 * 独立建模。高速旋转、晶光转转这类规则只清除使用者自身身上的寄生种子，不影响其它成员。
	 */
	data class LeechSeedCleared(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	// 伤害、状态和能力阶级事件：描述成员身上的可观察战斗事实变化。
	/**
	 * 一次伤害已经结算到目标身上。
	 *
	 * `amount` 可以为 0，用于表达属性免疫等“技能已经命中流程但没有造成 HP 变化”的情况。
	 * `targetMultiplier` 记录范围技能在双打等站位中应用的目标倍率，普通单体技能为 1.0。
	 * `criticalHit` 标记本次伤害是否按击中要害公式计算。它放在伤害事件上，而不是单独事件上，
	 * 是为了让回放系统直接从同一条事实里读取“扣了多少 HP”和“为什么有这个倍率”。
	 */
	data class DamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val amount: Int,
		val effectiveness: Double,
		val targetMultiplier: Double = 1.0,
		val criticalHit: Boolean = false,
	) : BattleEvent

	/**
	 * 目标成功获得主要异常状态。
	 *
	 * 该事件只在状态真正写入成员运行态后产生；命中但被场地、特性、道具或既有状态阻止的情况，
	 * 应使用独立阻止事件表达，避免 replay 端误判目标已经带有该状态。
	 */
	data class StatusApplied(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val status: BattleMajorStatus,
	) : BattleEvent

	/**
	 * 目标试图获得主要异常状态，但被规则条件阻止。
	 *
	 * 阻止事件保留行动者、目标、状态和稳定原因，便于对照测试确认“没有写入状态”也是一个可观察结果。
	 * 当前覆盖目标已有主要异常状态、属性免疫、场地免疫、特性免疫和道具免疫。
	 */
	data class StatusApplicationBlocked(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val status: BattleMajorStatus,
		val reason: BattleStatusBlockReason,
	) : BattleEvent

	/**
	 * 成员已有的主要异常状态被清除。
	 *
	 * 该事件由状态计数归零或后续治愈规则产生，不表示目标重新获得了行动机会；
	 * 行动是否已经被阻止仍以同一回合内更早的 [SkillPrevented] 事件为准。
	 */
	data class StatusCleared(
		override val turnNumber: Int,
		val actorId: String,
		val status: BattleMajorStatus,
	) : BattleEvent

	/**
	 * 目标成功获得临时状态。
	 *
	 * 临时状态可以和主要异常状态共存，并且一般会在行动前、回合末或离场时清理。
	 * 该事件只在状态真正写入成员运行态后产生。
	 */
	data class VolatileStatusApplied(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val status: BattleVolatileStatus,
	) : BattleEvent

	/**
	 * 目标试图获得临时状态，但被规则条件阻止。
	 *
	 * 当前用于已有混乱状态阻止刷新持续计数、薄雾场地阻止混乱，以及特性/道具提供的临时状态免疫。
	 * 与主要异常状态一样，阻止事件表示状态没有写入成员运行态，也不会消费该临时状态的私有持续时间随机数。
	 */
	data class VolatileStatusApplicationBlocked(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val status: BattleVolatileStatus,
		val reason: BattleStatusBlockReason,
	) : BattleEvent

	/**
	 * 成员已有的临时状态被清除。
	 *
	 * 当前用于混乱行动前计数归零、回复封锁回合末持续时间归零，以及治愈道具解除临时状态。
	 * 畏缩在回合末静默消失，不额外产生解除事件。
	 */
	data class VolatileStatusCleared(
		override val turnNumber: Int,
		val actorId: String,
		val status: BattleVolatileStatus,
	) : BattleEvent

	/**
	 * 成员某项能力阶级已经实际改变。
	 *
	 * [delta] 是本次变化量，可能经过能力下限、上限、清除强化或交换类规则修正；[currentStage] 是写入后的最终
	 * 阶级。事件只在运行态真的发生变化时出现，白雾、防尘护目镜等阻挡或能力已经到达边界但规则要求记录失败的
	 * 场景，应使用独立阻挡事件或不产生该事件，避免 replay 把“尝试改变”和“已经改变”混在一起。
	 */
	data class StatStageChanged(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val stat: BattleStat,
		val delta: Int,
		val currentStage: Int,
	) : BattleEvent

	/**
	 * 能力阶级变化被一侧防护阻止。
	 *
	 * 该事件目前用于白雾类效果。技能已经命中并尝试降低目标能力阶级，但目标所在侧的场上防护让运行态保持不变。
	 * 记录 `attemptedDelta` 可以让 replay 区分原本要降低的能力项和幅度，而不是只看到一次没有状态变化的技能使用。
	 */
	data class StatStageChangeBlocked(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val stat: BattleStat,
		val attemptedDelta: Int,
		val reason: BattleStatusBlockReason,
	) : BattleEvent

	/**
	 * 技能效果让成员本场在场期间的有效体重进一步降低。
	 *
	 * 事件记录的是累计减轻量，而不是最终有效体重；最终有效体重还会按特性和携带道具倍率继续计算。这样 replay
	 * 可以恢复“技能造成了多少临时减重”，同时不会把后续特性/道具生效顺序固化到事件里。
	 */
	data class WeightReductionChanged(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val previousReduction: Int,
		val currentReduction: Int,
	) : BattleEvent

	/**
	 * 技能效果把目标指定能力阶级清除为 0。
	 *
	 * 清除不是普通降低阶级：无论目标原来是正阶级还是负阶级，结果都回到 0。事件记录清除前的数值，方便 replay
	 * 还原黑雾、清除之烟等效果的真实语义。
	 */
	data class StatStageCleared(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val stat: BattleStat,
		val previousStage: Int,
	) : BattleEvent

	/**
	 * 技能效果把一个成员的指定能力阶级复制给另一个成员。
	 *
	 * 该事件用于自我暗示等效果。`copiedStage` 是来源成员当时的阶级值；目标成员最终写入同一个值。
	 */
	data class StatStageCopied(
		override val turnNumber: Int,
		val actorId: String,
		val sourceActorId: String,
		val targetActorId: String,
		val skillId: Long,
		val stat: BattleStat,
		val copiedStage: Int,
	) : BattleEvent

	/**
	 * 技能效果交换两个成员的指定能力阶级。
	 *
	 * 事件中的 `firstCurrentStage` 与 `secondCurrentStage` 是交换完成后的值。来源与目标使用当前技能语境命名：
	 * 一般情况下 first 是操作目标，second 是操作来源。
	 */
	data class StatStageSwapped(
		override val turnNumber: Int,
		val actorId: String,
		val firstActorId: String,
		val secondActorId: String,
		val skillId: Long,
		val stat: BattleStat,
		val firstCurrentStage: Int,
		val secondCurrentStage: Int,
	) : BattleEvent

	/**
	 * 技能效果把目标指定能力阶级取反。
	 *
	 * 该事件用于颠倒类效果。0 阶级取反后仍为 0，不会产生事件；非 0 时记录前后值。
	 */
	data class StatStageInverted(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val stat: BattleStat,
		val previousStage: Int,
		val currentStage: Int,
	) : BattleEvent

	/**
	 * 一侧成功建立了防守方标准伤害减免屏障。
	 *
	 * 屏障属于一侧场上状态，而不是某个成员的临时状态。`turnsRemaining` 记录建立时写入的持续回合；
	 * 如果为 null，表示外部测试用例或调用方暂不要求引擎递减该屏障。
	 */
	data class SideDamageReductionStarted(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val skillId: Long,
		val kind: BattleSideDamageReductionKind,
		val turnsRemaining: Int?,
	) : BattleEvent

	/**
	 * 一侧已有的防守方伤害减免屏障被技能清除。
	 *
	 * 该事件用于击破屏障类伤害技能，以及清除浓雾类变化技能。击破屏障类伤害技能会在普通伤害公式之前产生本事件，
	 * 让同一次技能不再读取这些被清除的屏障倍率；变化技能则在成功后的清场阶段产生本事件。`removedKinds` 只记录
	 * 目标侧当时实际存在并被删除的屏障种类，不把不存在的屏障写入 replay。
	 */
	data class SideDamageReductionsRemoved(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val sideId: String,
		val skillId: Long,
		val removedKinds: List<BattleSideDamageReductionKind>,
	) : BattleEvent

	/**
	 * 一侧已有的非伤害型防护被技能清除。
	 *
	 * 白雾和神秘守护不改变伤害倍率，但会阻止能力下降或异常附加；清除浓雾类技能需要把这些目标侧状态从运行态
	 * 删除。单独事件可以让 replay 区分“光墙/反射壁被清除”和“白雾/神秘守护被清除”。
	 */
	data class SideProtectionsRemoved(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val sideId: String,
		val skillId: Long,
		val removedKinds: List<BattleSideProtectionKind>,
	) : BattleEvent

	/**
	 * 目标当前的保护类屏障被技能破除。
	 *
	 * 佯攻这类技能和普通“穿透保护”不同：它不只是本次技能无视保护，还会删除目标个人保护屏障和目标侧本回合
	 * 临时侧防护，让同回合后续技能不再被同一层保护拦下。`brokeActorProtection` 表示是否破除了目标自身的
	 * 守住/看穿类屏障；`brokenSideProtectionKinds` 记录实际移除的广域防守/快速防守等一侧临时防护。
	 */
	data class ProtectionBroken(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val sideId: String,
		val skillId: Long,
		val brokeActorProtection: Boolean,
		val brokenSideProtectionKinds: List<BattleSideProtectionKind>,
	) : BattleEvent

	// 一侧场地、入场陷阱和全场顺序事件：描述不直接挂在单个成员身上的持续规则。
	/**
	 * 一侧成功建立了速度结算修正。
	 *
	 * 速度修正属于一侧场上状态，而不是某个成员的临时状态。`multiplier` 记录行动排序时应用的倍率；
	 * `turnsRemaining` 记录建立时写入的持续回合，如果为 null，表示外部测试用例或调用方暂不要求引擎递减该效果。
	 */
	data class SideSpeedModifierStarted(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val skillId: Long,
		val kind: BattleSideSpeedModifierKind,
		val multiplier: Double,
		val turnsRemaining: Int?,
	) : BattleEvent

		/**
		 * 一侧成功建立了非伤害型防护效果。
		 *
		 * 白雾、神秘守护、广域防守和快速防守都属于这种事件语义：它们不改变伤害或速度，而是在后续能力下降、
		 * 状态附加、范围技能或先制技能入口提供阻止条件。`turnsRemaining = null` 表示该防护只存在于当前回合
		 * 临时上下文中，不写入跨回合状态。单独记录事件可以让 replay 区分“建立了光墙”与“建立了一侧防护”。
		 */
	data class SideProtectionStarted(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val skillId: Long,
		val kind: BattleSideProtectionKind,
		val turnsRemaining: Int?,
	) : BattleEvent

	/**
	 * 一侧入场陷阱的层数已经建立或增加。
	 *
	 * 该事件发生在技能命中并成功改变目标侧状态之后。`layers` 记录当前最终层数，而不是本次增加的层数；这样
	 * replay 可以直接重建一侧场上陷阱状态。若同类陷阱已经达到最大层数，引擎不会产生该事件。
	 */
	data class SideEntryHazardChanged(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val skillId: Long,
		val kind: BattleSideEntryHazardKind,
		val layers: Int,
		val maxLayers: Int,
	) : BattleEvent

	/**
	 * 一侧入场陷阱已经被成员吸收或技能清除。
	 *
	 * 毒菱被接地毒属性成员换入吸收时 `skillId` 为空；高速旋转、晶光转转这类技能清除使用者一侧全部陷阱时，
	 * `skillId` 记录来源技能。事件保留触发成员 `actorId`，便于 replay 区分自然持续结束、入场吸收和技能清场。
	 */
	data class SideEntryHazardRemoved(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val kind: BattleSideEntryHazardKind,
		val skillId: Long? = null,
	) : BattleEvent

	/**
	 * 入场陷阱在成员换入后造成了 HP 伤害。
	 *
	 * 该事件独立于普通技能伤害，避免 replay 端误把伤害归因到本回合使用的技能。`effectiveness` 只对按属性克制
	 * 计算的陷阱有意义；其它固定比例陷阱使用 1.0。
	 */
	data class EntryHazardDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val kind: BattleSideEntryHazardKind,
		val amount: Int,
		val layers: Int,
		val effectiveness: Double = 1.0,
	) : BattleEvent

	/**
	 * 入场陷阱在成员换入后成功附加主要异常状态。
	 *
	 * 当前用于毒菱。由于来源是一侧场上状态而不是某个行动者，事件只记录换入成员、所属侧和陷阱种类。
	 */
	data class EntryHazardStatusApplied(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val kind: BattleSideEntryHazardKind,
		val status: BattleMajorStatus,
	) : BattleEvent

	/**
	 * 入场陷阱试图附加主要异常状态，但被稳定免疫规则阻止。
	 *
	 * 当前用于毒菱遇到钢属性、薄雾场地、免疫特性或免疫道具等情况。阻止事件是可观察事实，能让公开对照测试
	 * 区分“没有触发陷阱”和“触发了但被规则免疫”。
	 */
	data class EntryHazardStatusApplicationBlocked(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val kind: BattleSideEntryHazardKind,
		val status: BattleMajorStatus,
		val reason: BattleStatusBlockReason,
	) : BattleEvent

	/**
	 * 入场陷阱在成员换入后改变了能力阶级。
	 *
	 * 当前用于黏黏网降低接地换入成员速度。若能力阶级已经在下限，状态不会改变，也不会产生该事件。
	 */
	data class EntryHazardStatStageChanged(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val kind: BattleSideEntryHazardKind,
		val stat: BattleStat,
		val delta: Int,
		val currentStage: Int,
	) : BattleEvent

	/**
	 * 全场速度顺序效果已经建立。
	 *
	 * 该事件用于戏法空间这类会改变行动队列比较方向的全场规则。它不表示任何成员速度数值被修改，只表示后续
	 * 行动排序在同优先度内应按该效果的定义比较有效速度。
	 */
	data class FieldSpeedOrderStarted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val kind: BattleFieldSpeedOrderKind,
		val turnsRemaining: Int?,
	) : BattleEvent

	/**
	 * 全场速度顺序效果已经结束。
	 *
	 * `actorId` 与 `skillId` 为空时表示持续回合自然耗尽；非空时表示某个技能触发了现代规则中的重启/解除语义。
	 */
	data class FieldSpeedOrderEnded(
		override val turnNumber: Int,
		val kind: BattleFieldSpeedOrderKind,
		val actorId: String? = null,
		val skillId: Long? = null,
	) : BattleEvent

	/**
	 * 主要异常状态在回合末造成了间接伤害。
	 *
	 * 当前用于中毒、剧毒和灼伤等持续性状态。事件记录 [status]，而不是只记录伤害量，因为同样的 HP 损失可能来自
	 * 天气、道具、寄生种子或反作用力；生产排障和公开用例需要知道伤害来源才能判断阶段顺序是否正确。
	 */
	data class ResidualDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val status: BattleMajorStatus,
		val amount: Int,
	) : BattleEvent

	/**
	 * 技能反作用力对使用者造成了 HP 损失。
	 *
	 * 该事件表示伤害后代价已经由本次技能触发并写入使用者 HP。它和混乱自伤、道具伤害、天气伤害分开，是为了在
	 * replay 中保留“由技能命中后的反作用力造成”的事实；实际扣血量可能已经按使用者剩余 HP 截断。
	 */
	data class RecoilDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val amount: Int,
	) : BattleEvent

	/**
	 * 携带道具在回合末对持有者造成了 HP 损失。
	 *
	 * 该事件和 [ResidualDamageApplied] 分开：异常状态伤害需要记录具体状态，而附着针这类道具伤害需要记录道具
	 * 身份。两者都属于间接伤害，但 replay、沙盒和公开对照测试必须能区分“因为中毒扣血”和“因为携带道具扣血”，
	 * 否则生产排障时只能从最终 HP 反推来源。
	 */
	data class HeldItemDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val itemId: Long,
		val amount: Int,
	) : BattleEvent

	/**
	 * 持有者的携带道具因为接触规则转移给了另一名成员。
	 *
	 * 现代公开实现不会为附着针换手播放额外对战消息，但引擎事件仍记录这一事实：最终快照只告诉我们“谁现在持有
	 * 道具”，事件才能解释“为什么这件道具在本回合换了人”。事件只记录道具 ID，不复制道具效果列表；效果列表属于
	 * 状态快照，转移时已经写入新持有者。
	 */
	data class HeldItemTransferred(
		override val turnNumber: Int,
		val fromActorId: String,
		val toActorId: String,
		val itemId: Long,
	) : BattleEvent

	// 回复、代价和伤害后事件：描述 HP 写入后的补充事实。
	/**
	 * 混乱自伤已经结算到行动者身上。
	 *
	 * 自伤使用公开实现中的 40 威力物理公式，不套用属性一致、属性克制、要害、道具和多数特性修正；
	 * `randomPercent` 记录 85..100 的伤害浮动，便于测试用例精确校验随机消费顺序。
	 */
	data class ConfusionDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val amount: Int,
		val randomPercent: Int,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	/**
	 * 通用回复效果已经为成员恢复 HP。
	 *
	 * 该事件用于不需要额外来源字段的回复，例如自我回复类技能或伤害吸收类技能最终写入的 HP。天气特性、寄生种子、
	 * 场地和道具回复有独立事件，以便生产日志能区分来源；[amount] 是实际恢复量，已经按最大 HP 截断。
	 */
	data class HealingApplied(
		override val turnNumber: Int,
		val actorId: String,
		val amount: Int,
	) : BattleEvent

	/**
	 * 寄生种子在回合末为来源站位上的成员回复 HP。
	 *
	 * `sourceTargetActorId` 是被寄生种子扣血的目标，`actorId` 是当前真正获得回复的成员。二者分开后，双打换人后
	 * 的 replay 不需要反查站位历史，也能看出这次回复来自哪个目标身上的寄生种子。
	 */
	data class LeechSeedHealingApplied(
		override val turnNumber: Int,
		val actorId: String,
		val sourceTargetActorId: String,
		val amount: Int,
	) : BattleEvent

	/**
	 * 技能自身效果为使用者回复了 HP。
	 *
	 * 该事件用于吸取类伤害技能和自我回复类变化技能。它记录技能 ID，方便 replay、日志和对照测试区分
	 * “技能带来的回复”和携带道具、场地、天气等其它回复来源。
	 */
	data class SkillHealingApplied(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val amount: Int,
	) : BattleEvent

	/**
	 * 技能自身反作用让使用者承受了 HP 伤害。
	 *
	 * 该事件专门表示“技能本身带来的自损”，与携带道具产生的 [RecoilDamageApplied] 分开。普通反作用伤害会把
	 * `sourceDamageAmount` 记录为目标本次实际损失的 HP，便于 replay 和对照测试确认反作用基数没有使用溢出公式
	 * 伤害；现代挣扎这类按使用者最大 HP 计算的技能代价，则把该字段记录为使用者最大 HP，表示本次自损的计算基数。
	 */
	data class SkillRecoilDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val amount: Int,
		val sourceDamageAmount: Int,
	) : BattleEvent

	/**
	 * 使用者因技能自身代价损失全部剩余 HP。
	 *
	 * 该事件和 [SkillRecoilDamageApplied] 分开：反作用伤害按目标实际损失 HP 推导，可能被反作用伤害免疫阻止；
	 * 自我牺牲伤害按使用者命中时的当前 HP 推导，表示技能规则自身让使用者倒下。
	 */
	data class SkillSelfSacrificeDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val amount: Int,
	) : BattleEvent

	/**
	 * 技能把使用者和目标当前 HP 重新分配为平均值。
	 *
	 * 该事件用于分担痛楚类规则。它不表示普通伤害，也不表示普通回复；因此不会被伤害后特性、反作用伤害、吸取回复
	 * 或回复封锁解释。记录双方前后 HP 可以让 replay 在不重新读取技能文本的情况下复原“双方 HP 被直接设定”的事实。
	 */
	data class HpAveragedBySkill(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val averageHp: Int,
		val actorPreviousHp: Int,
		val actorCurrentHp: Int,
		val targetPreviousHp: Int,
		val targetCurrentHp: Int,
	) : BattleEvent

	/**
	 * 目标承受致命技能伤害时，通过特性、携带道具或当回合技能状态保留了 HP。
	 *
	 * 特性和道具来源要求目标在伤害前为满 HP；挺住类技能来源只要求本回合姿态仍然存在。`incomingDamage` 是普通
	 * 伤害公式或固定伤害规则产出的原始伤害，`preventedDamage` 是为了让目标保留 HP 而抵消的部分。目标最终损失
	 * 的 HP 仍由同一次 [DamageApplied] 事件记录；本事件只说明为什么没有倒下。
	 */
	data class FatalDamageSurvived(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val source: BattleFatalDamageSurvivalSource,
		val sourceId: Long?,
		val consumed: Boolean,
		val incomingDamage: Int,
		val preventedDamage: Int,
	) : BattleEvent

	/**
	 * 目标携带道具削弱了本次即将写入的技能伤害。
	 *
	 * 该事件发生在最终 HP 写入前，用于记录抗性树果等一次性减伤道具已经参与伤害公式。`multiplier` 是道具
	 * 贡献到最终伤害倍率链中的数值，通常为 0.5；如果 `consumed=true`，目标快照中的携带道具已经在该事件后
	 * 移除。事件不单独记录抵消了多少 HP，因为最终取整后的实际伤害仍由随后的 [DamageApplied] 表达。
	 */
	data class DamageReducedByItem(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val itemId: Long,
		val elementId: Long,
		val multiplier: Double,
		val consumed: Boolean,
	) : BattleEvent

	/**
	 * 使用者成功支付 HP 并建立替身。
	 *
	 * `hpCost` 是从本体扣除的 HP，也等于替身建立时的初始 HP。该事件只在替身真正写入运行态后产生；
	 * 当前已有替身或 HP 不足导致技能没有建立替身时，不会产生该事件。
	 */
	data class SubstituteStarted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val hpCost: Int,
		val substituteHp: Int,
	) : BattleEvent

	/**
	 * 目标的替身吸收了一次对手技能伤害。
	 *
	 * `amount` 是本次实际扣除的替身 HP，已经按替身剩余 HP 夹取；`substituteHpRemaining` 为本次扣除后的
	 * 剩余 HP。该事件不表示目标本体 HP 变化，因此不会和 [DamageApplied] 混用。
	 */
	data class SubstituteDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val amount: Int,
		val substituteHpRemaining: Int,
	) : BattleEvent

	/**
	 * 目标替身因伤害耗尽而破裂。
	 */
	data class SubstituteBroken(
		override val turnNumber: Int,
		val actorId: String,
	) : BattleEvent

	/**
	 * 成员的替身被全场清理技能直接移除。
	 *
	 * 这类移除不是伤害，也不是替身 HP 被打空；单独事件可以让 replay 和对照测试区分“替身被攻击打破”和
	 * “全场整理效果清除了替身”两种现代规则路径。`actorId` 指拥有替身的成员，`skillId` 指触发清理的技能。
	 */
	data class SubstituteCleared(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 技能成功造成实际伤害后，使用者进入休整状态。
	 *
	 * `turnsRemainingAfterCurrent` 表示未来还会阻止几次技能行动。该事件不表示当前回合的行动被阻止，而是为
	 * 下一次行动前钩子写入可复盘的运行态。
	 */
	data class RechargeStarted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val turnsRemainingAfterCurrent: Int,
	) : BattleEvent

	/**
	 * 技能首次宣告后进入蓄力状态。
	 *
	 * `turnsRemainingBeforeUse` 表示未来还需要等待几次技能行动才会释放；常规蓄力技能为 1。
	 */
	data class SkillChargeStarted(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val turnsRemainingBeforeUse: Int,
	) : BattleEvent

	/**
	 * 携带道具让本次蓄力技能跳过等待回合。
	 *
	 * 该事件表示道具已经在技能宣告后被触发；如果 `consumed=true`，成员快照中的携带道具已经被移除。
	 * 后续仍会继续普通命中、保护、免疫和伤害流程，因此本事件不是技能命中或造成效果的替代事实。
	 */
	data class SkillChargeSkippedByItem(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val itemId: Long,
		val consumed: Boolean,
	) : BattleEvent

	/**
	 * 已蓄力技能在后续行动中释放。
	 *
	 * 释放不再次消耗 PP，也不会重新选择技能；目标仍按首次选择的目标槽位进行重定向。
	 */
	data class SkillChargeReleased(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 已蓄力技能在释放前被行动前状态中断。
	 *
	 * 这类中断会清除蓄力状态，避免后续回合重复释放同一技能。
	 */
	data class SkillChargeInterrupted(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	// 天气、场地和战斗收尾事件：描述全局环境变化以及回合/战斗结束。
	/**
	 * 场地在回合末为成员恢复 HP。
	 *
	 * 当前用于青草场地等按最大 HP 比例回复的场地效果。事件记录具体 [terrain]，因为同样的回复数值也可能来自技能、
	 * 道具或天气特性；把来源写进事件能让 replay 校验“场地回复发生在回合末环境阶段”，而不是只从最终 HP 推断。
	 */
	data class TerrainHealingApplied(
		override val turnNumber: Int,
		val actorId: String,
		val terrain: BattleTerrain,
		val amount: Int,
	) : BattleEvent

	/**
	 * 天气在回合末对成员造成了伤害。
	 *
	 * 目前用于沙暴固定比例伤害。天气带来的能力修正属于伤害公式输入，不通过该事件表达。
	 */
	data class WeatherDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val weather: BattleWeather,
		val amount: Int,
	) : BattleEvent

	/**
	 * 天气相关特性在回合末为成员回复了 HP。
	 *
	 * 该事件和普通 [HealingApplied] 分开，避免 replay 端把天气特性回复误判成携带道具或其它通用治疗来源。
	 */
	data class WeatherHealingApplied(
		override val turnNumber: Int,
		val actorId: String,
		val weather: BattleWeather,
		val amount: Int,
	) : BattleEvent

	/**
	 * 当前天气被出场特性或后续环境规则设置。
	 *
	 * 事件记录触发者、目标天气和写入时的剩余回合。它不表达天气带来的伤害、回复、命中或能力修正；这些副作用
	 * 在对应阶段继续用独立事件或伤害公式输入表达。若尝试设置的天气与当前天气相同且持续回合也相同，引擎不会
	 * 产生重复事件。
	 */
	data class WeatherStarted(
		override val turnNumber: Int,
		val actorId: String,
		val weather: BattleWeather,
		val turnsRemaining: Int?,
	) : BattleEvent

	/**
	 * 当前天气因持续回合耗尽而结束。
	 *
	 * 事件只表示环境事实变化，不暗含本回合天气伤害或免疫效果；这些副作用应在更早的回合末阶段单独记录。
	 */
	data class WeatherEnded(
		override val turnNumber: Int,
		val weather: BattleWeather,
	) : BattleEvent

	/**
	 * 当前场地被出场特性或后续环境规则设置。
	 *
	 * 事件记录触发者、目标场地和写入时的剩余回合。它不表达场地带来的状态免疫、回复、先制封锁或伤害修正；
	 * 这些效果仍在各自规则阶段独立产生事件或参与公式计算。
	 */
	data class TerrainStarted(
		override val turnNumber: Int,
		val actorId: String,
		val terrain: BattleTerrain,
		val turnsRemaining: Int?,
	) : BattleEvent

	/**
	 * 当前场地因持续回合耗尽或清场技能而结束。
	 *
	 * 事件只表示环境事实变化，不暗含场地回复、状态免疫或优先度封锁等副作用。`actorId` 和 `skillId` 为空时表示
	 * 自然持续时间耗尽；有值时表示来源技能主动清除了场地。
	 */
	data class TerrainEnded(
		override val turnNumber: Int,
		val terrain: BattleTerrain,
		val actorId: String? = null,
		val skillId: Long? = null,
	) : BattleEvent

	/**
	 * 成员已经因为 HP 归零或规则指定效果无法继续战斗。
	 *
	 * 该事件是倒下事实的统一出口，不携带伤害来源；来源应由更早的伤害、反作用力、天气、状态或直接倒下事件表达。
	 * 分开记录可以让同一次伤害同时产生“受到了多少伤害”和“因此倒下”两条事实，便于胜负判定和强制替换阶段按
	 * 事件顺序复盘。
	 */
	data class ParticipantFainted(
		override val turnNumber: Int,
		val actorId: String,
	) : BattleEvent

	/**
	 * 当前回合所有行动、持续效果和倒下/胜负检查已经处理完毕。
	 *
	 * 该事件只在战斗仍未结束时追加，表示下一次调用可以提交下一回合行动。它不表示一定发生了天气或场地递减；
	 * 若回合末环境效果存在，对应事件必须已经出现在 [TurnEnded] 之前。
	 */
	data class TurnEnded(
		override val turnNumber: Int,
	) : BattleEvent

	/**
	 * 战斗已经结束并给出胜负结论。
	 *
	 * [winningSideId] 为空表示平局或无胜者结束；非空时指向获胜方。[reason] 是稳定机器可读原因，例如一方全员
	 * 无法战斗。事件一旦出现，核心引擎不会再接受后续回合行动；replay 也会以它作为最终状态边界。
	 */
	data class BattleEnded(
		override val turnNumber: Int,
		val winningSideId: String?,
		val reason: String,
	) : BattleEvent
}

/**
 * 主动替换请求被拒绝的稳定原因。
 *
 * 枚举值表达规则来源，而不是 Kotlin 实现类名。这样事件流可以在保持可读性的同时承载不同替换限制的共享事实：
 * 成员仍留在原席位、相关倒计时不在替换阶段消费，后续技能阶段仍按原状态继续推进。
 */
enum class SwitchPreventionReason {
	/** 成员仍处于锁招状态，必须继续使用被锁定的技能。 */
	LOCKED_MOVE,

	/** 成员处于休整状态，不能通过非法替换跳过休整。 */
	RECHARGE,

	/** 成员正在蓄力，下一次行动会自动释放蓄力技能。 */
	CHARGING,

	/** 成员被仍在场的来源成员束缚，不能主动离场。 */
	BINDING,
}

/**
 * 技能行动在行动前被阻止的稳定原因。
 *
 * 枚举值和公开规则行为一一对应，但多个原因共用 [BattleEvent.SkillPrevented] 事件结构。调用方通过原因字段
 * 判断展示文案和 replay 分支，通过事件上的可选字段读取对应规则需要的技能、临时状态或剩余回合事实。
 */
enum class SkillPreventionReason {
	/** 睡眠阻止行动，并会消费一次睡眠阻止计数。 */
	SLEEP,

	/** 冰冻自然解冻失败，本次行动被阻止。 */
	FREEZE,

	/** 麻痹随机判定触发，本次行动被阻止。 */
	PARALYSIS,

	/** 上次成功使用的休整技能要求本次行动空过。 */
	RECHARGE,

	/** 回复封锁阻止回复类技能或吸取回复类技能。 */
	HEAL_BLOCK,

	/** 挑衅阻止变化分类技能。 */
	TAUNT,

	/** 定身法阻止被指定禁用的技能。 */
	DISABLE,

	/** 无理取闹阻止连续使用上一次成功使用的技能。 */
	TORMENT,

	/** 畏缩、混乱等临时状态阻止本次行动。 */
	VOLATILE_STATUS,
}
