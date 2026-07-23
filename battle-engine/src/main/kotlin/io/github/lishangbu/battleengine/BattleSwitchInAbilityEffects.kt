package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 负责成员进入场地时立即触发的结构化特性效果。
 *
 * 出场特性的触发入口有两个：战斗开始时的初始上场，以及替换/强制替换后的新成员上场。两者共享同一套效果分派：
 * 出场能力阶级变化由本类写入，出场天气和场地则委托 [BattleEnvironmentEffects]，保证技能与特性建立环境时使用
 * 相同事件和持续回合延长规则。
 */
internal class BattleSwitchInAbilityEffects(
	private val actionOrdering: BattleActionOrdering,
	private val environmentEffects: BattleEnvironmentEffects,
) {
	/**
	 * 结算战斗开始时所有当前上场成员的出场特性。
	 *
	 * 初始上场不是一次替换行动，但现代规则中“出场时触发”的特性同样会在战斗开始阶段生效。当前按有效速度排序
	 * 触发，戏法空间存在时复用引擎已有的速度比较器反转速度顺序；同速成员保持初始侧和席位顺序。
	 */
	fun applyInitial(state: BattleState): BattleState =
		initialActorIds(state).fold(state) { current, actorId -> apply(current, actorId) }
			.synchronizeTerrainElementIdentities()

	/**
	 * 结算单个成员成功进入场地后的出场特性。
	 *
	 * 成员必须当前仍在场且可战斗；如果刚换入后已经被入场陷阱击倒，则不会触发出场特性。当前支持对手当前上场
	 * 成员的能力阶级变化、全场天气覆盖和全场场地覆盖。
	 */
	fun apply(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!state.isActive(actor.actorId) || !actor.canBattle()) {
			return state
		}
		val afterAbilities = actor.abilityEffects
			.fold(state) { current, effect -> applyEffect(current, actor.actorId, effect) }
		val afterTerrainItem = environmentEffects.applyTerrainActivatedItemOnSwitchIn(afterAbilities, actor.actorId)
		return applyHighestStatBoosterItem(afterTerrainItem, actor.actorId)
			.synchronizeTerrainElementIdentities(setOf(actor.actorId))
	}

	private fun applyHighestStatBoosterItem(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		val itemId = actor.itemId ?: return state
		val effect = actor.itemEffects.filterIsInstance<BattleItemEffect.HighestStatBoosterActivation>().firstOrNull()
			?: return state
		val abilityId = actor.abilityId ?: return state
		if (abilityId !in effect.abilityIds) return state
		if (
			(abilityId == PROTOSYNTHESIS_ABILITY_ID && state.environment.weather == BattleWeather.SUN) ||
			(abilityId == QUARK_DRIVE_ABILITY_ID && state.environment.terrain == BattleTerrain.ELECTRIC)
		) {
			return state
		}
		val stat = listOf(
			BattleStat.ATTACK to actor.attack,
			BattleStat.DEFENSE to actor.defense,
			BattleStat.SPECIAL_ATTACK to actor.specialAttack,
			BattleStat.SPECIAL_DEFENSE to actor.specialDefense,
			BattleStat.SPEED to actor.speed,
		).maxBy { it.second }.first
		val multiplier = if (stat == BattleStat.SPEED) SPEED_BOOST_MULTIPLIER else OTHER_STAT_BOOST_MULTIPLIER
		return state
			.replaceParticipant(actor.copy(boosterEnergyStat = stat).consumeHeldItem())
			.appendEvent(BattleEvent.HeldItemHighestStatBoostActivated(state.turnNumber, actorId, itemId, stat, multiplier))
	}

	/**
	 * 计算战斗开始阶段出场特性的稳定触发顺序。
	 *
	 * 输出只保留成员 ID，避免后续某个成员的出场特性改变环境后导致已排序队列重新计算。有效速度在初始状态上
	 * 一次性计算，包含麻痹、天气速度特性、道具速度倍率和一侧速度修正。
	 */
	private fun initialActorIds(state: BattleState): List<String> =
		state.sides
			.flatMap { side -> side.activeActorIds.mapNotNull { actorId -> state.participant(actorId) } }
			.groupBy { participant -> actionOrdering.effectiveSpeed(state, participant) }
			.toSortedMap(actionOrdering.speedComparator(state))
			.values
			.flatMap { sameSpeedParticipants -> sameSpeedParticipants.map { it.actorId } }

	/**
	 * 将单个结构化特性效果分派到出场阶段实现。
	 *
	 * 只有明确属于 SWITCH_IN 生命周期的效果会改变状态；其它效果返回原状态，避免每新增一种非出场特性都要维护
	 * 一条无意义分支。
	 */
	private fun applyEffect(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect,
	): BattleState =
		when (effect) {
			is BattleAbilityEffect.SwitchInStrongWeatherChange -> state.startStrongWeather(actorId, effect.weather)
			is BattleAbilityEffect.WeatherFormChange -> state.synchronizeWeatherForms(setOf(actorId))
			is BattleAbilityEffect.SwitchInFormChange -> applyFormChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInStatStageChange -> applyStatStageChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInOpponentDefenseComparisonBoost -> applyDefenseComparisonBoost(state, actorId)
			is BattleAbilityEffect.SwitchInAllyHeal -> applyAllyHeal(state, actorId, effect)
			is BattleAbilityEffect.SwitchInAllyStatStageCopy -> applyAllyStatStageCopy(state, actorId)
			is BattleAbilityEffect.SwitchInAllyStatStageReset -> applyAllyStatStageReset(state, actorId)
			is BattleAbilityEffect.SwitchInClearAllSideDamageReductions -> clearAllSideDamageReductions(state, actorId)
			is BattleAbilityEffect.SwitchInCopyOpponentAbility -> copyOpponentAbility(state, actorId)
			is BattleAbilityEffect.SwitchInTerrainChange -> environmentEffects.applySwitchInTerrainChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInWeatherChange -> environmentEffects.applySwitchInWeatherChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInRevealOpponentHeldItems -> revealOpponentHeldItems(state, actorId)
			is BattleAbilityEffect.SwitchInRevealOpponentHighestPowerSkill -> revealOpponentHighestPowerSkill(state, actorId)
			is BattleAbilityEffect.SwitchInTransformIntoOpponent -> transformIntoOpponent(state, actorId)
			is BattleAbilityEffect.SwitchInDetectDangerousOpponentSkill -> detectDangerousOpponentSkill(state, actorId)
			is BattleAbilityEffect.SwitchInDisguiseAsLastHealthyAlly -> disguiseAsLastHealthyAlly(state, actorId)
			is BattleAbilityEffect.HeldItemElementIdentity -> applyHeldItemElementIdentity(state, actorId)
			else -> state
		}

	private fun applyFormChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInFormChange,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		val baseProfile = actor.battleFormProfiles[effect.baseFormCode] ?: return state
		val alternateProfile = actor.battleFormProfiles[effect.alternateFormCode] ?: return state
		if (actor.creatureId != baseProfile.creatureId) return state
		val previousMaxHp = actor.maxHp
		val previousCurrentHp = actor.currentHp
		var changed = actor.changeBattleForm(alternateProfile)
		if (effect.addsMaximumHpDifference && alternateProfile.maxHp > previousMaxHp) {
			changed = changed.copy(
				currentHp = (previousCurrentHp + alternateProfile.maxHp - previousMaxHp)
					.coerceAtMost(alternateProfile.maxHp),
			)
		}
		return state.replaceParticipant(changed).appendEvent(
			BattleEvent.FormChanged(state.turnNumber, actorId, actor.creatureId, alternateProfile.creatureId),
		)
	}

	private fun disguiseAsLastHealthyAlly(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		val apparent = state.sideOf(actorId)?.participants
			?.lastOrNull { it.actorId != actorId && it.canBattle() }
			?: return state
		return state.replaceParticipant(actor.copy(apparentCreatureId = apparent.creatureId))
	}

	private fun applyHeldItemElementIdentity(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		val itemId = actor.itemId ?: return state
		val elementId = actor.itemEffects.filterIsInstance<BattleItemEffect.ElementDamageBoost>()
			.firstOrNull()?.elementId ?: return state
		if (actor.elementIds == setOf(elementId)) return state
		return state.replaceParticipant(actor.copy(elementIds = setOf(elementId))).appendEvent(
			BattleEvent.HeldItemElementIdentityApplied(state.turnNumber, actorId, itemId, elementId),
		)
	}

	private fun detectDangerousOpponentSkill(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		val actorSideId = state.sideOf(actorId)?.sideId ?: return state
		val detected = state.sides.filterNot { it.sideId == actorSideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
			.flatMap { opponent -> opponent.skillSlots.map { skill -> opponent to skill } }
			.filter { (opponent, skill) ->
				skill.oneHitKnockOut != null || (
					skill.power != null &&
						!skill.typelessDamage &&
						state.rules.elementChart.multiplier(
							skill.effectiveElementId(state.effectiveWeatherFor(opponent), state.environment.terrain, opponent),
							actor.elementIds,
						) > 1.0
				)
			}
			.minWithOrNull(compareBy({ it.first.actorId }, { it.second.skillId })) ?: return state
		return state.appendEvent(
			BattleEvent.DangerousOpponentSkillDetected(
				state.turnNumber,
				actorId,
				detected.first.actorId,
				detected.second.skillId,
			),
		)
	}

	private fun transformIntoOpponent(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (actor.transformSnapshot != null) return state
		val actorSideId = state.sideOf(actorId)?.sideId ?: return state
		val target = state.sides.filterNot { it.sideId == actorSideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
			.minByOrNull { it.actorId } ?: return state
		val snapshot = io.github.lishangbu.battleengine.model.BattleTransformSnapshot(
			actor.creatureId,
			actor.attack,
			actor.defense,
			actor.specialAttack,
			actor.specialDefense,
			actor.speed,
			actor.weight,
			actor.elementIds,
			actor.skillSlots,
			actor.abilityId,
			actor.abilityEffects,
		)
		val copiedSkills = target.skillSlots.map { skill ->
			val copiedPp = minOf(5, skill.maxPp)
			skill.copy(remainingPp = copiedPp, maxPp = copiedPp)
		}
		val transformed = actor.copy(
			creatureId = target.creatureId,
			attack = target.attack,
			defense = target.defense,
			specialAttack = target.specialAttack,
			specialDefense = target.specialDefense,
			speed = target.speed,
			weight = target.weight,
			elementIds = target.elementIds,
			skillSlots = copiedSkills,
			abilityId = target.abilityId,
			abilityEffects = target.allAbilityEffects(),
			suppressedAbilityEffects = emptyList(),
			statStages = target.statStages,
			transformSnapshot = snapshot,
		)
		return state.replaceParticipant(transformed).appendEvent(
			BattleEvent.ParticipantTransformed(state.turnNumber, actorId, target.actorId, target.creatureId),
		)
	}

	private fun revealOpponentHeldItems(state: BattleState, actorId: String): BattleState {
		val actorSideId = state.sideOf(actorId)?.sideId ?: return state
		val events = state.sides.filterNot { it.sideId == actorSideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() && it.itemId != null }
			.sortedBy { it.actorId }
			.map { target -> BattleEvent.OpponentHeldItemRevealed(state.turnNumber, actorId, target.actorId, requireNotNull(target.itemId)) }
		return state.appendEvents(events)
	}

	private fun revealOpponentHighestPowerSkill(state: BattleState, actorId: String): BattleState {
		val actorSideId = state.sideOf(actorId)?.sideId ?: return state
		val candidates = state.sides.filterNot { it.sideId == actorSideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
			.flatMap { target -> target.skillSlots.map { skill -> target to skill } }
		val selected = candidates.maxWithOrNull(
			compareBy<Pair<io.github.lishangbu.battleengine.model.BattleParticipant, io.github.lishangbu.battleengine.model.BattleSkillSlot>>
				{ it.second.power ?: 0 }
				.thenByDescending { it.second.skillId },
		) ?: return state
		return state.appendEvent(
			BattleEvent.OpponentSkillRevealed(state.turnNumber, actorId, selected.first.actorId, selected.second.skillId),
		)
	}

	private fun copyOpponentAbility(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		val actorSideId = state.sideOf(actorId)?.sideId ?: return state
		val source = state.sides.filterNot { it.sideId == actorSideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() && it.abilityId != null }
			.minByOrNull { it.actorId } ?: return state
		val updated = actor.copy(
			abilityId = source.abilityId,
			abilityEffects = source.allAbilityEffects(),
			suppressedAbilityEffects = emptyList(),
		)
		return state.replaceParticipant(updated).appendEvent(
			BattleEvent.AbilityChanged(state.turnNumber, actor.actorId, source.actorId, actor.abilityId, source.abilityId),
		)
	}

	/**
	 * 执行出场特性的能力阶级变化。
	 *
	 * 目标集合为触发者对侧当前上场且仍可战斗的成员。每个目标独立夹取 -6..6 的现代能力阶级边界；如果某个目标
	 * 已经达到边界，本次不会写入状态，也不会产生事件。该函数不消费随机数。
	 */
	private fun applyStatStageChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInStatStageChange,
	): BattleState {
		val actorSide = state.sideOf(actorId) ?: return state
		val eventStartIndex = state.events.size
		val targetActorIds = when (effect.target) {
			io.github.lishangbu.battleengine.model.BattleEffectTarget.USER -> listOf(actorId)
			io.github.lishangbu.battleengine.model.BattleEffectTarget.TARGET -> state.sides
				.filter { it.sideId != actorSide.sideId }
				.flatMap { it.activeParticipants() }
				.filter { it.canBattle() }
				.map { it.actorId }
		}

		val afterChanges = targetActorIds.fold(state) { current, targetActorId ->
			val target = current.participant(targetActorId) ?: return@fold current
			if (
				effect.stageDelta < 0 &&
				target.actorId != actorId &&
				target.abilityEffects.any { it is BattleAbilityEffect.OpponentStatStageReductionReflection }
			) {
				val source = current.participant(actorId) ?: return@fold current
				val before = source.statStage(effect.stat)
				val reflected = source.changeStatStage(effect.stat, effect.stageDelta)
				val delta = reflected.statStage(effect.stat) - before
				return@fold if (delta == 0) current else current
					.replaceParticipant(reflected)
					.appendEvent(
						BattleEvent.StatStageChanged(
							current.turnNumber,
							target.actorId,
							source.actorId,
							effect.stat,
							delta,
							reflected.statStage(effect.stat),
						),
					)
					.applyNegativeStatStageResetItem(source.actorId)
			}
			val switchInImmunity = effect.stageDelta < 0 && target.abilityEffects
				.filterIsInstance<BattleAbilityEffect.SwitchInStatStageReductionImmunity>()
				.any { effect.stat in it.stats }
			val reactiveBoost = if (effect.stageDelta < 0) target.abilityEffects
				.filterIsInstance<BattleAbilityEffect.SwitchInStatReductionReactiveBoost>()
				.firstOrNull { it.triggerStat == effect.stat } else null
			if (switchInImmunity || reactiveBoost != null) {
				val blocked = current.appendEvent(
					BattleEvent.StatStageChangeBlocked(
						turnNumber = current.turnNumber,
						actorId = actorId,
						targetActorId = target.actorId,
						stat = effect.stat,
						attemptedDelta = effect.stageDelta,
						reason = io.github.lishangbu.battleengine.model.BattleStatusBlockReason.ABILITY,
					),
				)
				if (reactiveBoost == null) return@fold blocked
				val boosted = target.changeStatStage(reactiveBoost.boostStat, reactiveBoost.stageDelta)
				val actualDelta = boosted.statStage(reactiveBoost.boostStat) - target.statStage(reactiveBoost.boostStat)
				return@fold if (actualDelta == 0) blocked else blocked.replaceParticipant(boosted).appendEvent(
					BattleEvent.StatStageChanged(
						current.turnNumber,
						target.actorId,
						target.actorId,
						reactiveBoost.boostStat,
						actualDelta,
						boosted.statStage(reactiveBoost.boostStat),
					),
				)
			}
			if (current.statStageDropBlockedByAbility(actorId, target, effect.stat, effect.stageDelta)) {
				return@fold current.appendEvent(
					BattleEvent.StatStageChangeBlocked(
						turnNumber = current.turnNumber,
						actorId = actorId,
						targetActorId = target.actorId,
						stat = effect.stat,
						attemptedDelta = effect.stageDelta,
						reason = io.github.lishangbu.battleengine.model.BattleStatusBlockReason.ABILITY,
					),
				)
			}
			if (current.statStageDropBlockedByItem(actorId, target, effect.stageDelta)) {
				return@fold current.appendEvent(
					BattleEvent.StatStageChangeBlocked(
						turnNumber = current.turnNumber,
						actorId = actorId,
						targetActorId = target.actorId,
						stat = effect.stat,
						attemptedDelta = effect.stageDelta,
						reason = io.github.lishangbu.battleengine.model.BattleStatusBlockReason.ITEM,
					),
				)
			}
			val beforeStage = target.statStage(effect.stat)
			val updated = target.changeStatStage(effect.stat, effect.stageDelta)
			val afterStage = updated.statStage(effect.stat)
			if (beforeStage == afterStage) {
				current
			} else {
				current
					.replaceParticipant(updated)
					.appendEvent(
						BattleEvent.StatStageChanged(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = target.actorId,
							stat = effect.stat,
							delta = afterStage - beforeStage,
							currentStage = afterStage,
						),
					)
					.applyAbilityStatReductionReactiveItem(actorId, target.actorId)
					.applyNegativeStatStageResetItem(target.actorId)
			}
		}
		return afterChanges.applyOpponentStatReductionReactiveAbilities(eventStartIndex, actorId)
	}

	private fun applyDefenseComparisonBoost(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		val actorSide = state.sideOf(actorId) ?: return state
		val opponents = state.sides.filterNot { it.sideId == actorSide.sideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
		if (opponents.isEmpty()) return state
		val defenseTotal = opponents.sumOf { it.defense }
		val specialDefenseTotal = opponents.sumOf { it.specialDefense }
		val stat = if (defenseTotal < specialDefenseTotal) BattleStat.ATTACK else BattleStat.SPECIAL_ATTACK
		val before = actor.statStage(stat)
		val updated = actor.changeStatStage(stat, 1)
		val delta = updated.statStage(stat) - before
		if (delta == 0) return state
		return state.replaceParticipant(updated).appendEvent(
			BattleEvent.StatStageChanged(state.turnNumber, actorId, actorId, stat, delta, updated.statStage(stat)),
		)
	}

	private fun applyAllyHeal(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInAllyHeal,
	): BattleState {
		val side = state.sideOf(actorId) ?: return state
		return side.activeParticipants().filterNot { it.actorId == actorId }.fold(state) { current, snapshot ->
			val ally = current.participant(snapshot.actorId) ?: return@fold current
			if (!ally.canReceiveHealing()) return@fold current
			val amount = (ally.maxHp / effect.healDenominator).coerceAtLeast(1).coerceAtMost(ally.maxHp - ally.currentHp)
			current.replaceParticipant(ally.heal(amount)).appendEvent(
				BattleEvent.HealingApplied(current.turnNumber, ally.actorId, amount),
			)
		}
	}

	private fun applyAllyStatStageCopy(state: BattleState, actorId: String): BattleState {
		val side = state.sideOf(actorId) ?: return state
		val ally = side.activeParticipants().firstOrNull { it.actorId != actorId && it.canBattle() } ?: return state
		var actor = state.participant(actorId) ?: return state
		val events = mutableListOf<BattleEvent>()
		BattleStat.entries.forEach { stat ->
			val before = actor.statStage(stat)
			actor = actor.setStatStage(stat, ally.statStage(stat))
			val delta = actor.statStage(stat) - before
			if (delta != 0) events += BattleEvent.StatStageChanged(
				state.turnNumber, actorId, actorId, stat, delta, actor.statStage(stat),
			)
		}
		return state.replaceParticipant(actor).appendEvents(events)
	}

	private fun applyAllyStatStageReset(state: BattleState, actorId: String): BattleState {
		val side = state.sideOf(actorId) ?: return state
		return side.activeParticipants().filterNot { it.actorId == actorId }.fold(state) { current, snapshot ->
			var ally = current.participant(snapshot.actorId) ?: return@fold current
			val events = mutableListOf<BattleEvent>()
			BattleStat.entries.forEach { stat ->
				val before = ally.statStage(stat)
				if (before != 0) {
					ally = ally.setStatStage(stat, 0)
					events += BattleEvent.StatStageChanged(current.turnNumber, actorId, ally.actorId, stat, -before, 0)
				}
			}
			current.replaceParticipant(ally).appendEvents(events)
		}
	}

	private fun clearAllSideDamageReductions(state: BattleState, actorId: String): BattleState =
		state.sides.fold(state) { current, sideSnapshot ->
			val removal = current.removeSideDamageReductions(
				sideSnapshot.sideId,
				io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind.entries.toSet(),
			) ?: return@fold current
			removal.state.appendEvent(
				BattleEvent.AbilitySideDamageReductionsRemoved(
					current.turnNumber,
					actorId,
					sideSnapshot.sideId,
					removal.removedKinds,
				),
			)
		}

	private companion object {
		private const val PROTOSYNTHESIS_ABILITY_ID = 281L
		private const val QUARK_DRIVE_ABILITY_ID = 282L
		private const val SPEED_BOOST_MULTIPLIER = 1.5
		private const val OTHER_STAT_BOOST_MULTIPLIER = 1.3
	}
}
