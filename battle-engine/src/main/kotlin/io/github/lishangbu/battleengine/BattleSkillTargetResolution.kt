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
 * - 伤害技能：先判定属性无效，再分流到直接伤害或普通公式伤害；多段伤害只在本目标内部循环。
 * - 成功伤害收尾：命中后附加效果、造成伤害后回复道具、锁招推进统一从这里收口。
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
	private val skillHpEffects: BattleSkillHpEffects,
	private val environmentEffects: BattleEnvironmentEffects,
	private val directDamage: BattleDirectDamage,
	private val damageHitResolution: BattleDamageHitResolution,
	private val postDamageEffects: BattlePostDamageEffects,
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
		val absorbedByAbility = absorbElementSkillByAbility(state, actor, target, skill, ignoresTargetAbilityEffects)
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
			return resolveStatusSkill(context, state, actor, target, skill, random)
		}
		return resolveDamageSkill(context, state, actor, target, skill, targetMultiplier, random)
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
	 * 变化技能不会进入属性无效、直接伤害和普通伤害公式；命中后只按固定顺序应用附加效果、使用者 HP 效果、
	 * 环境效果，再推进锁招状态。这个顺序与伤害技能的成功收尾分开，是因为变化技能没有“本次造成伤害”供
	 * 造成伤害后回复道具读取。
	 */
	private fun resolveStatusSkill(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): TurnContext {
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

	/**
	 * 结算伤害类技能对单个目标的成功命中入口。
	 *
	 * 这里先做属性无效，因为固定伤害、比例伤害和普通公式伤害都需要遵守“目标属性完全无效时造成 0 伤害”的
	 * 现代规则口径。属性有效后再分流直接伤害和普通公式伤害，两条路径最后都会回到
	 * [finishSuccessfulDamageMove]，保证命中后附加效果、造成伤害后回复道具和锁招推进只维护一份顺序。
	 */
	private fun resolveDamageSkill(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext {
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
		return if (directDamageAttempt == null) {
			resolveFormulaDamage(context, state, actor, target, skill, targetMultiplier, random)
		} else {
			resolveDirectDamageAttempt(
				context = context,
				state = state,
				actor = actor,
				target = target,
				skill = skill,
				targetMultiplier = targetMultiplier,
				effectiveness = effectiveness,
				attempt = directDamageAttempt,
				random = random,
			)
		}
	}

	/**
	 * 结算固定、比例或 HP 派生直接伤害技能。
	 *
	 * 直接伤害不会进入普通公式，但失败分支和成功分支仍必须从同一个目标结算入口返回：失败时中断锁招，成功时
	 * 先写入 HP，再统计本次技能实际造成的伤害，最后复用伤害技能成功收尾。这样直接伤害技能不会绕过替身、
	 * 低体力道具、接触触发、反伤、倒下判定或胜负判定。
	 */
	private fun resolveDirectDamageAttempt(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		effectiveness: Double,
		attempt: BattleDirectDamageAttempt,
		random: BattleRandom,
	): TurnContext =
		when (attempt) {
			is BattleDirectDamageAttempt.Failed -> context.interruptSkillWithEvent(
				state = state,
				actor = actor,
				skill = skill,
				random = random,
				event = BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					reason = attempt.reason,
				),
			)
			is BattleDirectDamageAttempt.Hit -> {
				val damageEventStartIndex = state.events.size
				val afterDirectDamage = resolveDirectDamageHit(
					context = context,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skill = skill,
					damageAmount = attempt.amount,
					faintActorAfterHit = attempt.faintActorAfterHit,
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
				finishSuccessfulDamageMove(
					context = afterDirectDamage,
					actor = actor,
					target = target,
					skill = skill,
					damageAmount = moveDamageAmount,
					random = random,
				)
			}
		}

	/**
	 * 结算普通公式伤害技能。
	 *
	 * 多段命中在单目标内部循环；每一段都独立消费要害与伤害浮动随机数，并在目标或使用者倒下、战斗结束时停止。
	 * 开始循环前记录事件下标，是为了最终按“本次技能动作”汇总伤害，避免贝壳之铃类造成伤害后回复道具在多段
	 * 技能里按每段分别读取伤害。
	 */
	private fun resolveFormulaDamage(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext {
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
	 * 写入 HP 后仍复用 [BattleDamageApplicationEffects.finishPostDamageEffects]，让目标低体力道具、接触触发特性、
	 * 使用者反伤、伤害后回复、倒下判定和胜负判定保持同一条事件顺序。若目标有替身且该技能不能穿透替身，
	 * 则直接伤害先扣替身 HP。
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
