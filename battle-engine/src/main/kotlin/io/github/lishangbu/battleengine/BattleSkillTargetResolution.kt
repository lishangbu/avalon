package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单个实际目标的技能结算器。
 *
 * [BattleSkillUseResolution] 已经完成行动前状态、目标收集、PP 消耗、技能宣告、蓄力和保护类技能自身结算；
 * 本类接手的是“一个已经宣告的技能命中某个实际目标”这一层。它只处理目标维度的规则，不重新排序行动，也不决定
 * 多目标范围倍率：
 * - 命中前 gate：保护、先制阻挡、场地阻挡、属性/粉末/声音/状态类免疫等会在这里阻止目标。
 * - 属性吸收：被目标特性吸收的技能会写入回复或能力阶级变化，并中断锁招/蓄力释放来源。
 * - 变化技能：不进入普通伤害公式，直接结算附加效果、HP 效果和环境设置。
 * - 伤害技能：交给 [BattleSkillDamageResolution] 处理属性无效、直接伤害、普通公式伤害和成功伤害收尾。
 *
 * 这个边界是刻意收窄的：伤害公式和 HP 写入仍在 [BattleDamageHitResolution]，命中前各类阻止仍在
 * [BattlePreHitTargetGate]。本类只把这些阶段按现代规则顺序串起来，避免 [BattleEngine] 同时背负回合生命周期
 * 和单目标细节两套职责。
 */
internal class BattleSkillTargetResolution(
	private val preHitTargetGate: BattlePreHitTargetGate,
	private val skillBlockEffects: BattleSkillBlockEffects,
	private val lockedMoves: BattleLockedMoveEffects,
	private val skillAdditionalEffects: BattleSkillAdditionalEffects,
	private val statusSkillHpEffects: BattleStatusSkillHpEffects,
	private val environmentEffects: BattleEnvironmentEffects,
	private val skillDamageResolution: BattleSkillDamageResolution,
) {
	/**
	 * 结算已经宣告使用的技能对单个实际目标的影响。
	 *
	 * 多目标技能在使用阶段只消耗一次 PP，并在这里按目标逐个处理保护、命中、属性免疫、伤害和附加效果。
	 * 命中、击中要害和伤害随机数按目标独立消费；范围伤害倍率由使用阶段根据原始目标集合提前计算，
	 * 因此某个目标后续被保护或闪避时，不会改变其它目标的范围修正。
	 */
	fun resolve(
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
		val stateAfterAccuracyLockConsumption = consumeAccuracyLockAfterHitCheck(state, actor, target)
		val actorAfterAccuracyLock = stateAfterAccuracyLockConsumption.participant(actor.actorId) ?: actor
		val targetAfterAccuracyLock = stateAfterAccuracyLockConsumption.participant(target.actorId) ?: target
		val contextAfterAccuracyLock = context.copy(state = stateAfterAccuracyLockConsumption)
		val absorbedByAbility = absorbElementSkillByAbility(
			stateAfterAccuracyLockConsumption,
			actorAfterAccuracyLock,
			targetAfterAccuracyLock,
			skill,
			ignoresTargetAbilityEffects,
		)
		if (absorbedByAbility != null) {
			return contextAfterAccuracyLock.copy(
				state = lockedMoves.endAfterDisruption(
					state = absorbedByAbility,
					actorId = actorAfterAccuracyLock.actorId,
					skill = skill,
					random = random,
				),
			)
		}
		if (skill.damageClass == BattleDamageClass.STATUS) {
			return resolveStatusSkill(
				contextAfterAccuracyLock,
				stateAfterAccuracyLockConsumption,
				actorAfterAccuracyLock,
				targetAfterAccuracyLock,
				skill,
				random,
			)
		}
		return skillDamageResolution.resolve(
			contextAfterAccuracyLock,
			stateAfterAccuracyLockConsumption,
			actorAfterAccuracyLock,
			targetAfterAccuracyLock,
			skill,
			targetMultiplier,
			random,
		)
	}

	/**
	 * 命中判定通过后消费命中锁定。
	 *
	 * 锁定类效果保证“下一次对该目标的命中判定”通过；一旦 gate 已经使用这个事实，状态就应立即清除。不能只依赖
	 * 回合末过期，因为一击必杀等直接结束战斗的技能不会再进入完整回合末流水线，若不在这里消费，胜负快照会残留
	 * 已经用掉的锁定目标。
	 */
	private fun consumeAccuracyLockAfterHitCheck(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
	): BattleState =
		if (actor.hasAccuracyLockOn(target.actorId)) {
			state.replaceParticipant(actor.clearAccuracyLock())
		} else {
			state
		}

	/**
	 * 处理被目标特性吸收的元素技能。
	 *
	 * 命中前 gate 会先判断技能是否无视目标特性；一旦无视，吸收类特性必须整段跳过。普通吸收回复和吸收后能力阶级
	 * 提升都属于“技能已经被目标吃掉”的同一类中断，因此这里只返回吸收后的状态，由外层统一中断锁招或蓄力释放。
	 */
	private fun absorbElementSkillByAbility(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		ignoresTargetAbilityEffects: Boolean,
	): BattleState? =
		if (ignoresTargetAbilityEffects) {
			null
		} else {
			skillBlockEffects.elementSkillAbsorbHeal(state, actor, target, skill)
				?: skillBlockEffects.elementSkillAbsorbStatStage(state, actor, target, skill)
		}

	/**
	 * 结算变化类技能对单个目标的成功命中。
	 *
	 * 变化技能不会进入属性无效、直接伤害和普通伤害公式；命中后先处理必须早于普通附加效果的 HP 规则，再按固定
	 * 顺序应用附加效果、剩余 HP 效果、环境效果，最后推进锁招状态。这个顺序与伤害技能的成功收尾分开，是因为
	 * 变化技能没有“本次造成伤害”供造成伤害后回复道具读取。
	 */
	private fun resolveStatusSkill(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): TurnContext {
		val afterPreHpEffects = statusSkillHpEffects.applyBeforeAdditionalEffects(
			state = state,
			actorId = actor.actorId,
			targetActorId = target.actorId,
			skill = skill,
		)
		val afterEffects = skillAdditionalEffects.apply(afterPreHpEffects, actor.actorId, target.actorId, skill, random)
		val afterHpEffects = statusSkillHpEffects.apply(afterEffects, actor.actorId, target.actorId, skill)
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
}
