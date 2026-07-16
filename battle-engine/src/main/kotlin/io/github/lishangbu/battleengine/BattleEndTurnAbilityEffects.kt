package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.BattleRandom

/** 集中执行回合末特性，保持伤害、回复、治愈和能力变化的固定顺序。 */
internal class BattleEndTurnAbilityEffects(
	private val damageResultEffects: BattleEndTurnDamageResultEffects,
) {
	fun applyDamage(state: BattleState, random: BattleRandom): BattleState {
		val afterWeather = activeParticipants(state).fold(state) { current, snapshot ->
			if (current.result != null) return@fold current
			val holder = current.participant(snapshot.actorId) ?: return@fold current
			val weather = current.effectiveWeatherFor(holder)
			holder.abilityEffects.filterIsInstance<BattleAbilityEffect.WeatherEndTurnDamage>()
				.filter { weather in it.weathers }
				.fold(current) damage@ { damageState, effect ->
					val latest = damageState.participant(holder.actorId) ?: return@damage damageState
					if (!latest.canBattle() || latest.hasIndirectDamageImmunity()) return@damage damageState
					val amount = (latest.maxHp / effect.damageDenominator).coerceAtLeast(1)
					damageResultEffects.apply(
						state = damageState,
						damaged = latest.receiveDamage(amount),
						event = BattleEvent.WeatherDamageApplied(
							damageState.turnNumber,
							latest.actorId,
							weather,
							amount,
						),
						random = random,
					)
				}
		}
		return activeParticipants(afterWeather).fold(afterWeather) holderFold@ { current, snapshot ->
			if (current.result != null) return@holderFold current
			val holder = current.participant(snapshot.actorId) ?: return@holderFold current
			val effects = holder.abilityEffects.filterIsInstance<BattleAbilityEffect.OpponentMajorStatusEndTurnDamage>()
			if (!holder.canBattle() || effects.isEmpty()) return@holderFold current
			val holderSide = current.sides.firstOrNull { side -> side.participants.any { it.actorId == holder.actorId } }
				?: return@holderFold current
			val opponents = current.sides.filterNot { it.sideId == holderSide.sideId }.flatMap { it.activeParticipants() }
			effects.fold(current) effectFold@ { effectState, effect ->
				opponents.fold(effectState) targetFold@ { targetState, targetSnapshot ->
					if (targetState.result != null) return@targetFold targetState
					val target = targetState.participant(targetSnapshot.actorId) ?: return@targetFold targetState
					val status = target.majorStatus ?: return@targetFold targetState
					if (!target.canBattle() || status !in effect.statuses || target.hasIndirectDamageImmunity()) {
						return@targetFold targetState
					}
					val amount = (target.maxHp / effect.damageDenominator).coerceAtLeast(1)
					damageResultEffects.apply(
						state = targetState,
						damaged = target.receiveDamage(amount),
						event = BattleEvent.ResidualDamageApplied(targetState.turnNumber, target.actorId, status, amount),
						random = random,
					)
				}
			}
		}
	}

	fun applyHealingAndCures(state: BattleState, random: BattleRandom): BattleState {
		val afterHealing = activeParticipants(state).fold(state) { current, snapshot ->
			val participant = current.participant(snapshot.actorId) ?: return@fold current
			val status = participant.majorStatus
			val effects = participant.abilityEffects.filterIsInstance<BattleAbilityEffect.MajorStatusEndTurnHeal>()
				.filter { status != null && status in it.statuses }
			effects.fold(current) heal@ { healingState, effect ->
				val latest = healingState.participant(participant.actorId) ?: return@heal healingState
				if (!latest.canReceiveHealing()) return@heal healingState
				val amount = (latest.maxHp / effect.healDenominator).coerceAtLeast(1)
					.coerceAtMost(latest.maxHp - latest.currentHp)
				healingState.replaceParticipant(latest.heal(amount)).appendEvent(
					BattleEvent.HealingApplied(healingState.turnNumber, latest.actorId, amount),
				)
			}
		}
		val afterSelfCure = activeParticipants(afterHealing).fold(afterHealing) { current, snapshot ->
			val participant = current.participant(snapshot.actorId) ?: return@fold current
			val status = participant.majorStatus ?: return@fold current
			val effect = participant.abilityEffects.filterIsInstance<BattleAbilityEffect.EndTurnMajorStatusCure>()
				.firstOrNull {
					it.requiredWeathers.isEmpty() || current.effectiveWeatherFor(participant) in it.requiredWeathers
				} ?: return@fold current
			if (effect.chancePercent < 100 && random.nextInt(100, "end-turn-status-cure:${participant.actorId}") >= effect.chancePercent) {
				return@fold current
			}
			current.replaceParticipant(participant.clearMajorStatus()).appendEvent(
				BattleEvent.StatusCleared(current.turnNumber, participant.actorId, status),
			)
		}
		return activeParticipants(afterSelfCure).fold(afterSelfCure) holderFold@ { current, snapshot ->
			val holder = current.participant(snapshot.actorId) ?: return@holderFold current
			val effect = holder.abilityEffects.filterIsInstance<BattleAbilityEffect.EndTurnAllyMajorStatusCure>()
				.firstOrNull() ?: return@holderFold current
			val side = current.sides.firstOrNull { it.participants.any { member -> member.actorId == holder.actorId } }
				?: return@holderFold current
			side.activeParticipants().filterNot { it.actorId == holder.actorId }.fold(current) allyFold@ { allyState, allySnapshot ->
				val ally = allyState.participant(allySnapshot.actorId) ?: return@allyFold allyState
				val status = ally.majorStatus ?: return@allyFold allyState
				if (random.nextInt(100, "end-turn-ally-status-cure:${holder.actorId}:${ally.actorId}") >= effect.chancePercent) {
					return@allyFold allyState
				}
				allyState.replaceParticipant(ally.clearMajorStatus()).appendEvent(
					BattleEvent.StatusCleared(allyState.turnNumber, ally.actorId, status),
				)
			}
		}
	}

	fun applyStatChanges(state: BattleState, random: BattleRandom): BattleState =
		activeParticipants(state).fold(state) { current, snapshot ->
			var participant = current.participant(snapshot.actorId) ?: return@fold current
			val events = mutableListOf<BattleEvent>()
			participant.abilityEffects.filterIsInstance<BattleAbilityEffect.EndTurnStatStageChange>().forEach { effect ->
				val before = participant.statStage(effect.stat)
				participant = participant.changeStatStage(effect.stat, effect.stageDelta)
				appendStatEvent(current, participant, effect.stat, before, events)
			}
			participant.abilityEffects.filterIsInstance<BattleAbilityEffect.EndTurnRandomStatStageChange>().forEach { effect ->
				val increaseCandidates = BattleStat.entries.filter { participant.statStage(it) < 6 }
				if (increaseCandidates.isEmpty()) return@forEach
				val increasedStat = increaseCandidates[random.nextInt(increaseCandidates.size, "end-turn-random-stat-increase:${participant.actorId}")]
				val beforeIncrease = participant.statStage(increasedStat)
				participant = participant.changeStatStage(increasedStat, effect.increase)
				appendStatEvent(current, participant, increasedStat, beforeIncrease, events)
				val decreaseCandidates = BattleStat.entries.filter { it != increasedStat && participant.statStage(it) > -6 }
				if (decreaseCandidates.isNotEmpty()) {
					val decreasedStat = decreaseCandidates[random.nextInt(decreaseCandidates.size, "end-turn-random-stat-decrease:${participant.actorId}")]
					val beforeDecrease = participant.statStage(decreasedStat)
					participant = participant.changeStatStage(decreasedStat, effect.decrease)
					appendStatEvent(current, participant, decreasedStat, beforeDecrease, events)
				}
			}
			current.replaceParticipant(participant).appendEvents(events)
		}

	private fun appendStatEvent(
		state: BattleState,
		participant: BattleParticipant,
		stat: BattleStat,
		before: Int,
		events: MutableList<BattleEvent>,
	) {
		val delta = participant.statStage(stat) - before
		if (delta != 0) {
			events += BattleEvent.StatStageChanged(
				state.turnNumber,
				participant.actorId,
				participant.actorId,
				stat,
				delta,
				participant.statStage(stat),
			)
		}
	}

	private fun activeParticipants(state: BattleState): List<BattleParticipant> =
		state.sides.flatMap { it.activeParticipants() }.filter { it.canBattle() }
}
