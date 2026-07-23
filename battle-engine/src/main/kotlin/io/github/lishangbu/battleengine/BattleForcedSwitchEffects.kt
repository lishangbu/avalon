package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能命中后的强制换人效果结算器。
 *
 * 强制换人发生在技能伤害和普通附加效果之后，因此巴投、龙尾这类技能会先造成伤害，再把仍可战斗的目标换下。
 * 本类只处理“选中后备成员、写入强制换人事件、应用换入后的入场阶段”这一段，不处理命中、保护、粉末/属性/特性
 * 免疫、伤害或普通附加效果。这样主状态机仍决定强制换人的阶段位置，而换入后的陷阱和入场特性复用主动替换的
 * 同一套 resolver，避免出现主动换人与强制换人的两套入场实现。
 */
internal class BattleForcedSwitchEffects(
	private val targetDefenseEffects: BattleTargetDefenseEffects,
	private val bindingEffects: BattleBindingEffects,
	private val entryHazardEffects: BattleEntryHazardEffects,
	private val switchInAbilityEffects: BattleSwitchInAbilityEffects,
	private val switchOutAbilityEffects: BattleSwitchOutAbilityEffects,
) {
	/**
	 * 应用技能声明的强制目标换人效果。
	 *
	 * 若目标已经倒下、目标不在场、目标侧没有可战斗后备成员，或目标替身阻止了对手技能效果，则状态保持不变且
	 * 不消费随机数。当存在多个合法后备成员时，随机源只消费一次并以稳定 reason 记录；单个后备成员时直接选中，
	 * 避免无意义随机消费破坏 replay 脚本。
	 */
	fun apply(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		if (!skill.forceTargetSwitch || state.result != null) {
			return state
		}
		return forceBySkill(state, actorId, targetActorId, skill, random)
	}

	fun applyDamagedItem(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		damageAmount: Int,
		random: BattleRandom,
	): BattleState {
		if (damageAmount <= 0 || state.result != null) return state
		val afterAbility = applyDamageThresholdAbility(state, targetActorId, damageAmount, random)
		val target = afterAbility.participant(targetActorId) ?: return afterAbility
		val itemId = target.itemId ?: return afterAbility
		return when {
			target.itemEffects.any { it is BattleItemEffect.DamagedForceSelfSwitch } ->
				forceByItem(afterAbility, target.actorId, target.actorId, itemId, random)
			target.itemEffects.any { it is BattleItemEffect.DamagedForceAttackerSwitch } ->
				forceByItem(afterAbility, target.actorId, actorId, itemId, random)
			else -> afterAbility
		}
	}

	private fun applyDamageThresholdAbility(
		state: BattleState,
		targetActorId: String,
		damageAmount: Int,
		random: BattleRandom,
	): BattleState {
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle() || !state.isActive(targetActorId)) return state
		val effect = target.abilityEffects
			.filterIsInstance<io.github.lishangbu.battleengine.model.BattleAbilityEffect.DamageCrossedHpThresholdForceSelfSwitch>()
			.firstOrNull { threshold ->
				val before = (target.currentHp + damageAmount).coerceAtMost(target.maxHp)
				before * threshold.thresholdDenominator > target.maxHp * threshold.thresholdNumerator &&
					target.currentHp * threshold.thresholdDenominator <= target.maxHp * threshold.thresholdNumerator
			} ?: return state
		val side = state.sideOf(targetActorId) ?: return state
		val candidates = side.participants.filter { it.canBattle() && !side.isActive(it.actorId) }
		if (candidates.isEmpty()) return state
		val next = if (candidates.size == 1) candidates.single() else {
			candidates[random.nextInt(candidates.size, "ability forced switch target for $targetActorId")]
		}
		return completeSwitch(
			state,
			targetActorId,
			next.actorId,
			random,
			BattleEvent.AbilityForcedSwitchSelected(state.turnNumber, targetActorId, next.actorId),
		)
	}

	fun applyNegativeStatStageItem(state: BattleState, eventStartIndex: Int, random: BattleRandom): BattleState {
		val loweredActorIds = state.events.drop(eventStartIndex)
			.filterIsInstance<BattleEvent.StatStageChanged>()
			.filter { it.delta < 0 }
			.map { it.targetActorId }
			.distinct()
		return loweredActorIds.fold(state) { current, actorId ->
			val holder = current.participant(actorId) ?: return@fold current
			val itemId = holder.itemId ?: return@fold current
			if (!current.isActive(actorId) || holder.itemEffects.none { it is BattleItemEffect.NegativeStatStageForceSelfSwitch }) {
				return@fold current
			}
			forceByItem(current, actorId, actorId, itemId, random)
		}
	}

	private fun forceBySkill(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle() || !state.isActive(target.actorId)) {
			return state
		}
		if (target.abilityEffects.any { it is io.github.lishangbu.battleengine.model.BattleAbilityEffect.ForcedSwitchImmunity }) {
			return state
		}
		if (targetDefenseEffects.substituteBlocksOpponentEffect(state, actorId, target.actorId, skill)) {
			return state
		}
		val side = state.sideOf(target.actorId) ?: return state
		val candidates = side.participants
			.filter { participant -> participant.canBattle() && !side.isActive(participant.actorId) }
		if (candidates.isEmpty()) {
			return state
		}
		val next = if (candidates.size == 1) {
			candidates.single()
		} else {
			candidates[random.nextInt(candidates.size, "forced switch target for ${skill.skillId}")]
		}
		return completeSwitch(
			state = state,
			targetActorId = target.actorId,
			nextActorId = next.actorId,
			random = random,
			selectionEvent = BattleEvent.TargetForcedSwitchSelected(
				turnNumber = state.turnNumber,
				actorId = actorId,
				targetActorId = target.actorId,
				skillId = skill.skillId,
				nextActorId = next.actorId,
			),
		)
	}

	private fun forceByItem(
		state: BattleState,
		sourceActorId: String,
		targetActorId: String,
		itemId: Long,
		random: BattleRandom,
	): BattleState {
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle() || !state.isActive(target.actorId) || state.result != null) return state
		if (
			targetActorId != sourceActorId &&
			target.abilityEffects.any { it is io.github.lishangbu.battleengine.model.BattleAbilityEffect.ForcedSwitchImmunity }
		) return state
		val side = state.sideOf(target.actorId) ?: return state
		val candidates = side.participants.filter { it.canBattle() && !side.isActive(it.actorId) }
		if (candidates.isEmpty()) return state
		val source = state.participant(sourceActorId) ?: return state
		if (source.itemId != itemId) return state
		val next = if (candidates.size == 1) candidates.single() else {
			candidates[random.nextInt(candidates.size, "item forced switch target for $itemId")]
		}
		return completeSwitch(
			state = state.replaceParticipant(source.consumeHeldItem()),
			targetActorId = target.actorId,
			nextActorId = next.actorId,
			random = random,
			selectionEvent = BattleEvent.ItemForcedSwitchSelected(
				turnNumber = state.turnNumber,
				sourceActorId = sourceActorId,
				targetActorId = target.actorId,
				itemId = itemId,
				nextActorId = next.actorId,
			),
		)
	}

	private fun completeSwitch(
		state: BattleState,
		targetActorId: String,
		nextActorId: String,
		random: BattleRandom,
		selectionEvent: BattleEvent,
	): BattleState {
		val side = state.sideOf(targetActorId) ?: return state
		val afterSwitchOutAbility = switchOutAbilityEffects.apply(state, targetActorId)
		val switched = afterSwitchOutAbility.switchActive(targetActorId, nextActorId)
			.appendEvents(
				listOf(
					selectionEvent,
					BattleEvent.ParticipantSwitched(
						turnNumber = state.turnNumber,
						sideId = side.sideId,
						previousActorId = targetActorId,
						nextActorId = nextActorId,
						forced = true,
					),
				),
			)
			.synchronizePassiveSuppressions()
			.synchronizeStrongWeather()
		val afterBindingSourceCleared = bindingEffects.clearBindingsFromSource(switched, targetActorId)
		val afterEntryHazards = entryHazardEffects.applyOnSwitchIn(afterBindingSourceCleared, side.sideId, nextActorId, random)
		return switchInAbilityEffects.apply(afterEntryHazards, nextActorId)
	}
}
