package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单次技能行动的使用阶段编排器。
 *
 * [BattleEngine] 仍然拥有完整回合顺序：替换先于技能、技能行动按优先度和速度排序、全部行动后再进入回合末效果。
 * 本类只接管“某个行动计划已经轮到执行”之后、正式逐目标结算之前的内部步骤：
 * - 跳过已经离场或倒下的行动者，避免后续阶段拿到失效成员。
 * - 结算睡眠、畏缩、麻痹、混乱等行动前状态；若被阻止，按行动来源清理锁招或蓄力释放。
 * - 根据当前站位重新取得实际目标集合，并只在普通提交行动时检查 PP。
 * - 宣告技能使用，按来源处理蓄力释放、PP 消耗、讲究类锁招和 `SkillUsed` 事件。
 * - 处理蓄力开始、蓄力跳过道具和保护类技能成功率。
 * - 对每个实际目标调用主引擎提供的单目标结算函数，保持保护、命中、伤害和附加效果的既有顺序不变。
 *
 * 这个拆分刻意没有把单目标命中/伤害逻辑一起搬走。那一段仍然很厚，但它与伤害公式、直接伤害、附加效果和锁招推进
 * 强耦合；先把“使用阶段编排”单独挪出，可以让主状态机短一截，同时避免一次重构横跨过多敏感规则。
 */
internal class BattleSkillUseResolution(
	private val beforeMoveEffects: BattleBeforeMoveEffects,
	private val chargeMoves: BattleChargeMoves,
	private val lockedMoves: BattleLockedMoveEffects,
	private val skillTargeting: BattleSkillTargeting,
	private val resolveTarget: (
		context: TurnContext,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		priorityContext: SkillPriorityContext,
		targetMultiplier: Double,
		random: BattleRandom,
	) -> TurnContext,
) {
	/**
	 * 执行一次使用技能行动。
	 *
	 * 行动者若已经倒下会被跳过；单体目标按席位语义重定向，范围目标按当前站位重新收集。
	 * 技能使用事件和 PP 消耗只发生一次，随后每个实际目标独立结算命中、要害、伤害和附加效果。
	 */
	fun resolve(context: TurnContext, plan: ActionPlan, random: BattleRandom): TurnContext {
		val state = context.state
		val action = plan.action
		val actor = state.participant(action.actorId) ?: return context
		if (!state.isActive(actor.actorId) || !actor.canBattle()) {
			return context
		}
		val beforeMove = beforeMoveEffects.resolve(state, actor, plan.skill, random)
		val beforeMoveContext = context.copy(state = beforeMove.state)
		if (beforeMove.blocked) {
			return beforeMoveContext.finishDisruptedPlannedSkill(plan.source, actor.actorId, plan.skill, random)
		}
		val actionState = beforeMoveContext.state
		val readyActor = actionState.participant(action.actorId) ?: return beforeMoveContext
		val skill = readyActor.skillSlot(action.skillId) ?: return beforeMoveContext
		val targets = skillTargeting.targetsForSkill(actionState, readyActor.actorId, action.targetActorId, skill, random)
		if (targets.isEmpty()) {
			return beforeMoveContext.finishDisruptedPlannedSkill(plan.source, readyActor.actorId, skill, random)
		}
		if (plan.source == SkillActionSource.SUBMITTED) {
			require(skill.remainingPp > 0) { "skill has no remaining PP: ${skill.skillId}" }
		}

		val declaration = declareSkillUse(
			action = action,
			source = plan.source,
			actionState = actionState,
			readyActor = readyActor,
			skill = skill,
			firstTarget = targets.first(),
		) ?: return beforeMoveContext

		val stateAfterChargeDecision =
			if (
				chargeMoves.requiresChargeBeforeUse(skill, actionState.environment.weather) &&
				plan.source == SkillActionSource.SUBMITTED
			) {
				chargeMoves.skipWithHeldItem(declaration.usedState, declaration.actorBeforePp.actorId, skill)
					?: return beforeMoveContext.copy(
						state = chargeMoves.startCharge(
							state = declaration.usedState,
							actorId = declaration.actorBeforePp.actorId,
							targetActorId = targets.first().actorId,
							skill = skill,
						),
					)
			} else {
				declaration.usedState
			}

		if (skill.protectsUser) {
			return resolveProtectionSkillUse(
				context = beforeMoveContext,
				stateAfterChargeDecision = stateAfterChargeDecision,
				actorBeforeProtection = readyActor,
				actorAfterActionSetup = declaration.actorAfterActionSetup,
				skill = skill,
				random = random,
			)
		}

		val targetMultiplier = skillTargeting.targetDamageMultiplier(skill, targets)
		return targets.fold(beforeMoveContext.copy(state = stateAfterChargeDecision)) { current, target ->
			if (current.state.result != null) {
				current
			} else {
				resolveTarget(
					current,
					readyActor.actorId,
					target.actorId,
					skill,
					plan.priorityContext,
					targetMultiplier,
					random,
				)
			}
		}
	}

	/**
	 * 宣告一次技能使用，并写入宣告阶段产生的持久变化。
	 *
	 * 这个阶段发生在行动前状态通过、目标集合确定之后，命中/保护/属性阻止之前。它统一处理四件顺序敏感的事情：
	 * - 蓄力释放行动先清理成员身上的蓄力状态，并追加释放事件。
	 * - 普通提交行动消耗 PP；锁招延续和蓄力释放不再次消耗 PP。
	 * - 普通提交行动在这里写入讲究类道具的技能锁定，确保即使后续被保护或属性免疫阻止，锁定也已经成为事实。
	 * - 最后追加 [BattleEvent.SkillUsed]，让 replay 看到“技能已宣告使用”，后续才是蓄力开始、保护、命中和伤害。
	 *
	 * 返回 null 只表示蓄力释放后成员快照异常缺失；调用方会保留宣告前上下文并停止本次行动。正常资料路径下不应发生。
	 */
	private fun declareSkillUse(
		action: BattleAction.UseSkill,
		source: SkillActionSource,
		actionState: BattleState,
		readyActor: BattleParticipant,
		skill: BattleSkillSlot,
		firstTarget: BattleParticipant,
	): SkillUseDeclaration? {
		val stateBeforeUse = if (source == SkillActionSource.CHARGED_RELEASE) {
			chargeMoves.releaseChargedSkill(actionState, readyActor, skill, firstTarget.actorId)
		} else {
			actionState
		}
		val actorBeforePp = stateBeforeUse.participant(action.actorId) ?: return null
		val actorAfterPp = if (source == SkillActionSource.SUBMITTED) {
			actorBeforePp.replaceSkillSlot(skill.consumePp())
		} else {
			actorBeforePp
		}
		val actorAfterActionSetup = if (source == SkillActionSource.SUBMITTED) {
			actorAfterPp.lockChoiceSkillIfNeeded(skill.skillId)
		} else {
			actorAfterPp
		}.markSuccessfulSkill(skill.skillId)
		val usedState = stateBeforeUse
			.replaceParticipant(actorAfterActionSetup)
			.appendEvent(
				BattleEvent.SkillUsed(
					turnNumber = stateBeforeUse.turnNumber,
					actorId = actorBeforePp.actorId,
					targetActorId = firstTarget.actorId,
					skillId = skill.skillId,
					skillName = skill.name,
				),
			)
		return SkillUseDeclaration(
			usedState = usedState,
			actorBeforePp = actorBeforePp,
			actorAfterActionSetup = actorAfterActionSetup,
		)
	}

	/**
	 * 收口“计划中的技能没有真正进入命中/伤害流程”时的来源清理。
	 *
	 * 主流程有多个提前返回点：行动前状态阻止、目标不存在、后续命中前 gate 失败等。对普通提交行动来说，提前返回
	 * 只需要保留已经追加的阻止事件；但锁招延续和蓄力释放都在成员身上持有跨回合状态，若被打断必须清理对应字段，
	 * 并追加可 replay 的结束/中断事件。这里把同一份分支集中起来，避免某个提前返回点忘记清理锁招或蓄力状态。
	 */
	private fun TurnContext.finishDisruptedPlannedSkill(
		source: SkillActionSource,
		actorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): TurnContext =
		when (source) {
			SkillActionSource.LOCKED_CONTINUATION -> copy(
				state = lockedMoves.endAfterDisruption(state, actorId, skill, random),
			)
			SkillActionSource.CHARGED_RELEASE -> copy(
				state = chargeMoves.endAfterDisruption(state, actorId, skill),
			)
			SkillActionSource.SUBMITTED -> this
		}

	/**
	 * 结算保护类技能自身的成功或失败。
	 *
	 * 技能使用事件、PP 消耗、讲究锁定和蓄力跳过判断都已经在调用方完成；本函数只处理“这个保护屏障是否建立”：
	 * - 失败时清零连续保护链，并追加 [BattleEvent.ProtectionFailed]。
	 * - 成功时推进连续保护链，追加 [BattleEvent.ProtectionStarted]，同时写入当前回合临时保护集合。
	 *
	 * 保护集合仍保存在 [TurnContext]，不进入 [BattleState]，因为它只对当前回合后续技能有效；回合末会用
	 * `successfulProtectionActorIds` 决定哪些成员保留连续保护计数，没成功保护的成员会被统一重置。
	 */
	private fun resolveProtectionSkillUse(
		context: TurnContext,
		stateAfterChargeDecision: BattleState,
		actorBeforeProtection: BattleParticipant,
		actorAfterActionSetup: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): TurnContext {
		if (!protectionSucceeds(actorBeforeProtection, skill, random)) {
			return context.copy(
				state = stateAfterChargeDecision
					.replaceParticipant(actorAfterActionSetup.resetProtectionChain())
					.appendEvent(
						BattleEvent.ProtectionFailed(
							turnNumber = stateAfterChargeDecision.turnNumber,
							actorId = actorBeforeProtection.actorId,
							skillId = skill.skillId,
						),
					),
			)
		}
		val protectedActor = actorAfterActionSetup.markProtectionSuccess()
		return context.copy(
			state = stateAfterChargeDecision
				.replaceParticipant(protectedActor)
				.appendEvent(
					BattleEvent.ProtectionStarted(
						turnNumber = stateAfterChargeDecision.turnNumber,
						actorId = actorBeforeProtection.actorId,
						skillId = skill.skillId,
					),
				),
			protectedActorIds = context.protectedActorIds + actorBeforeProtection.actorId,
			successfulProtectionActorIds = context.successfulProtectionActorIds + actorBeforeProtection.actorId,
		)
	}

	/**
	 * 技能宣告阶段的结果。
	 *
	 * [usedState] 已经写入蓄力释放、PP 消耗、讲究锁定、最近成功技能和 `SkillUsed` 事件；调用方会继续判断是否需要
	 * 开始蓄力、是否建立保护，以及如何逐目标结算。[actorBeforePp] 保留消耗 PP 前的成员快照，用于蓄力跳过道具
	 * 和蓄力开始事件维持原有顺序；[actorAfterActionSetup] 则是保护成功/失败需要继续写回的成员快照。
	 */
	private data class SkillUseDeclaration(
		val usedState: BattleState,
		val actorBeforePp: BattleParticipant,
		val actorAfterActionSetup: BattleParticipant,
	)
}

/**
 * 单个回合技能阶段的临时上下文。
 *
 * `state` 是不断推进的不可变战斗状态；`protectedActorIds` 保存本回合已经成功建立保护屏障的成员；
 * `successfulProtectionActorIds` 保存回合结束后应保留连续保护计数的成员。
 * 这类回合内临时标记不进入 `BattleState`，避免被误认为跨回合持久状态，也方便后续扩展击掌奇袭、
 * 守住连续成功率、先制阻挡等同样只在当前回合有效的规则。
 */
internal data class TurnContext(
	val state: BattleState,
	val protectedActorIds: Set<String> = emptySet(),
	val successfulProtectionActorIds: Set<String> = emptySet(),
)
