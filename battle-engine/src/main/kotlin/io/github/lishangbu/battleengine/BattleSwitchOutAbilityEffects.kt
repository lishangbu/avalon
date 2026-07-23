package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 统一结算成员成功离场时触发的特性。
 *
 * 主动替换、技能强制替换和道具或特性强制替换都会经过本结算器，确保形态变化、状态治愈和回复
 * 不会因离场来源不同而产生分叉。倒下后的补位不触发通常的离场收益，但仍允许盾剑形态回到防御形态。
 */
internal class BattleSwitchOutAbilityEffects {
	fun apply(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		var updated = actor
		val events = mutableListOf<BattleEvent>()
		val stanceChange = updated.abilityEffects.filterIsInstance<BattleAbilityEffect.StanceChange>().firstOrNull()
		val defensiveProfile = stanceChange?.let { updated.battleFormProfiles[it.defensiveFormCode] }
		if (defensiveProfile != null && defensiveProfile.creatureId != updated.creatureId) {
			val previousCreatureId = updated.creatureId
			updated = updated.changeBattleForm(defensiveProfile)
			if (actor.canBattle()) {
				events += BattleEvent.FormChanged(state.turnNumber, actor.actorId, previousCreatureId, defensiveProfile.creatureId)
			}
		}
		if (!actor.canBattle()) return state.replaceParticipant(updated)

		updated.abilityEffects.filterIsInstance<BattleAbilityEffect.SwitchOutFormChange>().forEach { effect ->
			val baseProfile = updated.battleFormProfiles[effect.baseFormCode]
			val alternateProfile = updated.battleFormProfiles[effect.alternateFormCode]
			if (baseProfile?.creatureId == updated.creatureId && alternateProfile != null) {
				val previousCreatureId = updated.creatureId
				updated = updated.changeBattleForm(alternateProfile)
				events += BattleEvent.FormChanged(
					state.turnNumber,
					actor.actorId,
					previousCreatureId,
					alternateProfile.creatureId,
				)
			}
		}
		if (
			updated.majorStatus != null &&
			updated.abilityEffects.any { it is BattleAbilityEffect.SwitchOutMajorStatusCure }
		) {
			val status = requireNotNull(updated.majorStatus)
			updated = updated.clearMajorStatus()
			events += BattleEvent.StatusCleared(state.turnNumber, updated.actorId, status)
		}
		updated.abilityEffects.filterIsInstance<BattleAbilityEffect.SwitchOutHeal>().forEach { effect ->
			if (updated.currentHp < updated.maxHp) {
				val amount = (updated.maxHp / effect.healDenominator).coerceAtLeast(1)
					.coerceAtMost(updated.maxHp - updated.currentHp)
				updated = updated.heal(amount)
				events += BattleEvent.HealingApplied(state.turnNumber, updated.actorId, amount)
			}
		}
		return state.replaceParticipant(updated).appendEvents(events)
	}
}
