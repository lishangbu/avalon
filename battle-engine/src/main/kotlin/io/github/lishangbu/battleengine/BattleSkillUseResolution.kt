package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
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
	private val useSetupResolution: BattleSkillUseSetupResolution,
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
		return when (val setup = useSetupResolution.resolve(context, plan, random)) {
			is SkillUseSetupResult.Stopped -> setup.context
			is SkillUseSetupResult.Ready -> if (setup.skill.isProtectionFamilySkill()) {
				resolveProtectionFamilySkillUse(
					context = setup.beforeMoveContext,
					stateAfterChargeDecision = setup.stateAfterChargeDecision,
					actorBeforeProtectionFamilyAction = setup.readyActor,
					actorAfterActionSetup = setup.actorAfterActionSetup,
					skill = setup.skill,
					random = random,
				)
			} else {
				resolveTargets(
					context = setup.beforeMoveContext.copy(state = setup.stateAfterChargeDecision),
					actorId = setup.readyActor.actorId,
					skill = setup.skill,
					priorityContext = plan.priorityContext,
					targets = setup.targets,
					targetMultiplier = setup.targetMultiplier,
					random = random,
				)
			}
		}
	}

	/**
	 * 逐个目标执行已经宣告成功的技能。
	 *
	 * 技能使用事件、PP、讲究锁定、蓄力和保护都已经在调用方完成。本函数只保留逐目标结算循环本身，并在任意目标
	 * 导致战斗结束时停止后续目标处理，保证范围技能不会在结果已经出现后继续追加命中、伤害或附加效果事件。
	 */
	private fun resolveTargets(
		context: TurnContext,
		actorId: String,
		skill: BattleSkillSlot,
		priorityContext: SkillPriorityContext,
		targets: List<BattleParticipant>,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext =
		targets.fold(context) { current, target ->
			if (current.state.result != null) {
				current
			} else {
				resolveTarget(
					current,
					actorId,
					target.actorId,
					skill,
					priorityContext,
					targetMultiplier,
					random,
				)
			}
		}

	/**
	 * 结算保护类同族技能自身的成功或失败。
	 *
	 * 技能使用事件、PP 消耗、讲究锁定和蓄力跳过判断都已经在调用方完成；本函数只处理“这个保护屏障是否建立”：
	 * - 失败时清零连续保护链，并追加 [BattleEvent.ProtectionFailed]。
	 * - 守住/看穿成功时推进连续保护链，追加 [BattleEvent.ProtectionStarted]，同时写入当前回合临时保护集合。
	 * - 广域/快速防守成功时推进同一条连续保护链，追加 [BattleEvent.SideProtectionStarted]，同时写入当前回合临时侧保护集合。
	 * - 挺住成功时推进同一条连续保护链，追加 [BattleEvent.FatalDamageEndureStarted]，并把来源技能写入成员快照。
	 *
	 * 守住屏障仍保存在 [TurnContext]，不进入 [BattleState]，因为它只对当前回合后续命中门禁有效。挺住姿态要由
	 * 伤害写入层读取，所以写入 [BattleParticipant.fatalDamageEndureSkillId]；回合末会被临时状态清理器统一移除。
	 * 广域/快速防守按公开规则只持续当前回合，也不写入 [BattleState] 的多回合一侧防护列表。
	 * `successfulProtectionActorIds` 仍表示本回合成功使用保护类同族行动的成员，回合结束后用它保留连续成功计数。
	 */
	private fun resolveProtectionFamilySkillUse(
		context: TurnContext,
		stateAfterChargeDecision: BattleState,
		actorBeforeProtectionFamilyAction: BattleParticipant,
		actorAfterActionSetup: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): TurnContext {
		val sideProtectionPendingActionFailure = sideProtectionPendingActionFailure(
			context = context.copy(state = stateAfterChargeDecision),
			actorId = actorBeforeProtectionFamilyAction.actorId,
			skill = skill,
		)
		if (sideProtectionPendingActionFailure != null) {
			return context.copy(
				state = stateAfterChargeDecision
					.replaceParticipant(actorAfterActionSetup.resetProtectionChain())
					.appendEvent(sideProtectionPendingActionFailure),
			)
		}
		if (!protectionSucceeds(actorBeforeProtectionFamilyAction, skill, random)) {
			return context.copy(
				state = stateAfterChargeDecision
					.replaceParticipant(actorAfterActionSetup.resetProtectionChain())
					.appendEvent(
						BattleEvent.ProtectionFailed(
							turnNumber = stateAfterChargeDecision.turnNumber,
							actorId = actorBeforeProtectionFamilyAction.actorId,
							skillId = skill.skillId,
						),
					),
			)
		}
		val actorAfterProtectionFamilySuccess = actorAfterActionSetup
			.markProtectionSuccess()
			.let { actor ->
				if (skill.enduresFatalDamage) actor.markFatalDamageEndure(skill.skillId) else actor
			}
		val successEvents = buildList {
			if (skill.protectsUser) {
				add(
					BattleEvent.ProtectionStarted(
						turnNumber = stateAfterChargeDecision.turnNumber,
						actorId = actorBeforeProtectionFamilyAction.actorId,
						skillId = skill.skillId,
					),
				)
			}
			val actorSideId = stateAfterChargeDecision.sideOf(actorBeforeProtectionFamilyAction.actorId)?.sideId
			if (skill.protectsUserSideFromMultiTargetSkills && actorSideId != null) {
				add(
					BattleEvent.SideProtectionStarted(
						turnNumber = stateAfterChargeDecision.turnNumber,
						actorId = actorBeforeProtectionFamilyAction.actorId,
						sideId = actorSideId,
						skillId = skill.skillId,
						kind = BattleSideProtectionKind.MULTI_TARGET_SKILL,
						turnsRemaining = null,
					),
				)
			}
			if (skill.protectsUserSideFromPrioritySkills && actorSideId != null) {
				add(
					BattleEvent.SideProtectionStarted(
						turnNumber = stateAfterChargeDecision.turnNumber,
						actorId = actorBeforeProtectionFamilyAction.actorId,
						sideId = actorSideId,
						skillId = skill.skillId,
						kind = BattleSideProtectionKind.PRIORITY_SKILL,
						turnsRemaining = null,
					),
				)
			}
			if (skill.enduresFatalDamage) {
				add(
					BattleEvent.FatalDamageEndureStarted(
						turnNumber = stateAfterChargeDecision.turnNumber,
						actorId = actorBeforeProtectionFamilyAction.actorId,
						skillId = skill.skillId,
					),
				)
			}
		}
		return context.copy(
			state = stateAfterChargeDecision
				.replaceParticipant(actorAfterProtectionFamilySuccess)
				.appendEvents(successEvents),
			protectedActorIds = if (skill.protectsUser) {
				context.protectedActorIds + actorBeforeProtectionFamilyAction.actorId
			} else {
				context.protectedActorIds
			},
			multiTargetProtectedSideIds = protectedSideIdsAfterSuccess(
				existingSideIds = context.multiTargetProtectedSideIds,
				state = stateAfterChargeDecision,
				actorId = actorBeforeProtectionFamilyAction.actorId,
				enabled = skill.protectsUserSideFromMultiTargetSkills,
			),
			priorityProtectedSideIds = protectedSideIdsAfterSuccess(
				existingSideIds = context.priorityProtectedSideIds,
				state = stateAfterChargeDecision,
				actorId = actorBeforeProtectionFamilyAction.actorId,
				enabled = skill.protectsUserSideFromPrioritySkills,
			),
			successfulProtectionActorIds = context.successfulProtectionActorIds + actorBeforeProtectionFamilyAction.actorId,
		)
	}
}

private fun BattleSkillSlot.isProtectionFamilySkill(): Boolean =
	protectsUser || enduresFatalDamage || protectsUserSideFromMultiTargetSkills || protectsUserSideFromPrioritySkills

/**
 * 判断本回合一侧临时防护是否因为后续没有任何技能行动而失败。
 *
 * 公开引擎中广域防守和快速防守会先检查行动队列里是否还存在将要行动的成员；如果本回合已经没有后续技能行动，
 * 技能虽然已经宣告并消耗 PP，但不会建立一侧防护，也不会推进连续保护成功计数。这里把该判断限制在一侧临时防护
 * 技能上，守住/看穿/挺住仍按自身规则允许在没有后续行动时成功。
 */
private fun sideProtectionPendingActionFailure(
	context: TurnContext,
	actorId: String,
	skill: BattleSkillSlot,
): BattleEvent.SkillFailed? {
	if (!skill.protectsUserSideFromMultiTargetSkills && !skill.protectsUserSideFromPrioritySkills) {
		return null
	}
	if (context.hasPendingSkillActionAfter(actorId)) {
		return null
	}
	return BattleEvent.SkillFailed(
		turnNumber = context.state.turnNumber,
		actorId = actorId,
		targetActorId = actorId,
		skillId = skill.skillId,
		reason = "no-pending-skill-action-after-side-protection",
	)
}

/**
 * 返回一侧临时防护成功后的 sideId 集合。
 *
 * 广域防守和快速防守只需要记录“哪一侧在当前回合已经建立对应防护”。这里不写入 [BattleState]，也不创建
 * [io.github.lishangbu.battleengine.model.BattleSideProtection]，因为该状态不会跨过本回合技能阶段。
 */
private fun protectedSideIdsAfterSuccess(
	existingSideIds: Set<String>,
	state: BattleState,
	actorId: String,
	enabled: Boolean,
): Set<String> {
	if (!enabled) {
		return existingSideIds
	}
	val sideId = state.sideOf(actorId)?.sideId ?: return existingSideIds
	return existingSideIds + sideId
}

/**
 * 单个回合技能阶段的临时上下文。
 *
 * `state` 是不断推进的不可变战斗状态；`plannedSkillActions` 是行动排序阶段冻结的本回合技能计划；
 * `resolvedSkillActorIds` 保存本回合技能阶段已经处理过的行动者；`protectedActorIds` 保存本回合已经成功建立守住屏障的成员；
 * `multiTargetProtectedSideIds` 和 `priorityProtectedSideIds` 保存只在本回合有效的一侧临时防护；
 * `successfulProtectionActorIds` 保存回合结束后应保留连续保护类行动计数的成员，包括守住、看穿、挺住、广域防守和快速防守。
 * 这类回合内临时标记不进入 `BattleState`，避免被误认为跨回合持久状态，也方便后续扩展击掌奇袭、
 * 守住连续成功率、先制阻挡等同样只在当前回合有效的规则。
 */
internal data class TurnContext(
	val state: BattleState,
	val plannedSkillActions: List<ActionPlan> = emptyList(),
	val resolvedSkillActorIds: Set<String> = emptySet(),
	val protectedActorIds: Set<String> = emptySet(),
	val multiTargetProtectedSideIds: Set<String> = emptySet(),
	val priorityProtectedSideIds: Set<String> = emptySet(),
	val successfulProtectionActorIds: Set<String> = emptySet(),
) {
	/**
	 * 标记某个技能行动者已经完成本回合技能阶段。
	 *
	 * 突袭、快手还击等技能需要知道目标是否“仍在准备行动”；这个集合只服务同一回合内的后续技能，不写入
	 * [BattleState]，避免 replay 快照出现本可由行动序列稳定推导的临时事实。
	 */
	fun markSkillActionResolved(actorId: String): TurnContext =
		copy(resolvedSkillActorIds = resolvedSkillActorIds + actorId)

	/**
	 * 查找目标本回合尚未处理的技能计划。
	 *
	 * 返回 null 表示目标没有技能行动、已经执行过、已经倒下/离场，或本回合选择了替换。调用方据此让依赖目标意图
	 * 的技能失败，而不是把缺失计划误当作普通伤害目标继续结算。
	 */
	fun pendingSkillAction(actorId: String): ActionPlan? =
		plannedSkillActions.firstOrNull { plan ->
			plan.action.actorId == actorId &&
				plan.action.actorId !in resolvedSkillActorIds &&
				state.isActive(plan.action.actorId) &&
				state.participant(plan.action.actorId)?.canBattle() == true
		}

	/**
	 * 判断当前行动之后是否仍存在其它可执行技能计划。
	 *
	 * 广域防守/快速防守只有在队列中还有后续行动时才会建立一侧临时防护。本函数复用排序阶段冻结的计划和已执行集合，
	 * 不根据速度或事件流重新推导“还有谁会行动”，避免同速随机、锁招续行动和蓄力释放在不同入口下出现两套口径。
	 */
	fun hasPendingSkillActionAfter(actorId: String): Boolean =
		plannedSkillActions.any { plan ->
			plan.action.actorId != actorId &&
				plan.action.actorId !in resolvedSkillActorIds &&
				state.isActive(plan.action.actorId) &&
				state.participant(plan.action.actorId)?.canBattle() == true
		}
}
