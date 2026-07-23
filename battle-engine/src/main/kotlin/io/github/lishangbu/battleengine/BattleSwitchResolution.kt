package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.SwitchPreventionReason
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 结算一个回合开始时的主动替换阶段。
 *
 * 替换阶段在现代回合制规则中位于所有技能行动之前，因此它不需要知道当前回合的保护集合、技能优先度、
 * 命中判定或伤害流程。它只关心五件事：
 * - 按离场成员的有效速度排序替换行动，并用同一个 [BattleRandom] 打破同速顺序，保证 replay 可复现。
 * - 检查成员是否因为休整、蓄力、锁招或束缚而不能主动替换。
 * - 真正替换上场成员并写入 [BattleEvent.ParticipantSwitched]。
 * - 清理离场成员作为束缚来源造成的持续束缚。
 * - 对新上场成员结算入场陷阱和入场特性。
 *
 * 这个类刻意不处理强制替换技能；强制替换发生在技能命中后的效果阶段，那里需要目标替身、无视特性、
 * 入场陷阱和换入特性一起配合，已经由 [BattleForcedSwitchEffects] 维护。主动替换和强制替换共享后半段
 * 入场规则，但触发来源和阻止条件不同，分开放能避免主引擎里出现一长串来源判断。
 */
internal class BattleSwitchResolution(
	private val actionOrdering: BattleActionOrdering,
	private val bindingEffects: BattleBindingEffects,
	private val entryHazardEffects: BattleEntryHazardEffects,
	private val switchInAbilityEffects: BattleSwitchInAbilityEffects,
) {
	/**
	 * 按速度顺序结算全部替换行动。
	 *
	 * 如果前一个替换已经让战斗结束，后续替换不会再执行。主动替换要求离场成员仍在场上；若成员已经倒下，
	 * 这次替换会被视作 forced=true 的补位替换，允许绕过休整、蓄力、锁招和束缚这些“能战斗时才阻止主动替换”
	 * 的限制。这样可以保持倒下补位与普通主动替换共用同一条入场陷阱和入场特性流水线。
	 */
	fun resolve(
		state: BattleState,
		actions: List<BattleAction.SwitchParticipant>,
		random: BattleRandom,
	): BattleState {
		val ordered = actions
			.map { action ->
				val actor = requireNotNull(state.participant(action.actorId)) { "switch actor not found: ${action.actorId}" }
				SwitchPlan(action, actor)
			}
			.groupBy { actionOrdering.effectiveSpeed(state, it.actor) }
			.toSortedMap(actionOrdering.speedComparator(state))
			.values
			.flatMap { sameSpeedPlans ->
				if (sameSpeedPlans.size == 1) {
					sameSpeedPlans
				} else {
					sameSpeedPlans.sortedByRandomTieBreak(random) { "switch speed tie for ${it.actor.actorId}" }
				}
			}
		return ordered.fold(state) { current, plan ->
			if (current.result != null) {
				return@fold current
			}
			val actor = current.participant(plan.action.actorId) ?: return@fold current
			val side = current.sideOf(actor.actorId) ?: return@fold current
			require(side.isActive(actor.actorId)) { "switch actor must be active: ${actor.actorId}" }
			if (actor.canBattle() && actor.rechargeTurnsRemaining > 0) {
				return@fold current.preventSwitch(actor, SwitchPreventionReason.RECHARGE)
			}
			if (actor.canBattle() && actor.chargingTurnsRemaining > 0) {
				val chargingSkillId = actor.chargingSkillId ?: return@fold current
				return@fold current.preventSwitch(actor, SwitchPreventionReason.CHARGING, skillId = chargingSkillId)
			}
			if (actor.canBattle() && actor.lockedMoveTurnsRemaining > 0) {
				val lockedSkillId = actor.lockedMoveSkillId ?: return@fold current
				return@fold current.preventSwitch(actor, SwitchPreventionReason.LOCKED_MOVE, skillId = lockedSkillId)
			}
			if (
				actor.canBattle() &&
				bindingEffects.isBindingSourceActive(current, actor) &&
				actor.itemEffects.none { it is BattleItemEffect.SwitchRestrictionImmunity }
			) {
				val sourceActorId = actor.boundByActorId ?: return@fold current
				return@fold current.preventSwitch(
					actor = actor,
					reason = SwitchPreventionReason.BINDING,
					sourceActorId = sourceActorId,
					turnsRemainingBefore = actor.bindingTurnsRemaining,
				)
			}
			val afterSwitchOutAbility = current.applySwitchOutAbilities(actor)
			val switched = afterSwitchOutAbility.switchActive(actor.actorId, plan.action.targetActorId)
			val withSwitchEvent = switched.appendEvent(
				BattleEvent.ParticipantSwitched(
					turnNumber = current.turnNumber,
					sideId = side.sideId,
					previousActorId = actor.actorId,
					nextActorId = plan.action.targetActorId,
					forced = !actor.canBattle(),
				),
			)
			val afterBindingSourceCleared = bindingEffects.clearBindingsFromSource(withSwitchEvent, actor.actorId)
			val afterEntryHazards = entryHazardEffects.applyOnSwitchIn(
				state = afterBindingSourceCleared,
				sideId = side.sideId,
				actorId = plan.action.targetActorId,
				random = random,
			)
			switchInAbilityEffects.apply(afterEntryHazards, plan.action.targetActorId)
		}
	}

	private fun BattleState.applySwitchOutAbilities(actor: BattleParticipant): BattleState {
		var updated = actor
		val events = mutableListOf<BattleEvent>()
		val stanceChange = updated.abilityEffects.filterIsInstance<BattleAbilityEffect.StanceChange>().firstOrNull()
		val defensiveProfile = stanceChange?.let { updated.battleFormProfiles[it.defensiveFormCode] }
		if (defensiveProfile != null && defensiveProfile.creatureId != updated.creatureId) {
			val previousCreatureId = updated.creatureId
			updated = updated.changeBattleForm(defensiveProfile)
			if (actor.canBattle()) {
				events += BattleEvent.FormChanged(turnNumber, actor.actorId, previousCreatureId, defensiveProfile.creatureId)
			}
		}
		if (!actor.canBattle()) return replaceParticipant(updated)
		if (
			updated.majorStatus != null &&
			updated.abilityEffects.any { it is BattleAbilityEffect.SwitchOutMajorStatusCure }
		) {
			val status = requireNotNull(updated.majorStatus)
			updated = updated.clearMajorStatus()
			events += BattleEvent.StatusCleared(turnNumber, updated.actorId, status)
		}
		updated.abilityEffects.filterIsInstance<BattleAbilityEffect.SwitchOutHeal>().forEach { effect ->
			if (updated.currentHp < updated.maxHp) {
				val amount = (updated.maxHp / effect.healDenominator).coerceAtLeast(1)
					.coerceAtMost(updated.maxHp - updated.currentHp)
				updated = updated.heal(amount)
				events += BattleEvent.HealingApplied(turnNumber, updated.actorId, amount)
			}
		}
		return replaceParticipant(updated).appendEvents(events)
	}

	/**
	 * 主动替换被阻止时只记录事件，不改写成员状态。
	 *
	 * 阻止原因字段保留技能 ID 或束缚来源，是为了让 replay 和管理端调试能说明“为什么这次替换没有发生”。
	 * 这里不追加额外失败事件，避免同一次替换阻止在事件流里出现两种语义接近的事实。
	 */
	private fun BattleState.preventSwitch(
		actor: BattleParticipant,
		reason: SwitchPreventionReason,
		skillId: Long? = null,
		sourceActorId: String? = null,
		turnsRemainingBefore: Int? = null,
	): BattleState =
		appendEvent(
			BattleEvent.SwitchPrevented(
				turnNumber = turnNumber,
				actorId = actor.actorId,
				reason = reason,
				skillId = skillId,
				sourceActorId = sourceActorId,
				turnsRemainingBefore = turnsRemainingBefore,
			),
		)

	/**
	 * 主动替换阶段排序前保留下来的行动快照。
	 *
	 * [action] 是玩家提交或倒下补位产生的替换意图，[actor] 是进入排序时读取到的离场成员状态。排序必须使用替换前
	 * 成员的有效速度；等真正执行时仍会从最新 [BattleState] 重新读取成员，避免前一个替换、倒下或战斗结束后继续
	 * 使用过期对象。这个小类型只在本类内部存在，目的是把“用于排序的快照”和“执行时的最新状态读取”区分清楚。
	 */
	private data class SwitchPlan(
		val action: BattleAction.SwitchParticipant,
		val actor: BattleParticipant,
	)
}
