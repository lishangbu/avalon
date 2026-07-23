package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
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
		val afterAllyCure = activeParticipants(afterSelfCure).fold(afterSelfCure) holderFold@ { current, snapshot ->
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
		val afterConsumedBerryReplay = activeParticipants(afterAllyCure).fold(afterAllyCure) { current, snapshot ->
			val participant = current.participant(snapshot.actorId) ?: return@fold current
			val consumedTurn = participant.lastConsumedItemTurn ?: return@fold current
			if (participant.lastConsumedItemEffects.none { it is BattleItemEffect.BerryMarker }) return@fold current
			val effect = participant.abilityEffects.filterIsInstance<BattleAbilityEffect.EndTurnConsumedBerryReplay>()
				.firstOrNull() ?: return@fold current
			if (current.turnNumber - consumedTurn < effect.delayTurns) return@fold current
			replayConsumedBerry(current, participant, random)
		}
		val afterPickup = activeParticipants(afterConsumedBerryReplay).fold(afterConsumedBerryReplay) { current, snapshot ->
			val holder = current.participant(snapshot.actorId) ?: return@fold current
			if (
				holder.itemId != null ||
				holder.abilityEffects.none { it is BattleAbilityEffect.EndTurnPickupConsumedItem }
			) return@fold current
			val source = current.sides.flatMap { it.participants }
				.filter { it.actorId != holder.actorId }
				.filter {
					it.lastConsumedItemTurn == current.turnNumber &&
						it.lastConsumedItemAvailableForPickup &&
						it.lastConsumedItemId != null
				}
				.maxByOrNull { it.lastConsumedItemOrder ?: 0 } ?: return@fold current
			val itemId = requireNotNull(source.lastConsumedItemId)
			val updatedHolder = holder.copy(
				itemId = itemId,
				itemEffects = source.lastConsumedItemEffects,
				itemLostSinceEntering = false,
			)
			val updatedSource = source.copy(lastConsumedItemAvailableForPickup = false)
			current.replaceParticipant(updatedHolder).replaceParticipant(updatedSource).appendEvent(
				BattleEvent.ConsumedItemPickedUp(current.turnNumber, holder.actorId, source.actorId, itemId),
			)
		}
		return activeParticipants(afterPickup).fold(afterPickup) { current, snapshot ->
			val participant = current.participant(snapshot.actorId) ?: return@fold current
			if (participant.itemId != null || participant.lastConsumedItemId == null ||
				participant.lastConsumedItemEffects.none { it is io.github.lishangbu.battleengine.model.BattleItemEffect.BerryMarker }
			) return@fold current
			val effect = participant.abilityEffects
				.filterIsInstance<BattleAbilityEffect.EndTurnConsumedBerryRestore>()
				.firstOrNull() ?: return@fold current
			val guaranteed = effect.guaranteedWeather != null &&
				current.effectiveWeatherFor(participant) == effect.guaranteedWeather
			if (!guaranteed && !chanceSucceeds(
				effect.chancePercent,
				random,
				"end-turn-consumed-berry-restore:${participant.actorId}",
			)) return@fold current
			current.replaceParticipant(participant.restoreLastConsumedBerry())
		}
	}

	private fun replayConsumedBerry(
		state: BattleState,
		participant: BattleParticipant,
		random: BattleRandom,
	): BattleState {
		var updated = participant
		val events = mutableListOf<BattleEvent>()
		participant.lastConsumedItemEffects.forEach { itemEffect ->
			when (itemEffect) {
				is BattleItemEffect.LowHpHeal -> if (updated.canReceiveHealing()) {
					val amount = itemEffect.healAmount(updated.maxHp).coerceAtMost(updated.maxHp - updated.currentHp)
					if (amount > 0) {
						updated = updated.heal(amount)
						events += BattleEvent.HealingApplied(state.turnNumber, updated.actorId, amount)
					}
					if (
						itemEffect.confusesIfNatureDecreases != null &&
						itemEffect.confusesIfNatureDecreases == updated.natureDecreasedStat &&
						updated.confusionTurnsRemaining == 0
					) {
						val turns = random.nextInt(4, "cud-chew-confusion:${updated.actorId}") + 2
						updated = updated.copy(confusionTurnsRemaining = turns)
						events += BattleEvent.VolatileStatusApplied(
							state.turnNumber,
							updated.actorId,
							updated.actorId,
							BattleVolatileStatus.CONFUSION,
						)
					}
				}
				is BattleItemEffect.LowHpStatStageBoost -> {
					val before = updated.statStage(itemEffect.stat)
					updated = updated.changeStatStage(itemEffect.stat, itemEffect.stageDelta)
					appendStatEvent(state, updated, itemEffect.stat, before, events)
				}
				is BattleItemEffect.LowHpRandomStatStageBoost -> {
					val candidates = itemEffect.stats.filter { updated.statStage(it) < 6 }
					if (candidates.isNotEmpty()) {
						val stat = candidates.sortedBy { it.ordinal }[
							random.nextInt(candidates.size, "cud-chew-random-stat:${updated.actorId}")
						]
						val before = updated.statStage(stat)
						updated = updated.changeStatStage(stat, itemEffect.stageDelta)
						appendStatEvent(state, updated, stat, before, events)
					}
				}
				is BattleItemEffect.LowHpNextSkillAccuracyBoost -> {
					updated = updated.copy(nextSkillAccuracyMultiplier = itemEffect.multiplier)
				}
				is BattleItemEffect.LowHpCriticalHitStageBoost -> {
					updated = updated.copy(criticalHitStageBonus = maxOf(updated.criticalHitStageBonus, itemEffect.stageBonus))
				}
				is BattleItemEffect.MajorStatusCure -> {
					val status = updated.majorStatus
					if (status != null && status in itemEffect.statuses) {
						updated = updated.clearMajorStatus()
						events += BattleEvent.StatusCleared(state.turnNumber, updated.actorId, status)
					}
				}
				is BattleItemEffect.VolatileStatusCure -> itemEffect.statuses.forEach { status ->
					val cleared = updated.clearVolatileStatus(status)
					if (cleared != updated) {
						updated = cleared
						events += BattleEvent.VolatileStatusCleared(state.turnNumber, updated.actorId, status)
					}
				}
				else -> Unit
			}
		}
		updated = updated.copy(
			lastConsumedItemId = null,
			lastConsumedItemEffects = emptyList(),
			lastConsumedItemTurn = null,
			lastConsumedItemOrder = null,
			lastConsumedItemAvailableForPickup = false,
		)
		return state.replaceParticipant(updated).appendEvents(events)
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
			participant.abilityEffects.filterIsInstance<BattleAbilityEffect.EndTurnFormToggle>().forEach { effect ->
				val first = participant.battleFormProfiles[effect.firstFormCode] ?: return@forEach
				val second = participant.battleFormProfiles[effect.secondFormCode] ?: return@forEach
				val target = if (participant.creatureId == first.creatureId) second else first
				val previousCreatureId = participant.creatureId
				participant = participant.changeBattleForm(target)
				events += BattleEvent.FormChanged(
					current.turnNumber,
					participant.actorId,
					previousCreatureId,
					target.creatureId,
				)
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
