package io.github.lishangbu.battleengine.random

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 验证生产随机源的基础边界。
 *
 * 这里不测试随机分布；分布质量由 JDK 的 [java.security.SecureRandom] 保证，单元测试只固定两件调用方可依赖的
 * 契约：返回值必须落在 `[0, bound)`，非法上界必须在进入底层随机算法前被拒绝。这样真实对战入口可以直接使用
 * [SecureBattleRandom]，公开规则测试仍继续使用脚本或 trace 随机源固定具体随机值。
 */
class SecureBattleRandomTests {
	@Test
	fun `returns values inside requested bound`() {
		val random = SecureBattleRandom(Random(123))

		val values = (1..50).map { index -> random.nextInt(7, "production random sample $index") }

		assertTrue(values.all { it in 0 until 7 })
	}

	@Test
	fun `rejects non positive bound`() {
		val random = SecureBattleRandom(Random(123))

		assertFailsWith<IllegalArgumentException> {
			random.nextInt(0, "invalid bound")
		}
	}
}
