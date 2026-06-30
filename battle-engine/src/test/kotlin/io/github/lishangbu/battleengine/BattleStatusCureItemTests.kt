package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证主要异常状态解除携带道具的现代规则。
 *
 * 场景类型：道具 after-status 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，特定携带道具不会阻止主要异常状态写入；
 * 它会在状态已经成为事实后立刻触发治愈，并通常消费自身。测试刻意覆盖技能附加状态和接触特性返还状态两个入口，
 * 确认二者都复用同一个状态附加后道具钩子。
 */
class BattleStatusCureItemTests {
	private val engine = BattleEngine()

	@Test
	fun `major status cure item clears status after successful status skill and consumes item`() {
		val scenario = publicBattleRuleScenario(
			name = "major-status-cure-item-clears-status-after-application",
			inputSummary = "目标携带可解除任意主要异常状态的一次性道具，对手使用 100% 附加灼伤的变化技能。",
			expectedSummary = "灼伤先写入目标运行态，随后携带道具立即解除灼伤并被消费。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = statusSkill(BattleMajorStatus.BURN)),
				second = participant(
					"holder",
					speed = 80,
					itemId = 134,
					itemEffects = listOf(BattleItemEffect.MajorStatusCure(allMajorStatuses)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 2, targetActorId = "holder")),
			ScriptedBattleRandom(emptyList()),
		)
		val holder = requireNotNull(resolved.participant("holder"))
		val applied = resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single()
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()

		scenario.assertNamed("major-status-cure-item-clears-status-after-application")
		assertEquals(null, holder.majorStatus)
		assertEquals(null, holder.itemId)
		assertEquals(emptyList(), holder.itemEffects)
		assertEquals(BattleMajorStatus.BURN, applied.status)
		assertEquals(BattleMajorStatus.BURN, cleared.status)
		assertTrue(resolved.events.indexOf(applied) < resolved.events.indexOf(cleared))
	}

	@Test
	fun `major status cure item also clears contact ability status on attacker`() {
		val scenario = publicBattleRuleScenario(
			name = "major-status-cure-item-clears-contact-ability-status",
			inputSummary = "攻击方携带主要异常状态解除道具，使用接触类技能命中拥有接触后麻痹攻击方特性的目标。",
			expectedSummary = "攻击方先因目标特性获得麻痹，随后自身携带道具立即解除麻痹并被消费。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true),
					itemId = 134,
					itemEffects = listOf(BattleItemEffect.MajorStatusCure(allMajorStatuses)),
				),
				second = participant(
					"defender",
					speed = 80,
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.PARALYSIS,
							chancePercent = 100,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val attacker = requireNotNull(resolved.participant("attacker"))
		val applied = resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single()
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()

		scenario.assertNamed("major-status-cure-item-clears-contact-ability-status")
		assertEquals(null, attacker.majorStatus)
		assertEquals(null, attacker.itemId)
		assertEquals(emptyList(), attacker.itemEffects)
		assertEquals("defender", applied.actorId)
		assertEquals("attacker", applied.targetActorId)
		assertEquals(BattleMajorStatus.PARALYSIS, cleared.status)
		assertTrue(resolved.events.indexOf(applied) < resolved.events.indexOf(cleared))
	}

	@Test
	fun `major status cure item keeps unmatched status and is not consumed`() {
		val scenario = publicBattleRuleScenario(
			name = "specific-status-cure-item-keeps-unmatched-status",
			inputSummary = "目标携带只解除睡眠的一次性道具，对手使用 100% 附加灼伤的变化技能。",
			expectedSummary = "灼伤不是该道具声明的解除状态，因此状态保留，道具也不被消费。",
		)
		val sleepOnlyCure = BattleItemEffect.MajorStatusCure(setOf(BattleMajorStatus.SLEEP))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = statusSkill(BattleMajorStatus.BURN)),
				second = participant(
					"holder",
					speed = 80,
					itemId = 127,
					itemEffects = listOf(sleepOnlyCure),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 2, targetActorId = "holder")),
			ScriptedBattleRandom(emptyList()),
		)
		val holder = requireNotNull(resolved.participant("holder"))

		scenario.assertNamed("specific-status-cure-item-keeps-unmatched-status")
		assertEquals(BattleMajorStatus.BURN, holder.majorStatus)
		assertEquals(127, holder.itemId)
		assertEquals(listOf(sleepOnlyCure), holder.itemEffects)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusCleared>())
	}

	private fun statusSkill(status: BattleMajorStatus) =
		damagingSkill(
			skillId = 2,
			name = "状态技能",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = status,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private companion object {
		private val allMajorStatuses = setOf(
			BattleMajorStatus.BURN,
			BattleMajorStatus.PARALYSIS,
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON,
			BattleMajorStatus.SLEEP,
			BattleMajorStatus.FREEZE,
		)
	}
}
