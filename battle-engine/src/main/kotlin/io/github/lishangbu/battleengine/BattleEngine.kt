package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 现代回合制战斗引擎的第一阶段核心状态机。
 *
 * 该类不依赖 Spring、Jimmer 或数据库。调用方传入已经冻结的初始状态、规则快照、行动列表和随机源，
 * 引擎返回新的不可变战斗状态和事件流。第一阶段实现基础战斗闭环：启动、回合开始、技能行动排序、
 * 替换、PP 消耗、命中/闪避判定、击中要害、基础伤害、保护、状态、天气、场地、双打范围目标修正、
 * 倒下检测和胜负判定。
 *
 * 当前不负责的边界包括：主动使用道具、状态持续效果细分、复杂技能脚本和完整官方竞技裁定。
 * 这些能力会通过后续规则处理器接入，但仍共享这里的事件流和确定性随机源。
 */
class BattleEngine(
	private val damageCalculator: BattleDamageCalculator = BattleDamageCalculator(),
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
) {
	/**
	 * 启动一场战斗并产出初始事件。
	 *
	 * @param initialState 已冻结的战斗初始快照。
	 * @return turnNumber 为 0 的战斗状态，事件流包含 `BattleStarted`。
	 */
	fun start(initialState: BattleInitialState): BattleState =
		BattleState(
			format = initialState.format,
			rules = initialState.rules,
			environment = initialState.environment,
			sides = initialState.sides,
			turnNumber = 0,
			events = listOf(
				BattleEvent.BattleStarted(
					turnNumber = 0,
					formatCode = initialState.format.code,
					sideIds = initialState.sides.map { it.sideId },
				),
			),
		)

	/**
	 * 结算一个完整回合。
	 *
	 * @param state 当前战斗状态，不能已经结束。
	 * @param actions 本回合行动。第一阶段要求每个可行动上场成员最多提交一个 `UseSkill`。
	 * @param random 所有命中、击中要害、伤害浮动和同速排序都从这里消费随机数。
	 * @return 结算后的新状态。若战斗结束，事件流最后会包含 `BattleEnded`；否则包含 `TurnEnded`。
	 */
	fun resolveTurn(state: BattleState, actions: List<BattleAction>, random: BattleRandom): BattleState {
		require(state.result == null) { "battle already ended" }
		require(actions.map { it.actorId }.toSet().size == actions.size) {
			"each actor can submit at most one action per turn"
		}
		val nextTurnNumber = state.turnNumber + 1
		val started = state
			.copy(turnNumber = nextTurnNumber)
			.appendEvent(BattleEvent.TurnStarted(nextTurnNumber))
		val afterSwitches = resolveSwitches(started, actions.filterIsInstance<BattleAction.SwitchParticipant>(), random)
		if (afterSwitches.result != null) {
			return afterSwitches
		}
		val skillActions = skillActionsForTurn(afterSwitches, actions.filterIsInstance<BattleAction.UseSkill>())
		val orderedActions = orderSkillActions(afterSwitches, skillActions, random)
		val resolvedContext = orderedActions.fold(TurnContext(afterSwitches)) { current, plan ->
			if (current.state.result != null) current else executeUseSkill(current, plan, random)
		}
		val resolved = resetProtectionChains(
			state = resolvedContext.state,
			successfulProtectionActorIds = resolvedContext.successfulProtectionActorIds,
		)
		val afterEndTurnEffects = resolved.result?.let { resolved } ?: applyEndTurnEffects(resolved)
		val afterEndTurnVolatileStatuses = afterEndTurnEffects.result?.let { afterEndTurnEffects }
			?: clearEndTurnVolatileStatuses(afterEndTurnEffects)
		val afterEnvironmentDurations = afterEndTurnVolatileStatuses.result?.let { afterEndTurnVolatileStatuses }
			?: advanceEnvironmentDurations(afterEndTurnVolatileStatuses)
		return afterEnvironmentDurations.result?.let { afterEnvironmentDurations }
			?: afterEnvironmentDurations.appendEvent(BattleEvent.TurnEnded(nextTurnNumber))
	}

	/**
	 * 按优先度、速度和同速随机数排序行动。
	 *
	 * 第一阶段只支持技能行动，所以优先度来自技能槽。速度相同的行动会消费随机数作为排序键；
	 * 这不是最终双打同速规则的完整实现，但已经保证同一随机脚本下的 replay 稳定。
	 */
	private fun orderSkillActions(state: BattleState, actions: List<SkillActionInput>, random: BattleRandom): List<ActionPlan> {
		val plans = actions.map { input ->
			val action = input.action
			val actor = requireNotNull(state.participant(action.actorId)) { "actor not found: ${action.actorId}" }
			val skill = requireNotNull(actor.skillSlot(action.skillId)) { "skill not found: ${action.skillId}" }
			ActionPlan(action, actor, skill, input.lockedContinuation)
		}
		return plans
			.groupBy { it.skill.priority to effectiveSpeed(it.actor) }
			.toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
			.values
			.flatMap { sameOrderPlans ->
				if (sameOrderPlans.size == 1) {
					sameOrderPlans
				} else {
					sameOrderPlans.sortedBy { random.nextInt(1_000_000, "speed tie for ${it.actor.actorId}") }
				}
			}
	}

	/**
	 * 组装技能阶段实际要执行的行动。
	 *
	 * 锁招成员会强制继续使用锁定技能：如果玩家提交了其它技能选择，会被这里替换；如果玩家没有提交技能行动，
	 * 引擎也会自动生成一次锁招行动。目标仍保存为首次锁定时选择的目标槽位，以复用现有目标重定向语义。
	 */
	private fun skillActionsForTurn(state: BattleState, submittedActions: List<BattleAction.UseSkill>): List<SkillActionInput> {
		val lockedActions = state.sides
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() && it.lockedMoveTurnsRemaining > 0 }
			.mapNotNull { actor ->
				val skillId = actor.lockedMoveSkillId ?: return@mapNotNull null
				val targetActorId = actor.lockedMoveTargetActorId ?: return@mapNotNull null
				SkillActionInput(
					action = BattleAction.UseSkill(actor.actorId, skillId = skillId, targetActorId = targetActorId),
					lockedContinuation = true,
				)
			}
		val lockedActorIds = lockedActions.map { it.action.actorId }.toSet()
		return submittedActions
			.filterNot { it.actorId in lockedActorIds }
			.map { SkillActionInput(it, lockedContinuation = false) } + lockedActions
	}

	/**
	 * 结算本回合所有替换行动。
	 *
	 * 替换阶段先于技能阶段。第一版按离场成员的有效速度排序，速度相同时消费随机数打破平手；这让未来接入
	 * 入场特性时能得到可复盘的确定顺序。若离场成员已经倒下，事件标记为 `forced=true`。
	 */
	private fun resolveSwitches(
		state: BattleState,
		actions: List<BattleAction.SwitchParticipant>,
		random: BattleRandom,
	): BattleState {
		val ordered = actions
			.map { action ->
				val actor = requireNotNull(state.participant(action.actorId)) { "switch actor not found: ${action.actorId}" }
				SwitchPlan(action, actor)
			}
			.groupBy { effectiveSpeed(it.actor) }
			.toSortedMap(compareByDescending { it })
			.values
			.flatMap { sameSpeedPlans ->
				if (sameSpeedPlans.size == 1) {
					sameSpeedPlans
				} else {
					sameSpeedPlans.sortedBy { random.nextInt(1_000_000, "switch speed tie for ${it.actor.actorId}") }
				}
			}
		return ordered.fold(state) { current, plan ->
			val actor = current.participant(plan.action.actorId) ?: return@fold current
			val side = current.sideOf(actor.actorId) ?: return@fold current
			require(side.isActive(actor.actorId)) { "switch actor must be active: ${actor.actorId}" }
			if (actor.canBattle() && actor.lockedMoveTurnsRemaining > 0) {
				val lockedSkillId = actor.lockedMoveSkillId ?: return@fold current
				return@fold current.appendEvent(
					BattleEvent.SwitchPreventedByLockedMove(
						turnNumber = current.turnNumber,
						actorId = actor.actorId,
						skillId = lockedSkillId,
					),
				)
			}
			val switched = current.switchActive(actor.actorId, plan.action.targetActorId)
			switched.appendEvent(
				BattleEvent.ParticipantSwitched(
					turnNumber = current.turnNumber,
					sideId = side.sideId,
					previousActorId = actor.actorId,
					nextActorId = plan.action.targetActorId,
					forced = !actor.canBattle(),
				),
			)
		}
	}

	/**
	 * 执行一次使用技能行动。
	 *
	 * 行动者若已经倒下会被跳过；单体目标按席位语义重定向，范围目标按当前站位重新收集。
	 * 技能使用事件和 PP 消耗只发生一次，随后每个实际目标独立结算命中、要害、伤害和附加效果。
	 */
	private fun executeUseSkill(context: TurnContext, plan: ActionPlan, random: BattleRandom): TurnContext {
		val state = context.state
		val action = plan.action
		val actor = state.participant(action.actorId) ?: return context
		if (!state.isActive(actor.actorId) || !actor.canBattle()) {
			return context
		}
		val beforeMove = resolveBeforeMoveEffects(context, actor, plan.skill, random)
		if (beforeMove.blocked) {
			return if (plan.lockedContinuation) {
				beforeMove.context.copy(
					state = endLockedMoveAfterDisruption(beforeMove.context.state, actor.actorId, plan.skill, random),
				)
			} else {
				beforeMove.context
			}
		}
		val actionState = beforeMove.context.state
		val readyActor = actionState.participant(action.actorId) ?: return beforeMove.context
		val skill = readyActor.skillSlot(action.skillId) ?: return beforeMove.context
		val targets = targetsForSkill(actionState, readyActor.actorId, action.targetActorId, skill)
		if (targets.isEmpty()) {
			return if (plan.lockedContinuation) {
				beforeMove.context.copy(
					state = endLockedMoveAfterDisruption(actionState, readyActor.actorId, skill, random),
				)
			} else {
				beforeMove.context
			}
		}
		if (!plan.lockedContinuation) {
			require(skill.remainingPp > 0) { "skill has no remaining PP: ${skill.skillId}" }
		}

		val actorAfterPp = if (plan.lockedContinuation) {
			readyActor
		} else {
			readyActor.replaceSkillSlot(skill.consumePp())
		}
		val usedState = actionState
			.replaceParticipant(actorAfterPp)
			.appendEvent(
				BattleEvent.SkillUsed(
					turnNumber = actionState.turnNumber,
					actorId = readyActor.actorId,
					targetActorId = targets.first().actorId,
					skillId = skill.skillId,
					skillName = skill.name,
				),
			)

		if (skill.protectsUser) {
			if (!protectionSucceeds(readyActor, skill, random)) {
				return beforeMove.context.copy(
					state = usedState
						.replaceParticipant(actorAfterPp.resetProtectionChain())
						.appendEvent(
							BattleEvent.ProtectionFailed(
								turnNumber = actionState.turnNumber,
								actorId = readyActor.actorId,
								skillId = skill.skillId,
							),
						),
				)
			}
			val protectedActor = actorAfterPp.markProtectionSuccess()
			return beforeMove.context.copy(
				state = usedState
					.replaceParticipant(protectedActor)
					.appendEvent(
						BattleEvent.ProtectionStarted(
							turnNumber = actionState.turnNumber,
							actorId = readyActor.actorId,
							skillId = skill.skillId,
						),
					),
				protectedActorIds = beforeMove.context.protectedActorIds + readyActor.actorId,
				successfulProtectionActorIds = beforeMove.context.successfulProtectionActorIds + readyActor.actorId,
			)
		}

		val targetMultiplier = targetDamageMultiplier(skill, targets)
		return targets.fold(beforeMove.context.copy(state = usedState)) { current, target ->
			if (current.state.result != null) {
				current
			} else {
				resolveSkillAgainstTarget(
					context = current,
					actorId = readyActor.actorId,
					targetActorId = target.actorId,
					skill = skill,
					targetMultiplier = targetMultiplier,
					random = random,
				)
			}
		}
	}

	/**
	 * 结算已经宣告使用的技能对单个实际目标的影响。
	 *
	 * 多目标技能在使用阶段只消耗一次 PP，并在这里按目标逐个处理保护、命中、属性免疫、伤害和附加效果。
	 * 命中、击中要害和伤害随机数按目标独立消费；范围伤害倍率由使用阶段根据原始目标集合提前计算，
	 * 因此某个目标后续被保护或闪避时，不会改变其它目标的范围修正。
	 */
	private fun resolveSkillAgainstTarget(
		context: TurnContext,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext {
		val state = context.state
		val actor = state.participant(actorId) ?: return context
		val target = state.participant(targetActorId) ?: return context
		if (!actor.canBattle() || !target.canBattle()) {
			return context
		}

		val powderBlockedElementId = powderBlockedElementId(state, target, skill)
		if (powderBlockedElementId != null) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillBlockedByElement(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							elementId = powderBlockedElementId,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		if (skillBlockedByTerrain(state, actor, target, skill)) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillBlockedByTerrain(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							terrain = state.environment.terrain,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		if (target.actorId in context.protectedActorIds && skill.affectedByProtect) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillBlockedByProtection(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		val accuracyCheck = accuracyCheck(actor, target, skill, random)
		if (!accuracyCheck.hit) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillMissed(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							accuracyRoll = accuracyCheck.roll ?: 0,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}
		if (skill.damageClass == BattleDamageClass.STATUS) {
			val afterEffects = applySkillEffects(state, actor.actorId, target.actorId, skill, random)
			return context.copy(
				state = updateLockedMoveAfterSuccessfulUse(
					state = afterEffects,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		val effectiveness = state.rules.elementChart.multiplier(skill.elementId, target.elementIds)
		if (effectiveness == 0.0) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.DamageApplied(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							amount = 0,
							effectiveness = 0.0,
							targetMultiplier = targetMultiplier,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		val hitCount = determineHitCount(skill, random)
		val stateWithHitCount = if (hitCount > 1) {
			state.appendEvent(
				BattleEvent.MultiHitCountDetermined(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					hitCount = hitCount,
				),
			)
		} else {
			state
		}
		val afterHits = (1..hitCount).fold(context.copy(state = stateWithHitCount)) { current, _ ->
			if (current.state.result != null) {
				current
			} else {
				resolveDamagingHit(
					context = current,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skill = skill,
					targetMultiplier = targetMultiplier,
					random = random,
				)
			}
		}
		if (afterHits.state.result != null) {
			return afterHits
		}
		val latestTarget = afterHits.state.participant(target.actorId) ?: target
		val afterEffects = applySkillEffects(afterHits.state, actor.actorId, latestTarget.actorId, skill, random)
		return afterHits.copy(
			state = updateLockedMoveAfterSuccessfulUse(
				state = afterEffects,
				actorId = actor.actorId,
				targetActorId = latestTarget.actorId,
				skill = skill,
				random = random,
			),
		)
	}

	/**
	 * 结算多段或单段伤害中的一段。
	 *
	 * 命中判定、PP 消耗和技能使用事件都已经在外层完成；这里每段独立消费要害和伤害浮动随机数，并在目标
	 * 或使用者因伤害、接触特性、反伤倒下时立即停止后续段数。
	 */
	private fun resolveDamagingHit(
		context: TurnContext,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext {
		val state = context.state
		val actor = state.participant(actorId) ?: return context
		val target = state.participant(targetActorId) ?: return context
		if (!actor.canBattle() || !target.canBattle()) {
			return context
		}
		val criticalHitCheck = criticalHitCheck(skill, random)
		val randomPercent = 85 + random.nextInt(16, "damage random for ${skill.skillId}")
		val damage = damageCalculator.calculate(
			BattleDamageRequest(
				attacker = actor,
				defender = target,
				skill = skill,
				rules = state.rules,
				environment = state.environment,
				randomPercent = randomPercent,
				targetMultiplier = targetMultiplier,
				criticalHit = criticalHitCheck.hit,
			),
		)
		val damagedTarget = target.receiveDamage(damage.amount)
		val damagedState = state
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.DamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = damage.amount,
					effectiveness = damage.effectiveness,
					targetMultiplier = damage.targetMultiplier,
					criticalHit = criticalHitCheck.hit,
				),
			)
		val afterFireThaw = clearFreezeAfterFireDamage(damagedState, damagedTarget, skill)
		val afterContactAbilities = applyContactAbilityEffects(
			state = afterFireThaw,
			actorId = actor.actorId,
			targetActorId = damagedTarget.actorId,
			skill = skill,
			random = random,
		)
		val afterRecoil = applyPostDamageItemEffects(
			state = afterContactAbilities,
			actorId = actor.actorId,
			skill = skill,
			damageAmount = damage.amount,
		)
		val targetAfterPostDamage = afterRecoil.participant(damagedTarget.actorId) ?: damagedTarget
		val actorAfterPostDamage = afterRecoil.participant(actor.actorId) ?: actor
		val afterTargetFaint = afterRecoil.handleFaintAndResult(targetAfterPostDamage)
		if (afterTargetFaint.result != null) {
			return context.copy(state = afterTargetFaint)
		}
		return context.copy(state = afterTargetFaint.handleFaintAndResult(actorAfterPostDamage))
	}

	/**
	 * 处理火属性伤害命中后解除目标冰冻。
	 *
	 * 现代规则中，冰冻目标被火属性伤害技能命中会解冻。这里要求目标在该段伤害后仍可战斗，避免对已经倒下的
	 * 成员追加无意义的状态解除事件；特定“冰冻中也可使用并解除自身冰冻”的技能会通过后续技能标签接入。
	 */
	private fun clearFreezeAfterFireDamage(
		state: BattleState,
		damagedTarget: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState {
		if (
			skill.elementId != state.rules.fireElementId ||
			damagedTarget.majorStatus != BattleMajorStatus.FREEZE ||
			!damagedTarget.canBattle()
		) {
			return state
		}
		return state
			.replaceParticipant(damagedTarget.clearMajorStatus())
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = damagedTarget.actorId,
					status = BattleMajorStatus.FREEZE,
				),
			)
	}

	/**
	 * 根据技能目标范围计算本次行动会尝试影响的实际目标。
	 *
	 * 单体技能保留“目标席位”语义：若行动选择的成员已经替换，技能会打到同一方当前可战斗的上场成员。
	 * 范围技能会根据行动者所在方重新收集当前上场成员，不把已经倒下的成员计入目标集合。
	 */
	private fun targetsForSkill(
		state: BattleState,
		actorId: String,
		selectedTargetActorId: String,
		skill: BattleSkillSlot,
	): List<BattleParticipant> =
		when (skill.targetScope) {
			BattleSkillTargetScope.SELECTED_TARGET -> listOfNotNull(state.activeTargetFor(selectedTargetActorId))
				.filter { it.canBattle() }
			BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS -> state.sides
				.filter { it.participant(actorId) == null }
				.flatMap { it.activeParticipants() }
				.filter { it.canBattle() }
			BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS -> state.sides
				.flatMap { it.activeParticipants() }
				.filter { it.actorId != actorId && it.canBattle() }
		}

	/**
	 * 计算现代双打范围技能的目标倍率。
	 *
	 * 公开规则中，能同时命中多个目标的伤害技能在实际存在多个目标时使用 0.75 倍目标修正。
	 * 若范围内只剩一个可战斗目标，则不套用该修正。
	 */
	private fun targetDamageMultiplier(skill: BattleSkillSlot, targets: List<BattleParticipant>): Double =
		if (skill.targetScope.canAffectMultipleTargets && targets.size > 1) {
			0.75
		} else {
			1.0
		}

	/**
	 * 判断技能是否被场地规则阻挡。
	 *
	 * 现代精神场地会保护接地成员免受对手先制技能影响。该判断按目标逐个执行：范围技能中某个目标被阻挡时，
	 * 其它不满足条件的目标仍可继续结算。
	 */
	private fun skillBlockedByTerrain(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): Boolean =
		state.environment.terrain == BattleTerrain.PSYCHIC &&
			skill.priority > 0 &&
			target.grounded &&
			state.sideOf(actor.actorId)?.sideId != state.sideOf(target.actorId)?.sideId

	/**
	 * 判断粉末类技能是否被目标属性免疫。
	 *
	 * 现代规则中，草属性成员天然免疫粉末/孢子类技能。这里返回实际触发免疫的属性 ID，便于事件流记录；
	 * 如果规则快照缺少草属性 ID，则不猜测资料编号，也不启用该免疫。
	 */
	private fun powderBlockedElementId(state: BattleState, target: BattleParticipant, skill: BattleSkillSlot): Long? {
		val grassElementId = state.rules.grassElementId ?: return null
		return if (skill.powderBased && target.hasElement(grassElementId)) {
			grassElementId
		} else {
			null
		}
	}

	/**
	 * 决定本次技能使用的实际命中段数。
	 *
	 * 单段技能不消费随机数。现代 2..5 段技能使用公开规则中的非均匀分布：2 段和 3 段各 35%，4 段和 5 段各 15%。
	 * 其它自定义范围暂按均匀分布处理，便于未来接入固定 2 段或资料驱动特殊段数时仍有确定行为。
	 */
	private fun determineHitCount(skill: BattleSkillSlot, random: BattleRandom): Int {
		if (skill.minHits == skill.maxHits) {
			return skill.minHits
		}
		if (skill.minHits == 2 && skill.maxHits == 5) {
			val roll = random.nextInt(100, "multi-hit count for ${skill.skillId}")
			return when {
				roll < 35 -> 2
				roll < 70 -> 3
				roll < 85 -> 4
				else -> 5
			}
		}
		return skill.minHits + random.nextInt(skill.maxHits - skill.minHits + 1, "multi-hit count for ${skill.skillId}")
	}

	/**
	 * 决定锁招技能本次会持续的总回合数。
	 *
	 * 公开成熟实现会在首次成功使用时决定 2 或 3 回合等持续时间，并把当前回合计入总数。固定持续时间不消费
	 * 随机数，避免普通单回合技能或测试 fixture 因无意义随机消费破坏 replay 脚本。
	 */
	private fun determineLockMoveTotalTurns(skill: BattleSkillSlot, random: BattleRandom): Int {
		if (skill.lockMoveTurnsMin == skill.lockMoveTurnsMax) {
			return skill.lockMoveTurnsMin
		}
		return skill.lockMoveTurnsMin +
			random.nextInt(skill.lockMoveTurnsMax - skill.lockMoveTurnsMin + 1, "locked move duration for ${skill.skillId}")
	}

	/**
	 * 在技能成功执行后推进锁招状态。
	 *
	 * 首次成功使用锁招技能时，成员进入“未来回合必须继续使用同一技能”的状态；后续锁定回合成功执行时，
	 * 只递减未来剩余次数，不再次扣 PP。剩余次数耗尽后，如果技能声明会疲劳混乱，则立即给使用者附加混乱。
	 */
	private fun updateLockedMoveAfterSuccessfulUse(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return if (actor.lockedMoveTurnsRemaining > 0) state.replaceParticipant(actor.clearLockedMove()) else state
		}
		if (actor.lockedMoveTurnsRemaining > 0) {
			return advanceLockedMoveAfterSuccessfulUse(state, actor, skill, random)
		}
		if (skill.lockMoveTurnsMax <= 1) {
			return state
		}
		val totalTurns = determineLockMoveTotalTurns(skill, random)
		val turnsRemainingAfterCurrent = totalTurns - 1
		if (turnsRemainingAfterCurrent <= 0) {
			return state
		}
		return state
			.replaceParticipant(
				actor.startLockedMove(
					skillId = skill.skillId,
					targetActorId = targetActorId,
					turnsRemainingAfterCurrent = turnsRemainingAfterCurrent,
					confusesOnEnd = skill.confusesUserAfterLock,
				),
			)
			.appendEvent(
				BattleEvent.LockedMoveStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					totalTurns = totalTurns,
					turnsRemainingAfterCurrent = turnsRemainingAfterCurrent,
				),
			)
	}

	/**
	 * 消耗一次已经存在的锁招强制行动。
	 *
	 * 锁招的剩余次数只表示未来还会被强制行动几次，因此每次后续成功发动后递减。若递减后仍大于 0，只记录
	 * `LockedMoveAdvanced`；若正好结束，则清除锁招并按技能配置处理疲劳混乱。
	 */
	private fun advanceLockedMoveAfterSuccessfulUse(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val updated = actor.consumeLockedMoveTurn()
		val afterConsume = state.replaceParticipant(updated)
		return if (actor.lockedMoveTurnsRemaining > 1) {
			afterConsume.appendEvent(
				BattleEvent.LockedMoveAdvanced(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					turnsRemainingAfterCurrent = updated.lockedMoveTurnsRemaining,
				),
			)
		} else {
			val shouldConfuse = actor.lockedMoveConfusesOnEnd && updated.canBattle()
			val ended = afterConsume.appendEvent(
				BattleEvent.LockedMoveEnded(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					confusesUser = shouldConfuse,
				),
			)
			if (shouldConfuse) {
				applyVolatileStatusEffect(
					state = ended,
					actorId = actor.actorId,
					recipient = updated,
					status = BattleVolatileStatus.CONFUSION,
					random = random,
					randomReason = "locked move confusion duration for ${skill.skillId}",
				)
			} else {
				ended
			}
		}
	}

	/**
	 * 处理中断锁招的失败分支。
	 *
	 * 现代规则中，锁招后续回合如果被行动前状态阻止、找不到目标、未命中、被保护/场地/属性免疫挡下，
	 * 会退出锁招。若中断正好发生在本应结束并疲劳的最后一次强制行动上，仍按公开说明附加疲劳混乱。
	 */
	private fun endLockedMoveAfterDisruption(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (actor.lockedMoveTurnsRemaining <= 0) {
			return state
		}
		val shouldConfuse = actor.lockedMoveConfusesOnEnd && actor.lockedMoveTurnsRemaining == 1 && actor.canBattle()
		val cleared = actor.clearLockedMove()
		val ended = state
			.replaceParticipant(cleared)
			.appendEvent(
				BattleEvent.LockedMoveEnded(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					confusesUser = shouldConfuse,
				),
			)
		return if (shouldConfuse) {
			applyVolatileStatusEffect(
				state = ended,
				actorId = actor.actorId,
				recipient = cleared,
				status = BattleVolatileStatus.CONFUSION,
				random = random,
				randomReason = "locked move confusion duration for ${skill.skillId}",
			)
		} else {
			ended
		}
	}

	/**
	 * 处理命中判定。
	 *
	 * 空命中表示必中；否则先应用攻击方命中阶级和目标闪避阶级，再消费一个 1 到 100 的随机掷点。
	 * 若修正后命中率已经达到或超过 100，则直接命中且不消费随机数。天气必中、无防守和蓄力中目标等
	 * 例外规则会在这里之前或这里内部追加结构化 modifier。
	 */
	private fun accuracyCheck(
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): AccuracyCheck {
		val accuracy = skill.accuracy ?: return AccuracyCheck(hit = true, roll = null)
		val modifiedAccuracy = floor(
			accuracy *
				statStageModifiers.accuracyMultiplier(actor.statStage(BattleStat.ACCURACY)) /
				statStageModifiers.accuracyMultiplier(target.statStage(BattleStat.EVASION)),
		).toInt().coerceAtLeast(1)
		if (modifiedAccuracy >= 100) {
			return AccuracyCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(100, "accuracy for ${skill.skillId}") + 1
		return AccuracyCheck(hit = roll <= modifiedAccuracy, roll = roll)
	}

	/**
	 * 结算击中要害概率。
	 *
	 * 现代规则下，普通等级概率为 1/24，+1 为 1/8，+2 为 1/2，+3 及以上视为必定击中要害。
	 * 必定要害不消费随机数；其它等级消费 `[0, denominator)`，掷到 0 表示成功。
	 */
	private fun criticalHitCheck(skill: BattleSkillSlot, random: BattleRandom): CriticalHitCheck {
		val denominator = when (skill.criticalHitStage.coerceAtMost(3)) {
			0 -> 24
			1 -> 8
			2 -> 2
			else -> 1
		}
		if (denominator == 1) {
			return CriticalHitCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(denominator, "critical hit for ${skill.skillId}")
		return CriticalHitCheck(hit = roll == 0, roll = roll)
	}

	/**
	 * 结算保护类行动的连续使用成功率。
	 *
	 * 第一次保护必定成功；如果上一回合已经成功保护过，则下一次按 `1 / 3^chain` 掷点，
	 * 例如第二次 1/3、第三次 1/9。该函数只负责概率，不消耗 PP，也不修改战斗状态。
	 */
	private fun protectionSucceeds(actor: BattleParticipant, skill: BattleSkillSlot, random: BattleRandom): Boolean {
		val denominator = protectionChanceDenominator(actor.protectionChain)
		if (denominator == 1) {
			return true
		}
		return random.nextInt(denominator, "protection chance for ${skill.skillId}") == 0
	}

	/**
	 * 根据已经连续成功保护的次数计算下一次保护成功率分母。
	 *
	 * 分母按 3 的幂增长，并夹在 Int 范围内，避免极端测试 fixture 构造出不可表示的概率。
	 */
	private fun protectionChanceDenominator(chain: Int): Int {
		var denominator = 1
		repeat(chain) {
			denominator = (denominator * 3L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
		}
		return denominator
	}

	/**
	 * 应用技能命中后的结构化附加效果。
	 *
	 * 第一批只处理主要异常状态和能力阶级变化。效果按技能槽中的顺序结算；概率小于 100 的效果会消费随机数。
	 * 若目标已经倒下、已有主要异常状态或阶级变化被上下限夹住，则保持状态不变并跳过事件。
	 */
	private fun applySkillEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val afterStatuses = skill.statusApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "status chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, application.target) ?: return@fold current
				if (!recipient.canBattle() || recipient.majorStatus != null) {
					current
				} else {
					applyMajorStatusEffect(
						state = current,
						actorId = actorId,
						recipient = recipient,
						status = application.status,
						random = random,
						randomReason = "sleep duration for ${skill.skillId}",
					)
				}
			}
		}
		val afterVolatileStatuses = skill.volatileStatusApplications.fold(afterStatuses) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "volatile status chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, application.target) ?: return@fold current
				if (!recipient.canBattle()) {
					current
				} else {
					applyVolatileStatusEffect(
						state = current,
						actorId = actorId,
						recipient = recipient,
						status = application.status,
						random = random,
						randomReason = "confusion duration for ${skill.skillId}",
					)
				}
			}
		}
		return skill.statStageEffects.fold(afterVolatileStatuses) { current, effect ->
			if (!chanceSucceeds(effect.chancePercent, random, "stat stage chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, effect.target) ?: return@fold current
				val beforeStage = recipient.statStage(effect.stat)
				val updated = recipient.changeStatStage(effect.stat, effect.stageDelta)
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
								targetActorId = recipient.actorId,
								stat = effect.stat,
								delta = afterStage - beforeStage,
								currentStage = afterStage,
							),
						)
				}
			}
		}
	}

	/**
	 * 处理行动前可能阻止技能的状态。
	 *
	 * 顺序参考公开成熟实现中的行动前钩子优先级：睡眠和冰冻最早，畏缩随后处理，混乱早于麻痹处理。
	 * 睡眠和畏缩必定阻止本次技能行动且不消耗 PP；冰冻先尝试自然解冻，未解冻才阻止行动；混乱会先递减
	 * 内部计数，若计数归零则解除并继续行动，否则按现代 33% 自伤概率判定。只有自伤分支会阻止本次技能行动；
	 * 麻痹最后按 25% 概率阻止行动。
	 */
	private fun resolveBeforeMoveEffects(
		context: TurnContext,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BeforeMoveResult {
		if (actor.majorStatus == BattleMajorStatus.SLEEP) {
			return BeforeMoveResult(
				context = context.copy(state = consumeSleepBlockedAction(context.state, actor)),
				blocked = true,
			)
		}
		if (actor.majorStatus == BattleMajorStatus.FREEZE) {
			return resolveFreezeBeforeMove(context, actor, skill, random)
		}
		if (actor.flinched) {
			return BeforeMoveResult(
				context = context.copy(state = consumeFlinchBlockedAction(context.state, actor)),
				blocked = true,
			)
		}
		if (actor.confusionTurnsRemaining > 0) {
			return resolveConfusionBeforeMove(context, actor, random)
		}
		if (actor.majorStatus == BattleMajorStatus.PARALYSIS && paralysisBlocksMove(actor, random)) {
			return BeforeMoveResult(
				context = context.copy(state = consumeParalysisBlockedAction(context.state, actor)),
				blocked = true,
			)
		}
		return BeforeMoveResult(context = context, blocked = false)
	}

	/**
	 * 处理冰冻的行动前自然解冻和行动阻止。
	 *
	 * 现代规则中，被冰冻成员每次准备行动时都有 20% 概率自然解冻；解冻后继续执行原技能，不额外消耗 PP。
	 * 若本次没有解冻，则技能不会使用、PP 不会消耗。当前技能槽尚未建模“使用后解除自身冰冻”的标记，
	 * 因此火属性攻击或特定技能自解冻会在技能规则字段具备后接入。
	 */
	private fun resolveFreezeBeforeMove(
		context: TurnContext,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BeforeMoveResult {
		if (skill.thawsUserBeforeMove) {
			return BeforeMoveResult(
				context = context.copy(state = clearFrozenActorBeforeMove(context.state, actor)),
				blocked = false,
			)
		}
		if (chanceSucceeds(FREEZE_THAW_CHANCE_PERCENT, random, "freeze thaw chance for ${actor.actorId}")) {
			return BeforeMoveResult(
				context = context.copy(state = clearFrozenActorBeforeMove(context.state, actor)),
				blocked = false,
			)
		}
		return BeforeMoveResult(
			context = context.copy(state = consumeFreezeBlockedAction(context.state, actor)),
			blocked = true,
		)
	}

	/**
	 * 清除行动者自身冰冻状态。
	 *
	 * 该函数同时服务自然解冻和带有自解冻标签的技能。调用方负责决定是否需要先消费自然解冻随机数。
	 */
	private fun clearFrozenActorBeforeMove(state: BattleState, actor: BattleParticipant): BattleState =
		state
			.replaceParticipant(actor.clearMajorStatus())
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = BattleMajorStatus.FREEZE,
				),
			)

	/**
	 * 消耗睡眠对本次技能行动的阻止效果。
	 *
	 * 睡眠不消耗 PP、不产生技能使用事件，也不会继续进入命中或伤害流程。`BattleParticipant` 保存的是还会
	 * 被阻止行动几次，因此每次阻止后递减；递减到 0 时立即清除睡眠，并记录状态解除事件，供 replay 对齐。
	 */
	private fun consumeSleepBlockedAction(state: BattleState, actor: BattleParticipant): BattleState {
		val turnsRemainingBefore = actor.sleepTurnsRemaining
		val updated = actor.consumeSleepBlockedTurn()
		val blocked = state
			.replaceParticipant(updated)
			.appendEvent(
				BattleEvent.SkillPreventedBySleep(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					turnsRemainingBefore = turnsRemainingBefore,
				),
			)
		return if (updated.majorStatus == null) {
			blocked.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = BattleMajorStatus.SLEEP,
				),
			)
		} else {
			blocked
		}
	}

	/**
	 * 记录冰冻未解冻时对本次技能行动的阻止效果。
	 *
	 * 冰冻状态本身保持不变；后续行动还会再次尝试自然解冻。该函数只追加事件，不修改成员快照。
	 */
	private fun consumeFreezeBlockedAction(state: BattleState, actor: BattleParticipant): BattleState =
		state.appendEvent(
			BattleEvent.SkillPreventedByFreeze(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
			),
		)

	/**
	 * 消耗畏缩对本次技能行动的阻止效果。
	 *
	 * 畏缩是回合内临时状态，只会阻止一次行动。阻止发生后立即清掉成员上的标记；如果成员本回合没有尝试行动，
	 * 回合末清理会静默移除该标记，不产生解除事件。
	 */
	private fun consumeFlinchBlockedAction(state: BattleState, actor: BattleParticipant): BattleState =
		state
			.replaceParticipant(actor.consumeFlinch())
			.appendEvent(
				BattleEvent.SkillPreventedByVolatileStatus(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = BattleVolatileStatus.FLINCH,
				),
			)

	/**
	 * 消耗麻痹对本次技能行动的阻止效果。
	 *
	 * 麻痹本身不会因为阻止一次行动而解除，也不消耗技能 PP。这里不修改成员快照，只把“本次行动被麻痹挡下”
	 * 写入事件流，方便 replay 和公开规则 fixture 验证随机消费顺序。
	 */
	private fun consumeParalysisBlockedAction(state: BattleState, actor: BattleParticipant): BattleState =
		state.appendEvent(
			BattleEvent.SkillPreventedByParalysis(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
			),
		)

	/**
	 * 判断麻痹是否阻止本次行动。
	 *
	 * 现代主系列规则为每次行动前 25% 概率无法行动。该判定发生在睡眠、畏缩和混乱之后；因此如果前置状态
	 * 已经阻止了本次行动，就不会额外消费麻痹随机数。
	 */
	private fun paralysisBlocksMove(actor: BattleParticipant, random: BattleRandom): Boolean =
		chanceSucceeds(PARALYSIS_FULLY_PARALYZED_CHANCE_PERCENT, random, "paralysis chance for ${actor.actorId}")

	/**
	 * 处理混乱的行动前计数、解除、自伤和行动阻止。
	 *
	 * 混乱保存的是公开实现中的内部计数，而不是“还会自伤判定几次”。行动前先递减；
	 * 递减到 0 表示成员恢复清醒并继续执行原技能，不消费混乱概率随机数。递减后仍大于 0 时，
	 * 消费 1 次 33/100 自伤判定；若自伤，再消费 1 次 85..100 伤害浮动并跳过原技能行动。
	 */
	private fun resolveConfusionBeforeMove(context: TurnContext, actor: BattleParticipant, random: BattleRandom): BeforeMoveResult {
		val turnsRemainingBefore = actor.confusionTurnsRemaining
		val decremented = actor.decrementConfusionBeforeMove()
		val afterDecrement = context.state.replaceParticipant(decremented)
		if (decremented.confusionTurnsRemaining == 0) {
			return BeforeMoveResult(
				context = context.copy(
					state = afterDecrement.appendEvent(
						BattleEvent.VolatileStatusCleared(
							turnNumber = context.state.turnNumber,
							actorId = actor.actorId,
							status = BattleVolatileStatus.CONFUSION,
						),
					),
				),
				blocked = false,
			)
		}
		if (!chanceSucceeds(CONFUSION_SELF_DAMAGE_CHANCE_PERCENT, random, "confusion self-hit chance for ${actor.actorId}")) {
			return BeforeMoveResult(context = context.copy(state = afterDecrement), blocked = false)
		}
		val randomPercent = 85 + random.nextInt(16, "confusion damage random for ${actor.actorId}")
		val damage = confusionSelfDamage(decremented, randomPercent)
		val damaged = decremented.receiveDamage(damage)
		val afterDamage = afterDecrement
			.replaceParticipant(damaged)
			.appendEvent(
				BattleEvent.SkillPreventedByVolatileStatus(
					turnNumber = context.state.turnNumber,
					actorId = actor.actorId,
					status = BattleVolatileStatus.CONFUSION,
				),
			)
			.appendEvent(
				BattleEvent.ConfusionDamageApplied(
					turnNumber = context.state.turnNumber,
					actorId = actor.actorId,
					amount = damage,
					randomPercent = randomPercent,
					turnsRemainingBefore = turnsRemainingBefore,
				),
			)
			.handleFaintAndResult(damaged)
		return BeforeMoveResult(context = context.copy(state = afterDamage), blocked = true)
	}

	/**
	 * 计算混乱自伤。
	 *
	 * 公开成熟实现把混乱自伤当作特殊的 40 威力物理伤害：使用攻击和防御能力阶级，带 85..100 随机浮动，
	 * 但不套用属性一致、属性克制、要害、道具和多数特性修正。这里独立实现公式，避免伪造一个普通技能后
	 * 意外吃到普通伤害管线中的额外 modifier。
	 */
	private fun confusionSelfDamage(actor: BattleParticipant, randomPercent: Int): Int {
		val attack = statStageModifiers.modifiedBattleStat(actor.attack, actor.statStage(BattleStat.ATTACK))
		val defense = statStageModifiers.modifiedBattleStat(actor.defense, actor.statStage(BattleStat.DEFENSE))
		require(defense > 0) { "confusion defending stat must be positive" }
		val levelFactor = (2 * actor.level) / 5 + 2
		val baseDamage = (((levelFactor * CONFUSION_BASE_POWER * attack) / defense) / 50) + 2
		return floor(baseDamage * (randomPercent / 100.0)).toInt().coerceAtLeast(1)
	}

	/**
	 * 附加主要异常状态并处理现代属性免疫、接地场地免疫和状态私有计数。
	 *
	 * 睡眠附加成功时消费一个 `[0, 3)` 随机数并转成 1..3 次阻止行动；其它主要异常状态不消费持续回合随机数。
	 * 属性免疫和场地免疫都会在消费状态私有随机数前短路，确保“无法附加状态”不会污染随机脚本。
	 * 该函数不覆盖已有主要异常状态，调用方需要在进入前完成“已有状态”判断。
	 */
	private fun applyMajorStatusEffect(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		random: BattleRandom,
		randomReason: String,
	): BattleState {
		val blockedReason = blockedMajorStatusReason(state, recipient, status)
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.StatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = blockedReason,
				),
			)
		}
		val sleepTurnsRemaining = if (status == BattleMajorStatus.SLEEP) {
			random.nextInt(3, randomReason) + 1
		} else {
			0
		}
		return state
			.replaceParticipant(recipient.applyMajorStatus(status, sleepTurnsRemaining))
			.appendEvent(
				BattleEvent.StatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
				),
			)
	}

	/**
	 * 判断主要异常状态是否会在附加前被稳定免疫规则阻止。
	 *
	 * 顺序选择先属性、后场地：如果目标自身属性已经免疫该状态，就不再把阻止原因归给场地，便于 fixture
	 * 明确定位是个体免疫还是全场效果。特性、道具和技能护盾等更细的免疫来源会以新的 reason 扩展。
	 */
	private fun blockedMajorStatusReason(
		state: BattleState,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	): BattleStatusBlockReason? =
		when {
			statusBlockedByElement(state.rules, recipient, status) -> BattleStatusBlockReason.ELEMENT
			statusBlockedByTerrain(state, recipient, status) -> BattleStatusBlockReason.TERRAIN
			else -> null
		}

	/**
	 * 判断目标属性是否天然免疫指定主要异常状态。
	 *
	 * 当前覆盖现代主系列最稳定的类型免疫：火属性免疫灼伤，电属性免疫麻痹，毒/钢属性免疫中毒和剧毒，
	 * 冰属性免疫冰冻。睡眠没有通用属性免疫，因此返回 false。
	 */
	private fun statusBlockedByElement(
		rules: BattleRuleSnapshot,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	): Boolean =
		when (status) {
			BattleMajorStatus.BURN -> recipient.hasElement(rules.fireElementId)
			BattleMajorStatus.PARALYSIS -> recipient.hasElement(rules.electricElementId)
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON -> recipient.hasElement(rules.poisonElementId) || recipient.hasElement(rules.steelElementId)
			BattleMajorStatus.FREEZE -> recipient.hasElement(rules.iceElementId)
			BattleMajorStatus.SLEEP -> false
		}

	/**
	 * 判断当前场地是否阻止目标获得主要异常状态。
	 *
	 * 现代场地免疫只影响当前上场且接地的成员。电气场地阻止睡眠；薄雾场地阻止所有主要异常状态。
	 * 由于成员是否接地已经显式进入运行态，飞行、漂浮、携带道具等来源可以在进入引擎前折算到该布尔值。
	 */
	private fun statusBlockedByTerrain(
		state: BattleState,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	): Boolean {
		if (!state.isActive(recipient.actorId) || !recipient.grounded) {
			return false
		}
		return when (state.environment.terrain) {
			BattleTerrain.ELECTRIC -> status == BattleMajorStatus.SLEEP
			BattleTerrain.MISTY -> true
			else -> false
		}
	}

	/**
	 * 判断成员是否具有指定属性。
	 */
	private fun BattleParticipant.hasElement(elementId: Long?): Boolean =
		elementId != null && elementId in elementIds

	/**
	 * 判断成员是否天然免疫沙暴回合末伤害。
	 */
	private fun BattleParticipant.immuneToSandstorm(rules: BattleRuleSnapshot): Boolean =
		hasElement(rules.rockElementId) || hasElement(rules.groundElementId) || hasElement(rules.steelElementId)

	/**
	 * 附加临时状态并处理状态私有计数。
	 *
	 * 畏缩只标记本回合行动前阻止；混乱成功时消费一个 `[0, 4)` 随机数并转成 2..5 的内部计数。
	 * 若目标已经处于同一种混乱状态，成员快照不会变化，事件也不会重复追加。
	 */
	private fun applyVolatileStatusEffect(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
		random: BattleRandom,
		randomReason: String,
	): BattleState {
		if (status == BattleVolatileStatus.CONFUSION && recipient.confusionTurnsRemaining > 0) {
			return state
		}
		val confusionTurnsRemaining = if (status == BattleVolatileStatus.CONFUSION) {
			random.nextInt(4, randomReason) + 2
		} else {
			0
		}
		return state
			.replaceParticipant(recipient.applyVolatileStatus(status, confusionTurnsRemaining))
			.appendEvent(
				BattleEvent.VolatileStatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
				),
			)
	}

	/**
	 * 处理目标方“受到接触技能后影响攻击方”的特性效果。
	 *
	 * 第一批只实现概率附加主要异常状态。该 hook 在伤害事件之后、反伤和倒下判定之前执行，
	 * 可以覆盖接触后攻击方被麻痹等常见场景。
	 */
	private fun applyContactAbilityEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		if (!skill.makesContact) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		return target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ContactStatusOnAttacker>()
			.fold(state) { current, effect ->
				val actor = current.participant(actorId) ?: return@fold current
				if (!actor.canBattle() || actor.majorStatus != null) {
					current
				} else if (!chanceSucceeds(effect.chancePercent, random, "contact status for $targetActorId")) {
					current
				} else {
					applyMajorStatusEffect(
						state = current,
						actorId = targetActorId,
						recipient = actor,
						status = effect.status,
						random = random,
						randomReason = "contact sleep duration for $targetActorId",
					)
				}
			}
	}

	/**
	 * 处理造成伤害后的道具反伤。
	 *
	 * 伤害增幅本身由伤害计算器读取道具效果完成；这里根据最终伤害扣除攻击方 HP 并产生反伤事件。
	 */
	private fun applyPostDamageItemEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		return state.participant(actorId)
			?.itemEffects
			.orEmpty()
			.filterIsInstance<BattleItemEffect.DamageBoostWithRecoil>()
			.fold(state) { current, effect ->
				val actor = current.participant(actorId) ?: return@fold current
				if (!actor.canBattle()) {
					current
				} else {
					val recoil = (damageAmount / effect.recoilDenominator).coerceAtLeast(1)
					current
						.replaceParticipant(actor.receiveDamage(recoil))
						.appendEvent(
							BattleEvent.RecoilDamageApplied(
								turnNumber = current.turnNumber,
								actorId = actor.actorId,
								amount = recoil,
							),
						)
				}
			}
	}

	/**
	 * 清理本回合没有成功保护的成员连续保护计数。
	 *
	 * 保护递减概率只看连续成功的保护类行动。成员使用其它技能、替换、无法行动或没有提交行动时，都应在回合末
	 * 失去连续计数；只有 `successfulProtectionActorIds` 中的成员把计数保留到下一回合。
	 */
	private fun resetProtectionChains(state: BattleState, successfulProtectionActorIds: Set<String>): BattleState =
		state.sides
			.flatMap { it.participants }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (latest.actorId in successfulProtectionActorIds || latest.protectionChain == 0) {
					current
				} else {
					current.replaceParticipant(latest.resetProtectionChain())
				}
			}

	/**
	 * 清理回合结束时不会跨回合保留的临时状态。
	 *
	 * 目前只有畏缩需要该阶段兜底：如果目标已经行动后才被附加畏缩，它不会阻止任何行动，也不应该进入下一回合。
	 * 混乱有自己的持续计数和解除事件，因此不会在这里清理。
	 */
	private fun clearEndTurnVolatileStatuses(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				val cleared = latest.clearEndTurnVolatileStatuses()
				if (cleared == latest) current else current.replaceParticipant(cleared)
			}

	/**
	 * 结算回合末主要异常状态伤害。
	 *
	 * 当前实现覆盖灼伤、中毒和剧毒扣血。剧毒按成员运行态中的递增计数计算，并在成员存活时推进计数；
	 * 后续接入替身、魔法防守、治愈类效果时，会在这里之前追加状态伤害 modifier。
	 */
	private fun applyEndTurnEffects(state: BattleState): BattleState {
		val afterResidual = state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle()) {
					current
				} else {
					val residualDamage = residualDamage(latest) ?: return@fold current
					val damaged = latest.receiveDamage(residualDamage)
					val afterStatusCounter = if (damaged.canBattle()) {
						damaged.advanceBadPoisonCounter()
					} else {
						damaged
					}
					current
						.replaceParticipant(afterStatusCounter)
						.appendEvent(
							BattleEvent.ResidualDamageApplied(
								turnNumber = current.turnNumber,
								actorId = latest.actorId,
								status = requireNotNull(latest.majorStatus),
								amount = residualDamage,
							),
						)
						.handleFaintAndResult(afterStatusCounter)
				}
			}
		return if (afterResidual.result != null) {
			afterResidual
		} else {
			val afterWeather = applyEndTurnWeatherEffects(afterResidual)
			if (afterWeather.result != null) {
				afterWeather
			} else {
				applyEndTurnHealing(applyEndTurnTerrainEffects(afterWeather))
			}
		}
	}

	/**
	 * 处理回合末天气伤害。
	 *
	 * 当前只覆盖现代沙暴固定伤害：当前上场、仍可战斗，且不是岩/地面/钢属性的成员会受到最大 HP 的 1/16 伤害。
	 * 特性、道具、潜水/挖洞等免疫来源尚未进入成员运行态，后续会在这里扩展结构化判断。
	 */
	private fun applyEndTurnWeatherEffects(state: BattleState): BattleState {
		if (state.environment.weather != BattleWeather.SANDSTORM) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || latest.immuneToSandstorm(current.rules)) {
					current
				} else {
					val damage = (latest.maxHp / WEATHER_DAMAGE_DENOMINATOR).coerceAtLeast(1)
					val damaged = latest.receiveDamage(damage)
					current
						.replaceParticipant(damaged)
						.appendEvent(
							BattleEvent.WeatherDamageApplied(
								turnNumber = current.turnNumber,
								actorId = latest.actorId,
								weather = BattleWeather.SANDSTORM,
								amount = damage,
							),
						)
						.handleFaintAndResult(damaged)
				}
			}
	}

	/**
	 * 处理回合末场地回复。
	 *
	 * 第一批只实现青草场地的固定比例回复，并只作用于当前上场、仍可战斗且接地的成员。
	 * 飞行、漂浮、携带道具免疫地面场地等来源应在进入引擎前折算为成员的 `grounded=false`。
	 */
	private fun applyEndTurnTerrainEffects(state: BattleState): BattleState {
		if (state.environment.terrain != BattleTerrain.GRASSY) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || !latest.grounded || latest.currentHp == latest.maxHp) {
					current
				} else {
					val healAmount = (latest.maxHp / current.rules.grassyTerrainHealDenominator).coerceAtLeast(1)
						.coerceAtMost(latest.maxHp - latest.currentHp)
					current
						.replaceParticipant(latest.heal(healAmount))
						.appendEvent(
							BattleEvent.TerrainHealingApplied(
								turnNumber = current.turnNumber,
								actorId = latest.actorId,
								terrain = BattleTerrain.GRASSY,
								amount = healAmount,
							),
						)
				}
			}
	}

	/**
	 * 处理回合末携带道具回复。
	 *
	 * 第一批只实现当前上场成员的固定最大 HP 比例回复，不处理道具消耗、回复封锁或复杂场地顺序。
	 */
	private fun applyEndTurnHealing(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || latest.currentHp == latest.maxHp) {
					current
				} else {
					latest.itemEffects
						.filterIsInstance<BattleItemEffect.HeldEndTurnHeal>()
						.fold(current) { healingState, effect ->
							val currentParticipant = healingState.participant(latest.actorId) ?: return@fold healingState
							val healAmount = (currentParticipant.maxHp / effect.healDenominator).coerceAtLeast(1)
								.coerceAtMost(currentParticipant.maxHp - currentParticipant.currentHp)
							if (healAmount <= 0) {
								healingState
							} else {
								healingState
									.replaceParticipant(currentParticipant.heal(healAmount))
									.appendEvent(
										BattleEvent.HealingApplied(
											turnNumber = healingState.turnNumber,
											actorId = currentParticipant.actorId,
											amount = healAmount,
										),
									)
							}
						}
				}
			}

	/**
	 * 推进天气和场地的持续回合。
	 *
	 * 剩余回合为空表示该环境来自永久规则或 fixture 不关心持续时间，不会被这里修改。剩余回合为 1 时，
	 * 本回合末结束环境并产生结束事件；大于 1 时只递减计数，不产生额外事件，避免 replay 事件流过于嘈杂。
	 */
	private fun advanceEnvironmentDurations(state: BattleState): BattleState {
		val afterWeather = advanceWeatherDuration(state)
		return advanceTerrainDuration(afterWeather)
	}

	/**
	 * 推进天气持续回合并在耗尽时恢复无天气。
	 */
	private fun advanceWeatherDuration(state: BattleState): BattleState {
		val turnsRemaining = state.environment.weatherTurnsRemaining ?: return state
		if (state.environment.weather == BattleWeather.NONE) {
			return state.copy(environment = state.environment.copy(weatherTurnsRemaining = null))
		}
		return if (turnsRemaining <= 1) {
			state
				.copy(environment = state.environment.copy(weather = BattleWeather.NONE, weatherTurnsRemaining = null))
				.appendEvent(
					BattleEvent.WeatherEnded(
						turnNumber = state.turnNumber,
						weather = state.environment.weather,
					),
				)
		} else {
			state.copy(environment = state.environment.copy(weatherTurnsRemaining = turnsRemaining - 1))
		}
	}

	/**
	 * 推进场地持续回合并在耗尽时恢复无场地。
	 */
	private fun advanceTerrainDuration(state: BattleState): BattleState {
		val turnsRemaining = state.environment.terrainTurnsRemaining ?: return state
		if (state.environment.terrain == BattleTerrain.NONE) {
			return state.copy(environment = state.environment.copy(terrainTurnsRemaining = null))
		}
		return if (turnsRemaining <= 1) {
			state
				.copy(environment = state.environment.copy(terrain = BattleTerrain.NONE, terrainTurnsRemaining = null))
				.appendEvent(
					BattleEvent.TerrainEnded(
						turnNumber = state.turnNumber,
						terrain = state.environment.terrain,
					),
				)
		} else {
			state.copy(environment = state.environment.copy(terrainTurnsRemaining = turnsRemaining - 1))
		}
	}

	/**
	 * 计算主要异常状态在回合末造成的固定伤害。
	 */
	private fun residualDamage(participant: BattleParticipant): Int? =
		when (participant.majorStatus) {
			BattleMajorStatus.BURN -> (participant.maxHp / 16).coerceAtLeast(1)
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON -> (participant.maxHp * participant.badPoisonCounter.coerceAtLeast(1) / 16)
				.coerceAtLeast(1)
			else -> null
		}

	/**
	 * 计算行动排序使用的有效速度。
	 *
	 * 速度先应用能力阶级，再应用麻痹减半。天气、道具、特性和顺风等速度修正会在后续 modifier 管线中加入。
	 */
	private fun effectiveSpeed(participant: BattleParticipant): Int {
		val staged = statStageModifiers.modifiedBattleStat(
			participant.speed,
			participant.statStage(BattleStat.SPEED),
		)
		return if (participant.majorStatus == BattleMajorStatus.PARALYSIS) {
			(staged / 2).coerceAtLeast(1)
		} else {
			staged
		}
	}

	/**
	 * 根据效果目标枚举找到实际承受效果的成员。
	 */
	private fun BattleState.effectRecipient(actorId: String, targetActorId: String, target: BattleEffectTarget): BattleParticipant? =
		when (target) {
			BattleEffectTarget.USER -> participant(actorId)
			BattleEffectTarget.TARGET -> participant(targetActorId)
		}

	/**
	 * 结算百分比概率。
	 *
	 * 100% 不消费随机数，0% 永远失败；中间概率消费 1..100 掷点。
	 */
	private fun chanceSucceeds(chancePercent: Int, random: BattleRandom, reason: String): Boolean =
		when (chancePercent) {
			100 -> true
			0 -> false
			else -> random.nextInt(100, reason) + 1 <= chancePercent
		}

	/**
	 * 在伤害后追加倒下事件并判断胜负。
	 *
	 * 第一阶段只要某一方没有可战斗成员就立即结束战斗。后续替换请求、双打多成员同时倒下和裁定规则会扩展这里。
	 */
	private fun BattleState.handleFaintAndResult(target: BattleParticipant): BattleState {
		val withFaint = if (!target.canBattle()) {
			appendEvent(BattleEvent.ParticipantFainted(turnNumber, target.actorId))
		} else {
			this
		}
		val defeatedSides = withFaint.sides.filterNot { it.hasRemainingParticipant() }
		if (defeatedSides.isEmpty()) {
			return withFaint
		}
		val winner = withFaint.sides.first { it !in defeatedSides }
		val result = BattleResult(winningSideId = winner.sideId, reason = "all-opponents-fainted")
		return withFaint
			.copy(result = result)
			.appendEvent(
				BattleEvent.BattleEnded(
					turnNumber = turnNumber,
					winningSideId = result.winningSideId,
					reason = result.reason,
				),
			)
	}

	private data class SkillActionInput(
		val action: BattleAction.UseSkill,
		val lockedContinuation: Boolean,
	)

	private data class ActionPlan(
		val action: BattleAction.UseSkill,
		val actor: BattleParticipant,
		val skill: BattleSkillSlot,
		val lockedContinuation: Boolean,
	)

	private data class SwitchPlan(
		val action: BattleAction.SwitchParticipant,
		val actor: BattleParticipant,
	)

	private data class BeforeMoveResult(
		val context: TurnContext,
		val blocked: Boolean,
	)

	/**
	 * 单个回合技能阶段的临时上下文。
	 *
	 * `state` 是不断推进的不可变战斗状态；`protectedActorIds` 保存本回合已经成功建立保护屏障的成员；
	 * `successfulProtectionActorIds` 保存回合结束后应保留连续保护计数的成员。
	 * 这类回合内临时标记不进入 `BattleState`，避免被误认为跨回合持久状态，也方便后续扩展击掌奇袭、
	 * 守住连续成功率、先制阻挡等同样只在当前回合有效的规则。
	 */
	private data class TurnContext(
		val state: BattleState,
		val protectedActorIds: Set<String> = emptySet(),
		val successfulProtectionActorIds: Set<String> = emptySet(),
	)

	private data class AccuracyCheck(
		val hit: Boolean,
		val roll: Int?,
	)

	private data class CriticalHitCheck(
		val hit: Boolean,
		val roll: Int?,
	)

	private companion object {
		private const val CONFUSION_BASE_POWER = 40
		private const val CONFUSION_SELF_DAMAGE_CHANCE_PERCENT = 33
		private const val FREEZE_THAW_CHANCE_PERCENT = 20
		private const val PARALYSIS_FULLY_PARALYZED_CHANCE_PERCENT = 25
		private const val WEATHER_DAMAGE_DENOMINATOR = 16
	}
}
