package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 伤害、状态或回合末结算后的倒下与胜负收口。
 *
 * 该文件只处理已经写入 HP 变化后的公共生命周期事实：哪些成员倒下、是否有一方已经没有可战斗成员，以及
 * 是否需要追加战斗结束事件。它不计算伤害、不挑选替换成员、不处理规则裁定分数，也不读取数据库规则。
 *
 * 输入是当前 [BattleState] 和本阶段可能倒下的成员快照；输出是追加了 [BattleEvent.ParticipantFainted]
 * 与可选 [BattleEvent.BattleEnded] 的新状态。事件顺序保持“倒下事件先于战斗结束事件”，用于 replay 和公开
 * 测试用例对齐终局。当前胜负不变量很朴素：任意一方没有剩余可战斗成员时立即结束；若双方同时没有剩余
 * 成员，则 [BattleResult.winningSideId] 为 `null`。
 */
internal fun BattleState.handleFaintAndResult(target: BattleParticipant): BattleState =
	handleFaintsAndResult(listOf(target))

/**
 * 对同一阶段的多个潜在倒下成员去重后统一收口。
 *
 * 多目标伤害、反伤、回合末持续伤害可能在同一小阶段产生多个候选成员。该函数先按 `actorId` 去重，避免同一
 * 成员在同一阶段重复产生倒下事件；随后基于更新后的双方成员列表判断胜负。调用方负责在传入前完成 HP、
 * 道具免死、低 HP 回复等更早阶段的结算。
 */
internal fun BattleState.handleFaintsAndResult(
	targets: List<BattleParticipant>,
	killerActorId: String? = null,
): BattleState {
	val faintedTargets = targets.distinctBy { it.actorId }.filterNot { it.canBattle() }
	val withFaint = faintedTargets
		.fold(this) { current, target ->
			current.appendEvent(BattleEvent.ParticipantFainted(turnNumber, target.actorId))
		}
	val afterFaintAbilities = applyFaintAbilityBoosts(
		withFaint.synchronizePassiveSuppressions(),
		faintedTargets.size,
		killerActorId,
	)
		.synchronizeStrongWeather()
	val afterFaintedAllyAbilityCopies = applyFaintedAllyAbilityCopies(afterFaintAbilities, faintedTargets)
	val defeatedSides = afterFaintedAllyAbilityCopies.sides.filterNot { it.hasRemainingParticipant() }
	if (defeatedSides.isEmpty()) {
		return afterFaintedAllyAbilityCopies
	}
	val remainingSides = afterFaintedAllyAbilityCopies.sides.filter { it !in defeatedSides }
	val winningSideId = remainingSides.singleOrNull()?.sideId
	val result = BattleResult(
		winningSideId = winningSideId,
		reason = if (winningSideId == null) "all-sides-fainted" else "all-opponents-fainted",
	)
	return afterFaintedAllyAbilityCopies
		.copy(result = result)
		.appendEvent(
			BattleEvent.BattleEnded(
				turnNumber = turnNumber,
				winningSideId = result.winningSideId,
				reason = result.reason,
			),
		)
}

private fun applyFaintedAllyAbilityCopies(
	state: BattleState,
	faintedTargets: List<BattleParticipant>,
): BattleState = state.sides.flatMap { it.activeParticipants() }
	.filter { it.canBattle() }
	.fold(state) { current, holderSnapshot ->
		val holder = current.participant(holderSnapshot.actorId) ?: return@fold current
		if (holder.abilityEffects.none {
				it is io.github.lishangbu.battleengine.model.BattleAbilityEffect.FaintedAllyAbilityCopy
			}) return@fold current
		val holderSideId = current.sideOf(holder.actorId)?.sideId ?: return@fold current
		val source = faintedTargets.firstOrNull { fainted ->
			current.sideOf(fainted.actorId)?.sideId == holderSideId && fainted.abilityId != null
		} ?: return@fold current
		val updated = holder.copy(
			abilityId = source.abilityId,
			abilityEffects = source.allAbilityEffects(),
			suppressedAbilityEffects = emptyList(),
		)
		current.replaceParticipant(updated).appendEvent(
			BattleEvent.AbilityChanged(
				current.turnNumber,
				holder.actorId,
				source.actorId,
				holder.abilityId,
				source.abilityId,
			),
		)
	}

private fun applyFaintAbilityBoosts(
	state: BattleState,
	faintCount: Int,
	killerActorId: String?,
): BattleState {
	if (faintCount == 0) return state
	val holders = state.sides.flatMap { it.activeParticipants() }.filter { it.canBattle() }
	return holders.fold(state) { current, holderSnapshot ->
		var holder = current.participant(holderSnapshot.actorId) ?: return@fold current
		val events = mutableListOf<BattleEvent>()
		holder.abilityEffects.forEach { effect ->
			if (effect is io.github.lishangbu.battleengine.model.BattleAbilityEffect.OncePerBattleCausedFaintMultiStatBoost) {
				if (holder.actorId != killerActorId || holder.oncePerBattleFaintBoostActivated) return@forEach
				effect.stats.forEach { stat ->
					val before = holder.statStage(stat)
					holder = holder.changeStatStage(stat, effect.stageDelta)
					val actualDelta = holder.statStage(stat) - before
					if (actualDelta > 0) {
						events += BattleEvent.StatStageChanged(
							current.turnNumber,
							holder.actorId,
							holder.actorId,
							stat,
							actualDelta,
							holder.statStage(stat),
						)
					}
				}
				holder = holder.copy(oncePerBattleFaintBoostActivated = true)
				return@forEach
			}
			val statAndDelta = when (effect) {
				is io.github.lishangbu.battleengine.model.BattleAbilityEffect.FaintStatStageBoost -> {
					if (effect.requiresHolderCausedFaint && holder.actorId != killerActorId) null else {
						effect.stat to effect.stageDelta * faintCount
					}
				}
				is io.github.lishangbu.battleengine.model.BattleAbilityEffect.FaintHighestStatBoost -> {
					if (holder.actorId != killerActorId) null else highestRawStat(holder) to faintCount
				}
				else -> null
			} ?: return@forEach
			val before = holder.statStage(statAndDelta.first)
			holder = holder.changeStatStage(statAndDelta.first, statAndDelta.second)
			val actualDelta = holder.statStage(statAndDelta.first) - before
			if (actualDelta > 0) {
				events += BattleEvent.StatStageChanged(
					turnNumber = current.turnNumber,
					actorId = holder.actorId,
					targetActorId = holder.actorId,
					stat = statAndDelta.first,
					delta = actualDelta,
					currentStage = holder.statStage(statAndDelta.first),
				)
			}
		}
		current.replaceParticipant(holder).appendEvents(events)
	}
}

private fun highestRawStat(participant: BattleParticipant): io.github.lishangbu.battleengine.model.BattleStat =
	listOf(
		io.github.lishangbu.battleengine.model.BattleStat.ATTACK to participant.attack,
		io.github.lishangbu.battleengine.model.BattleStat.DEFENSE to participant.defense,
		io.github.lishangbu.battleengine.model.BattleStat.SPECIAL_ATTACK to participant.specialAttack,
		io.github.lishangbu.battleengine.model.BattleStat.SPECIAL_DEFENSE to participant.specialDefense,
		io.github.lishangbu.battleengine.model.BattleStat.SPEED to participant.speed,
	).maxBy { it.second }.first
