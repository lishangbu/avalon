package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能使用前置阶段结算器。
 *
 * [BattleSkillUseResolution] 只需要知道“本次技能是否已经准备好逐目标结算”；准备好之前的步骤却非常多，并且
 * 顺序敏感：行动者有效性、睡眠/麻痹/混乱等行动前状态、目标解析、PP 检查、技能宣告、蓄力释放、PP 消耗、
 * 讲究类锁定、蓄力开始或跳过都发生在命中/保护/伤害之前。本类把这些前置步骤集中起来，返回一个明确的
 * [SkillUseSetupResult]：
 * - [SkillUseSetupResult.Stopped] 表示本次计划已经静默跳过、被行动前状态阻止、没有有效目标，或进入了蓄力开始。
 * - [SkillUseSetupResult.Ready] 表示技能已经宣告，PP/蓄力/讲究锁定等准备工作已写入状态，可以继续处理保护或目标。
 *
 * 这里仍然不是事件总线，也不是技能脚本系统；它只整理固定阶段顺序，让使用阶段主流程不再同时背负所有提前返回点。
 */
internal class BattleSkillUseSetupResolution(
	private val beforeMoveEffects: BattleBeforeMoveEffects,
	private val chargeMoves: BattleChargeMoves,
	private val lockedMoves: BattleLockedMoveEffects,
	private val skillTargeting: BattleSkillTargeting,
) {
	/**
	 * 执行技能进入逐目标结算前的全部准备工作。
	 *
	 * 返回 [SkillUseSetupResult.Ready] 时，状态中已经包含 `SkillUsed` 事件和所有宣告阶段副作用；返回
	 * [SkillUseSetupResult.Stopped] 时，调用方应直接使用其中的上下文作为本次行动结果，不再执行保护或命中伤害。
	 */
	fun resolve(context: TurnContext, plan: ActionPlan, random: BattleRandom): SkillUseSetupResult {
		val action = plan.action
		val actor = activeActor(context.state, action.actorId) ?: return SkillUseSetupResult.Stopped(context)
		val actionAttemptContext = context.copy(
			state = context.state.replaceParticipant(actor.recordSkillActionAttempt()),
		)
		val actorAfterActionAttempt = actionAttemptContext.state.participant(action.actorId)
			?: return SkillUseSetupResult.Stopped(actionAttemptContext)
		val beforeMoveOutcome = resolveBeforeMove(actionAttemptContext, plan, actorAfterActionAttempt, random)
		if (beforeMoveOutcome.blocked) {
			return SkillUseSetupResult.Stopped(beforeMoveOutcome.context)
		}
		val beforeMoveContext = beforeMoveOutcome.context
		val actionState = beforeMoveContext.state
		val readyActor = actionState.participant(action.actorId) ?: return SkillUseSetupResult.Stopped(beforeMoveContext)
		val skill = plan.skill
		val targets = resolveTargetsForReadySkill(actionState, action, readyActor, skill, random)
			?: return SkillUseSetupResult.Stopped(
				beforeMoveContext.finishDisruptedPlannedSkill(plan.source, readyActor.actorId, skill, random),
			)
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
		) ?: return SkillUseSetupResult.Stopped(beforeMoveContext)
		val stateAfterChargeDecision = resolveChargeDecision(
			plan = plan,
			actionState = actionState,
			skill = skill,
			declaration = declaration,
		) ?: return SkillUseSetupResult.Stopped(
			beforeMoveContext.copy(
				state = chargeMoves.startCharge(
					state = declaration.usedState,
					actorId = declaration.actorBeforePp.actorId,
					targetActorId = targets.first().actorId,
					skill = skill,
				),
			),
		)
		val firstSkillActionFailure = firstSkillActionFailure(
			state = stateAfterChargeDecision,
			actorId = readyActor.actorId,
			targetActorId = targets.first().actorId,
			skill = skill,
		)
		if (firstSkillActionFailure != null) {
			return SkillUseSetupResult.Stopped(
				beforeMoveContext
					.copy(state = stateAfterChargeDecision.appendEvent(firstSkillActionFailure))
					.finishDisruptedPlannedSkill(plan.source, readyActor.actorId, skill, random),
			)
		}
		return SkillUseSetupResult.Ready(
			beforeMoveContext = beforeMoveContext,
			stateAfterChargeDecision = stateAfterChargeDecision,
			readyActor = readyActor,
			actorAfterActionSetup = declaration.actorAfterActionSetup,
			skill = skill,
			targets = targets,
			targetMultiplier = skillTargeting.targetDamageMultiplier(skill, targets),
		)
	}

	/**
	 * 判断技能是否因错过本次上场后的第一次行动而失败。
	 *
	 * Fake Out / First Impression 的公开规则不是“第一回合数字”，而是“本次上场后的第一次技能行动”：主动替换那回合
	 * 不会消耗资格，但睡眠、畏缩、麻痹、混乱等已经轮到行动的阻止会消耗资格。因此 [recordSkillActionAttempt]
	 * 在行动前状态之前递增，本 gate 在技能宣告和 PP 消耗之后读取递增后的计数；计数大于 1 时，技能已经被宣告但
	 * 不再进入保护、命中、伤害和附加效果。
	 */
	private fun firstSkillActionFailure(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleEvent.SkillFailed? {
		if (!skill.usableOnlyFirstSkillActionSinceEntering) {
			return null
		}
		val actor = state.participant(actorId) ?: return null
		if (actor.isFirstSkillActionSinceEntering()) {
			return null
		}
		return BattleEvent.SkillFailed(
			turnNumber = state.turnNumber,
			actorId = actor.actorId,
			targetActorId = targetActorId,
			skillId = skill.skillId,
			reason = "not-first-skill-action-since-entering",
		)
	}

	/**
	 * 读取本次行动仍然有效的行动者快照。
	 *
	 * 技能行动可能来自玩家提交、锁招延续或蓄力释放；无论来源如何，只要行动者已经不在当前上场席位或已经倒下，
	 * 本次计划都必须静默跳过。把这个入口条件独立出来，可以让主流程只保留“有效行动者继续进入行动前状态检查”
	 * 的阶段语义。
	 */
	private fun activeActor(state: BattleState, actorId: String): BattleParticipant? {
		val actor = state.participant(actorId) ?: return null
		return actor.takeIf { state.isActive(it.actorId) && it.canBattle() }
	}

	/**
	 * 结算行动前状态，并在技能被阻止时完成来源相关清理。
	 *
	 * 睡眠、畏缩、麻痹、混乱等状态都发生在 PP 消耗和技能使用事件之前；如果它们阻止了本次行动，普通提交行动
	 * 只保留阻止事件，锁招延续和蓄力释放还需要清理成员身上的跨回合运行态。返回值用 [BeforeMoveOutcome]
	 * 明确区分“状态已经更新但行动继续”和“行动已被完全阻止”。
	 */
	private fun resolveBeforeMove(
		context: TurnContext,
		plan: ActionPlan,
		actor: BattleParticipant,
		random: BattleRandom,
	): BeforeMoveOutcome {
		val beforeMove = beforeMoveEffects.resolve(context.state, actor, plan.skill, random)
		val beforeMoveContext = context.copy(state = beforeMove.state)
		return if (beforeMove.blocked) {
			BeforeMoveOutcome(
				context = beforeMoveContext.finishDisruptedPlannedSkill(plan.source, actor.actorId, plan.skill, random),
				blocked = true,
			)
		} else {
			BeforeMoveOutcome(context = beforeMoveContext, blocked = false)
		}
	}

	/**
	 * 按当前站位解析本次技能实际目标。
	 *
	 * 目标解析放在行动前状态通过之后，是因为睡眠、麻痹、混乱自伤等会在 PP 消耗和目标重定向前阻止行动。若此时
	 * 已经没有任何有效目标，调用方会按行动来源中断锁招或蓄力释放；这里返回 null 来表达“目标阶段已经失败”。
	 */
	private fun resolveTargetsForReadySkill(
		state: BattleState,
		action: BattleAction.UseSkill,
		readyActor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): List<BattleParticipant>? {
		val targets = skillTargeting.targetsForSkill(state, readyActor.actorId, action.targetActorId, skill, random)
		return targets.ifEmpty { null }
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
	 * 处理蓄力技能在宣告后的继续或中断。
	 *
	 * 返回非空状态表示技能可以继续进入保护或逐目标结算；返回 null 表示普通提交行动已经进入蓄力开始，本回合不会
	 * 命中目标。蓄力跳过道具会在这里消费并返回可继续结算的状态，确保 `SkillUsed` 事件、道具消费和后续命中顺序
	 * 与原主流程一致。
	 */
	private fun resolveChargeDecision(
		plan: ActionPlan,
		actionState: BattleState,
		skill: BattleSkillSlot,
		declaration: SkillUseDeclaration,
	): BattleState? =
		if (
			chargeMoves.requiresChargeBeforeUse(skill, actionState.environment.weather) &&
			plan.source == SkillActionSource.SUBMITTED
		) {
			chargeMoves.skipWithHeldItem(declaration.usedState, declaration.actorBeforePp.actorId, skill)
		} else {
			declaration.usedState
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
			SkillActionSource.SUBMITTED,
			SkillActionSource.STRUGGLE_FALLBACK,
			-> this
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

	/**
	 * 行动前状态阶段的结果。
	 *
	 * [context] 总是包含行动前状态阶段已经产生的最新事件和成员运行态；[blocked] 只表达本次技能是否已经被状态
	 * 完全阻止。调用方据此决定是否继续进入目标解析和 PP 消耗阶段。
	 */
	private data class BeforeMoveOutcome(
		val context: TurnContext,
		val blocked: Boolean,
	)
}

/**
 * 技能前置阶段的公开返回值。
 *
 * 该类型只在 `battleengine` 包内部流转，避免把准备阶段拆分结果编码成多个可空字段。调用方通过 sealed 分支可以
 * 明确区分“本次行动已经结束”和“可以继续保护/逐目标结算”，也避免遗漏蓄力开始这种不是失败、但本回合不命中的情况。
 */
internal sealed interface SkillUseSetupResult {
	data class Stopped(val context: TurnContext) : SkillUseSetupResult

	data class Ready(
		val beforeMoveContext: TurnContext,
		val stateAfterChargeDecision: BattleState,
		val readyActor: BattleParticipant,
		val actorAfterActionSetup: BattleParticipant,
		val skill: BattleSkillSlot,
		val targets: List<BattleParticipant>,
		val targetMultiplier: Double,
	) : SkillUseSetupResult
}
