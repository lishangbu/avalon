package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能目标范围解析器。
 *
 * 本类只负责把“行动里选择的目标席位”和“技能声明的目标范围”转换为本次技能实际尝试影响的成员列表。它不判断
 * 命中、不处理保护、不检查属性免疫，也不写事件；这些阶段仍由 [BattleEngine] 的单目标流程逐个执行。这样可以
 * 保持现代规则中的两个关键语义：
 * - 单体技能攻击的是目标席位。若行动提交后目标成员被替换，技能会重新指向该席位当前可战斗的上场成员。
 * - 范围技能和随机对手技能按执行时站位收集目标，已经倒下或不在场的成员不会进入候选列表。
 */
internal class BattleSkillTargeting {
	/**
	 * 根据技能目标范围计算本次行动会尝试影响的实际目标。
	 *
	 * 随机对手技能只有在候选超过一名时才消费随机数；没有候选或只有一个候选都不会污染 replay 随机脚本。返回
	 * 空列表表示本次技能没有合法目标，调用方会按行动来源决定是否中断锁招/蓄力，并保留或跳过后续技能流程。
	 */
	fun targetsForSkill(
		state: BattleState,
		actorId: String,
		selectedTargetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): List<BattleParticipant> =
		when (skill.targetScope) {
			BattleSkillTargetScope.SELF -> listOfNotNull(state.participant(actorId))
				.filter { state.isActive(it.actorId) && it.canBattle() }
			BattleSkillTargetScope.SELECTED_TARGET -> listOfNotNull(state.activeTargetFor(selectedTargetActorId))
				.filter { it.canBattle() }
			BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS -> state.sides
				.filter { it.participant(actorId) == null }
				.flatMap { it.activeParticipants() }
				.filter { it.canBattle() }
			BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS -> state.sides
				.flatMap { it.activeParticipants() }
				.filter { it.actorId != actorId && it.canBattle() }
			BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT -> randomAdjacentOpponentTargets(state, actorId, skill, random)
		}

	/**
	 * 计算现代双打范围技能的目标倍率。
	 *
	 * 公开规则中，能同时命中多个目标的伤害技能在实际存在多个目标时使用 0.75 倍目标修正。若范围内只剩一个可
	 * 战斗目标，则不套用该修正。调用方应在逐目标结算前用原始目标列表计算一次倍率，避免某个目标后续被保护或
	 * 闪避时改变其它目标的范围修正。
	 */
	fun targetDamageMultiplier(skill: BattleSkillSlot, targets: List<BattleParticipant>): Double =
		if (skill.targetScope.canAffectMultipleTargets && targets.size > 1) {
			MULTI_TARGET_DAMAGE_MULTIPLIER
		} else {
			SINGLE_TARGET_DAMAGE_MULTIPLIER
		}

	/**
	 * 选择一个随机相邻对手作为本次技能的唯一目标。
	 *
	 * 现代双打里的“随机对手”目标先按当前站位过滤掉同侧成员和已经无法战斗的对手，再在剩余候选中随机抽取。
	 * 若只有一个候选，目标已经确定，不需要额外随机消费；若没有候选，外层会在技能使用前取消行动并保留 PP。
	 */
	private fun randomAdjacentOpponentTargets(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): List<BattleParticipant> {
		val candidates = state.sides
			.filter { it.participant(actorId) == null }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
		return when (candidates.size) {
			0 -> emptyList()
			1 -> candidates
			else -> listOf(
				candidates[
					random.nextInt(candidates.size, "random adjacent opponent target for ${skill.skillId}"),
				],
			)
		}
	}

	private companion object {
		/**
		 * 范围技能实际命中多个目标时的现代伤害倍率。
		 */
		private const val MULTI_TARGET_DAMAGE_MULTIPLIER = 0.75

		/**
		 * 单目标或范围内只剩一个目标时不修改伤害。
		 */
		private const val SINGLE_TARGET_DAMAGE_MULTIPLIER = 1.0
	}
}
