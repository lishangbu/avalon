package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
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
	 * 顺序不能随意调整：粉末属性免疫、一击必杀专用属性免疫、恶属性先制免疫、一击必杀等级失败和特殊能力阶级
	 * 失败都发生在命中判定前；需要移除自身属性的技能若使用者已经没有对应属性，也会在这里失败，避免错误消费
	 * 命中随机数。精神场地、目标侧先制免疫特性、声音免疫和保护屏障也都早于命中随机数。只有全部通过之后才消费
	 * 命中随机数，并把本次是否无视目标特性传给后续吸收、状态和伤害阶段复用。
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

		val oneHitKnockOutBlockedElementId = skillBlockEffects.oneHitKnockOutBlockedElementId(state, target, skill)
		if (oneHitKnockOutBlockedElementId != null) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillBlockedByElement(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					elementId = oneHitKnockOutBlockedElementId,
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

		if (skill.oneHitKnockOut != null && target.level > actor.level) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					reason = "target-level-greater-than-user-level",
				),
			)
		}

		if (skill.healsByTargetCurrentAttack() && target.statStage(BattleStat.ATTACK) <= -6) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					reason = "target-attack-stage-minimum",
				),
			)
		}

		if (skill.healsAfterTargetMajorStatusCure() && target.majorStatus == null) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					reason = "target-has-no-major-status",
				),
			)
		}

		if (skill.removesUserElementAfterDamage && skill.elementId !in actor.elementIds) {
			return BattlePreHitTargetGateResult.Interrupted(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					reason = "user-lacks-removable-element",
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
 * 判断技能是否拥有“按目标当前攻击实数回复使用者”的特殊 HP 效果。
 *
 * 该谓词放在 gate 文件旁边，是因为当前唯一需要命中前失败条件的 HP 效果就是这一类：目标攻击阶级已到 -6 时，
 * 技能不能继续进入回复或附加效果阶段。保持为私有扩展可以避免把具体 HP 效果判断散落到调用点。
 */
private fun BattleSkillSlot.healsByTargetCurrentAttack(): Boolean =
	hpEffects.any { it is BattleSkillHpEffect.SelfHealByTargetCurrentAttack }

/**
 * 判断技能是否拥有“先治愈目标主要异常再回复使用者”的特殊 HP 效果。
 *
 * 这类技能的失败条件取决于目标当前是否真的有主要异常状态。把判断留在命中前 gate 中，可以保证失败时不会误清状态、
 * 不会误回复，也不会让后续 HP helper 需要同时表示成功和失败两种阶段语义。
 */
private fun BattleSkillSlot.healsAfterTargetMajorStatusCure(): Boolean =
	hpEffects.any { it is BattleSkillHpEffect.SelfHealAfterTargetMajorStatusCure }

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
