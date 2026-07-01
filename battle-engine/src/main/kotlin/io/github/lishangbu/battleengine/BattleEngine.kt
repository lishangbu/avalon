package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

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
	private val skillTargeting = BattleSkillTargeting()
	private val hitResolution = BattleHitResolution(statStageModifiers)
	private val directDamage = BattleDirectDamage()
	private val damageDefenseEffects = BattleDamageDefenseEffects()
	private val targetDefenseEffects = BattleTargetDefenseEffects()
	private val skillHpEffects = BattleSkillHpEffects()
	private val statStageEffects = BattleStatStageEffects(
		substituteBlocksOpponentEffect = { state, actorId, targetActorId, skill ->
			targetDefenseEffects.substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)
		},
	)
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
	 * 主要异常和临时状态的写入、阻止原因、状态治愈道具都放在这里；替身阻挡和无视目标特性的共享判断由目标
	 * 防守 resolver 提供，状态规则本身只关心“能不能写入这个状态”。
	 */
	private val statusEffects = BattleStatusEffects(
		substituteBlocksOpponentEffect = { state, actorId, targetActorId, skill ->
			targetDefenseEffects.substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)
		},
		skillIgnoresTargetAbilityEffects = { state, actorId, targetActorId ->
			targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)
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
			targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actor, target)
		},
	)
	private val preHitTargetGate = BattlePreHitTargetGate(
		skillBlockEffects = skillBlockEffects,
		targetDefenseEffects = targetDefenseEffects,
		hitResolution = hitResolution,
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
			targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)
		},
	)
	private val damageApplicationEffects = BattleDamageApplicationEffects(
		damageDefenseEffects = damageDefenseEffects,
		skillHpEffects = skillHpEffects,
		postDamageEffects = postDamageEffects,
	)
	private val damageHitResolution = BattleDamageHitResolution(
		damageCalculator = damageCalculator,
		hitResolution = hitResolution,
		targetDefenseEffects = targetDefenseEffects,
		damageDefenseEffects = damageDefenseEffects,
		skillHpEffects = skillHpEffects,
		damageApplicationEffects = damageApplicationEffects,
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
	private val switchResolution = BattleSwitchResolution(
		actionOrdering = actionOrdering,
		endTurnEffects = endTurnEffects,
		entryHazardEffects = entryHazardEffects,
		switchInAbilityEffects = switchInAbilityEffects,
	)
	private val forcedSwitchEffects = BattleForcedSwitchEffects(
		targetDefenseEffects = targetDefenseEffects,
		endTurnEffects = endTurnEffects,
		entryHazardEffects = entryHazardEffects,
		switchInAbilityEffects = switchInAbilityEffects,
	)
	private val skillAdditionalEffects = BattleSkillAdditionalEffects(
		statusEffects = statusEffects,
		statStageEffects = statStageEffects,
		fieldEffects = fieldEffects,
		targetDefenseEffects = targetDefenseEffects,
		forcedSwitchEffects = forcedSwitchEffects,
	)
	private val skillUseResolution = BattleSkillUseResolution(
		beforeMoveEffects = beforeMoveEffects,
		chargeMoves = chargeMoves,
		lockedMoves = lockedMoves,
		skillTargeting = skillTargeting,
		resolveTarget = ::resolveSkillAgainstTarget,
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
		val afterSwitches = switchResolution.resolve(started, actions.filterIsInstance<BattleAction.SwitchParticipant>(), random)
		if (afterSwitches.result != null) {
			return afterSwitches
		}
		val skillActions = actionPlanner.skillActionsForTurn(afterSwitches, actions.filterIsInstance<BattleAction.UseSkill>())
		val orderedActions = actionPlanner.orderSkillActions(afterSwitches, skillActions, random)
		val resolvedContext = orderedActions.fold(TurnContext(afterSwitches)) { current, plan ->
			if (current.state.result != null) current else skillUseResolution.resolve(current, plan, random)
		}
		return finishTurnAfterActions(resolvedContext, nextTurnNumber)
	}

	/**
	 * 结算所有行动之后的回合末流水线。
	 *
	 * 主回合入口已经完成替换、技能排序和逐个行动执行；这里只处理“行动都结束后必定按固定顺序发生”的阶段：
	 * 连续保护链重置、回合末伤害/回复、一次性临时状态清理、持续状态回合推进、天气/场地回合推进、一侧场上效果回合推进、
	 * 回合上限判定和最终 `TurnEnded` 事件。每一步都先检查战斗是否已经结束，原因是现代规则中回合末天气、异常状态、
	 * 束缚伤害或回合上限都可能直接产生胜负结果；一旦结束，后续阶段不应继续写事件。
	 */
	private fun finishTurnAfterActions(context: TurnContext, nextTurnNumber: Int): BattleState {
		val resolved = endTurnVolatileStatuses.resetProtectionChains(
			state = context.state,
			successfulProtectionActorIds = context.successfulProtectionActorIds,
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
		val preHitGate = preHitTargetGate.resolve(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			priorityContext = priorityContext,
			protectedActorIds = context.protectedActorIds,
			random = random,
		)
		val ignoresTargetAbilityEffects = when (preHitGate) {
			is BattlePreHitTargetGateResult.Interrupted -> return context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = preHitGate.event,
			)
			is BattlePreHitTargetGateResult.Passed -> preHitGate.ignoresTargetAbilityEffects
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
			val afterEffects = skillAdditionalEffects.apply(state, actor.actorId, target.actorId, skill, random)
			val afterHpEffects = skillHpEffects.applyStatusSkillHpEffects(afterEffects, actor.actorId, skill)
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

		val directDamageAttempt = directDamage.attempt(skill, actor, target)
		if (directDamageAttempt != null) {
			if (directDamageAttempt is BattleDirectDamageAttempt.Failed) {
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
			val directDamageHit = directDamageAttempt as BattleDirectDamageAttempt.Hit
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
			return finishSuccessfulDamageMove(
				context = afterDirectDamage,
				actor = actor,
				target = target,
				skill = skill,
				damageAmount = moveDamageAmount,
				random = random,
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
		return finishSuccessfulDamageMove(
			context = afterHits,
			actor = actor,
			target = target,
			skill = skill,
			damageAmount = moveDamageAmount,
			random = random,
		)
	}

	/**
	 * 收拢一次技能成功造成伤害后的共同流程。
	 *
	 * 普通公式伤害和固定/比例/HP 派生直接伤害在写入 HP 之前差异很大：前者要消费要害和伤害浮动随机数，
	 * 后者完全跳过普通伤害公式。但只要已经产生实际伤害，两条路径后续顺序一致：
	 * - 如果写入伤害时已经判定胜负，只允许造成伤害后回复类道具读取本次实际伤害，不再追加命中后技能效果或锁招推进。
	 * - 如果战斗还在继续，以最新目标快照结算命中后附加效果，避免目标在前序伤害流程中被保命、解除状态或替换引用后
	 *   仍使用旧对象。
	 * - 再让攻击方造成伤害后回复道具读取完整实际伤害。
	 * - 最后推进锁招/连续招式状态，保证锁招目标记录看到的是附加效果后的最新目标。
	 *
	 * 把这段顺序集中在这里，避免后续新增直接伤害变体时复制出一份事件顺序略有偏差的实现。
	 */
	private fun finishSuccessfulDamageMove(
		context: TurnContext,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		random: BattleRandom,
	): TurnContext {
		if (context.state.result != null) {
			return context.copy(
				state = postDamageEffects.applyPostMoveDamageDealtHealingItem(
					state = context.state,
					actorId = actor.actorId,
					skill = skill,
					damageAmount = damageAmount,
				),
			)
		}
		val latestTarget = context.state.participant(target.actorId) ?: target
		val afterEffects = skillAdditionalEffects.apply(context.state, actor.actorId, latestTarget.actorId, skill, random)
		val afterPostMoveItemEffects = postDamageEffects.applyPostMoveDamageDealtHealingItem(
			state = afterEffects,
			actorId = actor.actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		return context.copy(
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
	): TurnContext =
		context.copy(
			state = damageHitResolution.resolveFormulaHit(
				state = context.state,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				targetMultiplier = targetMultiplier,
				random = random,
			),
		)

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
	): TurnContext =
		context.copy(
			state = damageHitResolution.resolveDirectHit(
				state = context.state,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				damageAmount = damageAmount,
				faintActorAfterHit = faintActorAfterHit,
				targetMultiplier = targetMultiplier,
				effectiveness = effectiveness,
				random = random,
			),
		)

}
