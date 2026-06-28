package io.github.lishangbu.battleengine.model

/**
 * 一回合内玩家提交给引擎的行动。
 *
 * 第一阶段只支持使用技能。替换、道具、强制行动、锁招和失控行动会继续扩展为新的 sealed 子类型，
 * 但所有行动都会保留 `actorId` 作为排序和可行动校验的入口。
 */
sealed interface BattleAction {
	val actorId: String

	data class UseSkill(
		override val actorId: String,
		val skillId: Long,
		val targetActorId: String,
	) : BattleAction {
		init {
			require(actorId.isNotBlank()) { "actorId must not be blank" }
			require(skillId > 0) { "skillId must be positive" }
			require(targetActorId.isNotBlank()) { "targetActorId must not be blank" }
		}
	}
}
