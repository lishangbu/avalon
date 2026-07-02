package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证成功造成伤害后移除使用者自身属性的技能规则。
 *
 * 场景类型：技能命中后使用者属性变化 场景。
 * 参考来源类型：本地中文资料集对现代技能效果的结构化整理，以及公开成熟对战实现中的事件顺序。
 * 验证重点：技能使用者必须先拥有技能基础属性；技能成功造成本体或替身伤害后，移除使用者对应属性。双属性成员
 * 只移除匹配的技能属性，保留其它属性；移除唯一属性后允许暂时成为无属性。
 */
class BattleUserElementRemovalSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `successful damage removes user's sole matching element`() {
		val scenario = publicBattleRuleScenario(
			name = "successful-damage-removes-user-sole-matching-element",
			inputSummary = "单火属性使用者以成功后移除自身火属性的特殊技能命中目标本体。",
			expectedSummary = "技能造成实际伤害后，使用者属性集合从火属性变为空集合，并记录属性变化事件。",
		)
		val skill = userElementRemovalSkill(elementId = 10, damageClass = BattleDamageClass.SPECIAL)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, elementId = 10, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = skill.skillId, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("successful-damage-removes-user-sole-matching-element")
		assertTrue(resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount > 0)
		assertEquals(emptySet(), resolved.participant("user")?.elementIds)
		val event = resolved.events.filterIsInstance<BattleEvent.ParticipantElementsChanged>().single()
		assertEquals("user", event.actorId)
		assertEquals(setOf(10L), event.previousElementIds)
		assertEquals(emptySet(), event.newElementIds)
	}

	@Test
	fun `successful damage removes only matching element from dual element user`() {
		val scenario = publicBattleRuleScenario(
			name = "successful-damage-removes-only-matching-element-from-dual-element-user",
			inputSummary = "火毒双属性使用者以成功后移除自身火属性的特殊技能命中目标本体。",
			expectedSummary = "技能造成实际伤害后只移除火属性，使用者仍保留毒属性。",
		)
		val skill = userElementRemovalSkill(elementId = 10, damageClass = BattleDamageClass.SPECIAL)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, elementId = 10, skill = skill).copy(elementIds = setOf(10L, 4L)),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = skill.skillId, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("successful-damage-removes-only-matching-element-from-dual-element-user")
		assertEquals(setOf(4L), resolved.participant("user")?.elementIds)
		val event = resolved.events.filterIsInstance<BattleEvent.ParticipantElementsChanged>().single()
		assertEquals(setOf(10L, 4L), event.previousElementIds)
		assertEquals(setOf(4L), event.newElementIds)
	}

	@Test
	fun `user element removal skill fails when user lacks matching element`() {
		val scenario = publicBattleRuleScenario(
			name = "user-element-removal-skill-fails-when-user-lacks-matching-element",
			inputSummary = "普通属性使用者尝试使用要求自身拥有火属性、成功后移除火属性的技能。",
			expectedSummary = "技能在命中随机前失败，不造成伤害，也不会产生属性变化事件。",
		)
		val skill = userElementRemovalSkill(elementId = 10, damageClass = BattleDamageClass.SPECIAL)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, elementId = 1, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = skill.skillId, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("user-element-removal-skill-fails-when-user-lacks-matching-element")
		assertEquals(setOf(1L), resolved.participant("user")?.elementIds)
		assertEquals("user-lacks-removable-element", resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.ParticipantElementsChanged>())
	}

	@Test
	fun `substitute damage still removes matching user element`() {
		val scenario = publicBattleRuleScenario(
			name = "substitute-damage-still-removes-matching-user-element",
			inputSummary = "单电属性使用者以成功后移除自身电属性的物理技能命中目标替身。",
			expectedSummary = "替身承受实际伤害也算技能成功造成伤害，使用者随后失去电属性。",
		)
		val skill = userElementRemovalSkill(skillId = 892, elementId = 13, damageClass = BattleDamageClass.PHYSICAL, power = 120)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, elementId = 13, skill = skill),
				second = participant("target", speed = 50).copy(substituteHp = 30),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = skill.skillId, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("substitute-damage-still-removes-matching-user-element")
		assertEquals(30, resolved.events.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single().amount)
		assertEquals(emptySet(), resolved.participant("user")?.elementIds)
		assertEquals(emptySet(), resolved.events.filterIsInstance<BattleEvent.ParticipantElementsChanged>().single().newElementIds)
	}

	private fun userElementRemovalSkill(
		skillId: Long = 682,
		elementId: Long,
		damageClass: BattleDamageClass,
		power: Int = 130,
	): BattleSkillSlot =
		damagingSkill(
			skillId = skillId,
			name = "属性移除伤害测试",
			elementId = elementId,
			damageClass = damageClass,
			power = power,
			removesUserElementAfterDamage = true,
		)
}
