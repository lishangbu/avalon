package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 技能目标侧的通用防守关系判断。
 *
 * 本类不推进战斗状态，也不写事件；它只回答两个会被多个阶段共享的问题：目标替身是否挡住来自对手的技能效果，
 * 以及本次技能是否忽略目标侧防守特性。普通伤害、直接伤害、状态附加、能力阶级操作、强制换人和部分伤后特性
 * 都要读取这些判断，因此集中到这里可以避免每个 resolver 各自复制一份“同侧/对侧、声音类、替身、无视特性”
 * 的关系规则。
 */
internal class BattleTargetDefenseEffects {
	/**
	 * 判断目标替身是否会阻止来自对手的技能伤害或状态效果。
	 *
	 * 替身只保护当前有替身的成员免受对手非声音类技能影响；使用者对自己施加的效果、同侧辅助效果、声音类技能
	 * 以及目标没有替身时都不会被这里阻止。接棒传递等例外后续会以明确技能标签或状态规则扩展，而不是让调用方
	 * 通过技能名称做临时判断。
	 */
	fun substituteBlocksOpponentEffect(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): Boolean {
		if (skill.soundBased) {
			return false
		}
		val target = state.participant(targetActorId) ?: return false
		if (!target.hasSubstitute()) {
			return false
		}
		val actorSide = state.sideOf(actorId)?.sideId ?: return false
		val targetSide = state.sideOf(targetActorId)?.sideId ?: return false
		return actorSide != targetSide
	}

	/**
	 * 判断本次技能是否应忽略目标侧防守特性。
	 *
	 * 该重载从运行态 ID 解析成员，适合只能拿到 actorId/targetActorId 的 resolver 回调。解析失败时返回 false，
	 * 因为没有明确的攻击方和目标方关系时，不能凭空扩大“无视目标特性”的范围。
	 */
	fun skillIgnoresTargetAbilityEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
	): Boolean {
		val actor = state.participant(actorId) ?: return false
		val target = state.participant(targetActorId) ?: return false
		return skillIgnoresTargetAbilityEffects(state, actor, target)
	}

	/**
	 * 判断本次技能是否应忽略目标侧防守特性。
	 *
	 * 纯引擎只读取 [BattleAbilityEffect.IgnoreTargetAbilityEffects] 这个结构化效果，不判断具体资料库特性名称。
	 * 双打中只要目标在对手侧，就会忽略目标本人以及目标侧伙伴提供的防守型特性；同侧目标始终返回 false。这个
	 * 判断不会影响目标道具、属性天然免疫、场地免疫或攻击方自己的攻击侧特性。
	 */
	fun skillIgnoresTargetAbilityEffects(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
	): Boolean {
		if (!actor.canBattle() || !target.canBattle()) {
			return false
		}
		val actorSide = state.sideOf(actor.actorId) ?: return false
		val targetSide = state.sideOf(target.actorId) ?: return false
		if (actorSide.sideId == targetSide.sideId) {
			return false
		}
		return actor.abilityEffects.any { it is BattleAbilityEffect.IgnoreTargetAbilityEffects }
	}
}
