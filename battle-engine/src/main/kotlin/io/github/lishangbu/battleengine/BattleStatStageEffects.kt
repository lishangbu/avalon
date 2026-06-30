package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能命中后的能力阶级特殊操作结算器。
 *
 * 普通能力阶级加减只是在当前数值上叠加 delta；这里处理的是必须先读取当前阶级再写回的新值，例如清除、复制、
 * 交换和取反。每条 [BattleStatStageOperation] 只处理一个能力项，资料层可以为同一技能配置多条记录来表达
 * “所有能力项”或“攻击/防御组”。本类不处理主要异常、临时状态、一侧屏障或强制替换，避免把所有命中后效果混成
 * 一个难维护的大型脚本执行器。
 */
internal class BattleStatStageEffects(
	private val substituteBlocksOpponentEffect: (
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	) -> Boolean,
) {
	/**
	 * 依序应用技能声明的能力阶级特殊操作。
	 *
	 * 概率判定与普通附加效果一致：只有规则声明小于 100% 的操作才会消费随机数。多条操作按资料顺序串行执行，
	 * 因此复制/交换/取反会读取前一条操作已经写回的运行态阶级；这正是复杂技能用多条结构化记录表达组合效果时
	 * 需要的行为。
	 */
	fun applyOperations(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.statStageOperations.fold(state) { current, operation ->
			if (!chanceSucceeds(operation.chancePercent, random, "stat stage operation chance for ${skill.skillId}")) {
				current
			} else {
				applyOperation(current, actorId, targetActorId, skill, operation)
			}
		}

	/**
	 * 分派单条能力阶级操作。
	 *
	 * 清除和取反可能作用于一个或多个目标；复制和交换必须有明确的单个来源与目标。若目标已经倒下、目标不存在，
	 * 或对手的替身阻挡了该技能效果，则该条操作保持状态不变且不产生事件。
	 */
	private fun applyOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState =
		when (operation.kind) {
			BattleStatStageOperationKind.CLEAR -> applyClearOperation(state, actorId, targetActorId, skill, operation)
			BattleStatStageOperationKind.COPY -> applyCopyOperation(state, actorId, targetActorId, skill, operation)
			BattleStatStageOperationKind.SWAP -> applySwapOperation(state, actorId, targetActorId, skill, operation)
			BattleStatStageOperationKind.INVERT -> applyInvertOperation(state, actorId, targetActorId, skill, operation)
		}

	/**
	 * 将目标范围内成员的指定能力阶级清除为 0。
	 *
	 * 普通目标清除会尊重对手替身；全场清除表示黑雾类场地事实，遍历所有仍在场且可战斗的当前上场成员，不被
	 * 单个成员的替身拦截。只有实际非 0 阶级被清除时才记录事件，避免 replay 被无变化的 0->0 填满。
	 */
	private fun applyClearOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState =
		operationParticipants(state, actorId, targetActorId, operation.target).fold(state) { current, participant ->
			val latest = current.participant(participant.actorId) ?: return@fold current
			if (!latest.canBattle()) {
				return@fold current
			}
			if (
				operation.target != BattleStatStageOperationTarget.ALL_ACTIVE &&
				substituteBlocksOpponentEffect(current, actorId, latest.actorId, skill)
			) {
				return@fold current
			}
			val previous = latest.statStage(operation.stat)
			if (previous == 0) {
				current
			} else {
				current
					.replaceParticipant(latest.setStatStage(operation.stat, 0))
					.appendEvent(
						BattleEvent.StatStageCleared(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = latest.actorId,
							skillId = skill.skillId,
							stat = operation.stat,
							previousStage = previous,
						),
					)
			}
		}

	/**
	 * 将来源成员的指定能力阶级复制到目标成员。
	 *
	 * 自我暗示类技能复制的是当前运行态阶级，不读取基础能力值。来源和目标必须都是单个成员；目标已有相同阶级时
	 * 不产生事件。复制动作不改变来源，因此多条能力项操作按顺序执行时彼此独立。
	 */
	private fun applyCopyOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState {
		val sourceTarget = operation.source ?: return state
		val source = operationParticipant(state, actorId, targetActorId, sourceTarget) ?: return state
		val target = operationParticipant(state, actorId, targetActorId, operation.target) ?: return state
		if (!source.canBattle() || !target.canBattle()) {
			return state
		}
		if (operationBlockedBySubstitute(state, actorId, target.actorId, skill, operation.target)) {
			return state
		}
		if (operationBlockedBySubstitute(state, actorId, source.actorId, skill, sourceTarget)) {
			return state
		}
		val copied = source.statStage(operation.stat)
		val previous = target.statStage(operation.stat)
		if (previous == copied) {
			return state
		}
		return state
			.replaceParticipant(target.setStatStage(operation.stat, copied))
			.appendEvent(
				BattleEvent.StatStageCopied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					sourceActorId = source.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					stat = operation.stat,
					copiedStage = copied,
				),
			)
	}

	/**
	 * 交换两个成员指定能力阶级的当前值。
	 *
	 * 力量互换、防守互换和心灵互换都可以拆成多条本操作：每条只交换一个能力项。若双方该项阶级相同，则状态和
	 * 事件保持不变；否则两个成员会在同一个状态转换中写回，避免只更新一方时被后续读取到半成品状态。
	 */
	private fun applySwapOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState {
		val sourceTarget = operation.source ?: return state
		val first = operationParticipant(state, actorId, targetActorId, operation.target) ?: return state
		val second = operationParticipant(state, actorId, targetActorId, sourceTarget) ?: return state
		if (!first.canBattle() || !second.canBattle() || first.actorId == second.actorId) {
			return state
		}
		if (operationBlockedBySubstitute(state, actorId, first.actorId, skill, operation.target)) {
			return state
		}
		if (operationBlockedBySubstitute(state, actorId, second.actorId, skill, sourceTarget)) {
			return state
		}
		val firstStage = first.statStage(operation.stat)
		val secondStage = second.statStage(operation.stat)
		if (firstStage == secondStage) {
			return state
		}
		return state
			.replaceParticipant(first.setStatStage(operation.stat, secondStage))
			.replaceParticipant(second.setStatStage(operation.stat, firstStage))
			.appendEvent(
				BattleEvent.StatStageSwapped(
					turnNumber = state.turnNumber,
					actorId = actorId,
					firstActorId = first.actorId,
					secondActorId = second.actorId,
					skillId = skill.skillId,
					stat = operation.stat,
					firstCurrentStage = secondStage,
					secondCurrentStage = firstStage,
				),
			)
	}

	/**
	 * 将目标范围内成员的指定能力阶级取反。
	 *
	 * 颠倒类效果使用当前阶级的相反数并保持在 -6..6 范围内。因为现有运行态已经保证阶级边界，本函数只需要写回
	 * `-previous`。0 阶级取反后没有可观察变化，因此不会产生事件。
	 */
	private fun applyInvertOperation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		operation: BattleStatStageOperation,
	): BattleState =
		operationParticipants(state, actorId, targetActorId, operation.target).fold(state) { current, participant ->
			val latest = current.participant(participant.actorId) ?: return@fold current
			if (!latest.canBattle()) {
				return@fold current
			}
			if (operationBlockedBySubstitute(current, actorId, latest.actorId, skill, operation.target)) {
				return@fold current
			}
			val previous = latest.statStage(operation.stat)
			if (previous == 0) {
				current
			} else {
				val next = -previous
				current
					.replaceParticipant(latest.setStatStage(operation.stat, next))
					.appendEvent(
						BattleEvent.StatStageInverted(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = latest.actorId,
							skillId = skill.skillId,
							stat = operation.stat,
							previousStage = previous,
							currentStage = next,
						),
					)
			}
		}

	/**
	 * 根据能力阶级操作目标解析当前参与成员集合。
	 *
	 * 返回值只包含当前可被运行态找到的成员；调用方再根据具体规则判断是否仍可战斗或是否被替身阻挡。这样复制、
	 * 交换和全场清除可以共享同一套目标解析口径。
	 */
	private fun operationParticipants(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		target: BattleStatStageOperationTarget,
	): List<BattleParticipant> =
		when (target) {
			BattleStatStageOperationTarget.USER -> listOfNotNull(state.participant(actorId))
			BattleStatStageOperationTarget.TARGET -> listOfNotNull(state.participant(targetActorId))
			BattleStatStageOperationTarget.ALL_ACTIVE -> state.sides.flatMap { side ->
				side.activeParticipants().filter { it.canBattle() }
			}
		}

	/**
	 * 解析必须唯一的能力阶级操作参与成员。
	 *
	 * 复制和交换在双打里如果遇到非唯一范围会直接跳过，避免把一个“来源”或“目标”隐式扩展成多名成员。资料模型
	 * 已经禁止 ALL_ACTIVE 作为来源；这里保留 singleOrNull 是为了让运行态规则对未来扩展仍然显式失败。
	 */
	private fun operationParticipant(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		target: BattleStatStageOperationTarget,
	): BattleParticipant? =
		operationParticipants(state, actorId, targetActorId, target).singleOrNull()

	/**
	 * 判断某个能力阶级操作是否被目标替身阻挡。
	 *
	 * 只有把效果施加到本次技能的 TARGET 时才检查替身；USER 和 ALL_ACTIVE 代表自身/全场规则，不应被目标替身拦截。
	 * 实际的“是否是对手、是否声音类技能”等替身细节由主引擎提供的回调判断，避免在这里复制另一份替身规则。
	 */
	private fun operationBlockedBySubstitute(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		target: BattleStatStageOperationTarget,
	): Boolean =
		target == BattleStatStageOperationTarget.TARGET &&
			substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)
}
