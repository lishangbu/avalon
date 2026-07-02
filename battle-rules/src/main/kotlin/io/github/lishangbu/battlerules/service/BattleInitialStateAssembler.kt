package io.github.lishangbu.battlerules.service

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
		val elementIds = dataLookup.coreElementIds()
		val runtime = dataLookup.runtimeSnapshotByFormatCode(normalized.formatCode, elementIds)
		val cache = BattleRuntimeAssemblyCache(elementIds)
		return BattleInitialState(
			format = runtime.format,
			rules = runtime.rules,
			sides = normalized.sides.map { it.toBattleSide(cache) },
		)
	}

	private fun BattlePreparationValidationRequest.normalized(): BattlePreparationValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
		)

	private fun BattlePreparationSideRequest.toBattleSide(cache: BattleRuntimeAssemblyCache): BattleSide {
		val normalizedSideId = sideId.requiredText("sideId", maxLength = 80)
		if (activeActorIds.isEmpty()) {
			invalidValue("activeActorIds", "activeActorIds 不能为空")
		}
		if (participants.isEmpty()) {
			invalidValue("participants", "participants 不能为空")
		}
		return BattleSide(
			sideId = normalizedSideId,
			activeActorIds = activeActorIds.map { it.requiredText("activeActorIds", maxLength = 80) },
			participants = participants.map { it.toBattleParticipant(cache) },
		)
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
