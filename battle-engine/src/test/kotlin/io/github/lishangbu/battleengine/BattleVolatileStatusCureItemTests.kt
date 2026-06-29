package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证临时状态解除携带道具的现代规则。
 *
 * 场景类型：道具 after-volatile-status fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。解除混乱的携带道具不会阻止混乱写入；混乱持续时间先按状态
 * 规则消费随机数并写入运行态，然后道具立即清除混乱并消费自身。这样 replay 能保留完整的“附加状态 -> 道具治愈”
 * 事实顺序。
 */
class BattleVolatileStatusCureItemTests {
	private val engine = BattleEngine()

	@Test
	fun `volatile status cure item clears confusion after application and consumes item`() {
		val fixture = publicBattleRuleFixture(
			name = "volatile-status-cure-item-clears-confusion-after-application",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Persim_Berry",
				"https://bulbapedia.bulbagarden.net/wiki/Confusion_(status_condition)",
			),
			inputSummary = "目标携带解除混乱的一次性道具，对手使用 100% 附加混乱的变化技能。",
			expectedSummary = "混乱先写入目标运行态并消费持续时间随机数，随后携带道具立即解除混乱并被消费。",
		)
		val random = ScriptedBattleRandom(listOf(1))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = confusionSkill()),
				second = participant(
					"holder",
					speed = 80,
					itemId = 133,
					itemEffects = listOf(
						BattleItemEffect.VolatileStatusCure(setOf(BattleVolatileStatus.CONFUSION)),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 2, targetActorId = "holder")),
			random,
		)
		val holder = requireNotNull(resolved.participant("holder"))
		val applied = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single()
		val cleared = resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().single()

		fixture.assertNamed("volatile-status-cure-item-clears-confusion-after-application")
		assertEquals(0, holder.confusionTurnsRemaining)
		assertEquals(null, holder.itemId)
		assertEquals(emptyList(), holder.itemEffects)
		assertEquals(BattleVolatileStatus.CONFUSION, applied.status)
		assertEquals(BattleVolatileStatus.CONFUSION, cleared.status)
		assertEquals(listOf("confusion duration for 2"), random.consumedReasons())
		assertTrue(resolved.events.indexOf(applied) < resolved.events.indexOf(cleared))
	}

	private fun confusionSkill() =
		damagingSkill(
			skillId = 2,
			name = "混乱技能",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
}
