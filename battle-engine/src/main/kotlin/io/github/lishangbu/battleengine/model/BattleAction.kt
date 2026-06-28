package io.github.lishangbu.battleengine.model

/**
 * 一回合内玩家提交给引擎的行动。
 *
 * 引擎把玩家或规则系统提交的动作统一建模为行动。所有行动都保留 `actorId`：
 * - 对使用技能而言，`actorId` 是当前执行技能的上场成员。
 * - 对替换而言，`actorId` 是即将离场的当前上场成员。
 *
 * 后续道具使用、强制行动、锁招和失控行动会继续扩展为新的 sealed 子类型，但仍会围绕同一入口做
 * 可行动校验和事件追踪。
 */
sealed interface BattleAction {
	val actorId: String

	/**
	 * 使用一格技能攻击或影响目标。
	 *
	 * `targetActorId` 表达玩家选择的目标槽位当前成员。若该目标在本回合先发生替换，单打第一版会把技能
	 * 结算到同一方新的上场成员身上，以保持“目标槽位”语义。
	 */
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

	/**
	 * 替换当前上场成员。
	 *
	 * `targetActorId` 必须是同一方未在场且仍可战斗的成员。若 `actorId` 已经倒下，该行动表示倒下后的
	 * 强制补位；否则表示主动替换。两者在事件中通过 `forced` 字段区分。
	 */
	data class SwitchParticipant(
		override val actorId: String,
		val targetActorId: String,
	) : BattleAction {
		init {
			require(actorId.isNotBlank()) { "actorId must not be blank" }
			require(targetActorId.isNotBlank()) { "targetActorId must not be blank" }
			require(actorId != targetActorId) { "switch target must be different from actor" }
		}
	}
}
