package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.MAX_BATTLE_SKILL_SLOTS
import io.github.lishangbu.battlerules.dto.BattlePreparationParticipantRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationSideRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.requiredText
import org.springframework.stereotype.Component

/**
 * 战斗初始状态装配器。
 *
 * 本组件是 battle-rules 请求 DTO 到 battle-engine 纯模型的边界：它读取成员画像、技能槽、特性策略和道具策略，
 * 并把这些资料冻结成 [BattleInitialState]。它不执行准备阶段规则校验，也不校验行动提交；这些仍由
 * [BattleRuntimeSnapshotService] 调用 battle-engine 的校验器完成。
 *
 * 缓存只存在于单次装配调用内。这样同一份队伍里重复出现的成员、技能、特性或道具不会反复查询数据库，但管理端
 * 修改资料后，下一次请求仍会重新读取最新事实。
 */
@Component
class BattleInitialStateAssembler(
	private val dataLookup: BattleRuntimeDataLookup,
	private val effectAssembler: BattleRuntimeEffectAssembler,
) {
	fun assemble(request: BattlePreparationValidationRequest): BattleInitialState {
		val normalized = request.normalized()
		normalized.requireTwoSides()
		val elementIds = dataLookup.coreElementIds()
		val runtime = dataLookup.runtimeSnapshotByFormatCode(normalized.formatCode, elementIds)
		val cache = BattleRuntimeAssemblyCache(elementIds)
		val sides = normalized.sides.map { it.toBattleSide(cache) }
		validateInitialStateShape(runtime.format, sides)
		return BattleInitialState(
			format = runtime.format,
			rules = runtime.rules,
			sides = sides,
		)
	}

	private fun BattlePreparationValidationRequest.normalized(): BattlePreparationValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
		)

	/**
	 * 校验请求层面的战斗骨架数量。
	 *
	 * 当前引擎只支持两方对战，`BattleInitialState` 也会用 `require` 守住这个不变量；但装配器是 HTTP 请求进入
	 * 纯领域模型前的应用层边界，必须在这里返回稳定字段错误。这样准备校验、行动校验和沙盒入口都会得到同一套
	 * 400 响应，而不是把“少传一侧队伍”误报成内部异常。
	 */
	private fun BattlePreparationValidationRequest.requireTwoSides() {
		if (sides.size != 2) {
			invalidValue("sides", "sides 必须包含两侧队伍")
		}
	}

	private fun BattlePreparationSideRequest.toBattleSide(cache: BattleRuntimeAssemblyCache): BattleSide {
		val normalizedSideId = sideId.requiredText("sideId", maxLength = 80)
		if (activeActorIds.isEmpty()) {
			invalidValue("activeActorIds", "activeActorIds 不能为空")
		}
		if (participants.isEmpty()) {
			invalidValue("participants", "participants 不能为空")
		}
		val normalizedActiveActorIds = activeActorIds.map { it.requiredText("activeActorIds", maxLength = 80) }
		if (normalizedActiveActorIds.toSet().size != normalizedActiveActorIds.size) {
			invalidValue("activeActorIds", "activeActorIds 不能包含重复成员")
		}
		val battleParticipants = participants.map { it.toBattleParticipant(cache) }
		val participantActorIds = battleParticipants.map { it.actorId }
		if (participantActorIds.toSet().size != participantActorIds.size) {
			invalidValue("actorId", "同一队伍内 actorId 不能重复")
		}
		if (normalizedActiveActorIds.any { it !in participantActorIds }) {
			invalidValue("activeActorIds", "activeActorIds 必须属于当前队伍成员")
		}
		return BattleSide(
			sideId = normalizedSideId,
			activeActorIds = normalizedActiveActorIds,
			participants = battleParticipants,
		)
	}

	/**
	 * 校验已经装配出的双方骨架是否满足当前赛制。
	 *
	 * 这些条件和 [BattleInitialState] 的 `init` 不变量保持一致，但在应用层先转成可读字段错误：
	 * - 队伍侧编号不能重复。
	 * - actorId 不能跨队伍重复。
	 * - 每侧上场成员数量必须等于赛制要求。
	 * - 若赛制限定队伍人数，登记成员不能超过上限。
	 *
	 * 领域模型仍然保留自己的 `require`，作为单元测试或未来非 HTTP 调用路径的最后防线；这里负责生产 API 体验。
	 */
	private fun validateInitialStateShape(format: BattleFormatSnapshot, sides: List<BattleSide>) {
		if (sides.map { it.sideId }.toSet().size != sides.size) {
			invalidValue("sideId", "sideId 不能重复")
		}
		val actorIds = sides.flatMap { side -> side.participants.map { it.actorId } }
		if (actorIds.toSet().size != actorIds.size) {
			invalidValue("actorId", "actorId 不能跨队伍重复")
		}
		if (sides.any { it.activeActorIds.size != format.activeParticipantsPerSide }) {
			invalidValue("activeActorIds", "activeActorIds 数量必须符合赛制")
		}
		format.teamSize?.let { teamSize ->
			if (sides.any { it.participants.size > teamSize }) {
				invalidValue("participants", "participants 数量不能超过赛制队伍人数")
			}
		}
	}

	private fun BattlePreparationParticipantRequest.toBattleParticipant(cache: BattleRuntimeAssemblyCache): BattleParticipant {
		val normalizedActorId = actorId.requiredText("actorId", maxLength = 80)
		if (creatureId <= 0) {
			invalidValue("creatureId", "creatureId 必须大于 0")
		}
		if (level !in 1..100) {
			invalidValue("level", "level 必须在 1 到 100 之间")
		}
		if (skillIds.isEmpty()) {
			invalidValue("skillIds", "skillIds 不能为空")
		}
		if (skillIds.size > MAX_BATTLE_SKILL_SLOTS) {
			invalidValue("skillIds", "skillIds 最多只能包含 $MAX_BATTLE_SKILL_SLOTS 个技能")
		}
		if (skillIds.toSet().size != skillIds.size) {
			invalidValue("skillIds", "skillIds 不能包含重复技能")
		}
		abilityId?.takeIf { it <= 0 }?.let {
			invalidValue("abilityId", "abilityId 必须大于 0")
		}
		itemId?.takeIf { it <= 0 }?.let {
			invalidValue("itemId", "itemId 必须大于 0")
		}
		val abilityPolicies = cache.abilityPolicies(abilityId)
		val itemPolicies = cache.itemPolicies(itemId)
		val profile = cache.creatureProfile(creatureId, level)
		return BattleParticipant(
			actorId = normalizedActorId,
			creatureId = creatureId,
			level = level,
			maxHp = profile.maxHp,
			currentHp = profile.maxHp,
			attack = profile.attack,
			defense = profile.defense,
			specialAttack = profile.specialAttack,
			specialDefense = profile.specialDefense,
			speed = profile.speed,
			weight = profile.weight,
			elementIds = profile.elementIds,
			skillSlots = cache.skillSlots(skillIds),
			abilityId = abilityId,
			itemId = itemId,
			grounded = effectAssembler.grounded(abilityPolicies),
			abilityEffects = effectAssembler.abilityEffects(abilityPolicies, cache.elementIds),
			itemEffects = effectAssembler.itemEffects(itemPolicies, cache.elementIds),
		)
	}

	private inner class BattleRuntimeAssemblyCache(
		val elementIds: Map<String, Long>,
	) {
		private val creatureProfiles = mutableMapOf<Pair<Long, Int>, BattleCreatureRuntimeProfile>()
		private val skillSlots = mutableMapOf<Long, BattleSkillSlot>()
		private val abilityPolicies = mutableMapOf<Long, List<String>>()
		private val itemPolicies = mutableMapOf<Long, List<String>>()

		fun creatureProfile(creatureId: Long, level: Int): BattleCreatureRuntimeProfile =
			creatureProfiles.getOrPut(creatureId to level) {
				dataLookup.creatureRuntimeProfile(creatureId, level)
			}

		fun skillSlots(skillIds: List<Long>): List<BattleSkillSlot> {
			if (skillIds.isEmpty()) {
				invalidValue("skillIds", "skillIds 不能为空")
			}
			return skillIds.map { skillId ->
				if (skillId <= 0) {
					invalidValue("skillIds", "skillIds 只能包含正数 ID")
				}
				skillSlots.getOrPut(skillId) {
					dataLookup.skillSlotBySkillId(skillId)
				}
			}
		}

		fun abilityPolicies(abilityId: Long?): List<String> =
			abilityId?.let { id ->
				abilityPolicies.getOrPut(id) {
					dataLookup.enabledAbilityPolicies(id)
				}
			}.orEmpty()

		fun itemPolicies(itemId: Long?): List<String> =
			itemId?.let { id ->
				itemPolicies.getOrPut(id) {
					dataLookup.enabledItemPolicies(id)
				}
			}.orEmpty()
	}
}
