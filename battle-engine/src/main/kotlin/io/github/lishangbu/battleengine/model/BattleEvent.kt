package io.github.lishangbu.battleengine.model

/**
 * 战斗事件流中的一条事实。
 *
 * 引擎所有可观察结果都通过事件表达：开始、回合、使用技能、命中、伤害、倒下和结束。
 * 事件是复盘和对照测试的事实来源；外部系统不应只依赖最终 HP，因为触发顺序错误也可能得到相同终局数值。
 */
sealed interface BattleEvent {
	val turnNumber: Int

	data class BattleStarted(
		override val turnNumber: Int,
		val formatCode: String,
		val sideIds: List<String>,
	) : BattleEvent

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
	 * 成员因锁招状态无法主动替换。
	 *
	 * 锁招期间成员会在技能阶段继续使用被锁定的技能。该事件只表示本次替换请求被忽略，不会清除锁招状态。
	 */
	data class SwitchPreventedByLockedMove(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	/**
	 * 成员因必须休整而不能主动替换。
	 *
	 * 该事件是引擎防御性事件；正常调用方应在行动校验阶段阻止这类提交。若非法提交进入引擎，成员不会离场，
	 * 休整计数也不会在替换阶段被消费。
	 */
	data class SwitchPreventedByRecharge(
		override val turnNumber: Int,
		val actorId: String,
	) : BattleEvent

	/**
	 * 成员因正在蓄力而不能主动替换。
	 *
	 * 蓄力技能的下一次行动会自动释放原技能；该事件只记录非法替换请求被忽略，不会消费蓄力计数。
	 */
	data class SwitchPreventedByCharging(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
	) : BattleEvent

	data class SkillUsed(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val skillName: String,
	) : BattleEvent

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
	 * 技能被目标本回合的保护屏障阻挡。
	 *
	 * 行动者已经使用技能并消耗 PP 后才会产生该事件；被阻挡后不再进行命中判定、伤害计算或附加效果结算。
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
	 * 当前用于草属性目标免疫粉末类技能，以及恶属性目标免疫由特性提升先制度的对手变化技能。技能已经使用且
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
	 * 行动者因睡眠无法执行本次技能行动。
	 *
	 * `turnsRemainingBefore` 记录本次判定前还会被阻止行动几次。事件产生后，引擎会消耗一次计数；
	 * 若计数归零，会继续追加 `StatusCleared` 事件。
	 */
	data class SkillPreventedBySleep(
		override val turnNumber: Int,
		val actorId: String,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	/**
	 * 行动者因冰冻无法执行本次技能行动。
	 *
	 * 冰冻每次行动前先尝试自然解冻；只有未解冻时才产生该事件。若该事件出现，技能不会使用，
	 * PP 不会消耗，也不会继续进入命中、伤害或附加效果流程。
	 */
	data class SkillPreventedByFreeze(
		override val turnNumber: Int,
		val actorId: String,
	) : BattleEvent

	/**
	 * 行动者因麻痹无法执行本次技能行动。
	 *
	 * 麻痹不会像睡眠那样保存持续计数；每次行动前独立按现代规则判定。若该事件出现，技能不会使用，
	 * PP 不会消耗，也不会继续进入命中、伤害或附加效果流程。
	 */
	data class SkillPreventedByParalysis(
		override val turnNumber: Int,
		val actorId: String,
	) : BattleEvent

	/**
	 * 行动者因上一回合成功使用休整技能而无法执行本次技能行动。
	 *
	 * 休整不会消耗本次提交技能的 PP，也不会进入命中、伤害或附加效果流程。事件中的 `turnsRemainingBefore`
	 * 记录本次行动前还会被休整阻止几次，方便 replay 校验计数消费。
	 */
	data class SkillPreventedByRecharge(
		override val turnNumber: Int,
		val actorId: String,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	/**
	 * 行动者因回复封锁无法执行本次回复类技能。
	 *
	 * 第六世代以后，处于回复封锁的成员不能使用自我回复技能或吸取回复类技能。该事件发生在 PP 消耗和
	 * `SkillUsed` 之前，因此被阻止的技能不会进入命中、伤害、回复或讲究类锁定流程。
	 */
	data class SkillPreventedByHealBlock(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	/**
	 * 行动者因挑衅无法执行本次变化技能。
	 *
	 * 挑衅只限制变化分类技能，不限制物理或特殊攻击技能。该事件发生在 PP 消耗和 `SkillUsed` 之前，
	 * 因此被阻止的技能不会进入命中、保护、附加效果或讲究类锁定流程。
	 */
	data class SkillPreventedByTaunt(
		override val turnNumber: Int,
		val actorId: String,
		val skillId: Long,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	/**
	 * 行动者因临时状态无法执行本次技能行动。
	 *
	 * 畏缩会在阻止行动后立即消失；混乱只有在自伤分支命中时才会阻止行动，并会继续产生
	 * `ConfusionDamageApplied` 事件记录自伤结果。
	 */
	data class SkillPreventedByVolatileStatus(
		override val turnNumber: Int,
		val actorId: String,
		val status: BattleVolatileStatus,
	) : BattleEvent

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
	 * 行动是否已经被阻止仍以同一回合内更早的 `SkillPreventedBySleep` 等事件为准。
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

	data class StatStageChanged(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val stat: BattleStat,
		val delta: Int,
		val currentStage: Int,
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
	 * 如果为 null，表示外部 fixture 或调用方暂不要求引擎递减该屏障。
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
	 * 一侧成功建立了速度结算修正。
	 *
	 * 速度修正属于一侧场上状态，而不是某个成员的临时状态。`multiplier` 记录行动排序时应用的倍率；
	 * `turnsRemaining` 记录建立时写入的持续回合，如果为 null，表示外部 fixture 或调用方暂不要求引擎递减该效果。
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
	 * 一侧入场陷阱已经被换入成员吸收并移除。
	 *
	 * 当前用于毒菱被接地毒属性成员换入吸收。事件保留吸收者 `actorId`，便于 replay 区分自然持续结束和由成员
	 * 入场触发的移除。
	 */
	data class SideEntryHazardRemoved(
		override val turnNumber: Int,
		val actorId: String,
		val sideId: String,
		val kind: BattleSideEntryHazardKind,
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

	data class ResidualDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val status: BattleMajorStatus,
		val amount: Int,
	) : BattleEvent

	data class RecoilDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val amount: Int,
	) : BattleEvent

	/**
	 * 混乱自伤已经结算到行动者身上。
	 *
	 * 自伤使用公开实现中的 40 威力物理公式，不套用属性一致、属性克制、要害、道具和多数特性修正；
	 * `randomPercent` 记录 85..100 的伤害浮动，便于 fixture 精确校验随机消费顺序。
	 */
	data class ConfusionDamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val amount: Int,
		val randomPercent: Int,
		val turnsRemainingBefore: Int,
	) : BattleEvent

	data class HealingApplied(
		override val turnNumber: Int,
		val actorId: String,
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
	 * 该事件专门表示带反作用伤害的普通攻击这类“技能本身带来的自损”，与携带道具产生的
	 * [RecoilDamageApplied] 分开。`sourceDamageAmount` 记录目标本次实际损失的 HP，便于 replay 和对照测试
	 * 确认反作用基数没有使用溢出公式伤害。
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
	 * 目标从满 HP 承受致命直接伤害时，通过特性或携带道具保留了 HP。
	 *
	 * `incomingDamage` 是普通伤害公式产出的原始伤害，`preventedDamage` 是为了让目标保留 HP 而抵消的部分。
	 * 目标最终损失的 HP 仍由同一次 [DamageApplied] 事件记录；本事件只说明为什么没有倒下。
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
	 * 当前场地因持续回合耗尽而结束。
	 *
	 * 事件只表示环境事实变化，不暗含场地回复、状态免疫或优先度封锁等副作用。
	 */
	data class TerrainEnded(
		override val turnNumber: Int,
		val terrain: BattleTerrain,
	) : BattleEvent

	data class ParticipantFainted(
		override val turnNumber: Int,
		val actorId: String,
	) : BattleEvent

	data class TurnEnded(
		override val turnNumber: Int,
	) : BattleEvent

	data class BattleEnded(
		override val turnNumber: Int,
		val winningSideId: String?,
		val reason: String,
	) : BattleEvent
}
