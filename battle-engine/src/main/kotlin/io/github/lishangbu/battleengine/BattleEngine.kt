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
	 * 后，执行技能流程只需要把返回的状态重新放回当前上下文。混乱自伤后可能触发低体力回复道具，所以这里把
	 * 统一伤害后结算器里的低体力道具处理函数作为回调传入，避免出现“普通伤害一套道具顺序，混乱自伤另一套
	 * 道具顺序”。
	 */
	private val beforeMoveEffects = BattleBeforeMoveEffects(
		statStageModifiers = statStageModifiers,
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
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
	 * 天气伤害、天气/场地/道具回复和回合上限收口都委托给该对象。低体力回复道具仍回调统一伤害后结算器，
	 * 保证所有伤害入口共享同一套道具消费与回复封锁判断。
	 */
	private val endTurnEffects = BattleEndTurnEffects(
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
	)
	/**
	 * 状态附加与状态免疫结算器。
	 *
	 * 主要异常和临时状态的写入、阻止原因、状态治愈道具都放在这里；主引擎继续保留替身阻挡和无视目标特性的
	 * 共享判断，因为这些判断同时服务伤害、固定伤害、接触特性和状态附加。这样状态规则只保留一份，跨阶段共享
	 * 的判定也只保留一份。
	 */
	private val statusEffects = BattleStatusEffects(
		substituteBlocksOpponentEffect = { state, actorId, targetActorId, skill ->
			substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)
		},
		skillIgnoresTargetAbilityEffects = { state, actorId, targetActorId ->
			skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)
		},
	)
	/**
	 * 锁招类技能的持续回合和结束结算器。
	 *
	 * 主状态机仍决定“成功后推进锁招”或“失败后中断锁招”的调用时机；该对象只维护锁招字段、锁招事件和结束后
	 * 可能发生的疲劳混乱。
	 */
	private val lockedMoves = BattleLockedMoveEffects(statusEffects)

	/**
	 * 单目标技能结算前的场地、属性、特性阻止与吸收规则。
	 *
	 * 主流程仍负责这些阻止点的先后顺序，以及被阻止后是否需要中断锁招；该对象只封装每个阻止点自身的判断和
	 * 吸收状态改写。
	 */
	private val skillBlockEffects = BattleSkillBlockEffects(
		skillIgnoresTargetAbilityEffects = { state, actor, target ->
			skillIgnoresTargetAbilityEffects(state, actor, target)
		},
	)

	/**
	 * 伤害后的接触特性与携带道具结算器。
	 *
	 * 该对象不计算伤害，只处理 HP 变化之后的附加效果。低体力回复道具也从这里提供给行动前、入场陷阱和回合末
	 * resolver，保证所有伤害入口共享同一套触发线、回复封锁和道具消费规则。
	 */
	private val postDamageEffects = BattlePostDamageEffects(
		statusEffects = statusEffects,
		skillIgnoresTargetAbilityEffects = { state, actorId, targetActorId ->
			skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)
		},
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
			statusEffects.blockedMajorStatusReason(state, actorId, recipient, status)
		},
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
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
					state = lockedMoves.endAfterDisruption(beforeMoveContext.state, actor.actorId, plan.skill, random),
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
					state = lockedMoves.endAfterDisruption(actionState, readyActor.actorId, skill, random),
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

		val powderBlockedElementId = skillBlockEffects.powderBlockedElementId(state, target, skill)
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

		val darkPriorityBlockedElementId = skillBlockEffects.darkPriorityBlockedElementId(state, actor, target, priorityContext)
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

		if (skillBlockEffects.skillBlockedByTerrain(state, actor, target, priorityContext)) {
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

		val priorityBlocker = skillBlockEffects.priorityMoveAbilityBlocker(state, actor, target, priorityContext)
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

		val soundBlocker = skillBlockEffects.soundBasedSkillAbilityBlocker(state, actor, target, skill)
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
			skillBlockEffects.elementSkillAbsorbHeal(state, actor, target, skill)
				?: skillBlockEffects.elementSkillAbsorbStatStage(state, actor, target, skill)
		}
		if (absorbedByAbility != null) {
			return context.copy(
				state = lockedMoves.endAfterDisruption(
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
				state = lockedMoves.updateAfterSuccessfulUse(
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
					state = postDamageEffects.applyPostMoveDamageDealtHealingItem(
						state = afterDirectDamage.state,
						actorId = actor.actorId,
						skill = skill,
						damageAmount = moveDamageAmount,
					),
				)
			}
			val latestTarget = afterDirectDamage.state.participant(target.actorId) ?: target
			val afterEffects = applySkillEffects(afterDirectDamage.state, actor.actorId, latestTarget.actorId, skill, random)
			val afterPostMoveItemEffects = postDamageEffects.applyPostMoveDamageDealtHealingItem(
				state = afterEffects,
				actorId = actor.actorId,
				skill = skill,
				damageAmount = moveDamageAmount,
			)
			return afterDirectDamage.copy(
				state = lockedMoves.updateAfterSuccessfulUse(
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
				state = postDamageEffects.applyPostMoveDamageDealtHealingItem(
					state = afterHits.state,
					actorId = actor.actorId,
					skill = skill,
					damageAmount = moveDamageAmount,
				),
			)
		}
		val latestTarget = afterHits.state.participant(target.actorId) ?: target
		val afterEffects = applySkillEffects(afterHits.state, actor.actorId, latestTarget.actorId, skill, random)
		val afterPostMoveItemEffects = postDamageEffects.applyPostMoveDamageDealtHealingItem(
			state = afterEffects,
			actorId = actor.actorId,
			skill = skill,
			damageAmount = moveDamageAmount,
		)
		return afterHits.copy(
			state = lockedMoves.updateAfterSuccessfulUse(
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
			state = lockedMoves.endAfterDisruption(
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
			postDamageEffects.applyLowHpHealingItem(afterSkillRecharge, targetActorId)
		} else {
			afterSkillRecharge
		}
		val afterContactAbilities = if (allowContactAbilities && random != null) {
			postDamageEffects.applyContactAbilityEffects(
				state = afterTargetLowHpItem,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				random = random,
			)
		} else {
			afterTargetLowHpItem
		}
		val afterRecoil = postDamageEffects.applyPostDamageItemEffects(
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
					statusEffects.applyMajorStatus(
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
					statusEffects.applyVolatileStatus(
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
		private const val MULTI_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 2.0 / 3.0
		private const val SINGLE_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 0.5
	}
}
