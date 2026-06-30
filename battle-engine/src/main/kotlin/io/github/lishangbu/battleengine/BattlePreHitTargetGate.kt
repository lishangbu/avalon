package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单目标技能命中前 gate。
 *
 * 技能已经宣告、目标已经重定向完成之后，并不能立刻进入伤害或附加效果；现代规则会先按固定顺序检查一批
 * “本次技能是否能影响这个目标”的阻止点。本组件只负责给出 gate 结果：
 * - [BattlePreHitTargetGateResult.Interrupted] 携带应追加的事实事件。
 * - [BattlePreHitTargetGateResult.Passed] 携带后续阶段复用的“是否无视目标防守特性”事实。
 *
 * 它不修改 [BattleState]，也不清理锁招/蓄力状态。这样 [BattleEngine] 仍然是唯一的阶段编排者：收到阻止事件后，
 * 由主状态机统一调用锁招/蓄力中断收口，避免 gate 组件暗中改变跨回合状态。
 */
internal class BattlePreHitTargetGate(
	private val skillBlockEffects: BattleSkillBlockEffects,
	private val targetDefenseEffects: BattleTargetDefenseEffects,
	private val hitResolution: BattleHitResolution,
) {
	/**
	 * 按现代规则顺序结算命中前阻止点。
	 *
	 * 顺序不能随意调整：粉末属性免疫和恶属性先制免疫发生在命中判定前；精神场地、目标侧先制免疫特性、声音免疫
	 * 和保护屏障也都早于命中随机数。只有全部通过之后才消费命中随机数，并把本次是否无视目标特性传给后续吸收、
	 * 状态和伤害阶段复用。
	 */
	fun resolve(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		priorityContext: SkillPriorityContext,
		protectedActorIds: Set<String>,
		random: BattleRandom,
	): BattlePreHitTargetGateResult {
		val powderBlockedElementId = skillBlockEffects.powderBlockedElementId(state, target, skill)
		if (powderBlockedElementId != null) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillBlockedByElement(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					elementId = powderBlockedElementId,
				),
			)
		}

		val darkPriorityBlockedElementId = skillBlockEffects.darkPriorityBlockedElementId(
			state = state,
			actor = actor,
			target = target,
			priorityContext = priorityContext,
		)
		if (darkPriorityBlockedElementId != null) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillBlockedByElement(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					elementId = darkPriorityBlockedElementId,
				),
			)
		}

		if (skillBlockEffects.skillBlockedByTerrain(state, actor, target, priorityContext)) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillBlockedByTerrain(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					terrain = state.environment.terrain,
				),
			)
		}

		val priorityBlocker = skillBlockEffects.priorityMoveAbilityBlocker(state, actor, target, priorityContext)
		if (priorityBlocker != null) {
			return BattlePreHitTargetGateResult.Interrupted(
				abilityBlockEvent(state, actor, target, skill, priorityBlocker),
			)
		}

		val soundBlocker = skillBlockEffects.soundBasedSkillAbilityBlocker(state, actor, target, skill)
		if (soundBlocker != null) {
			return BattlePreHitTargetGateResult.Interrupted(
				abilityBlockEvent(state, actor, target, skill, soundBlocker),
			)
		}

		if (target.actorId in protectedActorIds && skill.affectedByProtect) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillBlockedByProtection(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
				),
			)
		}

		val ignoresTargetAbilityEffects = targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actor, target)
		val accuracyCheck = hitResolution.accuracyCheck(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			ignoresTargetAbilityEffects = ignoresTargetAbilityEffects,
			random = random,
		)
		if (!accuracyCheck.hit) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillMissed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					accuracyRoll = accuracyCheck.roll ?: 0,
				),
			)
		}

		return BattlePreHitTargetGateResult.Passed(ignoresTargetAbilityEffects)
	}

	private fun abilityBlockEvent(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		blocker: BattleParticipant,
	): BattleEvent.SkillBlockedByAbility =
		BattleEvent.SkillBlockedByAbility(
			turnNumber = state.turnNumber,
			actorId = actor.actorId,
			targetActorId = target.actorId,
			skillId = skill.skillId,
			abilityHolderActorId = blocker.actorId,
			abilityId = blocker.abilityId,
		)
}

/**
 * 命中前 gate 的结算结果。
 *
 * [Interrupted] 表示技能已经被属性、场地、特性、保护或命中失败阻止，调用方必须追加事件并结束后续流程。
 * [Passed] 表示技能可以继续进入属性吸收、状态效果或伤害阶段，同时携带已计算好的防守特性忽略事实。
 */
internal sealed interface BattlePreHitTargetGateResult {
	data class Interrupted(val event: BattleEvent) : BattlePreHitTargetGateResult

	data class Passed(val ignoresTargetAbilityEffects: Boolean) : BattlePreHitTargetGateResult
}
