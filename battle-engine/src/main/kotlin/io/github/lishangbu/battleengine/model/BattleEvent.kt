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
	 * 成员因锁招状态无法主动替换。
	 *
	 * 锁招期间成员会在技能阶段继续使用被锁定的技能。该事件只表示本次替换请求被忽略，不会清除锁招状态。
	 */
	data class SwitchPreventedByLockedMove(
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
	 * 技能被目标属性天然免疫。
	 *
	 * 当前用于草属性目标免疫粉末类技能。技能已经使用且 PP 已消耗，但不会继续进入命中、伤害或附加效果流程。
	 */
	data class SkillBlockedByElement(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val elementId: Long,
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
	 * 第一批主要用于场地阻止睡眠；后续免疫类规则会继续扩展 [BattleStatusBlockReason]。
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
	 * 当前用于薄雾场地阻止混乱，以及特性/道具提供的临时状态免疫。与主要异常状态一样，阻止事件表示
	 * 状态没有写入成员运行态，也不会消费该临时状态的私有持续时间随机数。
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
	 * 第一批主要用于混乱行动前计数归零。畏缩在回合末静默消失，不额外产生解除事件。
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
	 * 当前天气因持续回合耗尽而结束。
	 *
	 * 事件只表示环境事实变化，不暗含本回合天气伤害或免疫效果；这些副作用应在更早的回合末阶段单独记录。
	 */
	data class WeatherEnded(
		override val turnNumber: Int,
		val weather: BattleWeather,
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
		val winningSideId: String,
		val reason: String,
	) : BattleEvent
}
