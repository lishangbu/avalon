package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
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
import io.github.lishangbu.battleengine.model.SwitchPreventionReason
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
 *
 * 本类继续采用“显式阶段状态机”，而不是完整事件驱动调度器。原因是战斗规则最敏感的是阶段顺序：替换必须先于
 * 技能行动，行动前状态必须先于 PP 消耗和命中判定，伤害后道具、倒下检查、回合末伤害、天气/场地持续时间也都
 * 有严格先后。这里的 [BattleEvent] 是已经发生的事实记录，用于 replay、测试断言和调试；它不是用来再次分发
 * 规则 hook 的事件总线。拆出的 resolver 只能封装某个阶段内部的细节，不能重新决定跨阶段顺序。
 */
class BattleEngine(
	private val damageCalculator: BattleDamageCalculator = BattleDamageCalculator(),
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
) {
	private val actionOrdering = BattleActionOrdering(statStageModifiers)
	private val actionPlanner = BattleTurnActionPlanner(actionOrdering)
	private val environmentEffects = BattleEnvironmentEffects()
	private val fieldEffects = BattleFieldEffects()
	private val chargeMoves = BattleChargeMoves()
	/**
	 * 行动前状态阻止 resolver。
	 *
	 * 该阶段只会推进 [BattleState]，不会修改 `TurnContext` 中的保护集合或其它回合内编排字段。因此它从主类抽出
	 * 后，执行技能流程只需要把返回的状态重新放回当前上下文。混乱自伤后可能触发低体力回复道具，所以这里把主
	 * 状态机的低体力道具处理函数作为回调传入，避免出现“普通伤害一套道具顺序，混乱自伤另一套道具顺序”。
	 */
	private val beforeMoveEffects = BattleBeforeMoveEffects(
		statStageModifiers = statStageModifiers,
		lowHpItemHealing = { state, actorId -> applyLowHpHealingItem(state, actorId) },
	)

	private val switchInAbilityEffects = BattleSwitchInAbilityEffects(actionOrdering, environmentEffects)
	/**
	 * 回合末临时状态推进器。
	 *
	 * 主状态机仍负责决定回合末阶段顺序：先结算伤害/回复，再清理不会跨回合保留的状态，再推进持续时间，
	 * 然后推进天气、场地、一侧状态和回合上限。这个对象只处理成员运行态上那些纯机械的临时状态字段，避免
	 * [BattleEngine.resolveTurn] 被大量 `flatMap activeParticipants + decrement` 细节淹没。
	 */
	private val endTurnVolatileStatuses = BattleEndTurnVolatileStatuses()
	/**
	 * 回合末环境与持续伤害结算器。
	 *
	 * 主类只保留“回合末阶段发生在技能阶段之后、持续时间推进之前”这个编排事实；具体异常伤害、束缚伤害、
	 * 天气伤害、天气/场地/道具回复和回合上限收口都委托给该对象。低体力回复道具仍回调主类中的实现，保证所有
	 * 伤害入口共享同一套道具消费与回复封锁判断。
	 */
	private val endTurnEffects = BattleEndTurnEffects(
		lowHpItemHealing = { state, actorId -> applyLowHpHealingItem(state, actorId) },
	)

	/**
	 * 入场陷阱结算从主状态机拆出，但仍复用主状态机里的主要异常阻止判断和低体力道具回复判断。
	 *
	 * 这样做的边界比较刻意：入场陷阱本身是一个独立阶段，适合从 4000 多行的主类里移走；但“毒菱是否被属性、
	 * 场地、特性或道具阻止”和“入场伤害后是否触发低体力回复道具”并不是入场陷阱专属规则。如果在新 resolver
	 * 里复制这些判断，后续修正状态免疫或道具回复顺序时就会出现两套实现。这里用闭包把共享判断接进 resolver，
	 * 保持行为只有一份，同时让换入阶段的主流程更短。
	 */
	private val entryHazardEffects = BattleEntryHazardEffects(
		majorStatusBlockReason = { state, actorId, recipient, status ->
			blockedMajorStatusReason(state, actorId, recipient, status)
		},
		lowHpItemHealing = { state, actorId -> applyLowHpHealingItem(state, actorId) },
	)

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
		return switchInAbilityEffects.applyInitial(started)
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
		val skillActions = actionPlanner.skillActionsForTurn(afterSwitches, actions.filterIsInstance<BattleAction.UseSkill>())
		val orderedActions = actionPlanner.orderSkillActions(afterSwitches, skillActions, random)
		val resolvedContext = orderedActions.fold(TurnContext(afterSwitches)) { current, plan ->
			if (current.state.result != null) current else executeUseSkill(current, plan, random)
		}
		val resolved = endTurnVolatileStatuses.resetProtectionChains(
			state = resolvedContext.state,
			successfulProtectionActorIds = resolvedContext.successfulProtectionActorIds,
		)
		val afterEndTurnEffects = resolved.result?.let { resolved } ?: endTurnEffects.apply(resolved)
		val afterEndTurnVolatileStatuses = afterEndTurnEffects.result?.let { afterEndTurnEffects }
			?: endTurnVolatileStatuses.clearEndTurnOnlyStatuses(afterEndTurnEffects)
		val afterEndTurnVolatileStatusDurations = afterEndTurnVolatileStatuses.result?.let { afterEndTurnVolatileStatuses }
			?: endTurnVolatileStatuses.advanceEndTurnDurations(afterEndTurnVolatileStatuses)
		val afterEnvironmentDurations = afterEndTurnVolatileStatusDurations.result?.let { afterEndTurnVolatileStatusDurations }
			?: environmentEffects.advanceDurations(afterEndTurnVolatileStatusDurations)
		val afterSideConditionDurations = afterEnvironmentDurations.result?.let { afterEnvironmentDurations }
			?: afterEnvironmentDurations.advanceSideConditionDurations()
		val afterTurnLimit = afterSideConditionDurations.result?.let { afterSideConditionDurations }
			?: endTurnEffects.applyTurnLimit(afterSideConditionDurations)
		return afterTurnLimit.result?.let { afterTurnLimit }
			?: afterTurnLimit.appendEvent(BattleEvent.TurnEnded(nextTurnNumber))
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
			if (actor.canBattle() && endTurnEffects.isBindingSourceActive(current, actor)) {
				val sourceActorId = actor.boundByActorId ?: return@fold current
				return@fold current.preventSwitch(
					actor = actor,
					reason = SwitchPreventionReason.BINDING,
					sourceActorId = sourceActorId,
					turnsRemainingBefore = actor.bindingTurnsRemaining,
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
			val afterBindingSourceCleared = endTurnEffects.clearBindingsFromSource(withSwitchEvent, actor.actorId)
			val afterEntryHazards = entryHazardEffects.applyOnSwitchIn(
				state = afterBindingSourceCleared,
				sideId = side.sideId,
				actorId = plan.action.targetActorId,
			)
			switchInAbilityEffects.apply(afterEntryHazards, plan.action.targetActorId)
		}
	}

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
		val beforeMove = beforeMoveEffects.resolve(state, actor, plan.skill, random)
		val beforeMoveContext = context.copy(state = beforeMove.state)
		if (beforeMove.blocked) {
			return when (plan.source) {
				SkillActionSource.LOCKED_CONTINUATION -> beforeMoveContext.copy(
					state = endLockedMoveAfterDisruption(beforeMoveContext.state, actor.actorId, plan.skill, random),
				)
				SkillActionSource.CHARGED_RELEASE -> beforeMoveContext.copy(
					state = chargeMoves.endAfterDisruption(beforeMoveContext.state, actor.actorId, plan.skill),
				)
				SkillActionSource.SUBMITTED -> beforeMoveContext
			}
		}
		val actionState = beforeMoveContext.state
		val readyActor = actionState.participant(action.actorId) ?: return beforeMoveContext
		val skill = readyActor.skillSlot(action.skillId) ?: return beforeMoveContext
		val targets = targetsForSkill(actionState, readyActor.actorId, action.targetActorId, skill, random)
		if (targets.isEmpty()) {
			return when (plan.source) {
				SkillActionSource.LOCKED_CONTINUATION -> beforeMoveContext.copy(
					state = endLockedMoveAfterDisruption(actionState, readyActor.actorId, skill, random),
				)
				SkillActionSource.CHARGED_RELEASE -> beforeMoveContext.copy(
					state = chargeMoves.endAfterDisruption(actionState, readyActor.actorId, skill),
				)
				SkillActionSource.SUBMITTED -> beforeMoveContext
			}
		}
		if (plan.source == SkillActionSource.SUBMITTED) {
			require(skill.remainingPp > 0) { "skill has no remaining PP: ${skill.skillId}" }
		}

		val stateBeforeUse = if (plan.source == SkillActionSource.CHARGED_RELEASE) {
			chargeMoves.releaseChargedSkill(actionState, readyActor, skill, targets.first().actorId)
		} else {
			actionState
		}
		val readyActorBeforePp = stateBeforeUse.participant(action.actorId) ?: return beforeMoveContext
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
			if (
				chargeMoves.requiresChargeBeforeUse(skill, actionState.environment.weather) &&
				plan.source == SkillActionSource.SUBMITTED
			) {
				chargeMoves.skipWithHeldItem(usedState, readyActorBeforePp.actorId, skill)
					?: return beforeMoveContext.copy(
						state = chargeMoves.startCharge(
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
				return beforeMoveContext.copy(
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
			return beforeMoveContext.copy(
				state = stateAfterChargeDecision
					.replaceParticipant(protectedActor)
					.appendEvent(
						BattleEvent.ProtectionStarted(
							turnNumber = actionState.turnNumber,
							actorId = readyActor.actorId,
							skillId = skill.skillId,
						),
					),
				protectedActorIds = beforeMoveContext.protectedActorIds + readyActor.actorId,
				successfulProtectionActorIds = beforeMoveContext.successfulProtectionActorIds + readyActor.actorId,
			)
		}

		val targetMultiplier = targetDamageMultiplier(skill, targets)
		return targets.fold(beforeMoveContext.copy(state = stateAfterChargeDecision)) { current, target ->
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
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillBlockedByElement(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					elementId = powderBlockedElementId,
				),
			)
		}

		val darkPriorityBlockedElementId = darkPriorityBlockedElementId(state, actor, target, priorityContext)
		if (darkPriorityBlockedElementId != null) {
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillBlockedByElement(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					elementId = darkPriorityBlockedElementId,
				),
			)
		}

		if (skillBlockedByTerrain(state, actor, target, priorityContext)) {
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillBlockedByTerrain(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					terrain = state.environment.terrain,
				),
			)
		}

		val priorityBlocker = priorityMoveAbilityBlocker(state, actor, target, priorityContext)
		if (priorityBlocker != null) {
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillBlockedByAbility(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					abilityHolderActorId = priorityBlocker.actorId,
					abilityId = priorityBlocker.abilityId,
				),
			)
		}

		val soundBlocker = soundBasedSkillAbilityBlocker(state, actor, target, skill)
		if (soundBlocker != null) {
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillBlockedByAbility(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					abilityHolderActorId = soundBlocker.actorId,
					abilityId = soundBlocker.abilityId,
				),
			)
		}

		if (target.actorId in context.protectedActorIds && skill.affectedByProtect) {
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillBlockedByProtection(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
				),
			)
		}

		val accuracyCheck = accuracyCheck(state, actor, target, skill, random)
		if (!accuracyCheck.hit) {
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillMissed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					accuracyRoll = accuracyCheck.roll ?: 0,
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
			val afterEnvironmentEffects = environmentEffects.applySkillEffects(afterHpEffects, actor.actorId, skill)
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
			return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.DamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = 0,
					effectiveness = 0.0,
					targetMultiplier = targetMultiplier,
				),
			)
		}

		val directDamageAttempt = directDamageAttempt(skill, actor, target)
		if (directDamageAttempt != null) {
			if (directDamageAttempt is DirectDamageAttempt.Failed) {
				return context.interruptSkillWithEvent(
					state = state,
					actor = actor,
					skill = skill,
					random = random,
					event = BattleEvent.SkillFailed(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						targetActorId = target.actorId,
						skillId = skill.skillId,
						reason = directDamageAttempt.reason,
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

	private fun TurnContext.interruptSkillWithEvent(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
		event: BattleEvent,
	): TurnContext =
		copy(
			state = endLockedMoveAfterDisruption(
				state = state.appendEvent(event),
				actorId = actor.actorId,
				skill = skill,
				random = random,
			),
		)

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
			.appendEvents(listOfNotNull(survival.event))
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
			.appendEvents(listOfNotNull(survival.event))
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
					else -> current
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
		if (!actor.canReceiveHealing()) {
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
					else -> current
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
		if (!actor.canReceiveHealing()) {
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
	 * 随机对手技能同样按执行时站位收集候选目标；候选超过一名时才消费随机数，避免无选择场景污染 replay。
	 */
	private fun targetsForSkill(
		state: BattleState,
		actorId: String,
		selectedTargetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
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
			BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT -> randomAdjacentOpponentTargets(state, actorId, skill, random)
		}

	/**
	 * 选择一个随机相邻对手作为本次技能的唯一目标。
	 *
	 * 现代双打里的“随机对手”目标先按当前站位过滤掉同侧成员和已经无法战斗的对手，再在剩余候选中随机抽取。
	 * 若只有一个候选，目标已经确定，不需要额外随机消费；若没有候选，外层会在技能使用前取消行动并保留 PP。
	 */
	private fun randomAdjacentOpponentTargets(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): List<BattleParticipant> {
		val candidates = state.sides
			.filter { it.participant(actorId) == null }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
		return when (candidates.size) {
			0 -> emptyList()
			1 -> candidates
			else -> listOf(
				candidates[
					random.nextInt(candidates.size, "random adjacent opponent target for ${skill.skillId}"),
				],
			)
		}
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
			if (target.healingBlocked()) {
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
	 * 决定锁招技能本次会持续的总回合数。
	 *
	 * 公开成熟实现会在首次成功使用时决定 2 或 3 回合等持续时间，并把当前回合计入总数。固定持续时间不消费
	 * 随机数，避免普通单回合技能或测试用例因无意义随机消费破坏 replay 脚本。
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
				fieldEffects.applySideCondition(current, actorId, targetActorId, skill, application)
			}
		}
		val afterSideSpeedModifiers = skill.sideSpeedModifierApplications.fold(afterSideDamageReductions) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side speed condition chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applySideSpeedModifier(current, actorId, targetActorId, skill, application)
			}
		}
		val afterSideEntryHazards = skill.sideEntryHazardApplications.fold(afterSideSpeedModifiers) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side entry hazard chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applySideEntryHazard(current, actorId, targetActorId, skill, application)
			}
		}
		val afterFieldSpeedOrder = skill.fieldSpeedOrderApplications.fold(afterSideEntryHazards) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "field speed order chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applyFieldSpeedOrder(current, actorId, skill, application)
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
			.appendEvents(
				listOf(
					BattleEvent.TargetForcedSwitchSelected(
						turnNumber = state.turnNumber,
						actorId = actorId,
						targetActorId = target.actorId,
						skillId = skill.skillId,
						nextActorId = next.actorId,
					),
					BattleEvent.ParticipantSwitched(
						turnNumber = state.turnNumber,
						sideId = side.sideId,
						previousActorId = target.actorId,
						nextActorId = next.actorId,
						forced = true,
					),
				),
			)
		val afterBindingSourceCleared = endTurnEffects.clearBindingsFromSource(switched, target.actorId)
		val afterEntryHazards = entryHazardEffects.applyOnSwitchIn(afterBindingSourceCleared, side.sideId, next.actorId)
		return switchInAbilityEffects.apply(afterEntryHazards, next.actorId)
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
	 * 事件流因此会稳定呈现“状态写入 -> 道具治愈”的顺序，便于 replay 和公开对照测试定位具体触发时机。
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
	 * 顺序选择先属性、后场地：如果目标自身属性已经免疫该状态，就不再把阻止原因归给场地，便于测试用例
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
	 * 附加临时状态并处理状态私有计数。
	 *
	 * 畏缩只标记本回合行动前阻止；混乱成功时消费一个 `[0, 4)` 随机数并转成 2..5 的内部计数；回复封锁写入
	 * 固定 5 回合计数并在回合末递减；挑衅和定身法写入固定 3/4 回合计数；无理取闹写入离场清除的布尔状态；
	 * 束缚写入来源成员和 4..5 回合计数。
	 * 若目标已经处于同类可持续临时状态，成员快照不会变化，旧持续计数也不会被刷新；状态机会追加阻止事件，
	 * 便于 replay 明确区分“没有命中/没有触发”和“目标已有同类临时状态”。
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
		val bindingTurnsRemaining = if (status == BattleVolatileStatus.BINDING) {
			random.nextInt(BINDING_TURN_SPAN, "binding duration for ${skill?.skillId ?: status.name}") + BINDING_MIN_TURNS
		} else {
			0
		}
		val appliedState = state
			.replaceParticipant(
				recipient.applyVolatileStatus(
					status = status,
					confusionTurnsRemaining = confusionTurnsRemaining,
					healBlockTurnsRemaining = healBlockTurnsRemaining,
					tauntTurnsRemaining = tauntTurnsRemaining,
					disabledSkillId = disabledSkillId,
					disabledSkillTurnsRemaining = disabledSkillTurnsRemaining,
					boundByActorId = if (status == BattleVolatileStatus.BINDING) actorId else null,
					bindingTurnsRemaining = bindingTurnsRemaining,
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
	 * 当前混乱、回复封锁、挑衅、定身法、无理取闹和束缚需要拒绝刷新。畏缩可以被多次尝试，但运行态只保存一个布尔值，
	 * 后续行动前或回合末都会清除，所以重复附加不会改变可观察持续时间。
	 */
	private fun volatileStatusAlreadyPresent(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		when (status) {
			BattleVolatileStatus.CONFUSION -> recipient.confusionTurnsRemaining > 0
			BattleVolatileStatus.HEAL_BLOCK -> recipient.healBlockTurnsRemaining > 0
			BattleVolatileStatus.TAUNT -> recipient.tauntTurnsRemaining > 0
			BattleVolatileStatus.DISABLE -> recipient.disabledSkillTurnsRemaining > 0
			BattleVolatileStatus.TORMENT -> recipient.tormented
			BattleVolatileStatus.BINDING -> recipient.bindingTurnsRemaining > 0
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
					else -> current
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
					else -> current
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
			if (!actor.canBattle() || actor.currentHp == actor.maxHp || actor.healingBlocked()) {
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
		if (!participant.canBattle() || participant.currentHp == participant.maxHp || participant.healingBlocked()) {
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
	 * 根据效果目标枚举找到实际承受效果的成员。
	 */
	private fun BattleState.effectRecipient(actorId: String, targetActorId: String, target: BattleEffectTarget): BattleParticipant? =
		when (target) {
			BattleEffectTarget.USER -> participant(actorId)
			BattleEffectTarget.TARGET -> participant(targetActorId)
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

	private data class SwitchPlan(
		val action: BattleAction.SwitchParticipant,
		val actor: BattleParticipant,
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
		private const val DISABLE_TURNS = 4
		private const val BINDING_MIN_TURNS = 4
		private const val BINDING_TURN_SPAN = 2
		private const val HEAL_BLOCK_TURNS = 5
		private const val TAUNT_TURNS = 3
		private const val MULTI_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 2.0 / 3.0
		private const val SINGLE_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 0.5
	}
}
