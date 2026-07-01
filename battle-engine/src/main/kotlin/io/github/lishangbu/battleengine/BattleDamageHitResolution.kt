package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单段技能命中后的伤害写入流程。
 *
 * [BattleEngine] 仍然负责技能行动的阶段编排：行动前状态、PP、保护、命中、属性吸收、属性免疫、多段循环、
 * 附加效果和锁招推进都不在这里决定。本类只接管“已经确认本段应该造成伤害”之后的细节：
 * - 普通公式伤害：消费击中要害和伤害浮动随机数，读取一侧屏障、替身、属性减伤道具和满 HP 保命规则。
 * - 直接伤害：跳过普通公式，只把固定/比例/HP 派生伤害写入目标或替身。
 * - 两条路径最终都回到 [BattleDamageApplicationEffects.finishPostDamageEffects]，保证低体力道具、接触特性、
 *   反伤、吸取、休整、倒下和胜负判定顺序一致。
 *
 * 这个拆分的边界刻意很窄：它不新增规则分发层，也不把事件当作二次调度源，只是把主状态机里最厚的一段 HP 写入
 * 细节移出来，让 [BattleEngine.resolveSkillAgainstTarget] 保持“选择哪条战斗阶段路径”的职责。
 */
internal class BattleDamageHitResolution(
	private val damageCalculator: BattleDamageCalculator,
	private val hitResolution: BattleHitResolution,
	private val targetDefenseEffects: BattleTargetDefenseEffects,
	private val damageDefenseEffects: BattleDamageDefenseEffects,
	private val skillHpEffects: BattleSkillHpEffects,
	private val damageApplicationEffects: BattleDamageApplicationEffects,
) {
	/**
	 * 结算多段或单段标准伤害中的一段。
	 *
	 * 调用方已经完成行动可用性、目标选择、保护和命中判定；这里每段独立消费要害和伤害浮动随机数，并在目标或
	 * 使用者因伤害、接触特性、反伤倒下时把结果写进返回状态。若行动者或目标已经不能战斗，直接返回原状态，
	 * 让外层多段循环自然停止。
	 */
	fun resolveFormulaHit(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		targetMultiplier: Double,
		random: BattleRandom,
	): BattleState {
		val (actor, target) = activeDamageParticipants(state, actorId, targetActorId) ?: return state
		val criticalHitCheck = criticalHitCheck(skill, random)
		val ignoresTargetAbilityEffects = targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actor, target)
		val criticalHit = criticalHitCheck.hit && (ignoresTargetAbilityEffects || !target.hasCriticalHitImmunity())
		val randomPercent = 85 + random.nextInt(16, "damage random for ${skill.skillId}")
		val sideDamageReductionMultiplier = hitResolution.sideDamageReductionMultiplier(
			state = state,
			target = target,
			skill = skill,
			criticalHit = criticalHit,
		)
		val substituteBlocksDamage =
			targetDefenseEffects.substituteBlocksOpponentEffect(state, actor.actorId, target.actorId, skill)
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
			return damageApplicationEffects.resolveDamageAgainstSubstitute(
				state = state,
				actor = actor,
				target = target,
				skill = skill,
				damageAmount = damage.amount,
			)
		}

		val itemReduction = damageDefenseEffects.heldItemDamageReduction(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			skillElementId = skill.effectiveElementId(state.environment.weather),
			effectiveness = damage.effectiveness,
		)
		val stateAfterItemReduction = itemReduction?.let { state.replaceParticipant(it.target).appendEvent(it.event) } ?: state
		val targetAfterItemReduction = itemReduction?.target ?: target
		val targetDamage = damageApplicationEffects.applyDamageToTarget(
			state = stateAfterItemReduction,
			actor = actor,
			target = targetAfterItemReduction,
			skill = skill,
			damageAmount = damage.amount,
			effectiveness = damage.effectiveness,
			targetMultiplier = damage.targetMultiplier,
			criticalHit = criticalHit,
			ignoreTargetAbilityEffects = ignoresTargetAbilityEffects,
		)
		val afterFireThaw = skillHpEffects.clearFreezeAfterFireDamage(targetDamage.state, targetDamage.damagedTarget, skill)
		return damageApplicationEffects.finishPostDamageEffects(
			state = afterFireThaw,
			actorId = actor.actorId,
			targetActorId = targetDamage.damagedTarget.actorId,
			skill = skill,
			damageAmount = targetDamage.actualDamageAmount,
			targetCanFaint = true,
			allowTargetLowHpItem = true,
			allowContactAbilities = true,
			random = random,
		)
	}

	/**
	 * 结算一段直接伤害技能。
	 *
	 * 直接伤害不会进入普通伤害公式，因此不消费击中要害和伤害浮动随机数；但替身、满 HP 保命、低体力道具、
	 * 接触特性、反伤、倒下和胜负判定仍然必须和普通伤害共享同一条后续流程。调用方传入的 [effectiveness] 只用于
	 * 伤害事件展示和回放，不会参与直接伤害数值计算。
	 */
	fun resolveDirectHit(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		faintActorAfterHit: Boolean,
		targetMultiplier: Double,
		effectiveness: Double,
		random: BattleRandom,
	): BattleState {
		val (actor, target) = activeDamageParticipants(state, actorId, targetActorId) ?: return state
		val substituteBlocksDamage =
			targetDefenseEffects.substituteBlocksOpponentEffect(state, actor.actorId, target.actorId, skill)
		if (substituteBlocksDamage) {
			return damageApplicationEffects.resolveDamageAgainstSubstitute(
				state = state,
				actor = actor,
				target = target,
				skill = skill,
				damageAmount = damageAmount,
				faintActorAfterHit = faintActorAfterHit,
			)
		}

		val ignoresTargetAbilityEffects = targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actor, target)
		val targetDamage = damageApplicationEffects.applyDamageToTarget(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			damageAmount = damageAmount,
			effectiveness = effectiveness,
			targetMultiplier = targetMultiplier,
			ignoreTargetAbilityEffects = ignoresTargetAbilityEffects,
		)
		return damageApplicationEffects.finishPostDamageEffects(
			state = targetDamage.state,
			actorId = actor.actorId,
			targetActorId = targetDamage.damagedTarget.actorId,
			skill = skill,
			damageAmount = targetDamage.actualDamageAmount,
			faintActorAfterHit = faintActorAfterHit,
			targetCanFaint = true,
			allowTargetLowHpItem = true,
			allowContactAbilities = true,
			random = random,
		)
	}

	/**
	 * 读取本段伤害仍然有效的行动者和目标。
	 *
	 * 普通公式伤害和直接伤害在数值来源上不同，但都不应该让已经离场、缺失或无法战斗的成员继续进入替身、要害、
	 * 道具和 HP 写入阶段。集中这一步可以保证两条伤害路径的入口短路口径一致；调用方仍负责决定是否消费随机数
	 * 和是否追加事件，因此本函数只返回快照，不修改状态。
	 */
	private fun activeDamageParticipants(
		state: BattleState,
		actorId: String,
		targetActorId: String,
	): Pair<BattleParticipant, BattleParticipant>? {
		val actor = state.participant(actorId) ?: return null
		val target = state.participant(targetActorId) ?: return null
		return (actor to target).takeIf { (currentActor, currentTarget) ->
			currentActor.canBattle() && currentTarget.canBattle()
		}
	}
}
