package io.github.lishangbu.battlerules.coverage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 验证战斗特性覆盖矩阵使用稳定、可审查的 Markdown 契约。 */
class BattleAbilityCoverageMatrixTests {
	@Test
	fun `matrix separates verified abilities from actionable gaps`() {
		val report = BattleAbilityCoverageMatrix().render(
			listOf(
				BattleAbilityCoverageEntry(
					code = "verified-ability",
					name = "已验证特性",
					enabled = true,
					policies = listOf("verified-policy"),
					jimmerLoaded = true,
					runtimeSupported = true,
					behaviorTestClasses = setOf("BattleVerifiedAbilityTests"),
					unverifiedPolicies = emptyList(),
				),
				BattleAbilityCoverageEntry(
					code = "missing-test",
					name = "缺测试特性",
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
			# 战斗特性覆盖矩阵

			> 本文件由 `./gradlew :battle-rules:generateBattleAbilityCoverage` 生成，请勿手工编辑。

			## 汇总

			- 主系列特性：2
			- 已启用资料：2
			- Jimmer 规则读取完整：2
			- 运行时策略完整：2
			- 具备行为测试证据：1
			- 待补缺口：1

			## 待补缺口

			| 特性 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |
			| --- | --- | --- | --- | --- | --- | --- |
			| `missing-test` 缺测试特性 | 启用 | `missing-policy` | 完整 | 支持 | — | `missing-policy` |

			## 完整矩阵

			| 特性 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |
			| --- | --- | --- | --- | --- | --- | --- |
			| `missing-test` 缺测试特性 | 启用 | `missing-policy` | 完整 | 支持 | — | `missing-policy` |
			| `verified-ability` 已验证特性 | 启用 | `verified-policy` | 完整 | 支持 | `BattleVerifiedAbilityTests` | — |
			""".trimIndent() + "\n",
			report,
		)
	}

	@Test
	fun `matrix classifies intentional no effect policies without creating a gap`() {
		val report = BattleAbilityCoverageMatrix().render(
			listOf(
				BattleAbilityCoverageEntry(
					code = "intentional-no-effect",
					name = "明确无效果特性",
					enabled = true,
					policies = listOf("single-battle-no-effect"),
					jimmerLoaded = true,
					runtimeSupported = true,
					behaviorTestClasses = emptySet(),
					unverifiedPolicies = emptyList(),
					intentionalNoEffectPolicies = listOf("single-battle-no-effect"),
				),
			),
		)
		val gapSection = report.substringAfter("## 待补缺口").substringBefore("## 完整矩阵")

		assertFalse("intentional-no-effect" in gapSection)
		assertTrue("不适用（明确无效果）" in report)
		assertTrue("- 明确无效果契约：1" in report)
	}
}
