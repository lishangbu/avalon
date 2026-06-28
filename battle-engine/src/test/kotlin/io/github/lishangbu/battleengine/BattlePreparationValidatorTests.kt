package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.MAX_BATTLE_SKILL_SLOTS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证战斗准备阶段规则校验器。
 *
 * 场景类型：准备阶段规则 fixture。
 * 参考来源类型：现代对战常见赛制条款和禁用规则；本测试不依赖数据库，只验证规则快照被引擎消费后的
 * 可执行行为。
 * 验证重点：等级上限、禁用成员/技能/特性/道具、唯一种类和唯一道具都能产出稳定违规 code。
 */
class BattlePreparationValidatorTests {
	private val validator = BattlePreparationValidator()

	@Test
	fun `valid initial state has no preparation violations`() {
		val violations = validator.validate(initialState())

		assertEquals(emptyList(), violations)
	}

	@Test
	fun `validator reports level and banned resource violations`() {
		val restricted = participant(
			"restricted",
			speed = 100,
			skill = damagingSkill(skillId = 99),
			abilityId = 88,
			itemId = 77,
		).copy(
			creatureId = 66,
			level = 60,
		)
		val state = initialState(
			first = restricted,
			second = participant("opponent", speed = 80),
			rules = neutralRules().copy(
				maxParticipantLevel = 50,
				bannedCreatureIds = setOf(66),
				bannedSkillIds = setOf(99),
				bannedAbilityIds = setOf(88),
				bannedItemIds = setOf(77),
			),
		)

		val violations = validator.validate(state)

		assertEquals(
			listOf("level-too-high", "banned-creature", "banned-skill", "banned-ability", "banned-item"),
			violations.map { it.code },
		)
		assertEquals(listOf("restricted"), violations.map { it.actorId }.distinct())
	}

	@Test
	fun `validator reports duplicate creature and item per side`() {
		val first = participant("first", speed = 100, itemId = 10).copy(creatureId = 20)
		val duplicate = participant("duplicate", speed = 90, itemId = 10).copy(creatureId = 20)
		val state = initialState(
			first = first,
			firstBench = listOf(duplicate),
			second = participant("opponent", speed = 80, itemId = 10).copy(creatureId = 20),
			rules = neutralRules().copy(
				uniqueCreatureRequired = true,
				uniqueItemRequired = true,
			),
		)

		val violations = validator.validate(state)

		assertEquals(2, violations.count { it.code == "duplicate-creature" })
		assertEquals(2, violations.count { it.code == "duplicate-item" })
		assertEquals(listOf("first", "duplicate"), violations.map { it.actorId }.distinct())
	}

	@Test
	fun `require valid throws when violations exist`() {
		val state = initialState(
			first = participant("restricted", speed = 100).copy(level = 60),
			rules = neutralRules().copy(maxParticipantLevel = 50),
		)

		assertFailsWith<IllegalArgumentException> {
			validator.requireValid(state)
		}
	}

	@Test
	fun `participant rejects more than four skill slots`() {
		val skills = (1L..(MAX_BATTLE_SKILL_SLOTS + 1L)).map { skillId ->
			damagingSkill(skillId = skillId, name = "技能$skillId")
		}

		assertFailsWith<IllegalArgumentException> {
			participant("too-many-skills", speed = 100).copy(skillSlots = skills)
		}
	}
}
