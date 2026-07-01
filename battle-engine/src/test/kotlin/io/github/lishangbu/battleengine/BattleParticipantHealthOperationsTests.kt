package io.github.lishangbu.battleengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证 `BattleParticipant` 的 HP 与替身底层状态迁移。
 *
 * 这些测试不经过完整回合引擎，是为了把“纯状态快照如何变化”和“回合流程如何产生事件”分开验证。完整引擎测试
 * 已经覆盖替身事件、伤害事件和倒下事件；这里固定的是更底层的不变量：HP 不会越界，替身伤害不会溢出到本体，
 * 建立替身时会同时扣除本体 HP 并写入替身 HP。
 */
class BattleParticipantHealthOperationsTests {
	@Test
	fun `damage and healing clamp hp and update battle availability`() {
		val actor = participant("health-target", speed = 100, currentHp = 40)

		val fainted = actor.receiveDamage(999)
		val fullyHealed = fainted.heal(999)

		assertEquals(0, fainted.currentHp)
		assertFalse(fainted.canBattle())
		assertEquals(100, fullyHealed.currentHp)
		assertTrue(fullyHealed.canBattle())
	}

	@Test
	fun `substitute uses paid hp as substitute hp and only damages substitute`() {
		val actor = participant("substitute-user", speed = 100, currentHp = 100)

		val started = actor.startSubstitute(25)
		val partiallyDamaged = started.damageSubstitute(10)
		val broken = partiallyDamaged.damageSubstitute(999)

		assertEquals(75, started.currentHp)
		assertEquals(25, started.substituteHp)
		assertTrue(started.hasSubstitute())
		assertEquals(75, partiallyDamaged.currentHp)
		assertEquals(15, partiallyDamaged.substituteHp)
		assertEquals(75, broken.currentHp)
		assertEquals(0, broken.substituteHp)
		assertFalse(broken.hasSubstitute())
	}
}
