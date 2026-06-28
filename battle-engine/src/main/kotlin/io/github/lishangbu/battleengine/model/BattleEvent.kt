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

	data class StatusApplied(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val status: BattleMajorStatus,
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
