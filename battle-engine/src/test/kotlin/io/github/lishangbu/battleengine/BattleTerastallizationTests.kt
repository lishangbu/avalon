package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleTerastallizationTests {
	private val engine = BattleEngine()

	@Test
	fun `tera is bound to a skill action and consumes the side opportunity before move resolution`() {
		val state = engine.start(initialState(
			first = participant(
				"first",
				speed = 100,
				elementId = 1,
				teraElementId = 2,
				skill = damagingSkill(elementId = 2),
			),
			second = participant("second", speed = 50),
		))

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("first", 1, "second", terastallize = true),
				BattleAction.UseSkill("second", 1, "first"),
			),
			ScriptedBattleRandom(listOf(15, 15, 15, 15)),
		)

		val first = resolved.participant("first")!!
		assertTrue(first.terastallized)
		assertEquals(setOf(2L), first.elementIds)
		assertEquals(setOf(1L), first.originalElementIds)
		assertTrue(resolved.sideOf("first")!!.terastallizationUsed)
		assertTrue(resolved.events.any { it is BattleEvent.ParticipantTerastallized && it.actorId == "first" })
	}

	@Test
	fun `validator rejects a second tera use from the same side`() {
		val first = participant("first", speed = 100, teraElementId = 2)
		val started = engine.start(initialState(first = first, second = participant("second", speed = 50)))
		val state = started.copy(
			sides = started.sides.map { side ->
				if (side.sideId == "side-a") side.copy(terastallizationUsed = true) else side
			},
		)

		val violations = BattleActionValidator().validate(
			state,
			listOf(BattleAction.UseSkill("first", 1, "second", terastallize = true)),
		)

		assertTrue(violations.any { it.code == "tera-already-used" })
	}
}
