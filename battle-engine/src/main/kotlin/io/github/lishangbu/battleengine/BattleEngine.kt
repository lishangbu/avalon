package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
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
	fun start(initialState: BattleInitialState): BattleState {
		val started = BattleState(
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
		return applyInitialSwitchInAbilityEffects(started)
	}

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
		val afterEndTurnVolatileStatusDurations = afterEndTurnVolatileStatuses.result?.let { afterEndTurnVolatileStatuses }
			?: advanceEndTurnVolatileStatusDurations(afterEndTurnVolatileStatuses)
		val afterEnvironmentDurations = afterEndTurnVolatileStatusDurations.result?.let { afterEndTurnVolatileStatusDurations }
			?: advanceEnvironmentDurations(afterEndTurnVolatileStatusDurations)
		val afterSideConditionDurations = afterEnvironmentDurations.result?.let { afterEnvironmentDurations }
			?: afterEnvironmentDurations.advanceSideConditionDurations()
		val afterTurnLimit = afterSideConditionDurations.result?.let { afterSideConditionDurations }
			?: applyTurnLimit(afterSideConditionDurations)
		return afterTurnLimit.result?.let { afterTurnLimit }
			?: afterTurnLimit.appendEvent(BattleEvent.TurnEnded(nextTurnNumber))
	}

	/**
	 * 按有效优先度、速度和同速随机数排序行动。
	 *
	 * 第一阶段只支持技能行动，所以优先度先来自技能槽，再叠加特性对变化类技能的修正。速度相同的行动会消费
	 * 随机数作为排序键；这不是最终双打同速规则的完整实现，但已经保证同一随机脚本下的 replay 稳定。
	 */
	private fun orderSkillActions(state: BattleState, actions: List<SkillActionInput>, random: BattleRandom): List<ActionPlan> {
		val plans = actions.map { input ->
			val action = input.action
			val actor = requireNotNull(state.participant(action.actorId)) { "actor not found: ${action.actorId}" }
			val skill = requireNotNull(actor.skillSlot(action.skillId)) { "skill not found: ${action.skillId}" }
			if (input.source == SkillActionSource.SUBMITTED) {
				require(!actor.choiceLockedToAnotherSkill(action.skillId)) {
					"choice locked to skill: ${actor.choiceLockedSkillId}"
				}
			}
			ActionPlan(
				action = action,
				actor = actor,
				skill = skill,
				source = input.source,
				priorityContext = skillPriorityContext(actor, skill),
			)
		}
		val speedComparator = speedComparator(state)
		val orderComparator = compareByDescending<Pair<Int, Int>> { it.first }
			.thenComparator { left, right -> speedComparator.compare(left.second, right.second) }
		return plans
			.groupBy { it.priorityContext.effectivePriority to effectiveSpeed(state, it.actor) }
			.toSortedMap(orderComparator)
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
	 * 锁招和蓄力成员会强制继续使用对应技能：如果玩家提交了其它技能选择，会被这里替换；如果玩家没有提交技能
	 * 行动，引擎也会自动生成一次强制行动。目标仍保存为首次选择的目标槽位，以复用现有目标重定向语义。
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
					source = SkillActionSource.LOCKED_CONTINUATION,
				)
			}
		val chargingActions = state.sides
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() && it.chargingTurnsRemaining > 0 }
			.mapNotNull { actor ->
				val skillId = actor.chargingSkillId ?: return@mapNotNull null
				val targetActorId = actor.chargingTargetActorId ?: return@mapNotNull null
				SkillActionInput(
					action = BattleAction.UseSkill(actor.actorId, skillId = skillId, targetActorId = targetActorId),
					source = SkillActionSource.CHARGED_RELEASE,
				)
			}
		val forcedActorIds = (lockedActions + chargingActions).map { it.action.actorId }.toSet()
		return submittedActions
			.filterNot { it.actorId in forcedActorIds }
			.map { SkillActionInput(it, source = SkillActionSource.SUBMITTED) } + lockedActions + chargingActions
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
			.groupBy { effectiveSpeed(state, it.actor) }
			.toSortedMap(speedComparator(state))
			.values
			.flatMap { sameSpeedPlans ->
				if (sameSpeedPlans.size == 1) {
					sameSpeedPlans
				} else {
					sameSpeedPlans.sortedBy { random.nextInt(1_000_000, "switch speed tie for ${it.actor.actorId}") }
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
				return@fold current.appendEvent(
					BattleEvent.SwitchPreventedByRecharge(
						turnNumber = current.turnNumber,
						actorId = actor.actorId,
					),
				)
			}
			if (actor.canBattle() && actor.chargingTurnsRemaining > 0) {
				val chargingSkillId = actor.chargingSkillId ?: return@fold current
				return@fold current.appendEvent(
					BattleEvent.SwitchPreventedByCharging(
						turnNumber = current.turnNumber,
						actorId = actor.actorId,
						skillId = chargingSkillId,
					),
				)
			}
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
			val withSwitchEvent = switched.appendEvent(
				BattleEvent.ParticipantSwitched(
					turnNumber = current.turnNumber,
					sideId = side.sideId,
					previousActorId = actor.actorId,
					nextActorId = plan.action.targetActorId,
					forced = !actor.canBattle(),
				),
			)
			val afterEntryHazards = applyEntryHazardsOnSwitchIn(
				state = withSwitchEvent,
				sideId = side.sideId,
				actorId = plan.action.targetActorId,
			)
			applySwitchInAbilityEffects(afterEntryHazards, plan.action.targetActorId)
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
			return when (plan.source) {
				SkillActionSource.LOCKED_CONTINUATION -> beforeMove.context.copy(
					state = endLockedMoveAfterDisruption(beforeMove.context.state, actor.actorId, plan.skill, random),
				)
				SkillActionSource.CHARGED_RELEASE -> beforeMove.context.copy(
					state = endChargingAfterDisruption(beforeMove.context.state, actor.actorId, plan.skill),
				)
				SkillActionSource.SUBMITTED -> beforeMove.context
			}
		}
		val actionState = beforeMove.context.state
		val readyActor = actionState.participant(action.actorId) ?: return beforeMove.context
		val skill = readyActor.skillSlot(action.skillId) ?: return beforeMove.context
		val targets = targetsForSkill(actionState, readyActor.actorId, action.targetActorId, skill)
		if (targets.isEmpty()) {
			return when (plan.source) {
				SkillActionSource.LOCKED_CONTINUATION -> beforeMove.context.copy(
					state = endLockedMoveAfterDisruption(actionState, readyActor.actorId, skill, random),
				)
				SkillActionSource.CHARGED_RELEASE -> beforeMove.context.copy(
					state = endChargingAfterDisruption(actionState, readyActor.actorId, skill),
				)
				SkillActionSource.SUBMITTED -> beforeMove.context
			}
		}
		if (plan.source == SkillActionSource.SUBMITTED) {
			require(skill.remainingPp > 0) { "skill has no remaining PP: ${skill.skillId}" }
		}

		val stateBeforeUse = if (plan.source == SkillActionSource.CHARGED_RELEASE) {
			releaseChargedSkill(actionState, readyActor, skill, targets.first().actorId)
		} else {
			actionState
		}
		val readyActorBeforePp = stateBeforeUse.participant(action.actorId) ?: return beforeMove.context
		val actorAfterPp = if (plan.source == SkillActionSource.SUBMITTED) {
			readyActorBeforePp.replaceSkillSlot(skill.consumePp())
		} else {
			readyActorBeforePp
		}
		val actorAfterActionSetup = if (plan.source == SkillActionSource.SUBMITTED) {
			actorAfterPp.lockChoiceSkillIfNeeded(skill.skillId)
		} else {
			actorAfterPp
		}.markSuccessfulSkill(skill.skillId)
		val usedState = stateBeforeUse
			.replaceParticipant(actorAfterActionSetup)
			.appendEvent(
				BattleEvent.SkillUsed(
					turnNumber = stateBeforeUse.turnNumber,
					actorId = readyActorBeforePp.actorId,
					targetActorId = targets.first().actorId,
					skillId = skill.skillId,
					skillName = skill.name,
				),
			)

		val stateAfterChargeDecision =
			if (skill.requiresChargeBeforeUse(actionState.environment.weather) && plan.source == SkillActionSource.SUBMITTED) {
				skipChargeWithHeldItem(usedState, readyActorBeforePp.actorId, skill)
					?: return beforeMove.context.copy(
						state = startSkillCharge(
							state = usedState,
							actorId = readyActorBeforePp.actorId,
							targetActorId = targets.first().actorId,
							skill = skill,
						),
					)
			} else {
				usedState
			}

		if (skill.protectsUser) {
			if (!protectionSucceeds(readyActor, skill, random)) {
				return beforeMove.context.copy(
					state = stateAfterChargeDecision
						.replaceParticipant(actorAfterActionSetup.resetProtectionChain())
						.appendEvent(
							BattleEvent.ProtectionFailed(
								turnNumber = actionState.turnNumber,
								actorId = readyActor.actorId,
								skillId = skill.skillId,
							),
						),
				)
			}
			val protectedActor = actorAfterActionSetup.markProtectionSuccess()
			return beforeMove.context.copy(
				state = stateAfterChargeDecision
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
		return targets.fold(beforeMove.context.copy(state = stateAfterChargeDecision)) { current, target ->
			if (current.state.result != null) {
				current
			} else {
				resolveSkillAgainstTarget(
					context = current,
					actorId = readyActor.actorId,
					targetActorId = target.actorId,
					skill = skill,
					priorityContext = plan.priorityContext,
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
		priorityContext: SkillPriorityContext,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext {
		val state = context.state
		val actor = state.participant(actorId) ?: return context
		val target = state.participant(targetActorId) ?: return context
		if (!actor.canBattle() || !target.canBattle()) {
			return context
		}
		val ignoresTargetAbilityEffects = skillIgnoresTargetAbilityEffects(state, actor, target)

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

		val darkPriorityBlockedElementId = darkPriorityBlockedElementId(state, actor, target, priorityContext)
		if (darkPriorityBlockedElementId != null) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillBlockedByElement(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							elementId = darkPriorityBlockedElementId,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		if (skillBlockedByTerrain(state, actor, target, priorityContext)) {
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

		val priorityBlocker = priorityMoveAbilityBlocker(state, actor, target, priorityContext)
		if (priorityBlocker != null) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillBlockedByAbility(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							abilityHolderActorId = priorityBlocker.actorId,
							abilityId = priorityBlocker.abilityId,
						),
					),
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		val soundBlocker = soundBasedSkillAbilityBlocker(state, actor, target, skill)
		if (soundBlocker != null) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = state.appendEvent(
						BattleEvent.SkillBlockedByAbility(
							turnNumber = state.turnNumber,
							actorId = actor.actorId,
							targetActorId = target.actorId,
							skillId = skill.skillId,
							abilityHolderActorId = soundBlocker.actorId,
							abilityId = soundBlocker.abilityId,
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

		val accuracyCheck = accuracyCheck(state, actor, target, skill, random)
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
		val absorbedByAbility = if (ignoresTargetAbilityEffects) {
			null
		} else {
			elementSkillAbsorbHeal(state, actor, target, skill)
				?: elementSkillAbsorbStatStage(state, actor, target, skill)
		}
		if (absorbedByAbility != null) {
			return context.copy(
				state = endLockedMoveAfterDisruption(
					state = absorbedByAbility,
					actorId = actor.actorId,
					skill = skill,
					random = random,
				),
			)
		}
		if (skill.damageClass == BattleDamageClass.STATUS) {
			val afterEffects = applySkillEffects(state, actor.actorId, target.actorId, skill, random)
			val afterHpEffects = applyStatusSkillHpEffects(afterEffects, actor.actorId, skill)
			val afterEnvironmentEffects = applySkillEnvironmentEffects(afterHpEffects, actor.actorId, skill)
			return context.copy(
				state = updateLockedMoveAfterSuccessfulUse(
					state = afterEnvironmentEffects,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skill = skill,
					random = random,
				),
			)
		}

		val effectiveness = state.rules.elementChart.multiplier(skill.effectiveElementId(state.environment.weather), target.elementIds)
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

		val directDamageAttempt = directDamageAttempt(skill, actor, target)
		if (directDamageAttempt != null) {
			if (directDamageAttempt is DirectDamageAttempt.Failed) {
				return context.copy(
					state = endLockedMoveAfterDisruption(
						state = state.appendEvent(
							BattleEvent.SkillFailed(
								turnNumber = state.turnNumber,
								actorId = actor.actorId,
								targetActorId = target.actorId,
								skillId = skill.skillId,
								reason = directDamageAttempt.reason,
							),
						),
						actorId = actor.actorId,
						skill = skill,
						random = random,
					),
				)
			}
			val directDamageHit = directDamageAttempt as DirectDamageAttempt.Hit
			val damageEventStartIndex = state.events.size
			val afterDirectDamage = resolveDirectDamageHit(
				context = context,
				actorId = actor.actorId,
				targetActorId = target.actorId,
				skill = skill,
				damageAmount = directDamageHit.amount,
				faintActorAfterHit = directDamageHit.faintActorAfterHit,
				targetMultiplier = targetMultiplier,
				effectiveness = effectiveness,
				random = random,
			)
			val moveDamageAmount = damageDealtByMove(
				state = afterDirectDamage.state,
				eventStartIndex = damageEventStartIndex,
				actorId = actor.actorId,
				skillId = skill.skillId,
			)
			if (afterDirectDamage.state.result != null) {
				return afterDirectDamage.copy(
					state = applyPostMoveDamageDealtHealingItem(
						state = afterDirectDamage.state,
						actorId = actor.actorId,
						skill = skill,
						damageAmount = moveDamageAmount,
					),
				)
			}
			val latestTarget = afterDirectDamage.state.participant(target.actorId) ?: target
			val afterEffects = applySkillEffects(afterDirectDamage.state, actor.actorId, latestTarget.actorId, skill, random)
			val afterPostMoveItemEffects = applyPostMoveDamageDealtHealingItem(
				state = afterEffects,
				actorId = actor.actorId,
				skill = skill,
				damageAmount = moveDamageAmount,
			)
			return afterDirectDamage.copy(
				state = updateLockedMoveAfterSuccessfulUse(
					state = afterPostMoveItemEffects,
					actorId = actor.actorId,
					targetActorId = latestTarget.actorId,
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
		val damageEventStartIndex = stateWithHitCount.events.size
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
		val moveDamageAmount = damageDealtByMove(
			state = afterHits.state,
			eventStartIndex = damageEventStartIndex,
			actorId = actor.actorId,
			skillId = skill.skillId,
		)
		if (afterHits.state.result != null) {
			return afterHits.copy(
				state = applyPostMoveDamageDealtHealingItem(
					state = afterHits.state,
					actorId = actor.actorId,
					skill = skill,
					damageAmount = moveDamageAmount,
				),
			)
		}
		val latestTarget = afterHits.state.participant(target.actorId) ?: target
		val afterEffects = applySkillEffects(afterHits.state, actor.actorId, latestTarget.actorId, skill, random)
		val afterPostMoveItemEffects = applyPostMoveDamageDealtHealingItem(
			state = afterEffects,
			actorId = actor.actorId,
			skill = skill,
			damageAmount = moveDamageAmount,
		)
		return afterHits.copy(
			state = updateLockedMoveAfterSuccessfulUse(
				state = afterPostMoveItemEffects,
				actorId = actor.actorId,
				targetActorId = latestTarget.actorId,
				skill = skill,
				random = random,
			),
		)
	}

	/**
	 * 汇总本次技能动作实际造成的 HP 损失。
	 *
	 * 调用方在多段命中开始前记录事件下标，本函数只扫描该下标之后由同一使用者、同一技能产生的普通伤害和替身
	 * 伤害事件。返回值是目标本体或替身实际扣掉的 HP，不包含命中免疫的 0 伤害、接触特性、反伤、天气、异常
	 * 状态或入场陷阱等非本次技能直接造成的伤害。该口径用于贝壳之铃类道具，避免多段技能按每段分别回复。
	 */
	private fun damageDealtByMove(
		state: BattleState,
		eventStartIndex: Int,
		actorId: String,
		skillId: Long,
	): Int =
		state.events.asSequence()
			.drop(eventStartIndex)
			.sumOf { event ->
				when (event) {
					is BattleEvent.DamageApplied ->
						if (event.actorId == actorId && event.skillId == skillId) event.amount else 0
					is BattleEvent.SubstituteDamageApplied ->
						if (event.actorId == actorId && event.skillId == skillId) event.amount else 0
					else -> 0
				}
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
		val ignoresTargetAbilityEffects = skillIgnoresTargetAbilityEffects(state, actor, target)
		val criticalHit = criticalHitCheck.hit && (ignoresTargetAbilityEffects || !target.hasCriticalHitImmunity())
		val randomPercent = 85 + random.nextInt(16, "damage random for ${skill.skillId}")
		val sideDamageReductionMultiplier = sideDamageReductionMultiplier(
			state = state,
			target = target,
			skill = skill,
			criticalHit = criticalHit,
		)
		val substituteBlocksDamage = substituteBlocksOpponentEffect(state, actor.actorId, target.actorId, skill)
		val damage = damageCalculator.calculate(
			BattleDamageRequest(
				attacker = actor,
				defender = target,
				skill = skill,
				rules = state.rules,
				environment = state.environment,
				randomPercent = randomPercent,
				targetMultiplier = targetMultiplier,
				sideDamageReductionMultiplier = sideDamageReductionMultiplier,
				criticalHit = criticalHit,
				ignoreDefenderAbilityEffects = ignoresTargetAbilityEffects,
				allowDefenderItemDamageReduction = !substituteBlocksDamage,
			),
		)
		if (substituteBlocksDamage) {
			return resolveDamageAgainstSubstitute(
				context = context,
				state = state,
				actor = actor,
				target = target,
				skill = skill,
				damageAmount = damage.amount,
			)
		}

		val itemReduction = heldItemDamageReduction(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			skillElementId = skill.effectiveElementId(state.environment.weather),
			effectiveness = damage.effectiveness,
		)
		val stateAfterItemReduction = itemReduction?.let { state.replaceParticipant(it.target).appendEvent(it.event) } ?: state
		val targetAfterItemReduction = itemReduction?.target ?: target
		val survival = fatalDamageSurvival(
			state = stateAfterItemReduction,
			actor = actor,
			target = targetAfterItemReduction,
			skill = skill,
			damageAmount = damage.amount,
			ignoreTargetAbilityEffects = ignoresTargetAbilityEffects,
		)
		val damagedTarget = survival.target.receiveDamage(survival.damageAmount)
		val actualDamageAmount = survival.target.currentHp - damagedTarget.currentHp
		val damagedState = stateAfterItemReduction
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.DamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = actualDamageAmount,
					effectiveness = damage.effectiveness,
					targetMultiplier = damage.targetMultiplier,
					criticalHit = criticalHit,
				),
			)
			.let { current ->
				survival.event?.let(current::appendEvent) ?: current
			}
		val afterFireThaw = clearFreezeAfterFireDamage(damagedState, damagedTarget, skill)
		return finishPostDamageEffects(
			context = context,
			state = afterFireThaw,
			actorId = actor.actorId,
			targetActorId = damagedTarget.actorId,
			skill = skill,
			damageAmount = actualDamageAmount,
			targetCanFaint = true,
			allowTargetLowHpItem = true,
			allowContactAbilities = true,
			random = random,
		)
	}

	/**
	 * 计算不进入普通伤害公式的直接技能伤害。
	 *
	 * 固定伤害、比例伤害和 HP 派生伤害都在命中、保护、属性吸收和属性免疫之后结算，但它们的数值来源不同：
	 * 固定伤害读取技能规则给出的固定数值或使用者等级；比例伤害读取目标当前 HP 并按规则比例向下取整；
	 * HP 派生伤害读取双方当前 HP，并可能附带技能自身失败或使用者倒下。
	 * 若技能没有直接伤害模型，返回 null，调用方继续走普通伤害公式。
	 */
	private fun directDamageAttempt(
		skill: BattleSkillSlot,
		actor: BattleParticipant,
		target: BattleParticipant,
	): DirectDamageAttempt? =
		when (val fixedDamage = skill.fixedDamage) {
			is BattleFixedDamage.FixedAmount -> DirectDamageAttempt.Hit(fixedDamage.amount)
			BattleFixedDamage.UserLevel -> DirectDamageAttempt.Hit(actor.level)
			null -> when (val proportionalDamage = skill.proportionalDamage) {
				is BattleProportionalDamage.TargetCurrentHpFraction ->
					DirectDamageAttempt.Hit(
						fractionAmount(
							value = target.currentHp,
							numerator = proportionalDamage.numerator,
							denominator = proportionalDamage.denominator,
						).coerceAtLeast(proportionalDamage.minimumDamage),
					)
				null -> when (skill.hpDerivedDamage) {
					BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp -> {
						val amount = target.currentHp - actor.currentHp
						if (amount <= 0) {
							DirectDamageAttempt.Failed("target-hp-not-greater-than-user-hp")
						} else {
							DirectDamageAttempt.Hit(amount)
						}
					}
					BattleHpDerivedDamage.UserCurrentHpAndUserFaints ->
						DirectDamageAttempt.Hit(
							amount = actor.currentHp,
							faintActorAfterHit = true,
						)
					null -> null
				}
			}
		}

	/**
	 * 结算一段直接伤害技能。
	 *
	 * 调用点已经完成行动可用性、PP、保护、命中、属性吸收和属性免疫判定；本函数只负责把固定、比例或 HP 派生
	 * 伤害写入目标或替身。直接伤害不会进入普通伤害公式，因此不会消费击中要害随机数或伤害浮动随机数，也不会
	 * 读取威力、攻防、能力阶级、属性一致加成、天气、场地、道具和特性伤害倍率。
	 *
	 * 写入 HP 后仍复用 [finishPostDamageEffects]，让目标低体力道具、接触触发特性、使用者反伤、伤害后回复、
	 * 倒下判定和胜负判定保持同一条事件顺序。若目标有替身且该技能不能穿透替身，则直接伤害先扣替身 HP。
	 */
	private fun resolveDirectDamageHit(
		context: TurnContext,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		faintActorAfterHit: Boolean,
		targetMultiplier: Double,
		effectiveness: Double,
		random: BattleRandom,
	): TurnContext {
		val state = context.state
		val actor = state.participant(actorId) ?: return context
		val target = state.participant(targetActorId) ?: return context
		if (!actor.canBattle() || !target.canBattle()) {
			return context
		}
		val substituteBlocksDamage = substituteBlocksOpponentEffect(state, actor.actorId, target.actorId, skill)
		if (substituteBlocksDamage) {
			return resolveDamageAgainstSubstitute(
				context = context,
				state = state,
				actor = actor,
				target = target,
				skill = skill,
				damageAmount = damageAmount,
				faintActorAfterHit = faintActorAfterHit,
			)
		}

		val ignoresTargetAbilityEffects = skillIgnoresTargetAbilityEffects(state, actor, target)
		val survival = fatalDamageSurvival(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			damageAmount = damageAmount,
			ignoreTargetAbilityEffects = ignoresTargetAbilityEffects,
		)
		val damagedTarget = survival.target.receiveDamage(survival.damageAmount)
		val actualDamageAmount = survival.target.currentHp - damagedTarget.currentHp
		val damagedState = state
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.DamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = actualDamageAmount,
					effectiveness = effectiveness,
					targetMultiplier = targetMultiplier,
				),
			)
			.let { current ->
				survival.event?.let(current::appendEvent) ?: current
			}
		return finishPostDamageEffects(
			context = context,
			state = damagedState,
			actorId = actor.actorId,
			targetActorId = damagedTarget.actorId,
			skill = skill,
			damageAmount = actualDamageAmount,
			faintActorAfterHit = faintActorAfterHit,
			targetCanFaint = true,
			allowTargetLowHpItem = true,
			allowContactAbilities = true,
			random = random,
		)
	}

	/**
	 * 处理防守方一次性属性减伤携带道具。
	 *
	 * 该函数与伤害计算器使用同一个 [BattleItemEffect.ElementDamageReduction.matches] 条件，因此“是否减伤”和
	 * “是否消费道具”不会分叉。调用点已经排除了替身挡住本体的场景；如果未来增加穿透替身、紧张感或道具禁用等
	 * 规则，应在进入这里前把是否允许触发表达成明确的结构化状态，而不是在函数中读取技能名称或道具名称。
	 */
	private fun heldItemDamageReduction(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		skillElementId: Long,
		effectiveness: Double,
	): HeldItemDamageReduction? {
		val itemId = target.itemId ?: return null
		val effect = target.itemEffects
			.filterIsInstance<BattleItemEffect.ElementDamageReduction>()
			.firstOrNull { it.matches(skillElementId, effectiveness) }
			?: return null
		val updatedTarget = if (effect.consumesItem) target.consumeHeldItem() else target
		return HeldItemDamageReduction(
			target = updatedTarget,
			event = BattleEvent.DamageReducedByItem(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = target.actorId,
				skillId = skill.skillId,
				itemId = itemId,
				elementId = skillElementId,
				multiplier = effect.multiplier,
				consumed = effect.consumesItem,
			),
		)
	}

	/**
	 * 在直接伤害写入目标 HP 前，套用“满 HP 致命伤害保留 1 HP”的特性和道具规则。
	 *
	 * 这类规则必须发生在低体力道具、接触特性和倒下判定之前；否则一次本应保留 1 HP 的攻击会先把目标写成倒下。
	 * 特性优先于道具，避免同一成员同时拥有两种来源时错误消耗携带道具。若本次技能声明无视目标防守特性，只跳过
	 * 特性来源的保命，道具来源仍按普通携带道具规则结算。
	 */
	private fun fatalDamageSurvival(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		ignoreTargetAbilityEffects: Boolean,
	): FatalDamageSurvivalResult {
		if (
			damageAmount <= 0 ||
			!target.canBattle() ||
			target.currentHp != target.maxHp ||
			damageAmount < target.currentHp
		) {
			return FatalDamageSurvivalResult(target = target, damageAmount = damageAmount)
		}
		val abilityEffect = if (ignoreTargetAbilityEffects) {
			null
		} else {
			target.abilityEffects
				.filterIsInstance<BattleAbilityEffect.SurviveFatalDamageAtFullHp>()
				.firstOrNull()
		}
		if (abilityEffect != null) {
			return target.toFatalDamageSurvivalResult(
				state = state,
				actor = actor,
				skill = skill,
				damageAmount = damageAmount,
				remainingHp = abilityEffect.remainingHp,
				source = BattleFatalDamageSurvivalSource.ABILITY,
				sourceId = target.abilityId,
				consumed = false,
			)
		}
		val itemId = target.itemId
		val itemEffect = target.itemEffects
			.filterIsInstance<BattleItemEffect.SurviveFatalDamageAtFullHp>()
			.firstOrNull()
		if (itemId != null && itemEffect != null) {
			val updatedTarget = if (itemEffect.consumesItem) target.consumeHeldItem() else target
			return updatedTarget.toFatalDamageSurvivalResult(
				state = state,
				actor = actor,
				skill = skill,
				damageAmount = damageAmount,
				remainingHp = itemEffect.remainingHp,
				source = BattleFatalDamageSurvivalSource.ITEM,
				sourceId = itemId,
				consumed = itemEffect.consumesItem,
			)
		}
		return FatalDamageSurvivalResult(target = target, damageAmount = damageAmount)
	}

	private fun BattleParticipant.toFatalDamageSurvivalResult(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		remainingHp: Int,
		source: BattleFatalDamageSurvivalSource,
		sourceId: Long?,
		consumed: Boolean,
	): FatalDamageSurvivalResult {
		val adjustedDamage = (currentHp - remainingHp).coerceAtLeast(0)
		return FatalDamageSurvivalResult(
			target = this,
			damageAmount = adjustedDamage,
			event = BattleEvent.FatalDamageSurvived(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = actorId,
				skillId = skill.skillId,
				source = source,
				sourceId = sourceId,
				consumed = consumed,
				incomingDamage = damageAmount,
				preventedDamage = damageAmount - adjustedDamage,
			),
		)
	}

	/**
	 * 让目标替身吸收本段普通伤害。
	 *
	 * 替身受击时目标本体 HP 不变，也不会触发目标低体力道具、接触反制特性或目标倒下判定。造成的替身 HP 损失
	 * 仍作为本次实际伤害传给吸取回复、休整和攻击方道具反伤等“成功造成伤害后”的来源规则，符合公开实现中
	 * 吸取类技能可以从替身伤害中回复的行为。
	 */
	private fun resolveDamageAgainstSubstitute(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		faintActorAfterHit: Boolean = false,
	): TurnContext {
		val actualDamageAmount = damageAmount.coerceAtMost(target.substituteHp)
		val damagedTarget = target.damageSubstitute(actualDamageAmount)
		val damagedState = state
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.SubstituteDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = actualDamageAmount,
					substituteHpRemaining = damagedTarget.substituteHp,
				),
			)
			.let { current ->
				if (target.substituteHp > 0 && damagedTarget.substituteHp == 0) {
					current.appendEvent(
						BattleEvent.SubstituteBroken(
							turnNumber = state.turnNumber,
							actorId = target.actorId,
						),
					)
				} else {
					current
				}
			}
		return finishPostDamageEffects(
			context = context,
			state = damagedState,
			actorId = actor.actorId,
			targetActorId = damagedTarget.actorId,
			skill = skill,
			damageAmount = actualDamageAmount,
			faintActorAfterHit = faintActorAfterHit,
			targetCanFaint = false,
			allowTargetLowHpItem = false,
			allowContactAbilities = false,
			random = null,
		)
	}

	/**
	 * 收拢普通伤害和替身伤害共享的“造成实际伤害后”流程。
	 *
	 * 目标本体受伤时启用低体力道具、接触特性和倒下判定；替身受伤时关闭这些目标侧 hook，但仍保留攻击方技能
	 * HP 后效、休整和道具反伤，避免两条伤害路径出现重复实现。
	 */
	private fun finishPostDamageEffects(
		context: TurnContext,
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		faintActorAfterHit: Boolean = false,
		targetCanFaint: Boolean,
		allowTargetLowHpItem: Boolean,
		allowContactAbilities: Boolean,
		random: BattleRandom?,
	): TurnContext {
		val afterActorSelfSacrifice = if (faintActorAfterHit) {
			applySkillSelfSacrificeDamage(state, actorId, skill)
		} else {
			state
		}
		val afterSkillHpEffects = applyPostDamageSkillHpEffects(
			state = afterActorSelfSacrifice,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val afterSkillRecharge = applySkillRechargeAfterDamage(
			state = afterSkillHpEffects,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val afterTargetLowHpItem = if (allowTargetLowHpItem) {
			applyLowHpHealingItem(afterSkillRecharge, targetActorId)
		} else {
			afterSkillRecharge
		}
		val afterContactAbilities = if (allowContactAbilities && random != null) {
			applyContactAbilityEffects(
				state = afterTargetLowHpItem,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				random = random,
			)
		} else {
			afterTargetLowHpItem
		}
		val afterRecoil = applyPostDamageItemEffects(
			state = afterContactAbilities,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val faintCandidates = buildList {
			if (targetCanFaint) {
				afterRecoil.participant(targetActorId)?.let(::add)
			}
			afterRecoil.participant(actorId)?.let(::add)
		}
		return context.copy(state = afterRecoil.handleFaintsAndResult(faintCandidates))
	}

	/**
	 * 写入技能自身代价造成的使用者倒下。
	 *
	 * 该 helper 只服务“命中后使用者以当前 HP 作为代价倒下”的直接伤害规则。它不读取目标实际损失 HP，也不检查
	 * 反作用伤害免疫；后续倒下事件和胜负判定仍交给 [finishPostDamageEffects] 统一处理，避免这里重复判胜。
	 */
	private fun applySkillSelfSacrificeDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val amount = actor.currentHp
		return state
			.replaceParticipant(actor.receiveDamage(amount))
			.appendEvent(
				BattleEvent.SkillSelfSacrificeDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = amount,
				),
			)
	}

	/**
	 * 处理造成伤害后的技能 HP 效果。
	 *
	 * 当前支持两类直接绑定在技能上的 HP 后效：按本次实际伤害吸取回复使用者，以及按本次实际伤害让使用者
	 * 承受反作用伤害。该函数在伤害事件之后、目标低体力道具和接触类反制之前运行；这样事件流能直接表达
	 * “本段真实扣掉了多少 HP，随后技能自身基于该数值造成了什么”。如果本段没有造成实际伤害，或使用者已经
	 * 无法战斗，则不产生技能 HP 后效事件。
	 */
	private fun applyPostDamageSkillHpEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0) {
			return state
		}
		return skill.hpEffects
			.fold(state) { current, effect ->
				when (effect) {
					is BattleSkillHpEffect.DrainDamage -> applySkillDrainDamage(
						state = current,
						actorId = actorId,
						skill = skill,
						damageAmount = damageAmount,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.RecoilByDamageDealt -> applySkillRecoilDamage(
						state = current,
						actorId = actorId,
						skill = skill,
						damageAmount = damageAmount,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.CreateSubstitute,
					is BattleSkillHpEffect.SelfHealMaxHpFraction,
					is BattleSkillHpEffect.SelfHealMaxHpByWeather -> current
				}
			}
	}

	/**
	 * 处理成功造成实际伤害后的技能休整写入。
	 *
	 * 休整只由真正扣掉目标 HP 的技能触发；未命中、保护、属性无效、目标已经没有可扣除 HP 等情况都不会写入。
	 * 写入发生在技能自身 HP 后效之后、目标低体力道具和接触类反制之前；由于休整只影响未来行动，这个顺序不会
	 * 改变当前回合其它后效，只是让事件流更贴近“技能自身效果先完成”的阅读顺序。
	 */
	private fun applySkillRechargeAfterDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (!skill.rechargesAfterUse || damageAmount <= 0) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.rechargeTurnsRemaining > 0) {
			return state
		}
		val recharging = actor.startRecharge()
		return state
			.replaceParticipant(recharging)
			.appendEvent(
				BattleEvent.RechargeStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					turnsRemainingAfterCurrent = recharging.rechargeTurnsRemaining,
				),
			)
	}

	/**
	 * 按本次实际伤害回复使用者。
	 *
	 * 吸取回复使用向下取整的比例口径，并在最后按当前缺失 HP 夹取；它不负责处理污泥浆、回复封锁、吸取强化等
	 * 额外规则，那些会以新的明确效果或 hook 接入，避免这里出现难以复盘的隐式分支。
	 */
	private fun applySkillDrainDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		numerator: Int,
		denominator: Int,
	): BattleState {
			val actor = state.participant(actorId) ?: return state
			if (!actor.canBattle() || actor.currentHp == actor.maxHp || healingBlocked(actor)) {
				return state
			}
		val healAmount = fractionAmount(damageAmount, numerator, denominator)
			.coerceAtMost(actor.maxHp - actor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.heal(healAmount))
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 按本次实际伤害让使用者承受技能反作用伤害。
	 *
	 * 反作用伤害使用现代公开实现中“按目标实际损失 HP 计算，四舍五入到最近整数，最少 1 点”的规则。这里不会
	 * 重新读取公式伤害，也不会因为目标已经倒下而跳过；只要本段确实让目标损失 HP，使用者仍会承受对应自损。
	 */
	private fun applySkillRecoilDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasIndirectDamageImmunity() || actor.hasSkillRecoilDamageImmunity()) {
			return state
		}
		val recoilAmount = roundedHalfUpFractionAmount(damageAmount, numerator, denominator)
			.coerceAtMost(actor.currentHp)
		if (recoilAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.receiveDamage(recoilAmount))
			.appendEvent(
				BattleEvent.SkillRecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = recoilAmount,
					sourceDamageAmount = damageAmount,
				),
			)
	}

	/**
	 * 处理变化技能成功后的 HP 回复效果。
	 *
	 * 自我回复类技能不依赖目标 HP，也不进入普通伤害公式；只要技能经过目标、保护、命中等前置判定并成功，
	 * 就按使用者最大 HP 的固定比例回复。满 HP 时保持状态不变且不产生事件，后续若需要“技能失败”事件再单独建模。
	 */
	private fun applyStatusSkillHpEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.hpEffects
			.fold(state) { current, effect ->
				when (effect) {
					is BattleSkillHpEffect.SelfHealMaxHpFraction -> applySelfHealMaxHpFraction(
						state = current,
						actorId = actorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.SelfHealMaxHpByWeather -> {
						val fraction = effect.weatherFractions[current.environment.weather] ?: effect.defaultFraction
						applySelfHealMaxHpFraction(
							state = current,
							actorId = actorId,
							skill = skill,
							numerator = fraction.numerator,
							denominator = fraction.denominator,
						)
					}
					is BattleSkillHpEffect.CreateSubstitute -> applyCreateSubstitute(
						state = current,
						actorId = actorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					is BattleSkillHpEffect.DrainDamage -> current
					is BattleSkillHpEffect.RecoilByDamageDealt -> current
				}
			}

	/**
	 * 按最大 HP 比例回复使用者。
	 *
	 * 固定回复和天气变量回复最终都汇入这里，确保满 HP 跳过、缺失 HP 夹取和事件写入规则完全一致。
	 */
		private fun applySelfHealMaxHpFraction(
			state: BattleState,
			actorId: String,
			skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
	): BattleState {
			val actor = state.participant(actorId) ?: return state
			if (!actor.canBattle() || actor.currentHp == actor.maxHp || healingBlocked(actor)) {
				return state
			}
		val healAmount = fractionAmount(actor.maxHp, numerator, denominator)
			.coerceAtMost(actor.maxHp - actor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.heal(healAmount))
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
					),
				)
		}

		/**
		 * 判断成员当前是否被回复封锁阻止 HP 回复。
		 *
		 * 该函数只回答“能不能把 HP 往上加”，不决定某个技能是否能被宣告。主动回复技能和吸取类技能会在行动前
		 * 被 [healBlockPreventsSkill] 阻止；这里负责兜住特性、道具、场地、天气和其它后续加入的被动回复入口。
		 */
		private fun healingBlocked(participant: BattleParticipant): Boolean =
			participant.healBlockTurnsRemaining > 0

		/**
		 * 支付使用者最大 HP 的固定比例来建立替身。
	 *
	 * 现代替身要求使用者当前 HP 必须严格大于费用，且不能已经拥有替身。失败时技能已经完成使用和 PP 消耗，
	 * 但不产生额外事件；成功时本体扣除费用、替身获得同等 HP，并用专用事件记录该运行态事实。
	 */
	private fun applyCreateSubstitute(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasSubstitute()) {
			return state
		}
		val hpCost = fractionAmount(actor.maxHp, numerator, denominator)
			.coerceAtMost(actor.maxHp - 1)
		if (actor.currentHp <= hpCost) {
			return state
		}
		val substituted = actor.startSubstitute(hpCost)
		return state
			.replaceParticipant(substituted)
			.appendEvent(
				BattleEvent.SubstituteStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					hpCost = hpCost,
					substituteHp = substituted.substituteHp,
				),
			)
	}

	/**
	 * 计算比例型 HP 变化的基础数值。
	 *
	 * 现代规则中的常见比例回复以整数 HP 结算；这里统一向下取整，并对正比例至少保留 1 点，避免小额伤害的
	 * 吸取回复被截成 0。调用方负责再根据当前缺失 HP 夹取最终回复量。
	 */
	private fun fractionAmount(value: Int, numerator: Int, denominator: Int): Int {
		if (value <= 0) {
			return 0
		}
		return ((value.toLong() * numerator) / denominator)
			.coerceIn(1, Int.MAX_VALUE.toLong())
			.toInt()
	}

	/**
	 * 计算四舍五入到最近整数的比例型 HP 变化。
	 *
	 * 技能反作用伤害和吸取回复的取整口径不同，因此单独保留这个函数。正比例在造成实际伤害后至少为 1 点，
	 * 让低伤害命中仍能产生可观察的反作用伤害。
	 */
	private fun roundedHalfUpFractionAmount(value: Int, numerator: Int, denominator: Int): Int {
		if (value <= 0) {
			return 0
		}
		return ((value.toLong() * numerator * 2 + denominator) / (denominator * 2L))
			.coerceIn(1, Int.MAX_VALUE.toLong())
			.toInt()
	}

	/**
	 * 处理技能成功后的全场环境效果。
	 *
	 * 环境写入放在技能命中成功之后执行，只读取 [BattleSkillEnvironmentEffect] 这类结构化效果。当前支持普通天气技能；
	 * 若天气和剩余回合没有变化，则不追加事件，避免 replay 端把重复设置误读成真实环境变化。
	 * 携带道具带来的持续时间延长在具体天气/场地写入函数中处理，确保普通环境写入仍是一个可复盘的原子事实。
	 */
	private fun applySkillEnvironmentEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.environmentEffects.fold(state) { current, effect ->
			when (effect) {
				is BattleSkillEnvironmentEffect.SetWeather -> applySkillWeatherChange(
					state = current,
					actorId = actorId,
					effect = effect,
				)
				is BattleSkillEnvironmentEffect.SetTerrain -> applySkillTerrainChange(
					state = current,
					actorId = actorId,
					effect = effect,
				)
			}
		}

	/**
	 * 将技能天气效果写入战斗环境。
	 *
	 * 该函数与出场天气特性保持相同事件类型和去重语义；区别只在触发来源来自技能槽而不是成员特性。
	 */
	private fun applySkillWeatherChange(
		state: BattleState,
		actorId: String,
		effect: BattleSkillEnvironmentEffect.SetWeather,
	): BattleState {
		val turnsRemaining = extendedWeatherTurnsRemaining(state, actorId, effect)
		if (
			state.environment.weather == effect.weather &&
			state.environment.weatherTurnsRemaining == turnsRemaining
		) {
			return state
		}
		return state
			.copy(
				environment = state.environment.copy(
					weather = effect.weather,
					weatherTurnsRemaining = turnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.WeatherStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					weather = effect.weather,
					turnsRemaining = turnsRemaining,
				),
			)
	}

	/**
	 * 将技能场地效果写入战斗环境。
	 *
	 * 该函数与出场场地特性保持相同事件类型和去重语义；区别只在触发来源来自技能槽而不是成员特性。
	 */
	private fun applySkillTerrainChange(
		state: BattleState,
		actorId: String,
		effect: BattleSkillEnvironmentEffect.SetTerrain,
	): BattleState {
		val turnsRemaining = extendedTerrainTurnsRemaining(state, actorId, effect)
		if (
			state.environment.terrain == effect.terrain &&
			state.environment.terrainTurnsRemaining == turnsRemaining
		) {
			return state
		}
		return state
			.copy(
				environment = state.environment.copy(
					terrain = effect.terrain,
					terrainTurnsRemaining = turnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.TerrainStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					terrain = effect.terrain,
					turnsRemaining = turnsRemaining,
				),
			)
	}

	/**
	 * 计算技能设置天气时最终写入的持续回合。
	 *
	 * 普通天气技能会先声明自己的基础持续回合；携带者若拥有匹配天气的延长道具效果，则以道具声明的回合数覆盖。
	 * 基础持续回合为空表示永久天气或调用方不希望引擎管理持续时间，此时道具不会把它改成有限回合，避免改变 fixture
	 * 或后续强天气规则的语义。
	 */
	private fun extendedWeatherTurnsRemaining(
		state: BattleState,
		actorId: String,
		effect: BattleSkillEnvironmentEffect.SetWeather,
	): Int? =
		extendedWeatherTurnsRemaining(
			state = state,
			actorId = actorId,
			weather = effect.weather,
			baseTurnsRemaining = effect.turnsRemaining,
		)

	/**
	 * 计算指定来源成员建立天气时最终写入的持续回合。
	 *
	 * 该 helper 同时供技能环境效果和出场天气特性使用。调用方给出目标天气与基础持续回合；这里只负责读取来源成员
	 * 的携带道具效果并选择是否延长，保持所有“建立天气”的入口在同一条规则路径上。
	 */
	private fun extendedWeatherTurnsRemaining(
		state: BattleState,
		actorId: String,
		weather: BattleWeather,
		baseTurnsRemaining: Int?,
	): Int? {
		if (baseTurnsRemaining == null) {
			return null
		}
		val actor = state.participant(actorId) ?: return baseTurnsRemaining
		return actor.itemEffects
			.filterIsInstance<BattleItemEffect.WeatherDurationExtension>()
			.filter { weather in it.weathers }
			.maxOfOrNull { it.turnsRemaining }
			?: baseTurnsRemaining
	}

	/**
	 * 计算技能设置场地时最终写入的持续回合。
	 *
	 * 场地延长道具只影响携带者自己成功展开的场地，并按匹配场地选择最长的结构化持续回合。这样能同时支持
	 * 普通场地延长器和未来可能出现的自定义场地道具，而不会把道具 ID 或本地化名称写进引擎状态机。
	 */
	private fun extendedTerrainTurnsRemaining(
		state: BattleState,
		actorId: String,
		effect: BattleSkillEnvironmentEffect.SetTerrain,
	): Int? =
		extendedTerrainTurnsRemaining(
			state = state,
			actorId = actorId,
			terrain = effect.terrain,
			baseTurnsRemaining = effect.turnsRemaining,
		)

	/**
	 * 计算指定来源成员建立场地时最终写入的持续回合。
	 *
	 * 该 helper 同时供技能环境效果和出场场地特性使用。场地延长道具只影响来源成员自己建立的场地；若基础持续回合
	 * 为空，则保留永久/不管理持续回合的语义。
	 */
	private fun extendedTerrainTurnsRemaining(
		state: BattleState,
		actorId: String,
		terrain: BattleTerrain,
		baseTurnsRemaining: Int?,
	): Int? {
		if (baseTurnsRemaining == null) {
			return null
		}
		val actor = state.participant(actorId) ?: return baseTurnsRemaining
		return actor.itemEffects
			.filterIsInstance<BattleItemEffect.TerrainDurationExtension>()
			.filter { terrain in it.terrains }
			.maxOfOrNull { it.turnsRemaining }
			?: baseTurnsRemaining
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
			skill.effectiveElementId(state.environment.weather) != state.rules.fireElementId ||
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
	 * 计算防守方一侧伤害减免倍率。
	 *
	 * 现代主系列中，防守方屏障只对普通物理/特殊伤害生效，击中要害会忽略屏障。单打或目标侧只剩一名可战斗上场
	 * 成员时使用 0.5；双打目标侧存在多名可战斗上场成员时使用约 2/3。若同一侧同时有多个可覆盖屏障，本次伤害
	 * 只套用一次倍率，不叠加。
	 */
	private fun sideDamageReductionMultiplier(
		state: BattleState,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		criticalHit: Boolean,
	): Double {
		if (criticalHit || skill.damageClass == BattleDamageClass.STATUS) {
			return 1.0
		}
		val targetSide = state.sideOf(target.actorId) ?: return 1.0
		val reduction = targetSide.damageReductions.firstOrNull { it.appliesTo(skill.damageClass) } ?: return 1.0
		return reduction.damageReductionMultiplier(state, targetSide)
	}

	private fun BattleSideDamageReduction.damageReductionMultiplier(
		state: BattleState,
		targetSide: BattleSide,
	): Double {
		val targetSideActiveCount = targetSide.activeParticipants().count { it.canBattle() }
		return if (state.format.mode == BattleMode.DOUBLE && targetSideActiveCount > 1) {
			MULTI_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER
		} else {
			SINGLE_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER
		}
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
		priorityContext: SkillPriorityContext,
	): Boolean =
		state.environment.terrain == BattleTerrain.PSYCHIC &&
			priorityContext.effectivePriority > 0 &&
			target.grounded &&
			state.sideOf(actor.actorId)?.sideId != state.sideOf(target.actorId)?.sideId

	/**
	 * 返回阻止本次先制技能影响目标侧的特性拥有者。
	 *
	 * 这类特性保护拥有者所在一侧的当前上场成员；同侧成员主动对自己或伙伴使用先制技能时不触发。返回具体拥有者
	 * 是为了让事件流在双打中能区分“目标自身阻挡”和“伙伴特性保护”。
	 */
	private fun priorityMoveAbilityBlocker(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		priorityContext: SkillPriorityContext,
	): BattleParticipant? {
		if (priorityContext.effectivePriority <= 0) {
			return null
		}
		val actorSide = state.sideOf(actor.actorId) ?: return null
		val targetSide = state.sideOf(target.actorId) ?: return null
		if (actorSide.sideId == targetSide.sideId) {
			return null
		}
		if (skillIgnoresTargetAbilityEffects(state, actor, target)) {
			return null
		}
		return targetSide.activeParticipants()
			.firstOrNull { participant ->
				val effect = participant.abilityEffects
					.filterIsInstance<BattleAbilityEffect.PriorityMoveImmunityForSide>()
					.firstOrNull() ?: return@firstOrNull false
				participant.canBattle() && (participant.actorId == target.actorId || effect.protectsAllies)
			}
	}

	/**
	 * 返回阻止本次声音类技能影响目标的特性拥有者。
	 *
	 * 声音免疫是目标自身特性，不保护伙伴；只要技能来源不是目标本人，且技能槽声明为声音类，就会在命中、伤害
	 * 和附加效果之前阻止本次影响。若攻击方本次技能无视目标特性，则该免疫被跳过。
	 */
	private fun soundBasedSkillAbilityBlocker(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleParticipant? {
		if (!skill.soundBased || actor.actorId == target.actorId) {
			return null
		}
		if (skillIgnoresTargetAbilityEffects(state, actor, target)) {
			return null
		}
		return target.takeIf { participant ->
			participant.abilityEffects.any { it is BattleAbilityEffect.SoundBasedSkillImmunity }
		}
	}

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
	 * 判断由特性提升优先度的对手变化技能是否被目标恶属性免疫。
	 *
	 * 该免疫只绑定“特性把变化技能提升为先制”这一事实；普通基础先制度的变化技能、未被特性提升的技能以及
	 * 同侧辅助技能都不会触发。这里返回恶属性 ID，便于复用属性阻挡事件并保持事件流不依赖本地化属性名称。
	 */
	private fun darkPriorityBlockedElementId(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		priorityContext: SkillPriorityContext,
	): Long? {
		val darkElementId = state.rules.darkElementId ?: return null
		if (!priorityContext.statusPriorityBoostedByAbility || !priorityContext.darkElementTargetsImmune) {
			return null
		}
		val actorSide = state.sideOf(actor.actorId) ?: return null
		val targetSide = state.sideOf(target.actorId) ?: return null
		return if (actorSide.sideId != targetSide.sideId && target.hasElement(darkElementId)) {
			darkElementId
		} else {
			null
		}
	}

	/**
	 * 应用目标特性对指定属性技能的吸收回复。
	 *
	 * 这类特性发生在技能通过保护和命中判定之后，但早于普通伤害、状态和能力阶级效果写入。满 HP 目标仍然会
	 * 吸收并阻止技能继续结算，只是事件中的实际回复量为 0；这样 replay 能区分“未触发”和“触发但无需回复”。
	 */
	private fun elementSkillAbsorbHeal(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState? {
			val effect = target.abilityEffects
				.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbHeal>()
				.firstOrNull { it.elementId == skill.effectiveElementId(state.environment.weather) }
				?: return null
			if (healingBlocked(target)) {
				return state.appendEvent(
					BattleEvent.SkillAbsorbedByAbility(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						targetActorId = target.actorId,
						skillId = skill.skillId,
						abilityHolderActorId = target.actorId,
						abilityId = target.abilityId,
						elementId = effect.elementId,
						healAmount = 0,
					),
				)
			}
			val rawHealAmount = (target.maxHp / effect.healDenominator).coerceAtLeast(1)
		val healedTarget = target.heal(rawHealAmount)
		val actualHealAmount = healedTarget.currentHp - target.currentHp
		return state
			.replaceParticipant(healedTarget)
			.appendEvent(
				BattleEvent.SkillAbsorbedByAbility(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					abilityHolderActorId = target.actorId,
					abilityId = target.abilityId,
					elementId = effect.elementId,
					healAmount = actualHealAmount,
				),
			)
	}

	/**
	 * 应用目标特性对指定属性技能的吸收和自身能力阶级提升。
	 *
	 * 技能被吸收后不会继续进入普通伤害或附加效果流程。能力阶级提升独立夹取，目标已经达到上限时只记录吸收事件；
	 * 没有阶级变化事件表示“触发了吸收，但提阶被上限吃掉”，而不是技能继续生效。
	 */
	private fun elementSkillAbsorbStatStage(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState? {
		val effect = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbStatStage>()
			.firstOrNull { it.elementId == skill.effectiveElementId(state.environment.weather) }
			?: return null
		val absorbedState = state.appendEvent(
			BattleEvent.SkillAbsorbedByAbility(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = target.actorId,
				skillId = skill.skillId,
				abilityHolderActorId = target.actorId,
				abilityId = target.abilityId,
				elementId = effect.elementId,
				healAmount = 0,
			),
		)
		val beforeStage = target.statStage(effect.stat)
		val updatedTarget = target.changeStatStage(effect.stat, effect.stageDelta)
		val afterStage = updatedTarget.statStage(effect.stat)
		return if (beforeStage == afterStage) {
			absorbedState
		} else {
			absorbedState
				.replaceParticipant(updatedTarget)
				.appendEvent(
					BattleEvent.StatStageChanged(
						turnNumber = state.turnNumber,
						actorId = target.actorId,
						targetActorId = target.actorId,
						stat = effect.stat,
						delta = afterStage - beforeStage,
						currentStage = afterStage,
					),
				)
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
	 * 判断技能在当前天气下是否仍需要等待蓄力回合。
	 *
	 * `chargesBeforeUse` 只说明技能存在蓄力流程；是否能被天气跳过由运行时快照中的
	 * `chargeSkippedByWeathers` 精确声明，避免把晴天加速误套到所有蓄力技能上。
	 */
	private fun BattleSkillSlot.requiresChargeBeforeUse(weather: BattleWeather): Boolean =
		chargesBeforeUse && weather !in chargeSkippedByWeathers

	/**
	 * 尝试用携带道具跳过本次蓄力等待。
	 *
	 * 该函数只服务首次提交的蓄力技能：技能已经宣告并消耗 PP，但还没有进入命中和伤害流程。若行动者携带
	 * [BattleItemEffect.ChargeSkipOnce]，引擎会按道具效果声明消费道具、追加可复盘事件，并返回继续结算用的新状态。
	 * 若没有可用道具，返回 null，让调用方写入常规蓄力等待状态。
	 */
	private fun skipChargeWithHeldItem(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState? {
		val actor = state.participant(actorId) ?: return null
		val itemId = actor.itemId ?: return null
		val effect = actor.itemEffects.filterIsInstance<BattleItemEffect.ChargeSkipOnce>().firstOrNull() ?: return null
		val updatedActor = if (effect.consumesItem) actor.consumeHeldItem() else actor
		return state
			.replaceParticipant(updatedActor)
			.appendEvent(
				BattleEvent.SkillChargeSkippedByItem(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					itemId = itemId,
					consumed = effect.consumesItem,
				),
			)
	}

	/**
	 * 首次使用蓄力技能时写入等待释放状态。
	 *
	 * 这里发生在 PP 已消耗和 `SkillUsed` 已记录之后，但早于命中、保护、属性和伤害流程；也就是说第一回合只是
	 * 宣告并进入蓄力，真正的攻击会由下一次自动生成的技能行动释放。
	 */
	private fun startSkillCharge(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val charging = actor.startChargingSkill(skill.skillId, targetActorId)
		return state
			.replaceParticipant(charging)
			.appendEvent(
				BattleEvent.SkillChargeStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					turnsRemainingBeforeUse = charging.chargingTurnsRemaining,
				),
			)
	}

	/**
	 * 释放已蓄力技能。
	 *
	 * 释放只清理蓄力计数并追加事件；PP、命中、保护和伤害仍由后续统一技能流程处理。
	 */
	private fun releaseChargedSkill(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		targetActorId: String,
	): BattleState {
		if (actor.chargingTurnsRemaining <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.consumeChargingTurn())
			.appendEvent(
				BattleEvent.SkillChargeReleased(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
				),
			)
	}

	/**
	 * 处理蓄力释放前被行动前状态阻止的情况。
	 *
	 * 如果成员在第二回合因为睡眠、冰冻、麻痹、休整或临时状态无法行动，本次蓄力会结束，不会在后续回合继续
	 * 反复尝试释放同一个技能。
	 */
	private fun endChargingAfterDisruption(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (actor.chargingTurnsRemaining <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.clearChargingSkill())
			.appendEvent(
				BattleEvent.SkillChargeInterrupted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
				),
			)
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
	 * 空命中表示必中；技能可按当前天气覆盖命中率，例如雨天必中或晴天降为固定命中。否则先应用攻击方命中阶级
	 * 和目标闪避阶级，再消费一个 1 到 100 的随机掷点。若修正后命中率已经达到或超过 100，则直接命中且不消费随机数。
	 */
	private fun accuracyCheck(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): AccuracyCheck {
		val accuracy = effectiveAccuracy(state, skill) ?: return AccuracyCheck(hit = true, roll = null)
		val ignoresTargetAbilityEffects = skillIgnoresTargetAbilityEffects(state, actor, target)
		val actorAccuracyStage = if (!ignoresTargetAbilityEffects && target.ignoresOpponentAccuracyStatStages()) {
			0
		} else {
			actor.statStage(BattleStat.ACCURACY)
		}
		val targetEvasionStage = if (actor.ignoresOpponentAccuracyStatStages()) {
			0
		} else {
			target.statStage(BattleStat.EVASION)
		}
		val modifiedAccuracy = floor(
			accuracy *
				statStageModifiers.accuracyMultiplier(actorAccuracyStage) /
				statStageModifiers.accuracyMultiplier(targetEvasionStage),
		).toInt().coerceAtLeast(1)
		if (modifiedAccuracy >= 100) {
			return AccuracyCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(100, "accuracy for ${skill.skillId}") + 1
		return AccuracyCheck(hit = roll <= modifiedAccuracy, roll = roll)
	}

	/**
	 * 读取当前天气下的技能命中率。
	 *
	 * 资料层可以显式声明某天气下覆盖为固定命中率，或覆盖为 null 表示必中；没有覆盖时使用技能基础命中率。
	 */
	private fun effectiveAccuracy(state: BattleState, skill: BattleSkillSlot): Int? =
		if (skill.accuracyOverridesByWeather.containsKey(state.environment.weather)) {
			skill.accuracyOverridesByWeather[state.environment.weather]
		} else {
			skill.accuracy
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
	 * 效果按技能槽中的顺序结算；概率小于 100 的效果会消费随机数。若目标已经倒下、已有主要异常状态、
	 * 阶级变化被上下限夹住，或同类一侧场上屏障已经存在，则保持状态不变并跳过对应事件。
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
				if (!recipient.canBattle()) {
					current
				} else {
					applyMajorStatusEffect(
						state = current,
						actorId = actorId,
						recipient = recipient,
						status = application.status,
						random = random,
						randomReason = "sleep duration for ${skill.skillId}",
						skill = skill,
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
						skill = skill,
					)
				}
			}
		}
		val afterStatStageEffects = skill.statStageEffects.fold(afterVolatileStatuses) { current, effect ->
			if (!chanceSucceeds(effect.chancePercent, random, "stat stage chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, effect.target) ?: return@fold current
				if (substituteBlocksOpponentEffect(current, actorId, recipient.actorId, skill)) {
					return@fold current
				}
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
		val afterStatStageOperations = applyStatStageOperations(afterStatStageEffects, actorId, targetActorId, skill, random)
		val afterSideDamageReductions = skill.sideConditionApplications.fold(afterStatStageOperations) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side condition chance for ${skill.skillId}")) {
				current
			} else {
				applySideConditionEffect(current, actorId, targetActorId, skill, application)
			}
		}
		val afterSideSpeedModifiers = skill.sideSpeedModifierApplications.fold(afterSideDamageReductions) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side speed condition chance for ${skill.skillId}")) {
				current
			} else {
				applySideSpeedModifierEffect(current, actorId, targetActorId, skill, application)
			}
		}
		val afterSideEntryHazards = skill.sideEntryHazardApplications.fold(afterSideSpeedModifiers) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side entry hazard chance for ${skill.skillId}")) {
				current
			} else {
				applySideEntryHazardEffect(current, actorId, targetActorId, skill, application)
			}
		}
		val afterFieldSpeedOrder = skill.fieldSpeedOrderApplications.fold(afterSideEntryHazards) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "field speed order chance for ${skill.skillId}")) {
				current
			} else {
				applyFieldSpeedOrderEffect(current, actorId, skill, application)
			}
		}
		return applyForcedTargetSwitchEffect(afterFieldSpeedOrder, actorId, targetActorId, skill, random)
	}

	/**
	 * 应用技能命中后的能力阶级特殊操作。
	 *
	 * 普通阶级加减只需要在当前数值上叠加 delta；这里处理的是读取当前阶级后再写回结果的效果，例如清除之烟、
	 * 黑雾、自我暗示、力量互换、防守互换和颠倒。每条操作只处理一个能力项，资料层可为同一技能配置多条记录来
	 * 表达“所有能力项”或“攻击/防御组”。概率判定与普通附加效果一致，只在规则声明小于 100% 时消费随机数。
	 */
	private fun applyStatStageOperations(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.statStageOperations.fold(state) { current, operation ->
			if (!chanceSucceeds(operation.chancePercent, random, "stat stage operation chance for ${skill.skillId}")) {
				current
			} else {
				applyStatStageOperation(current, actorId, targetActorId, skill, operation)
			}
		}

	/**
	 * 分派单条能力阶级操作。
	 *
	 * 清除和取反可能作用于一个或多个目标；复制和交换必须有明确的单个来源与目标。若目标已经倒下、目标不存在，
	 * 或对手的替身阻挡了该技能效果，则该条操作保持状态不变且不产生事件。
	 */
	private fun applyStatStageOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState =
		when (operation.kind) {
			BattleStatStageOperationKind.CLEAR -> applyStatStageClearOperation(state, actorId, targetActorId, skill, operation)
			BattleStatStageOperationKind.COPY -> applyStatStageCopyOperation(state, actorId, targetActorId, skill, operation)
			BattleStatStageOperationKind.SWAP -> applyStatStageSwapOperation(state, actorId, targetActorId, skill, operation)
			BattleStatStageOperationKind.INVERT -> applyStatStageInvertOperation(state, actorId, targetActorId, skill, operation)
		}

	/**
	 * 将目标范围内成员的指定能力阶级清除为 0。
	 *
	 * 普通目标清除会尊重对手替身；全场清除表示黑雾类场地事实，遍历所有仍在场且可战斗的当前上场成员，不被
	 * 单个成员的替身拦截。只有实际非 0 阶级被清除时才记录事件，避免 replay 被无变化的 0->0 填满。
	 */
	private fun applyStatStageClearOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState =
		statStageOperationParticipants(state, actorId, targetActorId, operation.target).fold(state) { current, participant ->
			val latest = current.participant(participant.actorId) ?: return@fold current
			if (!latest.canBattle()) {
				return@fold current
			}
			if (
				operation.target != BattleStatStageOperationTarget.ALL_ACTIVE &&
				substituteBlocksOpponentEffect(current, actorId, latest.actorId, skill)
			) {
				return@fold current
			}
			val previous = latest.statStage(operation.stat)
			if (previous == 0) {
				current
			} else {
				current
					.replaceParticipant(latest.setStatStage(operation.stat, 0))
					.appendEvent(
						BattleEvent.StatStageCleared(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = latest.actorId,
							skillId = skill.skillId,
							stat = operation.stat,
							previousStage = previous,
						),
					)
			}
		}

	/**
	 * 将来源成员的指定能力阶级复制到目标成员。
	 *
	 * 自我暗示类技能复制的是当前运行态阶级，不读取基础能力值。来源和目标必须都是单个成员；目标已有相同阶级时
	 * 不产生事件。复制动作不改变来源，因此多条能力项操作按顺序执行时彼此独立。
	 */
	private fun applyStatStageCopyOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState {
		val sourceTarget = operation.source ?: return state
		val source = statStageOperationParticipant(state, actorId, targetActorId, sourceTarget) ?: return state
		val target = statStageOperationParticipant(state, actorId, targetActorId, operation.target) ?: return state
		if (!source.canBattle() || !target.canBattle()) {
			return state
		}
		if (statStageOperationBlockedBySubstitute(state, actorId, target.actorId, skill, operation.target)) {
			return state
		}
		if (statStageOperationBlockedBySubstitute(state, actorId, source.actorId, skill, sourceTarget)) {
			return state
		}
		val copied = source.statStage(operation.stat)
		val previous = target.statStage(operation.stat)
		if (previous == copied) {
			return state
		}
		return state
			.replaceParticipant(target.setStatStage(operation.stat, copied))
			.appendEvent(
				BattleEvent.StatStageCopied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					sourceActorId = source.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					stat = operation.stat,
					copiedStage = copied,
				),
			)
	}

	/**
	 * 交换两个成员指定能力阶级的当前值。
	 *
	 * 力量互换、防守互换和心灵互换都可以拆成多条本操作：每条只交换一个能力项。若双方该项阶级相同，则状态和
	 * 事件保持不变；否则两个成员会在同一个状态转换中写回，避免只更新一方时被后续读取到半成品状态。
	 */
	private fun applyStatStageSwapOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState {
		val sourceTarget = operation.source ?: return state
		val first = statStageOperationParticipant(state, actorId, targetActorId, operation.target) ?: return state
		val second = statStageOperationParticipant(state, actorId, targetActorId, sourceTarget) ?: return state
		if (!first.canBattle() || !second.canBattle() || first.actorId == second.actorId) {
			return state
		}
		if (statStageOperationBlockedBySubstitute(state, actorId, first.actorId, skill, operation.target)) {
			return state
		}
		if (statStageOperationBlockedBySubstitute(state, actorId, second.actorId, skill, sourceTarget)) {
			return state
		}
		val firstStage = first.statStage(operation.stat)
		val secondStage = second.statStage(operation.stat)
		if (firstStage == secondStage) {
			return state
		}
		return state
			.replaceParticipant(first.setStatStage(operation.stat, secondStage))
			.replaceParticipant(second.setStatStage(operation.stat, firstStage))
			.appendEvent(
				BattleEvent.StatStageSwapped(
					turnNumber = state.turnNumber,
					actorId = actorId,
					firstActorId = first.actorId,
					secondActorId = second.actorId,
					skillId = skill.skillId,
					stat = operation.stat,
					firstCurrentStage = secondStage,
					secondCurrentStage = firstStage,
				),
			)
	}

	/**
	 * 将目标范围内成员的指定能力阶级取反。
	 *
	 * 颠倒类效果使用当前阶级的相反数并保持在 -6..6 范围内。因为现有运行态已经保证阶级边界，本函数只需要写回
	 * `-previous`。0 阶级取反后没有可观察变化，因此不会产生事件。
	 */
	private fun applyStatStageInvertOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState =
		statStageOperationParticipants(state, actorId, targetActorId, operation.target).fold(state) { current, participant ->
			val latest = current.participant(participant.actorId) ?: return@fold current
			if (!latest.canBattle()) {
				return@fold current
			}
			if (statStageOperationBlockedBySubstitute(current, actorId, latest.actorId, skill, operation.target)) {
				return@fold current
			}
			val previous = latest.statStage(operation.stat)
			if (previous == 0) {
				current
			} else {
				val next = -previous
				current
					.replaceParticipant(latest.setStatStage(operation.stat, next))
					.appendEvent(
						BattleEvent.StatStageInverted(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = latest.actorId,
							skillId = skill.skillId,
							stat = operation.stat,
							previousStage = previous,
							currentStage = next,
						),
					)
			}
		}

	/**
	 * 根据能力阶级操作目标解析当前参与成员集合。
	 *
	 * 返回值只包含当前可被运行态找到的成员；调用方再根据具体规则判断是否仍可战斗或是否被替身阻挡。这样复制、
	 * 交换和全场清除可以共享同一套目标解析口径。
	 */
	private fun statStageOperationParticipants(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		target: BattleStatStageOperationTarget,
	): List<BattleParticipant> =
		when (target) {
			BattleStatStageOperationTarget.USER -> listOfNotNull(state.participant(actorId))
			BattleStatStageOperationTarget.TARGET -> listOfNotNull(state.participant(targetActorId))
			BattleStatStageOperationTarget.ALL_ACTIVE -> state.sides.flatMap { side ->
				side.activeParticipants().filter { it.canBattle() }
			}
		}

	private fun statStageOperationParticipant(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		target: BattleStatStageOperationTarget,
	): BattleParticipant? =
		statStageOperationParticipants(state, actorId, targetActorId, target).singleOrNull()

	private fun statStageOperationBlockedBySubstitute(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		target: BattleStatStageOperationTarget,
	): Boolean =
		target == BattleStatStageOperationTarget.TARGET &&
			substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)

	/**
	 * 处理技能命中后的强制替换效果。
	 *
	 * 该效果发生在技能伤害和普通附加效果之后，因此巴投、龙尾这类技能会先造成伤害，再把仍可战斗的目标换下。
	 * 变化类强制替换技能也走同一入口：命中、保护、粉末/属性/特性免疫等前置流程已经在外层处理完毕。若目标已经
	 * 倒下、目标不在场、目标侧没有可战斗后备成员，或目标替身阻止了对手技能效果，则状态保持不变且不消费随机数。
	 * 当存在多个合法后备成员时，随机源只消费一次并以稳定 reason 记录；单个后备成员时直接选中，避免无意义随机
	 * 消费破坏 replay 脚本。换入后的入场陷阱和出场特性沿用主动替换的同一条规则路径。
	 */
	private fun applyForcedTargetSwitchEffect(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		if (!skill.forceTargetSwitch || state.result != null) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle() || !state.isActive(target.actorId)) {
			return state
		}
		if (substituteBlocksOpponentEffect(state, actorId, target.actorId, skill)) {
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
		val switched = state.switchActive(target.actorId, next.actorId)
			.appendEvent(
				BattleEvent.TargetForcedSwitchSelected(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					nextActorId = next.actorId,
				),
			)
			.appendEvent(
				BattleEvent.ParticipantSwitched(
					turnNumber = state.turnNumber,
					sideId = side.sideId,
					previousActorId = target.actorId,
					nextActorId = next.actorId,
					forced = true,
				),
			)
		val afterEntryHazards = applyEntryHazardsOnSwitchIn(switched, side.sideId, next.actorId)
		return applySwitchInAbilityEffects(afterEntryHazards, next.actorId)
	}

	/**
	 * 将命中后的技能一侧场上效果写入对应战斗侧。
	 *
	 * 当前只接入防守方伤害减免屏障。目标侧解析在这里完成：使用者侧屏障不依赖本次目标是否仍可战斗；
	 * 目标侧屏障则跟随实际命中的目标所属侧。若同种屏障已存在，`BattleState` 会拒绝写入，本函数也不会产生
	 * 新事件，避免把重复使用误记录为刷新持续回合。携带者若有匹配屏障种类的持续时间延长道具，则只在首次
	 * 成功建立屏障时改写即将写入的完整持续回合。
	 */
	private fun applySideConditionEffect(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideConditionApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val side = when (application.targetSide) {
			BattleSideConditionTarget.USER_SIDE -> state.sideOf(actorId)
			BattleSideConditionTarget.TARGET_SIDE -> state.sideOf(targetActorId)
		} ?: return state
		val damageReduction = extendedSideDamageReduction(state, actorId, application.damageReduction)
		return state.addSideDamageReduction(side.sideId, damageReduction)
			?.appendEvent(
				BattleEvent.SideDamageReductionStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					sideId = side.sideId,
					skillId = skill.skillId,
					kind = damageReduction.kind,
					turnsRemaining = damageReduction.turnsRemaining,
				),
			)
			?: state
	}

	/**
	 * 计算一侧伤害减免屏障建立时最终写入的持续回合。
	 *
	 * 普通屏障技能会在资料中声明基础持续回合；携带者若拥有匹配屏障种类的延长道具效果，则用道具声明的最长回合
	 * 覆盖基础值。基础持续回合为空时保持为空，避免测试 fixture 或未来永久屏障规则被普通道具强行改成有限回合。
	 */
	private fun extendedSideDamageReduction(
		state: BattleState,
		actorId: String,
		damageReduction: BattleSideDamageReduction,
	): BattleSideDamageReduction {
		if (damageReduction.turnsRemaining == null) {
			return damageReduction
		}
		val actor = state.participant(actorId) ?: return damageReduction
		val turnsRemaining = actor.itemEffects
			.filterIsInstance<BattleItemEffect.SideDamageReductionDurationExtension>()
			.filter { damageReduction.kind in it.kinds }
			.maxOfOrNull { it.turnsRemaining }
			?: damageReduction.turnsRemaining
		return damageReduction.copy(turnsRemaining = turnsRemaining)
	}

	/**
	 * 将命中后的技能一侧速度修正写入对应战斗侧。
	 *
	 * 速度修正和伤害减免同属一侧场上状态，但它们影响的结算阶段完全不同：伤害减免在伤害公式阶段读取，
	 * 速度修正在下一次行动排序时读取。因此这里单独建模、单独发事件，避免后续新增速度规则时误走伤害屏障分支。
	 */
	private fun applySideSpeedModifierEffect(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideSpeedModifierApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val side = when (application.targetSide) {
			BattleSideConditionTarget.USER_SIDE -> state.sideOf(actorId)
			BattleSideConditionTarget.TARGET_SIDE -> state.sideOf(targetActorId)
		} ?: return state
		return state.addSideSpeedModifier(side.sideId, application.speedModifier)
			?.appendEvent(
				BattleEvent.SideSpeedModifierStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					sideId = side.sideId,
					skillId = skill.skillId,
					kind = application.speedModifier.kind,
					multiplier = application.speedModifier.multiplier,
					turnsRemaining = application.speedModifier.turnsRemaining,
				),
			)
			?: state
	}

	/**
	 * 将命中后的技能入场陷阱写入对应战斗侧。
	 *
	 * 入场陷阱属于一侧场上状态，但触发时机是后续成员换入，因此这里仅负责建立或叠层，不立即造成伤害或状态。
	 * 目标侧解析规则与其它一侧场上效果一致：使用者侧效果直接绑定使用者所属侧，目标侧效果绑定本次实际命中的
	 * 目标所属侧。若同类陷阱无法再叠层，状态保持不变，也不会产生层数变化事件。
	 */
	private fun applySideEntryHazardEffect(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideEntryHazardApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val side = when (application.targetSide) {
			BattleSideConditionTarget.USER_SIDE -> state.sideOf(actorId)
			BattleSideConditionTarget.TARGET_SIDE -> state.sideOf(targetActorId)
		} ?: return state
		val change = state.addSideEntryHazard(side.sideId, application.hazard) ?: return state
		return change.state.appendEvent(
			BattleEvent.SideEntryHazardChanged(
				turnNumber = state.turnNumber,
				actorId = actorId,
				sideId = side.sideId,
				skillId = skill.skillId,
				kind = change.hazard.kind,
				layers = change.hazard.layers,
				maxLayers = change.hazard.maxLayers,
			),
		)
	}

	/**
	 * 将命中后的全场速度顺序效果写入环境。
	 *
	 * 公开成熟实现中，戏法空间在已经存在时再次使用会解除该全场效果；未存在时建立新的持续效果。这里把这条
	 * “重启即解除”的规则放在引擎层处理，资料层只声明技能会尝试建立哪种全场速度顺序效果。
	 */
	private fun applyFieldSpeedOrderEffect(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		application: BattleFieldSpeedOrderApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val current = state.environment.fieldSpeedOrderEffect
		if (current?.kind == application.speedOrderEffect.kind) {
			return state
				.copy(environment = state.environment.copy(fieldSpeedOrderEffect = null))
				.appendEvent(
					BattleEvent.FieldSpeedOrderEnded(
						turnNumber = state.turnNumber,
						kind = current.kind,
						actorId = actorId,
						skillId = skill.skillId,
					),
				)
		}
		if (current != null) {
			return state
		}
		return state
			.copy(environment = state.environment.copy(fieldSpeedOrderEffect = application.speedOrderEffect))
			.appendEvent(
				BattleEvent.FieldSpeedOrderStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					skillId = skill.skillId,
					kind = application.speedOrderEffect.kind,
					turnsRemaining = application.speedOrderEffect.turnsRemaining,
				),
			)
	}

	/**
	 * 结算成员换入后所在侧已有的入场陷阱。
	 *
	 * 调用点位于 `ParticipantSwitched` 事件之后，因此事件流会稳定表达“先进入场地，再承受入场效果”。同一侧存在
	 * 多种入场陷阱时按状态列表顺序结算；每个陷阱都会重新读取当前成员快照，若成员已经倒下或战斗已经结束，
	 * 后续陷阱不再继续触发。该策略避免已经无法战斗的成员继续获得异常状态或能力阶级变化。
	 */
	private fun applyEntryHazardsOnSwitchIn(
		state: BattleState,
		sideId: String,
		actorId: String,
	): BattleState {
		val hazards = state.sides.firstOrNull { it.sideId == sideId }?.entryHazards.orEmpty()
		return hazards.fold(state) { current, hazard ->
			if (current.result != null) {
				current
			} else {
				val participant = current.participant(actorId) ?: return@fold current
				if (!participant.canBattle()) {
					current
				} else {
					applyEntryHazardOnSwitchIn(current, sideId, participant, hazard)
				}
			}
		}
	}

	/**
	 * 结算单个入场陷阱对换入成员的影响。
	 *
	 * 每种陷阱的公开现代规则差异较大，因此这里保持显式分支，而不是把行为藏进字符串策略或反射脚本。资料层只负责
	 * 把技能和场上规则映射成 [BattleSideEntryHazardKind]；真正会改变 HP、异常状态或能力阶级的语义由引擎持有。
	 */
	private fun applyEntryHazardOnSwitchIn(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState =
		when (hazard.kind) {
			BattleSideEntryHazardKind.STEALTH_ROCK -> applyStealthRockEntryDamage(state, sideId, participant, hazard)
			BattleSideEntryHazardKind.SPIKES -> applySpikesEntryDamage(state, sideId, participant, hazard)
			BattleSideEntryHazardKind.TOXIC_SPIKES -> applyToxicSpikesEntryEffect(state, sideId, participant, hazard)
			BattleSideEntryHazardKind.STICKY_WEB -> applyStickyWebEntryEffect(state, sideId, participant, hazard)
		}

	/**
	 * 结算隐形岩类入场伤害。
	 *
	 * 现代规则按岩属性攻击换入成员的属性克制倍率计算 `最大 HP * 倍率 / 8`，向下取整且正倍率至少造成 1 点伤害。
	 * 如果规则快照没有提供岩属性 ID，本场战斗无法可靠计算克制关系，函数会保持状态不变，而不是硬编码资料编号。
	 */
	private fun applyStealthRockEntryDamage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState {
		val rockElementId = state.rules.rockElementId ?: return state
		val effectiveness = state.rules.elementChart.multiplier(rockElementId, participant.elementIds)
		val damage = entryHazardFractionDamage(participant.maxHp, effectiveness / STEALTH_ROCK_DAMAGE_DENOMINATOR)
		return applyEntryHazardDamage(
			state = state,
			sideId = sideId,
			participant = participant,
			hazard = hazard,
			amount = damage,
			effectiveness = effectiveness,
		)
	}

	/**
	 * 结算撒菱类入场伤害。
	 *
	 * 撒菱只影响接地成员。现代规则层数伤害为一层最大 HP 的 1/8，二层 1/6，三层 1/4；层数超过上限在模型层
	 * 已经被拒绝，因此这里仍使用 `when` 让伤害分母和公开规则直接对应。
	 */
	private fun applySpikesEntryDamage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState {
		if (!participant.grounded) {
			return state
		}
		val denominator = when (hazard.layers) {
			1 -> SPIKES_ONE_LAYER_DAMAGE_DENOMINATOR
			2 -> SPIKES_TWO_LAYER_DAMAGE_DENOMINATOR
			else -> SPIKES_THREE_LAYER_DAMAGE_DENOMINATOR
		}
		return applyEntryHazardDamage(
			state = state,
			sideId = sideId,
			participant = participant,
			hazard = hazard,
			amount = (participant.maxHp / denominator).coerceAtLeast(1),
			effectiveness = 1.0,
		)
	}

	/**
	 * 结算毒菱类入场效果。
	 *
	 * 毒菱只影响接地成员。接地毒属性成员换入时会吸收并移除该侧毒菱；其它接地成员在一层时获得普通中毒、
	 * 两层时获得剧毒。毒/钢属性免疫、薄雾场地、特性和道具免疫复用主要异常状态的统一阻止逻辑，确保毒菱和
	 * 普通技能附加中毒不会出现两套相互矛盾的免疫判断。
	 */
	private fun applyToxicSpikesEntryEffect(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState {
		if (!participant.grounded) {
			return state
		}
		if (participant.hasElement(state.rules.poisonElementId)) {
			return state.removeSideEntryHazard(sideId, BattleSideEntryHazardKind.TOXIC_SPIKES)
				?.appendEvent(
					BattleEvent.SideEntryHazardRemoved(
						turnNumber = state.turnNumber,
						actorId = participant.actorId,
						sideId = sideId,
						kind = BattleSideEntryHazardKind.TOXIC_SPIKES,
					),
				)
				?: state
		}
		if (participant.majorStatus != null) {
			return state
		}
		val status = if (hazard.layers >= 2) BattleMajorStatus.BAD_POISON else BattleMajorStatus.POISON
		val blockedReason = blockedMajorStatusReason(state, participant.actorId, participant, status)
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.EntryHazardStatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					status = status,
					reason = blockedReason,
				),
			)
		}
		return state
			.replaceParticipant(participant.applyMajorStatus(status))
			.appendEvent(
				BattleEvent.EntryHazardStatusApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					status = status,
				),
			)
	}

	/**
	 * 结算黏黏网类入场效果。
	 *
	 * 黏黏网只影响接地成员，并在换入时降低速度能力阶级 1 级。能力阶级已经达到 -6 时不会产生状态变化或事件；
	 * 这让 replay 可以把事件直接视为“能力阶级确实发生变化”的事实。
	 */
	private fun applyStickyWebEntryEffect(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState {
		if (!participant.grounded) {
			return state
		}
		val beforeStage = participant.statStage(BattleStat.SPEED)
		val updated = participant.changeStatStage(BattleStat.SPEED, -1)
		val afterStage = updated.statStage(BattleStat.SPEED)
		if (beforeStage == afterStage) {
			return state
		}
		return state
			.replaceParticipant(updated)
			.appendEvent(
				BattleEvent.EntryHazardStatStageChanged(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					stat = BattleStat.SPEED,
					delta = afterStage - beforeStage,
					currentStage = afterStage,
				),
			)
	}

	/**
	 * 写入入场陷阱伤害并接续道具、倒下和胜负判定。
	 *
	 * 入场伤害不是普通技能伤害，但仍会触发低体力回复类道具，并可能导致成员倒下和战斗结束。因此这里复用
	 * 已有的低体力道具与倒下处理函数，只把可观察事件换成入场陷阱专用事件。
	 */
	private fun applyEntryHazardDamage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
		amount: Int,
		effectiveness: Double,
	): BattleState {
		if (amount <= 0 || participant.hasIndirectDamageImmunity()) {
			return state
		}
		val damaged = participant.receiveDamage(amount)
		val afterDamage = state
			.replaceParticipant(damaged)
			.appendEvent(
				BattleEvent.EntryHazardDamageApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					amount = amount,
					layers = hazard.layers,
					effectiveness = effectiveness,
				),
			)
		val afterLowHpItem = applyLowHpHealingItem(afterDamage, damaged.actorId)
		val latestAfterItem = afterLowHpItem.participant(damaged.actorId) ?: damaged
		return afterLowHpItem.handleFaintAndResult(latestAfterItem)
	}

	/**
	 * 计算最大 HP 比例型入场伤害。
	 *
	 * 公开规则中的比例伤害通常向下取整；当倍率为正但向下取整得到 0 时，仍至少造成 1 点伤害。倍率为 0 或负数
	 * 表示没有实际伤害，调用方会跳过伤害事件。
	 */
	private fun entryHazardFractionDamage(maxHp: Int, fraction: Double): Int =
		if (fraction <= 0.0) {
			0
		} else {
			floor(maxHp * fraction).toInt().coerceAtLeast(1)
		}

	/**
	 * 结算战斗开始时所有当前上场成员的出场特性。
	 *
	 * 初始上场不是一次 `SwitchParticipant` 行动，但现代规则中“出场时触发”的特性同样会在战斗开始阶段生效。
	 * 当前按有效速度排序触发，戏法空间存在时复用引擎已有的速度比较器反转速度顺序；同速成员保持初始侧和席位
	 * 顺序，直到战斗开始阶段引入随机源。这样天气覆盖、出场降能力等效果在没有同速争议时已经与公开实现一致。
	 */
	private fun applyInitialSwitchInAbilityEffects(state: BattleState): BattleState =
		initialSwitchInActorIds(state)
			.fold(state) { current, actorId -> applySwitchInAbilityEffects(current, actorId) }

	/**
	 * 计算战斗开始阶段出场特性的稳定触发顺序。
	 *
	 * 输入是当前初始状态中的所有上场成员；输出只保留成员 ID，避免后续某个成员的出场特性改变环境后导致已排序
	 * 队列重新计算。有效速度在初始状态上一次性计算，包含麻痹、天气速度特性、道具速度倍率和一侧速度修正。
	 */
	private fun initialSwitchInActorIds(state: BattleState): List<String> =
		state.sides
			.flatMap { side -> side.activeActorIds.mapNotNull { actorId -> state.participant(actorId) } }
			.groupBy { participant -> effectiveSpeed(state, participant) }
			.toSortedMap(speedComparator(state))
			.values
			.flatMap { sameSpeedParticipants -> sameSpeedParticipants.map { it.actorId } }

	/**
	 * 结算单个成员成功进入场地后的出场特性。
	 *
	 * 该函数要求成员当前仍在场且可战斗；如果成员刚换入后已经被入场陷阱击倒，则不会触发出场特性。当前支持
	 * 对手当前上场成员的能力阶级变化、全场天气覆盖和全场场地覆盖。其它能力效果会在它们所属阶段显式忽略，
	 * 避免出场阶段误处理低体力伤害增幅、接触反制或免疫类稳定效果。
	 */
	private fun applySwitchInAbilityEffects(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!state.isActive(actor.actorId) || !actor.canBattle()) {
			return state
		}
		return actor.abilityEffects
			.fold(state) { current, effect -> applySwitchInAbilityEffect(current, actor.actorId, effect) }
	}

	/**
	 * 将单个结构化特性效果分派到出场阶段实现。
	 *
	 * 只有明确属于 SWITCH_IN 生命周期的效果会改变状态；其它效果返回原状态。保持这个穷尽分派可以让新增特性效果
	 * 时编译器提示所有阶段是否需要处理，而不是让字符串 policy 悄悄穿透到纯引擎内部。
	 */
	private fun applySwitchInAbilityEffect(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect,
	): BattleState =
		when (effect) {
			is BattleAbilityEffect.SwitchInStatStageChange -> applySwitchInStatStageChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInTerrainChange -> applySwitchInTerrainChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInWeatherChange -> applySwitchInWeatherChange(state, actorId, effect)
			is BattleAbilityEffect.ContactBasedSkillDamageBoost,
			is BattleAbilityEffect.ContactStatusOnAttacker,
			is BattleAbilityEffect.CriticalHitImmunity,
			is BattleAbilityEffect.AttackingStatMultiplier,
			is BattleAbilityEffect.DamageClassDamageReduction,
			is BattleAbilityEffect.DefendingStatMultiplier,
			is BattleAbilityEffect.SameElementBonusOverride,
			is BattleAbilityEffect.ElementSkillAbsorbHeal,
			is BattleAbilityEffect.ElementSkillAbsorbStatStage,
			is BattleAbilityEffect.ElementSkillDamageBoost,
			is BattleAbilityEffect.FullHpDamageReduction,
			is BattleAbilityEffect.IgnoreOpponentAccuracyStatStages,
			is BattleAbilityEffect.IgnoreOpponentDamageStatStages,
			is BattleAbilityEffect.IgnoreTargetAbilityEffects,
			is BattleAbilityEffect.IndirectDamageImmunity,
			is BattleAbilityEffect.LowHpElementDamageBoost,
			is BattleAbilityEffect.MajorStatusImmunity,
			is BattleAbilityEffect.PriorityMoveImmunityForSide,
			is BattleAbilityEffect.PunchBasedSkillDamageBoost,
			is BattleAbilityEffect.SkillRecoilDamageImmunity,
			is BattleAbilityEffect.SlicingBasedSkillDamageBoost,
			is BattleAbilityEffect.SoundBasedSkillDamageBoost,
			is BattleAbilityEffect.SoundBasedSkillDamageReduction,
			is BattleAbilityEffect.SoundBasedSkillImmunity,
			is BattleAbilityEffect.StatusSkillPriorityBoost,
			is BattleAbilityEffect.SurviveFatalDamageAtFullHp,
			is BattleAbilityEffect.SuperEffectiveDamageReduction,
			is BattleAbilityEffect.TerrainSpeedMultiplier,
			is BattleAbilityEffect.VolatileStatusImmunity,
			is BattleAbilityEffect.WeatherDamageImmunity,
			is BattleAbilityEffect.WeatherElementDamageBoost,
			is BattleAbilityEffect.WeatherEndTurnHeal,
			is BattleAbilityEffect.WeatherSpeedMultiplier -> state
		}

	/**
	 * 执行出场特性的能力阶级变化。
	 *
	 * 目标集合为触发者对侧当前上场且仍可战斗的成员。每个目标独立夹取 -6..6 的现代能力阶级边界；
	 * 如果某个目标已经达到边界，本次不会写入状态，也不会产生事件。该函数不消费随机数。
	 */
	private fun applySwitchInStatStageChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInStatStageChange,
	): BattleState {
		val actorSide = state.sideOf(actorId) ?: return state
		val targetActorIds = state.sides
			.filter { it.sideId != actorSide.sideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
			.map { it.actorId }
		return targetActorIds.fold(state) { current, targetActorId ->
			val target = current.participant(targetActorId) ?: return@fold current
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
			}
		}
	}

	/**
	 * 执行出场特性的天气设置。
	 *
	 * 现代普通天气特性会覆盖当前普通天气并写入固定持续回合。携带者若有匹配天气的延长道具效果，则与天气技能
	 * 使用同一套持续回合延长逻辑。若当前环境已经是同一天气且剩余回合一致，则保持状态并跳过事件，避免 replay
	 * 端看到没有状态变化的重复天气开始事实。
	 */
	private fun applySwitchInWeatherChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInWeatherChange,
	): BattleState {
		val turnsRemaining = extendedWeatherTurnsRemaining(
			state = state,
			actorId = actorId,
			weather = effect.weather,
			baseTurnsRemaining = effect.turnsRemaining,
		)
		if (
			state.environment.weather == effect.weather &&
			state.environment.weatherTurnsRemaining == turnsRemaining
		) {
			return state
		}
		return state
			.copy(
				environment = state.environment.copy(
					weather = effect.weather,
					weatherTurnsRemaining = turnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.WeatherStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					weather = effect.weather,
					turnsRemaining = turnsRemaining,
				),
			)
	}

	/**
	 * 执行出场特性的场地设置。
	 *
	 * 普通场地会覆盖当前场地并写入固定持续回合。若当前环境已经是同一场地且剩余回合一致，则不产生事件；
	 * 这样 replay 可以把 `TerrainStarted` 当作真实环境变化，而不是触发尝试日志。携带者若拥有匹配场地的延长
	 * 道具效果，则与场地技能使用同一套持续回合延长逻辑；强制封锁或特殊机制后续会以独立效果扩展。
	 */
	private fun applySwitchInTerrainChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInTerrainChange,
	): BattleState {
		val turnsRemaining = extendedTerrainTurnsRemaining(
			state = state,
			actorId = actorId,
			terrain = effect.terrain,
			baseTurnsRemaining = effect.turnsRemaining,
		)
		if (
			state.environment.terrain == effect.terrain &&
			state.environment.terrainTurnsRemaining == turnsRemaining
		) {
			return state
		}
		return state
			.copy(
				environment = state.environment.copy(
					terrain = effect.terrain,
					terrainTurnsRemaining = turnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.TerrainStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					terrain = effect.terrain,
					turnsRemaining = turnsRemaining,
				),
			)
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
		if (actor.rechargeTurnsRemaining > 0) {
			return BeforeMoveResult(
				context = context.copy(state = consumeRechargeBlockedAction(context.state, actor)),
				blocked = true,
			)
		}
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
		if (actor.healBlockTurnsRemaining > 0 && healBlockPreventsSkill(skill)) {
			return BeforeMoveResult(
				context = context.copy(state = consumeHealBlockBlockedAction(context.state, actor, skill)),
				blocked = true,
			)
		}
		if (actor.tauntTurnsRemaining > 0 && tauntPreventsSkill(skill)) {
			return BeforeMoveResult(
				context = context.copy(state = consumeTauntBlockedAction(context.state, actor, skill)),
				blocked = true,
			)
		}
		if (actor.disabledSkillTurnsRemaining > 0 && actor.disabledSkillId == skill.skillId) {
			return BeforeMoveResult(
				context = context.copy(state = consumeDisableBlockedAction(context.state, actor, skill)),
				blocked = true,
			)
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
	 * 消耗休整对本次技能行动的阻止效果。
	 *
	 * 休整是由上一次成功技能写入的强制空过状态。它不消耗本次提交技能的 PP，也不会触发讲究锁定、命中、
	 * 伤害或其它行动前随机判定。阻止发生后递减计数，第一批休整技能只需要一次阻止，因此通常会直接清零。
	 */
	private fun consumeRechargeBlockedAction(state: BattleState, actor: BattleParticipant): BattleState {
		val turnsRemainingBefore = actor.rechargeTurnsRemaining
		return state
			.replaceParticipant(actor.consumeRechargeTurn())
			.appendEvent(
				BattleEvent.SkillPreventedByRecharge(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					turnsRemainingBefore = turnsRemainingBefore,
				),
			)
	}

	/**
	 * 消耗一次“回复封锁阻止本次回复类技能”的行动结果。
	 *
	 * 回复封锁本身不会因为阻止一次行动而立即减少持续回合；它只在回合末统一递减。这里不消耗 PP、不写入
	 * `SkillUsed`，也不触发讲究锁定或命中随机数，让主动回复技能和吸取回复类技能在现代规则下稳定失败。
	 */
	private fun consumeHealBlockBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		state.appendEvent(
			BattleEvent.SkillPreventedByHealBlock(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				skillId = skill.skillId,
				turnsRemainingBefore = actor.healBlockTurnsRemaining,
			),
		)

	/**
	 * 判断回复封锁是否禁止成员宣告该技能。
	 *
	 * 现代规则中，直接回复使用者的变化技能和伤害后吸取回复的攻击技能都不能在回复封锁下使用。建立替身、
	 * 反作用伤害和其它非回复类 HP 变化不在此列；它们继续按各自规则结算。
	 */
	private fun healBlockPreventsSkill(skill: BattleSkillSlot): Boolean =
		skill.hpEffects.any { effect ->
			effect is BattleSkillHpEffect.SelfHealMaxHpFraction ||
				effect is BattleSkillHpEffect.SelfHealMaxHpByWeather ||
				effect is BattleSkillHpEffect.DrainDamage
		}

	/**
	 * 消耗一次“挑衅阻止本次变化技能”的行动结果。
	 *
	 * 挑衅不会因为成功阻止一次行动而提前减少持续回合；它只在完整回合末统一递减。这里不消耗 PP、不写入
	 * `SkillUsed`，也不触发命中、保护、附加效果或讲究类锁定流程，确保被挑衅成员的变化技能稳定停在行动前。
	 */
	private fun consumeTauntBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		state.appendEvent(
			BattleEvent.SkillPreventedByTaunt(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				skillId = skill.skillId,
				turnsRemainingBefore = actor.tauntTurnsRemaining,
			),
		)

	/**
	 * 判断挑衅是否禁止成员宣告该技能。
	 *
	 * 现代规则中挑衅只阻止变化分类技能；物理和特殊分类技能即使没有造成伤害，仍不由挑衅在这里拦截。
	 */
	private fun tauntPreventsSkill(skill: BattleSkillSlot): Boolean =
		skill.damageClass == BattleDamageClass.STATUS

	/**
	 * 消耗一次“定身法阻止本次被禁用技能”的行动结果。
	 *
	 * 定身法不会因为阻止一次行动而立即减少持续回合；它只在回合末统一递减。这里不消耗 PP、不写入
	 * `SkillUsed`，也不触发命中、保护、附加效果或讲究类锁定流程。
	 */
	private fun consumeDisableBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		state.appendEvent(
			BattleEvent.SkillPreventedByDisable(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				skillId = skill.skillId,
				turnsRemainingBefore = actor.disabledSkillTurnsRemaining,
			),
		)

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
		val blockedState = afterDecrement.appendEvent(
			BattleEvent.SkillPreventedByVolatileStatus(
				turnNumber = context.state.turnNumber,
				actorId = actor.actorId,
				status = BattleVolatileStatus.CONFUSION,
			),
		)
		if (decremented.hasIndirectDamageImmunity()) {
			return BeforeMoveResult(context = context.copy(state = blockedState), blocked = true)
		}
		val damaged = decremented.receiveDamage(damage)
		val afterDamage = blockedState
			.replaceParticipant(damaged)
			.appendEvent(
				BattleEvent.ConfusionDamageApplied(
					turnNumber = context.state.turnNumber,
					actorId = actor.actorId,
					amount = damage,
					randomPercent = randomPercent,
					turnsRemainingBefore = turnsRemainingBefore,
				),
			)
		val afterLowHpItem = applyLowHpHealingItem(afterDamage, damaged.actorId)
		val latest = afterLowHpItem.participant(damaged.actorId) ?: damaged
		val afterFaint = afterLowHpItem.handleFaintAndResult(latest)
		return BeforeMoveResult(context = context.copy(state = afterFaint), blocked = true)
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
	 * 该函数不覆盖已有主要异常状态；已有状态、属性免疫、场地免疫、特性免疫和道具免疫都会在消费状态私有
	 * 随机数前短路并产生阻止事件，保证 replay 可以区分“概率没触发”和“规则阻止写入”。
	 */
	private fun applyMajorStatusEffect(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		random: BattleRandom,
		randomReason: String,
		skill: BattleSkillSlot? = null,
	): BattleState {
		val blockedReason = if (recipient.majorStatus != null) {
			BattleStatusBlockReason.EXISTING_STATUS
		} else {
			blockedMajorStatusReason(state, actorId, recipient, status, skill)
		}
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
		val appliedState = state
			.replaceParticipant(recipient.applyMajorStatus(status, sleepTurnsRemaining))
			.appendEvent(
				BattleEvent.StatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
				),
			)
		return applyMajorStatusCureItem(appliedState, recipient.actorId)
	}

	/**
	 * 处理成功获得主要异常状态后的即时治愈携带道具。
	 *
	 * 该阶段只在 [applyMajorStatusEffect] 已经写入状态并追加 [BattleEvent.StatusApplied] 后运行，因此它不会和属性、
	 * 场地、特性或道具免疫混淆。若成员当前没有主要异常状态，或携带道具没有声明可解除该状态的
	 * [BattleItemEffect.MajorStatusCure]，函数原样返回状态。
	 *
	 * 触发成功时先清除主要异常状态及其附属计数，再按效果声明消费携带道具，最后追加 [BattleEvent.StatusCleared]。
	 * 事件流因此会稳定呈现“状态写入 -> 道具治愈”的顺序，便于 replay 和公开 fixture 对照具体触发时机。
	 */
	private fun applyMajorStatusCureItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		val status = participant.majorStatus ?: return state
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.MajorStatusCure>()
			.firstOrNull { status in it.statuses }
			?: return state
		val cured = if (effect.consumesItem) {
			participant.clearMajorStatus().consumeHeldItem()
		} else {
			participant.clearMajorStatus()
		}
		return state
			.replaceParticipant(cured)
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
	}

	/**
	 * 判断主要异常状态是否会在附加前被稳定免疫规则阻止。
	 *
	 * 顺序选择先属性、后场地：如果目标自身属性已经免疫该状态，就不再把阻止原因归给场地，便于 fixture
	 * 明确定位是个体免疫还是全场效果。特性和道具作为资料驱动的稳定免疫排在场地之后，避免它们遮蔽
	 * 场地这类全场公开状态。
	 */
	private fun blockedMajorStatusReason(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		skill: BattleSkillSlot? = null,
	): BattleStatusBlockReason? =
		when {
			statusBlockedByElement(state.rules, recipient, status) -> BattleStatusBlockReason.ELEMENT
			statusBlockedByTerrain(state, recipient, status) -> BattleStatusBlockReason.TERRAIN
			skill != null && substituteBlocksOpponentEffect(state, actorId, recipient.actorId, skill) -> BattleStatusBlockReason.SUBSTITUTE
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				statusBlockedByAbility(recipient, status) -> BattleStatusBlockReason.ABILITY
			statusBlockedByItem(recipient, status) -> BattleStatusBlockReason.ITEM
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
	 * 判断目标替身是否会阻止来自对手的技能伤害或状态效果。
	 *
	 * 替身只保护当前有替身的成员免受对手非声音类技能影响；使用者对自己施加的效果、同侧辅助效果、声音类技能
	 * 以及目标没有替身时都不会被这里阻止。接棒传递等例外后续会以明确技能标签或状态规则扩展。
	 */
	private fun substituteBlocksOpponentEffect(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): Boolean {
		if (skill.soundBased) {
			return false
		}
		val target = state.participant(targetActorId) ?: return false
		if (!target.hasSubstitute()) {
			return false
		}
		val actorSide = state.sideOf(actorId)?.sideId ?: return false
		val targetSide = state.sideOf(targetActorId)?.sideId ?: return false
		return actorSide != targetSide
	}

	/**
	 * 判断目标特性是否稳定免疫指定主要异常状态。
	 */
	private fun statusBlockedByAbility(recipient: BattleParticipant, status: BattleMajorStatus): Boolean =
		recipient.abilityEffects.any { effect ->
			effect is BattleAbilityEffect.MajorStatusImmunity && status in effect.statuses
		}

	/**
	 * 判断目标携带道具是否稳定免疫指定主要异常状态。
	 */
	private fun statusBlockedByItem(recipient: BattleParticipant, status: BattleMajorStatus): Boolean =
		recipient.itemEffects.any { effect ->
			effect is BattleItemEffect.MajorStatusImmunity && status in effect.statuses
		}

	/**
	 * 判断成员是否具有指定属性。
	 */
	private fun BattleParticipant.hasElement(elementId: Long?): Boolean =
		elementId != null && elementId in elementIds

	/**
	 * 判断本次技能是否应忽略目标侧防守特性。
	 *
	 * 该 helper 只处理一次技能结算中的“攻击方对对手目标”的关系。它不会让同侧辅助、自身目标或非技能来源
	 * 绕过目标特性，也不会影响目标道具、属性天然免疫、场地免疫或攻击方自己的攻击侧特性。
	 */
	private fun skillIgnoresTargetAbilityEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
	): Boolean {
		val actor = state.participant(actorId) ?: return false
		val target = state.participant(targetActorId) ?: return false
		return skillIgnoresTargetAbilityEffects(state, actor, target)
	}

	/**
	 * 判断本次技能是否应忽略目标侧防守特性。
	 *
	 * 纯引擎只读取 [BattleAbilityEffect.IgnoreTargetAbilityEffects] 这个结构化效果，不判断具体资料库特性名称。
	 * 双打中只要目标在对手侧，就会忽略目标本人以及目标侧伙伴提供的防守型特性；同侧目标始终返回 false。
	 */
	private fun skillIgnoresTargetAbilityEffects(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
	): Boolean {
		if (!actor.canBattle() || !target.canBattle()) {
			return false
		}
		val actorSide = state.sideOf(actor.actorId) ?: return false
		val targetSide = state.sideOf(target.actorId) ?: return false
		if (actorSide.sideId == targetSide.sideId) {
			return false
		}
		return actor.abilityEffects.any { it is BattleAbilityEffect.IgnoreTargetAbilityEffects }
	}

	/**
	 * 判断成员是否免疫非技能直接伤害。
	 *
	 * 该入口只读取结构化特性效果，不判断具体特性名称。调用方负责保证传入的伤害来源确实不是普通技能直接命中；
	 * 因此普通 [BattleEvent.DamageApplied] 不会经过这里，而异常状态回合末伤害、天气伤害、入场陷阱、混乱自伤、
	 * 技能反作用伤害和携带道具反伤等都会在写入 HP 前调用它。
	 */
	private fun BattleParticipant.hasIndirectDamageImmunity(): Boolean =
		abilityEffects.any { it is BattleAbilityEffect.IndirectDamageImmunity }

	/**
	 * 判断成员是否免疫技能自身带来的反作用伤害。
	 *
	 * 该效果只服务 [BattleEvent.SkillRecoilDamageApplied] 写入前的窄范围判断；携带道具反伤仍由道具流程处理，
	 * 不会因为这里返回 true 而被跳过。
	 */
	private fun BattleParticipant.hasSkillRecoilDamageImmunity(): Boolean =
		abilityEffects.any { it is BattleAbilityEffect.SkillRecoilDamageImmunity }

	/**
	 * 判断成员是否免疫被技能击中要害。
	 *
	 * 调用方在普通要害概率已经结算后读取该效果，把本次伤害请求降回非要害；这样随机轨迹和必定要害技能的
	 * 前置事实都不会被抹掉，只是最终伤害不再按要害倍率和要害绕过屏障规则处理。
	 */
	private fun BattleParticipant.hasCriticalHitImmunity(): Boolean =
		abilityEffects.any { it is BattleAbilityEffect.CriticalHitImmunity }

	/**
	 * 判断成员是否在命中判定中忽略对手的命中或闪避阶级变化。
	 *
	 * 攻击方拥有该效果时，命中流程不读取目标闪避阶级；防守方拥有该效果时，命中流程不读取攻击方命中阶级。
	 * 这里不判断具体特性名称，让资料层可以用同一个结构化效果挂接同类现代规则。
	 */
	private fun BattleParticipant.ignoresOpponentAccuracyStatStages(): Boolean =
		abilityEffects.any { it is BattleAbilityEffect.IgnoreOpponentAccuracyStatStages }

	/**
	 * 判断成员是否免疫指定天气的回合末伤害。
	 *
	 * 沙暴天然不会伤害岩、地面、钢属性成员；特性和道具提供的天气伤害免疫作为更通用的结构化效果处理，
	 * 便于表达防尘类道具或防天气伤害特性。
	 */
	private fun BattleParticipant.immuneToWeatherDamage(state: BattleState, weather: BattleWeather): Boolean =
		hasIndirectDamageImmunity() ||
			weatherDamageBlockedByAbility(weather) ||
			weatherDamageBlockedByItem(weather) ||
			when (weather) {
				BattleWeather.SANDSTORM -> hasElement(state.rules.rockElementId) ||
					hasElement(state.rules.groundElementId) ||
					hasElement(state.rules.steelElementId)
				BattleWeather.NONE,
				BattleWeather.SUN,
				BattleWeather.RAIN,
				BattleWeather.SNOW -> false
			}

	/**
	 * 判断成员特性是否免疫指定天气伤害。
	 */
	private fun BattleParticipant.weatherDamageBlockedByAbility(weather: BattleWeather): Boolean =
		abilityEffects.any { effect ->
			effect is BattleAbilityEffect.WeatherDamageImmunity && weather in effect.weathers
		}

	/**
	 * 判断成员携带道具是否免疫指定天气伤害。
	 */
	private fun BattleParticipant.weatherDamageBlockedByItem(weather: BattleWeather): Boolean =
		itemEffects.any { effect ->
			effect is BattleItemEffect.WeatherDamageImmunity && weather in effect.weathers
		}

	/**
	 * 附加临时状态并处理状态私有计数。
	 *
	 * 畏缩只标记本回合行动前阻止；混乱成功时消费一个 `[0, 4)` 随机数并转成 2..5 的内部计数；回复封锁写入
	 * 固定 5 回合计数并在回合末递减；挑衅和定身法写入固定 3/4 回合计数。若目标已经处于同类可持续临时状态，成员快照不会变化，旧持续计数也不会
	 * 被刷新；状态机会追加阻止事件，便于 replay 明确区分“没有命中/没有触发”和“目标已有同类临时状态”。
	 * 场地、特性或道具免疫会在消费混乱持续时间随机数前短路，保证无法附加状态时 replay 随机脚本保持稳定。
	 */
	private fun applyVolatileStatusEffect(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
		random: BattleRandom,
		randomReason: String,
		skill: BattleSkillSlot? = null,
	): BattleState {
		if (volatileStatusAlreadyPresent(recipient, status)) {
			return state.appendEvent(
				BattleEvent.VolatileStatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = BattleStatusBlockReason.EXISTING_STATUS,
				),
			)
		}
		val blockedReason = blockedVolatileStatusReason(state, actorId, recipient, status, skill)
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.VolatileStatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = blockedReason,
				),
			)
		}
		val disabledSkillId = if (status == BattleVolatileStatus.DISABLE) {
			disableTargetSkillId(recipient)
				?: return state.appendEvent(
					BattleEvent.VolatileStatusApplicationBlocked(
						turnNumber = state.turnNumber,
						actorId = actorId,
						targetActorId = recipient.actorId,
						status = status,
						reason = BattleStatusBlockReason.NO_ELIGIBLE_SKILL,
					),
				)
		} else {
			null
		}
		val confusionTurnsRemaining = if (status == BattleVolatileStatus.CONFUSION) {
			random.nextInt(4, randomReason) + 2
		} else {
			0
		}
		val healBlockTurnsRemaining = if (status == BattleVolatileStatus.HEAL_BLOCK) HEAL_BLOCK_TURNS else 0
		val tauntTurnsRemaining = if (status == BattleVolatileStatus.TAUNT) TAUNT_TURNS else 0
		val disabledSkillTurnsRemaining = if (status == BattleVolatileStatus.DISABLE) DISABLE_TURNS else 0
		val appliedState = state
			.replaceParticipant(
				recipient.applyVolatileStatus(
					status = status,
					confusionTurnsRemaining = confusionTurnsRemaining,
					healBlockTurnsRemaining = healBlockTurnsRemaining,
					tauntTurnsRemaining = tauntTurnsRemaining,
					disabledSkillId = disabledSkillId,
					disabledSkillTurnsRemaining = disabledSkillTurnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.VolatileStatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
				),
			)
		val afterDisableEvent = if (disabledSkillId != null) {
			appliedState.appendEvent(
				BattleEvent.SkillDisabled(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					disabledSkillId = disabledSkillId,
					turnsRemaining = DISABLE_TURNS,
				),
			)
		} else {
			appliedState
		}
		return applyVolatileStatusCureItem(afterDisableEvent, recipient.actorId, status)
	}

	/**
	 * 解析定身法可以禁用的目标技能。
	 *
	 * 现代规则中定身法读取目标最近一次成功使用的技能；如果目标还没有使用过技能，或者该技能已经没有 PP，
	 * 定身法不会写入状态。这里不按名称判断具体技能，资料层只需把定身法映射为 [BattleVolatileStatus.DISABLE]。
	 */
	private fun disableTargetSkillId(recipient: BattleParticipant): Long? {
		val skillId = recipient.lastSuccessfulSkillId ?: return null
		val slot = recipient.skillSlot(skillId) ?: return null
		return if (slot.remainingPp > 0) skillId else null
	}

	/**
	 * 判断目标是否已经拥有同类临时状态。
	 *
	 * 当前只有混乱、回复封锁、挑衅和定身法有跨回合持续计数，需要拒绝刷新。畏缩可以被多次尝试，但运行态只保存一个布尔值，
	 * 后续行动前或回合末都会清除，所以重复附加不会改变可观察持续时间。
	 */
	private fun volatileStatusAlreadyPresent(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		when (status) {
			BattleVolatileStatus.CONFUSION -> recipient.confusionTurnsRemaining > 0
			BattleVolatileStatus.HEAL_BLOCK -> recipient.healBlockTurnsRemaining > 0
			BattleVolatileStatus.TAUNT -> recipient.tauntTurnsRemaining > 0
			BattleVolatileStatus.DISABLE -> recipient.disabledSkillTurnsRemaining > 0
			BattleVolatileStatus.FLINCH -> false
		}

	/**
	 * 处理成功获得临时状态后的即时治愈携带道具。
	 *
	 * 该阶段只在 [applyVolatileStatusEffect] 已经写入临时状态并追加 [BattleEvent.VolatileStatusApplied] 后运行，
	 * 因此不会遮蔽薄雾场地、特性免疫、道具免疫或已有混乱的前置阻止语义。触发成功时，函数先清除目标临时状态，
	 * 再按 [BattleItemEffect.VolatileStatusCure.consumesItem] 决定是否消费携带道具，最后追加
	 * [BattleEvent.VolatileStatusCleared]。
	 *
	 * 畏缩和混乱的运行态字段不同：畏缩是布尔标记，混乱是剩余检查次数。这里统一调用
	 * [BattleParticipant.clearVolatileStatus]，由成员模型维护每种临时状态的清理不变量。
	 */
	private fun applyVolatileStatusCureItem(
		state: BattleState,
		actorId: String,
		status: BattleVolatileStatus,
	): BattleState {
		val participant = state.participant(actorId) ?: return state
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.VolatileStatusCure>()
			.firstOrNull { status in it.statuses }
			?: return state
		val cured = if (effect.consumesItem) {
			participant.clearVolatileStatus(status).consumeHeldItem()
		} else {
			participant.clearVolatileStatus(status)
		}
		return state
			.replaceParticipant(cured)
			.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
	}

	/**
	 * 判断临时状态是否会在附加前被稳定免疫规则阻止。
	 *
	 * 薄雾场地只阻止接地成员获得混乱；特性和道具的 [BattleAbilityEffect.VolatileStatusImmunity] /
	 * [BattleItemEffect.VolatileStatusImmunity] 可以阻止资料层声明的任意临时状态，例如畏缩或混乱。
	 * 阻止发生在混乱持续时间随机数消费和畏缩行动前检查之前，保证 replay 随机脚本稳定。
	 */
	private fun blockedVolatileStatusReason(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
		skill: BattleSkillSlot? = null,
	): BattleStatusBlockReason? =
		when {
			volatileStatusBlockedByTerrain(state, recipient, status) -> BattleStatusBlockReason.TERRAIN
			skill != null && substituteBlocksOpponentEffect(state, actorId, recipient.actorId, skill) -> BattleStatusBlockReason.SUBSTITUTE
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				volatileStatusBlockedByAbility(recipient, status) -> BattleStatusBlockReason.ABILITY
			volatileStatusBlockedByItem(recipient, status) -> BattleStatusBlockReason.ITEM
			else -> null
		}

	/**
	 * 判断当前场地是否阻止目标获得临时状态。
	 */
	private fun volatileStatusBlockedByTerrain(
		state: BattleState,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
	): Boolean =
		state.isActive(recipient.actorId) &&
			recipient.grounded &&
			state.environment.terrain == BattleTerrain.MISTY &&
			status == BattleVolatileStatus.CONFUSION

	/**
	 * 判断目标特性是否稳定免疫指定临时状态。
	 */
	private fun volatileStatusBlockedByAbility(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		recipient.abilityEffects.any { effect ->
			effect is BattleAbilityEffect.VolatileStatusImmunity && status in effect.statuses
		}

	/**
	 * 判断目标携带道具是否稳定免疫指定临时状态。
	 */
	private fun volatileStatusBlockedByItem(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		recipient.itemEffects.any { effect ->
			effect is BattleItemEffect.VolatileStatusImmunity && status in effect.statuses
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
		if (skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) {
			return state
		}
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
	 * 处理造成伤害后的携带道具效果。
	 *
	 * 当前 hook 覆盖生命宝珠类道具：成功造成伤害后按使用者最大 HP 固定比例反伤。贝壳之铃类道具需要读取
	 * 整次技能的总实际伤害，因此由多段命中循环之后的 [applyPostMoveDamageDealtHealingItem] 单独处理。
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
			.fold(state) { current, effect ->
				when (effect) {
					is BattleItemEffect.DamageBoostWithRecoil -> applyDamageBoostRecoilItem(current, actorId, effect)
					is BattleItemEffect.ChargeSkipOnce,
					is BattleItemEffect.ChoiceSkillLock,
					is BattleItemEffect.DamageClassPowerBoost,
					is BattleItemEffect.DamageDealtHeal,
					is BattleItemEffect.ElementDamageBoost,
					is BattleItemEffect.ElementDamageReduction,
					is BattleItemEffect.HeldEndTurnHeal,
					is BattleItemEffect.LowHpHeal,
					is BattleItemEffect.MajorStatusCure,
					is BattleItemEffect.MajorStatusImmunity,
					is BattleItemEffect.SideDamageReductionDurationExtension,
					is BattleItemEffect.SuperEffectiveDamageBoost,
					is BattleItemEffect.SurviveFatalDamageAtFullHp,
					is BattleItemEffect.TerrainDurationExtension,
					is BattleItemEffect.VolatileStatusCure,
					is BattleItemEffect.VolatileStatusImmunity,
					is BattleItemEffect.WeatherDamageImmunity,
					is BattleItemEffect.WeatherDurationExtension -> current
				}
			}
	}

	/**
	 * 处理整次技能结束后的“按造成伤害回复”携带道具效果。
	 *
	 * 公开规则中贝壳之铃类道具按本次技能总实际伤害回复，而不是每一段命中各自回复。因此该 hook 放在多段循环
	 * 之后，只读取 [damageDealtByMove] 汇总出的普通伤害和替身伤害总量。变化类技能、未造成实际 HP 损失的
	 * 技能、已经倒下或满 HP 的使用者都会自然短路；道具本身不被消费，也不改变锁招、反伤或目标侧触发流程。
	 */
	private fun applyPostMoveDamageDealtHealingItem(
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
			.fold(state) { current, effect ->
				when (effect) {
					is BattleItemEffect.DamageDealtHeal -> applyDamageDealtHealingItem(
						state = current,
						actorId = actorId,
						damageAmount = damageAmount,
						effect = effect,
					)
					is BattleItemEffect.ChargeSkipOnce,
					is BattleItemEffect.ChoiceSkillLock,
					is BattleItemEffect.DamageBoostWithRecoil,
					is BattleItemEffect.DamageClassPowerBoost,
					is BattleItemEffect.ElementDamageBoost,
					is BattleItemEffect.ElementDamageReduction,
					is BattleItemEffect.HeldEndTurnHeal,
					is BattleItemEffect.LowHpHeal,
					is BattleItemEffect.MajorStatusCure,
					is BattleItemEffect.MajorStatusImmunity,
					is BattleItemEffect.SideDamageReductionDurationExtension,
					is BattleItemEffect.SuperEffectiveDamageBoost,
					is BattleItemEffect.SurviveFatalDamageAtFullHp,
					is BattleItemEffect.TerrainDurationExtension,
					is BattleItemEffect.VolatileStatusCure,
					is BattleItemEffect.VolatileStatusImmunity,
					is BattleItemEffect.WeatherDamageImmunity,
					is BattleItemEffect.WeatherDurationExtension -> current
				}
			}
	}

	/**
	 * 处理生命宝珠类道具造成的最大 HP 比例反伤。
	 *
	 * 生命宝珠类现代主系列规则不是“按造成伤害反伤”，而是“按使用者最大 HP 反伤”；这个函数故意只读取
	 * 成员快照中的 `maxHp`，避免伤害随机浮动、属性倍率或屏障倍率改变反伤数值。
	 */
	private fun applyDamageBoostRecoilItem(
		state: BattleState,
		actorId: String,
		effect: BattleItemEffect.DamageBoostWithRecoil,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasIndirectDamageImmunity()) {
			return state
		}
		val recoil = (actor.maxHp / effect.recoilDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.currentHp)
		return state
			.replaceParticipant(actor.receiveDamage(recoil))
			.appendEvent(
				BattleEvent.RecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = recoil,
				),
			)
	}

	/**
	 * 处理按实际造成伤害量回复的携带道具。
	 *
	 * 回复量使用本次实际 HP 损失向下取整，最少 1 点，并夹取到使用者缺失 HP。调用方已经保证 `damageAmount > 0`，
	 * 因此本函数只需要处理使用者倒下或已经满 HP 的情况。
	 */
	private fun applyDamageDealtHealingItem(
		state: BattleState,
		actorId: String,
		damageAmount: Int,
		effect: BattleItemEffect.DamageDealtHeal,
	): BattleState {
			val actor = state.participant(actorId) ?: return state
			if (!actor.canBattle() || actor.currentHp == actor.maxHp || healingBlocked(actor)) {
				return state
			}
		val healAmount = (damageAmount / effect.healDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.maxHp - actor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.heal(healAmount))
			.appendEvent(
				BattleEvent.HealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 处理低体力一次性回复类携带道具。
	 *
	 * 现代主系列中，这类道具在持有者受到伤害后，如果 HP 降到触发线及以下且仍未倒下，会立刻回复并被消费。
	 * 这里把触发点集中成一个小函数，供普通伤害、混乱自伤、异常状态伤害和天气伤害复用。函数不会处理主动使用道具、
	 * 回复封锁、紧张感等更复杂来源；这些规则后续会以结构化字段加入，而不是让调用方传入自由文本开关。
	 */
	private fun applyLowHpHealingItem(state: BattleState, actorId: String): BattleState {
			val participant = state.participant(actorId) ?: return state
			if (!participant.canBattle() || participant.currentHp == participant.maxHp || healingBlocked(participant)) {
				return state
			}
		val effect = participant.itemEffects.filterIsInstance<BattleItemEffect.LowHpHeal>().firstOrNull() ?: return state
		if (!effect.shouldTrigger(participant.currentHp, participant.maxHp)) {
			return state
		}
		val healAmount = effect.healAmount(participant.maxHp)
			.coerceAtMost(participant.maxHp - participant.currentHp)
		if (healAmount <= 0) {
			return state
		}
		val healed = participant.heal(healAmount).consumeHeldItem()
		return state
			.replaceParticipant(healed)
			.appendEvent(
				BattleEvent.HealingApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					amount = healAmount,
				),
			)
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
	 * 推进跨回合临时状态的持续回合。
	 *
	 * 畏缩在前一个清理步骤已经静默消失；混乱按行动前计数，不在回合末递减。当前这里推进回复封锁、挑衅和定身法：
	 * 每个回合末减少 1，归零时追加 [BattleEvent.VolatileStatusCleared]，让 replay 能看到状态自然结束的时间点。
	 */
	private fun advanceEndTurnVolatileStatusDurations(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				val afterHealBlock = advanceEndTurnVolatileStatusDuration(
					state = current,
					participant = latest,
					status = BattleVolatileStatus.HEAL_BLOCK,
					turnsRemaining = latest.healBlockTurnsRemaining,
					decrement = BattleParticipant::decrementHealBlockEndTurn,
				)
				val latestAfterHealBlock = afterHealBlock.participant(participant.actorId) ?: return@fold afterHealBlock
				val afterTaunt = advanceEndTurnVolatileStatusDuration(
					state = afterHealBlock,
					participant = latestAfterHealBlock,
					status = BattleVolatileStatus.TAUNT,
					turnsRemaining = latestAfterHealBlock.tauntTurnsRemaining,
					decrement = BattleParticipant::decrementTauntEndTurn,
				)
				val latestAfterTaunt = afterTaunt.participant(participant.actorId) ?: return@fold afterTaunt
				advanceEndTurnVolatileStatusDuration(
					state = afterTaunt,
					participant = latestAfterTaunt,
					status = BattleVolatileStatus.DISABLE,
					turnsRemaining = latestAfterTaunt.disabledSkillTurnsRemaining,
					decrement = BattleParticipant::decrementDisableEndTurn,
				)
			}

	/**
	 * 推进一个按回合末递减的临时状态计数。
	 *
	 * 该 helper 只处理“计数大于 0 时递减，归零时追加解除事件”的共同行为；具体状态字段仍由
	 * [BattleParticipant] 的专用方法维护，避免用字符串或反射读写运行态。
	 */
	private fun advanceEndTurnVolatileStatusDuration(
		state: BattleState,
		participant: BattleParticipant,
		status: BattleVolatileStatus,
		turnsRemaining: Int,
		decrement: BattleParticipant.() -> BattleParticipant,
	): BattleState {
		if (turnsRemaining <= 0) {
			return state
		}
		val updated = participant.decrement()
		val advanced = state.replaceParticipant(updated)
		return if (turnsRemaining == 1) {
			advanced.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
		} else {
			advanced
		}
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
				if (!latest.canBattle() || latest.hasIndirectDamageImmunity()) {
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
						.let { afterDamage ->
							val afterLowHpItem = applyLowHpHealingItem(afterDamage, afterStatusCounter.actorId)
							val latestAfterItem = afterLowHpItem.participant(afterStatusCounter.actorId) ?: afterStatusCounter
							afterLowHpItem.handleFaintAndResult(latestAfterItem)
						}
				}
			}
		return if (afterResidual.result != null) {
			afterResidual
		} else {
			val afterWeather = applyEndTurnWeatherEffects(afterResidual)
			if (afterWeather.result != null) {
				afterWeather
			} else {
				val afterWeatherHealing = applyEndTurnWeatherHealing(afterWeather)
				applyEndTurnHealing(applyEndTurnTerrainEffects(afterWeatherHealing))
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
				if (!latest.canBattle() || latest.immuneToWeatherDamage(current, BattleWeather.SANDSTORM)) {
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
						.let { afterDamage ->
							val afterLowHpItem = applyLowHpHealingItem(afterDamage, damaged.actorId)
							val latestAfterItem = afterLowHpItem.participant(damaged.actorId) ?: damaged
							afterLowHpItem.handleFaintAndResult(latestAfterItem)
						}
				}
			}
	}

	/**
	 * 处理天气阶段的特性回复。
	 *
	 * 现代规则中，部分特性会在指定天气存在时于回合末按最大 HP 固定比例回复。这里放在天气伤害之后、
	 * 场地回复之前，保持事件流阶段清晰：天气先造成或免除伤害，再处理同属天气阶段的回复，最后进入场地和道具。
	 */
	private fun applyEndTurnWeatherHealing(state: BattleState): BattleState {
		val weather = state.environment.weather
		if (weather == BattleWeather.NONE) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
					if (!latest.canBattle() || latest.currentHp == latest.maxHp || healingBlocked(latest)) {
						current
					} else {
					val healEffects = latest.abilityEffects
						.filterIsInstance<BattleAbilityEffect.WeatherEndTurnHeal>()
						.filter { weather in it.weathers }
					healEffects.fold(current) { healingState, effect ->
						val currentParticipant = healingState.participant(latest.actorId)
						if (
								currentParticipant == null ||
								!currentParticipant.canBattle() ||
								currentParticipant.currentHp == currentParticipant.maxHp ||
								healingBlocked(currentParticipant)
							) {
								healingState
							} else {
							val healAmount = (currentParticipant.maxHp / effect.healDenominator).coerceAtLeast(1)
								.coerceAtMost(currentParticipant.maxHp - currentParticipant.currentHp)
							healingState
								.replaceParticipant(currentParticipant.heal(healAmount))
								.appendEvent(
									BattleEvent.WeatherHealingApplied(
										turnNumber = healingState.turnNumber,
										actorId = currentParticipant.actorId,
										weather = weather,
										amount = healAmount,
									),
								)
						}
					}
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
					if (!latest.canBattle() || !latest.grounded || latest.currentHp == latest.maxHp || healingBlocked(latest)) {
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
					if (!latest.canBattle() || latest.currentHp == latest.maxHp || healingBlocked(latest)) {
						current
					} else {
					latest.itemEffects
						.filterIsInstance<BattleItemEffect.HeldEndTurnHeal>()
							.fold(current) { healingState, effect ->
								val currentParticipant = healingState.participant(latest.actorId) ?: return@fold healingState
								if (healingBlocked(currentParticipant)) {
									return@fold healingState
								}
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
		val afterTerrain = advanceTerrainDuration(afterWeather)
		return advanceFieldSpeedOrderDuration(afterTerrain)
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
	 * 推进全场速度顺序效果持续回合。
	 *
	 * 戏法空间等全场速度顺序效果在回合末按天气/场地同样的生命周期递减，耗尽时恢复普通速度排序并记录事件。
	 */
	private fun advanceFieldSpeedOrderDuration(state: BattleState): BattleState {
		val effect = state.environment.fieldSpeedOrderEffect ?: return state
		val nextEffect = effect.advanceTurn()
		return if (nextEffect == null) {
			state
				.copy(environment = state.environment.copy(fieldSpeedOrderEffect = null))
				.appendEvent(
					BattleEvent.FieldSpeedOrderEnded(
						turnNumber = state.turnNumber,
						kind = effect.kind,
					),
				)
		} else {
			state.copy(environment = state.environment.copy(fieldSpeedOrderEffect = nextEffect))
		}
	}

	/**
	 * 应用格式级回合上限裁定。
	 *
	 * 回合上限只在完整回合末检查，因此最后一回合的主要异常伤害、天气/场地副作用和持续时间推进都会先结算。
	 * 当前格式快照没有声明点数裁定规则时，到达上限按平局结束，`winningSideId=null` 明确表示没有胜方。
	 */
	private fun applyTurnLimit(state: BattleState): BattleState {
		val maxTurns = state.format.maxTurns ?: return state
		if (state.turnNumber < maxTurns) {
			return state
		}
		val result = BattleResult(winningSideId = null, reason = MAX_TURNS_REACHED_REASON)
		return state
			.copy(result = result)
			.appendEvent(
				BattleEvent.BattleEnded(
					turnNumber = state.turnNumber,
					winningSideId = result.winningSideId,
					reason = result.reason,
				),
			)
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
	 * 速度先应用能力阶级，再应用麻痹减半，最后应用天气触发的特性倍率。天气速度特性放在这里而不是动作排序外部，
	 * 是为了替换排序、技能排序和未来追击类规则共享同一套有效速度定义。
	 */
	private fun effectiveSpeed(state: BattleState, participant: BattleParticipant): Int {
		val staged = statStageModifiers.modifiedBattleStat(
			participant.speed,
			participant.statStage(BattleStat.SPEED),
		)
		val afterStatus = if (participant.majorStatus == BattleMajorStatus.PARALYSIS) {
			(staged / 2).coerceAtLeast(1)
		} else {
			staged
		}
		return floor(
			afterStatus *
				weatherSpeedMultiplier(state, participant) *
				terrainSpeedMultiplier(state, participant) *
				itemSpeedMultiplier(participant) *
				sideSpeedModifierMultiplier(state, participant),
		)
			.toInt()
			.coerceAtLeast(1)
	}

	/**
	 * 返回当前环境下行动队列使用的速度比较器。
	 *
	 * 普通环境中高有效速度先行动；戏法空间存在时只反转速度比较方向，优先度、锁招续回合和同速随机仍沿用
	 * 原有排序层次。
	 */
	private fun speedComparator(state: BattleState): Comparator<Int> =
		if (state.environment.fieldSpeedOrderEffect?.kind?.reversesSpeedOrder == true) {
			compareBy<Int> { it }
		} else {
			compareByDescending<Int> { it }
		}

	/**
	 * 计算技能行动的有效优先度和随优先度提升产生的目标免疫标记。
	 *
	 * 变化类先制度特性会同时影响行动排序、精神场地/先制阻挡特性的判断，以及现代规则中恶属性目标对这类
	 * 对手变化技能的免疫。把这些事实集中成上下文，可以保证同一次行动在所有判断点使用同一份结论。
	 */
	private fun skillPriorityContext(actor: BattleParticipant, skill: BattleSkillSlot): SkillPriorityContext {
		if (skill.damageClass != BattleDamageClass.STATUS) {
			return SkillPriorityContext(effectivePriority = skill.priority)
		}
		val effects = actor.abilityEffects.filterIsInstance<BattleAbilityEffect.StatusSkillPriorityBoost>()
		val priorityDelta = effects.maxOfOrNull { it.priorityDelta } ?: 0
		return SkillPriorityContext(
			effectivePriority = skill.priority + priorityDelta,
			statusPriorityBoostedByAbility = priorityDelta > 0,
			darkElementTargetsImmune = priorityDelta > 0 && effects.any { it.darkElementTargetsImmune },
		)
	}

	/**
	 * 计算天气触发的速度倍率。
	 */
	private fun weatherSpeedMultiplier(state: BattleState, participant: BattleParticipant): Double =
		participant.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.WeatherSpeedMultiplier ->
					if (state.environment.weather == effect.weather) multiplier * effect.multiplier else multiplier
				is BattleAbilityEffect.ContactBasedSkillDamageBoost,
				is BattleAbilityEffect.ContactStatusOnAttacker,
				is BattleAbilityEffect.CriticalHitImmunity,
				is BattleAbilityEffect.AttackingStatMultiplier,
				is BattleAbilityEffect.DamageClassDamageReduction,
				is BattleAbilityEffect.DefendingStatMultiplier,
				is BattleAbilityEffect.SameElementBonusOverride,
				is BattleAbilityEffect.ElementSkillAbsorbHeal,
				is BattleAbilityEffect.ElementSkillAbsorbStatStage,
				is BattleAbilityEffect.ElementSkillDamageBoost,
				is BattleAbilityEffect.FullHpDamageReduction,
				is BattleAbilityEffect.IgnoreOpponentAccuracyStatStages,
				is BattleAbilityEffect.IgnoreOpponentDamageStatStages,
				is BattleAbilityEffect.IgnoreTargetAbilityEffects,
				is BattleAbilityEffect.IndirectDamageImmunity,
				is BattleAbilityEffect.LowHpElementDamageBoost,
				is BattleAbilityEffect.MajorStatusImmunity,
				is BattleAbilityEffect.PriorityMoveImmunityForSide,
				is BattleAbilityEffect.PunchBasedSkillDamageBoost,
				is BattleAbilityEffect.SkillRecoilDamageImmunity,
				is BattleAbilityEffect.SlicingBasedSkillDamageBoost,
				is BattleAbilityEffect.SoundBasedSkillDamageBoost,
				is BattleAbilityEffect.SoundBasedSkillDamageReduction,
				is BattleAbilityEffect.SoundBasedSkillImmunity,
				is BattleAbilityEffect.StatusSkillPriorityBoost,
				is BattleAbilityEffect.SwitchInStatStageChange,
				is BattleAbilityEffect.SurviveFatalDamageAtFullHp,
				is BattleAbilityEffect.SuperEffectiveDamageReduction,
				is BattleAbilityEffect.SwitchInTerrainChange,
				is BattleAbilityEffect.SwitchInWeatherChange,
				is BattleAbilityEffect.TerrainSpeedMultiplier,
				is BattleAbilityEffect.VolatileStatusImmunity,
				is BattleAbilityEffect.WeatherDamageImmunity,
				is BattleAbilityEffect.WeatherElementDamageBoost,
				is BattleAbilityEffect.WeatherEndTurnHeal -> multiplier
			}
		}

	/**
	 * 计算场地触发的速度倍率。
	 */
	private fun terrainSpeedMultiplier(state: BattleState, participant: BattleParticipant): Double =
		participant.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.TerrainSpeedMultiplier ->
					if (state.environment.terrain == effect.terrain) multiplier * effect.multiplier else multiplier
				is BattleAbilityEffect.ContactBasedSkillDamageBoost,
				is BattleAbilityEffect.ContactStatusOnAttacker,
				is BattleAbilityEffect.CriticalHitImmunity,
				is BattleAbilityEffect.AttackingStatMultiplier,
				is BattleAbilityEffect.DamageClassDamageReduction,
				is BattleAbilityEffect.DefendingStatMultiplier,
				is BattleAbilityEffect.SameElementBonusOverride,
				is BattleAbilityEffect.ElementSkillAbsorbHeal,
				is BattleAbilityEffect.ElementSkillAbsorbStatStage,
				is BattleAbilityEffect.ElementSkillDamageBoost,
				is BattleAbilityEffect.FullHpDamageReduction,
				is BattleAbilityEffect.IgnoreOpponentAccuracyStatStages,
				is BattleAbilityEffect.IgnoreOpponentDamageStatStages,
				is BattleAbilityEffect.IgnoreTargetAbilityEffects,
				is BattleAbilityEffect.IndirectDamageImmunity,
				is BattleAbilityEffect.LowHpElementDamageBoost,
				is BattleAbilityEffect.MajorStatusImmunity,
				is BattleAbilityEffect.PriorityMoveImmunityForSide,
				is BattleAbilityEffect.PunchBasedSkillDamageBoost,
				is BattleAbilityEffect.SkillRecoilDamageImmunity,
				is BattleAbilityEffect.SlicingBasedSkillDamageBoost,
				is BattleAbilityEffect.SoundBasedSkillDamageBoost,
				is BattleAbilityEffect.SoundBasedSkillDamageReduction,
				is BattleAbilityEffect.SoundBasedSkillImmunity,
				is BattleAbilityEffect.StatusSkillPriorityBoost,
				is BattleAbilityEffect.SwitchInStatStageChange,
				is BattleAbilityEffect.SurviveFatalDamageAtFullHp,
				is BattleAbilityEffect.SuperEffectiveDamageReduction,
				is BattleAbilityEffect.SwitchInTerrainChange,
				is BattleAbilityEffect.SwitchInWeatherChange,
				is BattleAbilityEffect.VolatileStatusImmunity,
				is BattleAbilityEffect.WeatherDamageImmunity,
				is BattleAbilityEffect.WeatherElementDamageBoost,
				is BattleAbilityEffect.WeatherEndTurnHeal,
				is BattleAbilityEffect.WeatherSpeedMultiplier -> multiplier
			}
		}

	/**
	 * 计算携带道具提供的速度倍率。
	 *
	 * 当前只接入讲究类速度道具。其它道具效果不参与速度排序，保持乘数不变；未来若加入铁球、围巾以外的速度道具，
	 * 也应继续通过结构化效果表达具体倍率，而不是在行动排序中判断道具 ID。
	 */
	private fun itemSpeedMultiplier(participant: BattleParticipant): Double =
		participant.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.ChoiceSkillLock -> multiplier * effect.speedMultiplier
				is BattleItemEffect.ChargeSkipOnce,
				is BattleItemEffect.DamageClassPowerBoost,
				is BattleItemEffect.DamageBoostWithRecoil,
				is BattleItemEffect.DamageDealtHeal,
				is BattleItemEffect.ElementDamageBoost,
				is BattleItemEffect.ElementDamageReduction,
				is BattleItemEffect.HeldEndTurnHeal,
				is BattleItemEffect.LowHpHeal,
				is BattleItemEffect.MajorStatusCure,
				is BattleItemEffect.MajorStatusImmunity,
				is BattleItemEffect.SideDamageReductionDurationExtension,
				is BattleItemEffect.SuperEffectiveDamageBoost,
				is BattleItemEffect.SurviveFatalDamageAtFullHp,
				is BattleItemEffect.TerrainDurationExtension,
				is BattleItemEffect.VolatileStatusCure,
				is BattleItemEffect.VolatileStatusImmunity,
				is BattleItemEffect.WeatherDurationExtension,
				is BattleItemEffect.WeatherDamageImmunity -> multiplier
			}
		}

	/**
	 * 计算一侧场上效果提供的速度倍率。
	 *
	 * 顺风等效果挂在战斗侧上，所有当前属于这一侧的成员都共享倍率。这里只读取结构化模型，不识别技能 ID、
	 * 数据库字段或本地化文本；运行时资料到引擎模型的转换由 battle-rules 适配层负责。
	 */
	private fun sideSpeedModifierMultiplier(state: BattleState, participant: BattleParticipant): Double =
		state.sideOf(participant.actorId)
			?.speedModifiers
			?.fold(1.0) { multiplier, modifier -> multiplier * modifier.multiplier }
			?: 1.0

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
	 * 第一阶段只要某一方没有可战斗成员就立即结束战斗。若双方都没有剩余成员，则以无胜方结果结束；后续替换
	 * 请求和复杂计分裁定规则会继续扩展这里。
	 */
	private fun BattleState.handleFaintAndResult(target: BattleParticipant): BattleState {
		return handleFaintsAndResult(listOf(target))
	}

	private fun BattleState.handleFaintsAndResult(targets: List<BattleParticipant>): BattleState {
		val withFaint = targets
			.distinctBy { it.actorId }
			.filterNot { it.canBattle() }
			.fold(this) { current, target ->
				current.appendEvent(BattleEvent.ParticipantFainted(turnNumber, target.actorId))
			}
		val defeatedSides = withFaint.sides.filterNot { it.hasRemainingParticipant() }
		if (defeatedSides.isEmpty()) {
			return withFaint
		}
		val remainingSides = withFaint.sides.filter { it !in defeatedSides }
		val winningSideId = remainingSides.singleOrNull()?.sideId
		val result = BattleResult(
			winningSideId = winningSideId,
			reason = if (winningSideId == null) "all-sides-fainted" else "all-opponents-fainted",
		)
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

	private sealed interface DirectDamageAttempt {
		data class Hit(
			val amount: Int,
			val faintActorAfterHit: Boolean = false,
		) : DirectDamageAttempt

		data class Failed(
			val reason: String,
		) : DirectDamageAttempt
	}

	private data class SkillActionInput(
		val action: BattleAction.UseSkill,
		val source: SkillActionSource,
	)

	private data class ActionPlan(
		val action: BattleAction.UseSkill,
		val actor: BattleParticipant,
		val skill: BattleSkillSlot,
		val source: SkillActionSource,
		val priorityContext: SkillPriorityContext,
	)

	private data class SkillPriorityContext(
		val effectivePriority: Int,
		val statusPriorityBoostedByAbility: Boolean = false,
		val darkElementTargetsImmune: Boolean = false,
	)

	private enum class SkillActionSource {
		SUBMITTED,
		LOCKED_CONTINUATION,
		CHARGED_RELEASE,
	}

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

	private data class FatalDamageSurvivalResult(
		val target: BattleParticipant,
		val damageAmount: Int,
		val event: BattleEvent.FatalDamageSurvived? = null,
	)

	private data class HeldItemDamageReduction(
		val target: BattleParticipant,
		val event: BattleEvent.DamageReducedByItem,
	)

	private companion object {
		private const val CONFUSION_BASE_POWER = 40
		private const val CONFUSION_SELF_DAMAGE_CHANCE_PERCENT = 33
		private const val FREEZE_THAW_CHANCE_PERCENT = 20
		private const val DISABLE_TURNS = 4
		private const val HEAL_BLOCK_TURNS = 5
		private const val TAUNT_TURNS = 3
		private const val MAX_TURNS_REACHED_REASON = "max-turns-reached"
		private const val MULTI_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 2.0 / 3.0
		private const val PARALYSIS_FULLY_PARALYZED_CHANCE_PERCENT = 25
		private const val SINGLE_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 0.5
		private const val SPIKES_ONE_LAYER_DAMAGE_DENOMINATOR = 8
		private const val SPIKES_TWO_LAYER_DAMAGE_DENOMINATOR = 6
		private const val SPIKES_THREE_LAYER_DAMAGE_DENOMINATOR = 4
		private const val STEALTH_ROCK_DAMAGE_DENOMINATOR = 8.0
		private const val WEATHER_DAMAGE_DENOMINATOR = 16
	}
}
