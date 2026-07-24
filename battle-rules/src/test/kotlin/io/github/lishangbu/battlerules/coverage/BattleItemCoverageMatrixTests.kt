package io.github.lishangbu.battlerules.coverage

import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证战斗道具覆盖矩阵使用稳定、可审查的 Markdown 契约。 */
class BattleItemCoverageMatrixTests {
	@Test
	fun `matrix separates verified held items from actionable gaps`() {
		val report = BattleItemCoverageMatrix().render(
			listOf(
				BattleItemCoverageEntry(
					code = "verified-item",
					name = "已验证道具",
					enabled = true,
					policies = listOf("verified-policy"),
					jimmerLoaded = true,
					runtimeSupported = true,
					behaviorTestClasses = setOf("BattleVerifiedItemTests"),
					unverifiedPolicies = emptyList(),
				),
				BattleItemCoverageEntry(
					code = "missing-test",
					name = "缺测试道具",
					enabled = true,
					policies = listOf("missing-policy"),
					jimmerLoaded = true,
					runtimeSupported = true,
					behaviorTestClasses = emptySet(),
					unverifiedPolicies = listOf("missing-policy"),
				),
			),
		)

		assertEquals(
			"""
			# 战斗道具覆盖矩阵

			> 本文件由 `./gradlew :battle-rules:generateBattleItemCoverage` 生成，请勿手工编辑。

			## 汇总

			- 携带道具：2
			- 已启用资料：2
			- Jimmer 规则读取完整：2
			- 运行时策略完整：2
			- 具备行为测试证据：1
			- 待补缺口：1

			## 待补缺口

			| 道具 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |
			| --- | --- | --- | --- | --- | --- | --- |
			| `missing-test` 缺测试道具 | 启用 | `missing-policy` | 完整 | 支持 | — | `missing-policy` |

			## 完整矩阵

			| 道具 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |
			| --- | --- | --- | --- | --- | --- | --- |
			| `missing-test` 缺测试道具 | 启用 | `missing-policy` | 完整 | 支持 | — | `missing-policy` |
			| `verified-item` 已验证道具 | 启用 | `verified-policy` | 完整 | 支持 | `BattleVerifiedItemTests` | — |
			""".trimIndent() + "\n",
			report,
		)
	}
}
