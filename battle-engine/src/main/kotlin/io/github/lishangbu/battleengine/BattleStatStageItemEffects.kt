package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleState

/** 判断对手来源的能力下降是否被目标携带道具阻止。 */
internal fun BattleState.statStageDropBlockedByItem(sourceActorId: String, target: BattleParticipant, delta: Int): Boolean {
	if (delta >= 0 || target.itemId == null) return false
	val sourceSide = sideOf(sourceActorId)?.sideId ?: return false
	val targetSide = sideOf(target.actorId)?.sideId ?: return false
	return sourceSide != targetSide && target.itemEffects.any { it is BattleItemEffect.OpponentStatStageReductionImmunity }
}

/** 判断对手来源的指定能力下降是否被目标特性阻止。 */
internal fun BattleState.statStageDropBlockedByAbility(
	sourceActorId: String,
	target: BattleParticipant,
	stat: BattleStat,
	delta: Int,
): Boolean {
	if (delta >= 0) return false
	val sourceSide = sideOf(sourceActorId)?.sideId ?: return false
	val targetSide = sideOf(target.actorId)?.sideId ?: return false
	if (sourceSide == targetSide) return false
	if (target.abilityEffects.any { effect ->
			effect is io.github.lishangbu.battleengine.model.BattleAbilityEffect.OpponentStatStageReductionImmunity &&
				stat in effect.stats
		}) return true
	return sideOf(target.actorId)?.activeParticipants()?.any { holder ->
		holder.canBattle() && holder.abilityEffects
			.filterIsInstance<io.github.lishangbu.battleengine.model.BattleAbilityEffect.SideElementStatDropImmunity>()
			.any { target.hasElement(it.elementId) }
	} == true
}

/** 在能力实际下降后触发白色香草类负阶级复原效果。 */
internal fun BattleState.applyNegativeStatStageResetItem(actorId: String): BattleState {
	val participant = participant(actorId) ?: return this
	if (participant.itemId == null || participant.itemEffects.none { it is BattleItemEffect.NegativeStatStageReset }) return this
	val negativeStats = BattleStat.entries.filter { participant.statStage(it) < 0 }
	if (negativeStats.isEmpty()) return this
	val reset = negativeStats.fold(participant) { current, stat -> current.setStatStage(stat, 0) }.consumeHeldItem()
	val events = negativeStats.map { stat ->
		BattleEvent.StatStageChanged(
			turnNumber = turnNumber,
			actorId = actorId,
			targetActorId = actorId,
			stat = stat,
			delta = -participant.statStage(stat),
			currentStage = 0,
		)
	}
	return replaceParticipant(reset).appendEvents(events)
}

/** 在对手出场特性实际降低能力后触发一次性反应强化。 */
internal fun BattleState.applyAbilityStatReductionReactiveItem(sourceActorId: String, targetActorId: String): BattleState {
	val target = participant(targetActorId) ?: return this
	val sourceSide = sideOf(sourceActorId)?.sideId ?: return this
	val targetSide = sideOf(targetActorId)?.sideId ?: return this
	if (sourceSide == targetSide || target.itemId == null) return this
	val effect = target.itemEffects.filterIsInstance<BattleItemEffect.AbilityStatReductionReactiveBoost>().firstOrNull()
		?: return this
	val boosted = target.changeStatStage(effect.stat, effect.stageDelta).consumeHeldItem()
	val actualDelta = boosted.statStage(effect.stat) - target.statStage(effect.stat)
	val consumed = replaceParticipant(boosted)
	return if (actualDelta == 0) consumed else {
		consumed.appendEvent(
			BattleEvent.StatStageChanged(
				turnNumber = turnNumber,
				actorId = target.actorId,
				targetActorId = target.actorId,
				stat = effect.stat,
				delta = actualDelta,
				currentStage = boosted.statStage(effect.stat),
			),
		)
	}
}

/** 对一次对手来源的能力降低结算，每名受影响持有者最多触发一次反应强化。 */
internal fun BattleState.applyOpponentStatReductionReactiveAbilities(
	eventStartIndex: Int,
	sourceActorId: String,
): BattleState {
	val sourceSideId = sideOf(sourceActorId)?.sideId ?: return this
	val targetActorIds = events.drop(eventStartIndex)
		.filterIsInstance<BattleEvent.StatStageChanged>()
		.filter { it.delta < 0 && sideOf(it.targetActorId)?.sideId != sourceSideId }
		.map { it.targetActorId }
		.distinct()
	return targetActorIds.fold(this) { current, targetActorId ->
		var target = current.participant(targetActorId) ?: return@fold current
		val events = mutableListOf<BattleEvent>()
		target.abilityEffects.filterIsInstance<
			io.github.lishangbu.battleengine.model.BattleAbilityEffect.OpponentStatReductionReactiveBoost
		>().forEach { effect ->
			val before = target.statStage(effect.stat)
			target = target.changeStatStage(effect.stat, effect.stageDelta)
			val actualDelta = target.statStage(effect.stat) - before
			if (actualDelta > 0) {
				events += BattleEvent.StatStageChanged(
					turnNumber,
					target.actorId,
					target.actorId,
					effect.stat,
					actualDelta,
					target.statStage(effect.stat),
				)
			}
		}
		current.replaceParticipant(target).appendEvents(events)
	}
}

/** 复制本次技能结算中新出现的对手正向能力变化。 */
internal fun BattleState.applyOpponentPositiveStatStageCopyItems(eventStartIndex: Int): BattleState {
	val positiveChanges = events.drop(eventStartIndex)
		.filterIsInstance<BattleEvent.StatStageChanged>()
		.filter { it.delta > 0 }
	if (positiveChanges.isEmpty()) return this
	val holders = sides.flatMap { side -> side.activeParticipants() }
		.filter { participant ->
			participant.canBattle() && participant.itemId != null &&
				participant.itemEffects.any { it is BattleItemEffect.OpponentPositiveStatStageCopy }
		}
	return holders.fold(this) { current, originalHolder ->
		val holder = current.participant(originalHolder.actorId) ?: return@fold current
		val holderSide = current.sideOf(holder.actorId)?.sideId ?: return@fold current
		val copiedChanges = positiveChanges.filter { change ->
			current.sideOf(change.targetActorId)?.sideId?.let { it != holderSide } == true
		}
		if (copiedChanges.isEmpty()) return@fold current
		val deltasByStat = copiedChanges.groupingBy { it.stat }.fold(0) { total, change -> total + change.delta }
		var updated = holder
		val copiedEvents = mutableListOf<BattleEvent>()
		deltasByStat.forEach { (stat, delta) ->
			val before = updated.statStage(stat)
			updated = updated.changeStatStage(stat, delta)
			val actualDelta = updated.statStage(stat) - before
			if (actualDelta > 0) {
				copiedEvents += BattleEvent.StatStageChanged(
					turnNumber = current.turnNumber,
					actorId = holder.actorId,
					targetActorId = holder.actorId,
					stat = stat,
					delta = actualDelta,
					currentStage = updated.statStage(stat),
				)
			}
		}
		current.replaceParticipant(updated.consumeHeldItem()).appendEvents(copiedEvents)
	}
}
