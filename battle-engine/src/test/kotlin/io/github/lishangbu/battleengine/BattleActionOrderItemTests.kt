package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 验证同优先度技能行动中的先行、后行与低体力一次性顺序道具。 */
class BattleActionOrderItemTests {
	private val engine = BattleEngine()

	@Test
	fun `quick claw activation lets slower holder act first`() {
		val resolved = resolve(
			first = participant("holder", speed = 50, itemId = 217, itemEffects = listOf(BattleItemEffect.RandomActionOrderBoost(20))),
			second = participant("fast", speed = 100),
			random = ScriptedBattleRandom(listOf(0, 1, 15, 1, 15)),
		)
		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}

	@Test
	fun `lagging tail makes faster holder act last`() {
		val resolved = resolve(
			first = participant("holder", speed = 100, itemId = 279, itemEffects = listOf(BattleItemEffect.ForcedLastActionOrder())),
			second = participant("slow", speed = 50),
			random = ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		assertEquals("slow", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}

	@Test
	fun `custap berry lets low hp holder act first and is consumed`() {
		val resolved = resolve(
			first = participant("holder", speed = 50, currentHp = 25, itemId = 210, itemEffects = listOf(BattleItemEffect.LowHpActionOrderBoost())),
			second = participant("fast", speed = 100),
			random = ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
		assertNull(resolved.participant("holder")?.itemId)
	}

	private fun resolve(first: io.github.lishangbu.battleengine.model.BattleParticipant, second: io.github.lishangbu.battleengine.model.BattleParticipant, random: ScriptedBattleRandom) =
		engine.resolveTurn(
			engine.start(initialState(first = first, second = second)),
			listOf(BattleAction.UseSkill(first.actorId, 1, second.actorId), BattleAction.UseSkill(second.actorId, 1, first.actorId)),
			random,
		)
}
