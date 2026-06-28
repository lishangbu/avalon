package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证特性提升变化类技能优先度，以及随该优先度提升产生的恶属性目标免疫。
 *
 * 场景类型：行动顺序和目标前置条件 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，这类特性让变化技能获得额外优先度；
 * 如果变化技能因为该特性获得先制并指向对手恶属性目标，目标不会受到该技能影响。同侧辅助目标不触发该免疫。
 */
class BattleStatusPriorityAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `status priority ability moves status skill before faster opponent`() {
		val fixture = publicBattleRuleFixture(
			name = "status-priority-ability-moves-status-skill-before-faster-opponent",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Prankster_(Ability)",
			),
			inputSummary = "低速成员拥有让变化技能获得额外优先度的结构化特性，对手速度更高，双方都使用基础优先度为 0 的变化技能。",
			expectedSummary = "拥有特性的成员先行动，事件流中的技能使用顺序先于高速对手。",
		)
		val priorityStatusSkill = targetAttackStageSkill(name = "先制变化测试")
		val fasterStatusSkill = selfAttackStageSkill(name = "高速变化测试")
		val state = engine.start(
			initialState(
				first = participant(
					"status-priority-user",
					speed = 40,
					skill = priorityStatusSkill,
					abilityId = 158,
					abilityEffects = listOf(BattleAbilityEffect.StatusSkillPriorityBoost()),
				),
				second = participant("faster-opponent", speed = 120, skill = fasterStatusSkill),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("status-priority-user", skillId = 1, targetActorId = "faster-opponent"),
				BattleAction.UseSkill("faster-opponent", skillId = 1, targetActorId = "faster-opponent"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-priority-ability-moves-status-skill-before-faster-opponent")
		assertEquals(
			listOf("status-priority-user", "faster-opponent"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
		assertEquals(0, resolved.participant("faster-opponent")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `dark target blocks opponent status skill boosted by status priority ability`() {
		val fixture = publicBattleRuleFixture(
			name = "dark-target-blocks-opponent-status-skill-boosted-by-status-priority-ability",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Prankster_(Ability)",
			),
			inputSummary = "拥有变化技能先制度特性的成员对对手恶属性目标使用基础优先度为 0 的变化技能。",
			expectedSummary = "技能消耗 PP 后被目标恶属性免疫，不产生能力阶级变化或其它附加效果。",
		)
		val priorityStatusSkill = targetAttackStageSkill(name = "恶属性免疫测试")
		val state = engine.start(
			initialState(
				first = participant(
					"status-priority-user",
					speed = 40,
					skill = priorityStatusSkill,
					abilityId = 158,
					abilityEffects = listOf(BattleAbilityEffect.StatusSkillPriorityBoost()),
				),
				second = participant("dark-target", speed = 120, elementId = 17),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-priority-user", skillId = 1, targetActorId = "dark-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByElement>().single()

		fixture.assertNamed("dark-target-blocks-opponent-status-skill-boosted-by-status-priority-ability")
		assertEquals(34, resolved.participant("status-priority-user")?.skillSlot(1)?.remainingPp)
		assertEquals(17, blocked.elementId)
		assertEquals("dark-target", blocked.targetActorId)
		assertEquals(0, resolved.participant("dark-target")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
	}

	@Test
	fun `status priority ability does not make ally dark target immune`() {
		val fixture = publicBattleRuleFixture(
			name = "status-priority-ability-does-not-make-ally-dark-target-immune",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Prankster_(Ability)",
			),
			inputSummary = "双打中拥有变化技能先制度特性的成员对同侧恶属性伙伴使用变化技能。",
			expectedSummary = "恶属性免疫只针对对手目标；同侧伙伴不会阻挡该技能，附加效果正常结算。",
		)
		val priorityStatusSkill = targetAttackStageSkill(name = "同侧辅助测试")
		val state = engine.start(
			doubleInitialState(
				firstA = participant(
					"status-priority-user",
					speed = 40,
					skill = priorityStatusSkill,
					abilityId = 158,
					abilityEffects = listOf(BattleAbilityEffect.StatusSkillPriorityBoost()),
				),
				firstB = participant("dark-ally", speed = 80, elementId = 17),
				secondA = participant("opponent-left", speed = 120),
				secondB = participant("opponent-right", speed = 110),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-priority-user", skillId = 1, targetActorId = "dark-ally")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-priority-ability-does-not-make-ally-dark-target-immune")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByElement>())
		assertEquals(-1, resolved.participant("dark-ally")?.statStage(BattleStat.ATTACK))
		assertEquals(listOf("dark-ally"), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().map { it.targetActorId })
	}

	private fun targetAttackStageSkill(name: String) =
		damagingSkill(
			name = name,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = 100,
				),
			),
		)

	private fun selfAttackStageSkill(name: String) =
		damagingSkill(
			name = name,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.USER,
					stageDelta = 1,
					chancePercent = 100,
				),
			),
		)
}
