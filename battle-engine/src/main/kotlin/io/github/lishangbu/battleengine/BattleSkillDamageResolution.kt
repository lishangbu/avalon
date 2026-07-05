package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单目标伤害类技能结算器。
 *
 * [BattleSkillTargetResolution] 已经完成行动者/目标有效性、命中前阻止、属性吸收和变化技能分流；本类只接收
 * “一个伤害类技能已经命中一个实际目标”这一层工作。把它拆出来的目的不是引入新的规则插件系统，而是把三条
 * 容易互相干扰的伤害路径放在同一个小边界里：
 * - 属性无效必须最先返回 0 伤害事件，并中断锁招或蓄力释放。
 * - 固定、比例和 HP 派生直接伤害不进入普通公式，但仍要复用替身、接触、反伤、低体力道具和胜负判定。
 * - 普通公式伤害可能多段命中，每段独立消费要害和伤害浮动随机数，但造成伤害后回复道具只读取本次技能总伤害。
 *
 * 伤害成功后的附加效果、造成伤害后回复道具和锁招推进也保留在这里统一收口，避免直接伤害与普通公式伤害复制出
 * 两套事件顺序。回合级顺序仍由 [BattleSkillUseResolution] 和 [BattleTurnResolution] 决定。
 */
internal class BattleSkillDamageResolution(
	private val directDamage: BattleDirectDamage,
	private val damageHitResolution: BattleDamageHitResolution,
	private val moveFinishResolution: BattleDamageMoveFinishResolution,
) {
	/**
	 * 结算伤害类技能对单个目标的成功命中入口。
	 *
	 * 这里先做属性无效，因为固定伤害、比例伤害和普通公式伤害都需要遵守“目标属性完全无效时造成 0 伤害”的
	 * 现代规则口径。属性有效后再分流直接伤害和普通公式伤害，两条路径最后都会回到
	 * [finishSuccessfulDamageMove]，保证命中后附加效果、造成伤害后回复道具和锁招推进只维护一份顺序。
	 */
	fun resolve(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		random: BattleRandom,
	): TurnContext {
		val effectiveness = if (skill.typelessDamage) {
			1.0
		} else {
			state.rules.elementChart.multiplier(
				skill.effectiveElementId(state.environment.weather, state.environment.terrain),
				target.elementIds,
			)
		}
		if (effectiveness == 0.0) {
			return moveFinishResolution.interruptSkillWithEvent(
				context = context,
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
			is BattleDirectDamageAttempt.Failed -> moveFinishResolution.interruptSkillWithEvent(
				context = context,
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
				val moveDamageAmount = moveFinishResolution.damageDealtByMove(
					state = afterDirectDamage.state,
					eventStartIndex = damageEventStartIndex,
					actorId = actor.actorId,
					skillId = skill.skillId,
				)
				moveFinishResolution.finishSuccessfulDamageMove(
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
	 * 开始循环前记录事件下标，是为了最终按“本次技能动作”汇总伤害，避免贝壳之铃类道具在多段技能里按每段
	 * 分别读取伤害。
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
		val moveDamageAmount = moveFinishResolution.damageDealtByMove(
			state = afterHits.state,
			eventStartIndex = damageEventStartIndex,
			actorId = actor.actorId,
			skillId = skill.skillId,
		)
		return moveFinishResolution.finishSuccessfulDamageMove(
			context = afterHits,
			actor = actor,
			target = target,
			skill = skill,
			damageAmount = moveDamageAmount,
			random = random,
		)
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
