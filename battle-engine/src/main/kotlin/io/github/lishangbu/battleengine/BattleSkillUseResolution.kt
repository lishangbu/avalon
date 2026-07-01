package io.github.lishangbu.battleengine

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
			is SkillUseSetupResult.Ready -> if (setup.skill.protectsUser) {
				resolveProtectionSkillUse(
					context = setup.beforeMoveContext,
					stateAfterChargeDecision = setup.stateAfterChargeDecision,
					actorBeforeProtection = setup.readyActor,
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
