package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单个回合内技能行动的收集与排序。
 *
 * 该组件位于回合生命周期的“行动准备”阶段：它把玩家提交的技能行动、锁招续行动和蓄力释放行动合并成统一队列，
 * 然后根据有效优先度、有效速度和同速随机键得到执行顺序。它不执行技能、不消耗 PP、不改写状态、不追加事件；
 * 输出的 [ActionPlan] 只是 [BattleEngine] 后续技能结算阶段的输入。
 *
 * 排序不变量：先比较有效优先度，再比较当前环境下的有效速度；同一优先度和速度内消费确定性随机数打破平手。
 * 锁招、蓄力和无可选技能时的挣扎 fallback 都会附带 [SkillActionSource]，让执行阶段决定是否消耗 PP、是否解除
 * 蓄力状态，以及被行动前状态阻止时需要如何收尾。
 */
internal class BattleTurnActionPlanner(
	private val actionOrdering: BattleActionOrdering,
) {
	/**
	 * 根据当前状态补齐本回合应参与技能阶段的行动。
	 *
	 * 锁招续行动和蓄力释放行动来自当前上场成员的运行态，并会覆盖同一成员提交的普通技能选择。普通提交行动若
	 * 已经没有任何可选技能，会被替换为内置挣扎行动；这里仍不判断睡眠、畏缩、混乱等运行时阻止，那些必须保留
	 * 可 replay 的事件并在后续阶段处理。
	 */
	internal fun skillActionsForTurn(
		state: BattleState,
		submittedActions: List<BattleAction.UseSkill>,
	): List<SkillActionInput> {
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
			.map { submittedAction -> submittedAction.toSubmittedOrStruggleInput(state) } + lockedActions + chargingActions
	}

	/**
	 * 将技能候选行动转换为可执行计划并排序。
	 *
	 * 该阶段会校验行动者和技能槽存在，并提前阻止被讲究类道具锁定到其它技能的提交行动。有效优先度上下文由
	 * [BattleActionOrdering] 计算，后续精神场地、先制阻挡和恶属性免疫读取同一份上下文。
	 */
	internal fun orderSkillActions(
		state: BattleState,
		actions: List<SkillActionInput>,
		random: BattleRandom,
	): List<ActionPlan> {
		val plans = actions.map { input ->
			val action = input.action
			val actor = requireNotNull(state.participant(action.actorId)) { "actor not found: ${action.actorId}" }
			val skill = input.skillOverride ?: requireNotNull(actor.skillSlot(action.skillId)) {
				"skill not found: ${action.skillId}"
			}
			if (input.source == SkillActionSource.SUBMITTED) {
				require(!actor.choiceLockedToAnotherSkill(action.skillId)) {
					"choice locked to skill: ${actor.choiceLockedSkillId}"
				}
			}
			val itemRandomOrderBoost = actor.itemEffects
				.filterIsInstance<BattleItemEffect.RandomActionOrderBoost>()
				.firstOrNull()
				?.let { chanceSucceeds(it.chancePercent, random, "held item action order for ${actor.actorId}") }
				?: false
			val abilityRandomOrderBoost = actor.abilityEffects
				.filterIsInstance<io.github.lishangbu.battleengine.model.BattleAbilityEffect.RandomActionOrderBoost>()
				.firstOrNull()
				?.let { chanceSucceeds(it.chancePercent, random, "ability action order for ${actor.actorId}") }
				?: false
			val lowHpOrderBoost = actor.itemEffects
				.filterIsInstance<BattleItemEffect.LowHpActionOrderBoost>()
				.firstOrNull()
				?.let { effect ->
					effect.shouldTrigger(actor.currentHp, actor.maxHp) ||
						actor.expandedQuarterHpItemThresholdReached(
							effect.triggerHpNumerator,
							effect.triggerHpDenominator,
						)
				}
				?: false
			val forcedLast = actor.itemEffects.any { it is BattleItemEffect.ForcedLastActionOrder } ||
				actor.abilityEffects.any { it is io.github.lishangbu.battleengine.model.BattleAbilityEffect.ForcedLastActionOrder }
			ActionPlan(
				action = action,
				actor = actor,
				skill = skill,
				source = input.source,
				priorityContext = actionOrdering.skillPriorityContext(state, actor, skill),
				orderBracket = when {
					forcedLast -> -1
					itemRandomOrderBoost || abilityRandomOrderBoost || lowHpOrderBoost -> 1
					else -> 0
				},
				consumesOrderItem = lowHpOrderBoost,
			)
		}
		val speedComparator = actionOrdering.speedComparator(state)
		val orderComparator = compareByDescending<Triple<Int, Int, Int>> { it.first }
			.thenByDescending { it.second }
			.thenComparator { left, right -> speedComparator.compare(left.third, right.third) }
		return plans
			.groupBy {
				Triple(it.priorityContext.effectivePriority, it.orderBracket, actionOrdering.effectiveSpeed(state, it.actor))
			}
			.toSortedMap(orderComparator)
			.values
			.flatMap { sameOrderPlans ->
				if (sameOrderPlans.size == 1) {
					sameOrderPlans
				} else {
					sameOrderPlans.sortedByRandomTieBreak(random) { "speed tie for ${it.actor.actorId}" }
				}
			}
	}

	/**
	 * 把普通提交行动转换为本回合真正要排序的技能行动。
	 *
	 * 现代规则中，成员没有任何可提交技能时会自动使用挣扎；这不是资料表中的一格技能，也不应该写回成员技能槽或
	 * 消耗原技能 PP。因此行动规划阶段只为本回合计划附带一个内置技能快照，后续结算照常产生 `SkillUsed`、伤害、
	 * 自损和倒下事件。若行动者缺失，保留原提交行动，让排序阶段的状态机不变量保护继续给出明确错误。
	 */
	private fun BattleAction.UseSkill.toSubmittedOrStruggleInput(state: BattleState): SkillActionInput {
		val actor = state.participant(actorId)
		return if (actor != null && actor.mustUseStruggle()) {
			SkillActionInput(
				action = copy(skillId = STRUGGLE_SKILL.skillId),
				source = SkillActionSource.STRUGGLE_FALLBACK,
				skillOverride = STRUGGLE_SKILL,
			)
		} else {
			SkillActionInput(this, source = SkillActionSource.SUBMITTED)
		}
	}

	internal companion object {
		/**
		 * 内置挣扎技能 ID。
		 *
		 * 该 ID 只存在于运行时事件和测试断言中，不来自资料库，也不会参与技能槽查找。使用极大正数可以避开正常资料
		 * ID，同时继续满足模型中“技能 ID 必须为正”的不变量。
		 */
		const val STRUGGLE_SKILL_ID: Long = Long.MAX_VALUE

		/**
		 * 内置挣扎技能快照。
		 *
		 * 现代挣扎按 50 威力物理无属性伤害结算，目标为随机相邻对手，不消耗任何原技能 PP；只要本次确实造成实际
		 * 伤害，使用者随后损失最大 HP 的 1/4，按四舍五入到最近整数处理。它仍会被保护阻挡，并且会经过行动前状态、
		 * 命中前 gate、普通伤害、伤害后自损和倒下判定等同一条引擎流程，避免在主状态机里复制一套特殊结算。
		 */
		private val STRUGGLE_SKILL = BattleSkillSlot(
			skillId = STRUGGLE_SKILL_ID,
			name = "挣扎",
			elementId = STRUGGLE_SKILL_ID,
			damageClass = BattleDamageClass.PHYSICAL,
			power = 50,
			accuracy = null,
			targetScope = BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT,
			typelessDamage = true,
			remainingPp = 1,
			maxPp = 1,
			hpEffects = listOf(BattleSkillHpEffect.RecoilByUserMaxHp(numerator = 1, denominator = 4)),
		)
	}
}
