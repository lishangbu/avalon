package io.github.lishangbu.battleengine.random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证脚本随机源的消费顺序。
 *
 * 场景类型：确定性 replay 基础设施。
 * 参考来源类型：公式级自有 fixture；后续公开对照 fixture 会依赖同一个随机源固定命中、伤害浮动和同速排序。
 * 验证重点：每一次随机消费都必须按顺序记录原因，脚本耗尽或越界时立即失败。
 */
class ScriptedBattleRandomTests {
	@Test
	fun `consumes scripted values and records reasons`() {
		val random = ScriptedBattleRandom(listOf(3, 7))

		assertEquals(3, random.nextInt(10, "accuracy"))
		assertEquals(7, random.nextInt(16, "damage"))

		assertEquals(listOf("accuracy", "damage"), random.consumedReasons())
		assertTrue(random.isFullyConsumed())
	}

	@Test
	fun `rejects exhausted script and out of range value`() {
		val exhausted = ScriptedBattleRandom(emptyList())
		assertFailsWith<IllegalStateException> {
			exhausted.nextInt(10, "accuracy")
		}

		val outOfRange = ScriptedBattleRandom(listOf(10))
		assertFailsWith<IllegalStateException> {
			outOfRange.nextInt(10, "damage")
		}
		assertFalse(outOfRange.isFullyConsumed())
	}
}
