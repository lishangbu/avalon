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
