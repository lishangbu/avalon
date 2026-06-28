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

	data class DamageApplied(
		override val turnNumber: Int,
		val actorId: String,
		val targetActorId: String,
		val skillId: Long,
		val amount: Int,
		val effectiveness: Double,
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
