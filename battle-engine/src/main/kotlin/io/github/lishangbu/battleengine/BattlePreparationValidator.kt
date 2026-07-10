package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSide

/**
 * 战斗准备阶段校验器。
 *
 * 该类只消费已经冻结的 `BattleInitialState`，不访问数据库，也不理解管理端资料表结构。上层应用负责把
 * battle-rules 中的赛制条款和限制组装进 `BattleRuleSnapshot`，这里负责执行确定性的队伍合法性检查。
 *
 * 当前支持的现代规则边界：
 * - 等级上限。
 * - 禁用成员、技能、特性和道具。
 * - 单方队伍内唯一成员种类。
 * - 单方队伍内唯一携带道具。
 *
 * 校验器返回全部违规项，便于管理端或未来匹配服务一次性展示问题；`requireValid` 则提供引擎启动前的
 * fail-fast 入口。
 */
class BattlePreparationValidator {
	/**
	 * 返回初始战斗快照中的全部准备阶段违规项。
	 */
	fun validate(initialState: BattleInitialState): List<BattlePreparationViolation> {
		val ruleViolations = initialState.sides.flatMap { side ->
			side.participants.flatMap { participant -> participantViolations(initialState, side, participant) }
		}
		val clauseViolations = initialState.sides.flatMap { side ->
			uniqueCreatureViolations(initialState, side) + uniqueItemViolations(initialState, side)
		}
		return ruleViolations + clauseViolations
	}

	/**
	 * 若存在违规项则抛出异常。
	 *
	 * 该方法适合战斗启动入口调用；需要本地化或字段级展示时，应使用 `validate` 获取结构化结果。
	 */
	fun requireValid(initialState: BattleInitialState) {
		val violations = validate(initialState)
		require(violations.isEmpty()) {
			violations.joinToString(separator = "; ") { it.message }
		}
	}

	private fun participantViolations(
		initialState: BattleInitialState,
		side: BattleSide,
		participant: BattleParticipant,
	): List<BattlePreparationViolation> {
		val rules = initialState.rules
		val violations = mutableListOf<BattlePreparationViolation>()
		val maxLevel = rules.maxParticipantLevel
		if (maxLevel != null && participant.level > maxLevel) {
			violations += violation(
				code = "level-too-high",
				side = side,
				participant = participant,
				message = "成员等级 ${participant.level} 超过上限 $maxLevel",
			)
		}
		if (participant.creatureId in rules.bannedCreatureIds) {
			violations += violation(
				code = "banned-creature",
				side = side,
				participant = participant,
				message = "成员种类已被当前规则禁用: ${participant.creatureId}",
			)
		}
		participant.skillSlots
			.map { it.skillId }
			.filter { it in rules.bannedSkillIds }
			.forEach { skillId ->
				violations += violation(
					code = "banned-skill",
					side = side,
					participant = participant,
					message = "技能已被当前规则禁用: $skillId",
					resourceId = skillId,
				)
			}
		participant.abilityId
			?.takeIf { it in rules.bannedAbilityIds }
			?.let { abilityId ->
				violations += violation(
					code = "banned-ability",
					side = side,
					participant = participant,
					message = "特性已被当前规则禁用: $abilityId",
					resourceId = abilityId,
				)
			}
		participant.itemId
			?.takeIf { it in rules.bannedItemIds }
			?.let { itemId ->
				violations += violation(
					code = "banned-item",
					side = side,
					participant = participant,
					message = "道具已被当前规则禁用: $itemId",
					resourceId = itemId,
				)
			}
		return violations
	}

	private fun uniqueCreatureViolations(
		initialState: BattleInitialState,
		side: BattleSide,
	): List<BattlePreparationViolation> {
		if (!initialState.rules.uniqueCreatureRequired) {
			return emptyList()
		}
		return side.participants
			.groupBy { it.creatureId }
			.filterValues { it.size > 1 }
			.flatMap { (creatureId, participants) ->
				participants.map { participant ->
					violation(
						code = "duplicate-creature",
						side = side,
						participant = participant,
						message = "同一队伍内成员种类不能重复: $creatureId",
						resourceId = creatureId,
					)
				}
			}
	}

	private fun uniqueItemViolations(
		initialState: BattleInitialState,
		side: BattleSide,
	): List<BattlePreparationViolation> {
		if (!initialState.rules.uniqueItemRequired) {
			return emptyList()
		}
		return side.participants
			.mapNotNull { participant -> participant.itemId?.let { itemId -> itemId to participant } }
			.groupBy(keySelector = { it.first }, valueTransform = { it.second })
			.filterValues { it.size > 1 }
			.flatMap { (itemId, participants) ->
				participants.map { participant ->
					violation(
						code = "duplicate-item",
						side = side,
						participant = participant,
						message = "同一队伍内携带道具不能重复: $itemId",
						resourceId = itemId,
					)
				}
			}
	}

	private fun violation(
		code: String,
		side: BattleSide,
		participant: BattleParticipant,
		message: String,
		resourceId: Long? = null,
	): BattlePreparationViolation =
		BattlePreparationViolation(
			code = code,
			sideId = side.sideId,
			actorId = participant.actorId,
			resourceId = resourceId ?: participant.creatureId,
			message = message,
		)
}
