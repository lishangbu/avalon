package io.github.lishangbu.battlerules.coverage

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** 验证技能覆盖矩阵沿用统一报告格式，并保留四类规则策略的诊断信息。 */
class BattleSkillCoverageMatrixTests {
	@Test
	fun `matrix renders covered skill and missing behavior evidence`() {
		val report = BattleSkillCoverageMatrix().render(
			listOf(
				BattleSkillCoverageEntry(
					code = "covered-skill",
					name = "已覆盖技能",
					enabled = true,
					policies = listOf(
						"effect:standard-damage",
						"target:selected-target",
						"hit:standard-hit",
						"damage:standard-damage",
					),
					jimmerLoaded = true,
					runtimeSupported = true,
					behaviorTestClasses = setOf("BattleDamageCalculatorTests"),
					unverifiedPolicies = emptyList(),
				),
				BattleSkillCoverageEntry(
					code = "missing-skill",
					name = "待补技能",
					enabled = true,
					policies = listOf("effect:missing-effect"),
					jimmerLoaded = true,
					runtimeSupported = true,
					behaviorTestClasses = emptySet(),
					unverifiedPolicies = listOf("effect:missing-effect"),
				),
			),
		)

		assertEquals(
			"""
			# 战斗技能覆盖矩阵

			> 本文件由 `./gradlew :battle-rules:generateBattleSkillCoverage` 生成，请勿手工编辑。
			> 测试证据按共享运行时机制统计，不表示每项资料的全部参数组合都由同名测试逐一执行。

			## 汇总

			- 启用技能：2
			- 已启用资料：2
			- Jimmer 规则读取完整：2
			- 运行时策略完整：2
			- 具备运行时机制测试证据：1
			- 待补缺口：1

			## 待补缺口

			| 技能 | 资料 | 规则策略 | Jimmer | 运行时 | 机制测试 | 待补策略 |
			| --- | --- | --- | --- | --- | --- | --- |
			| `missing-skill` 待补技能 | 启用 | `effect:missing-effect` | 完整 | 支持 | — | `effect:missing-effect` |

			## 完整矩阵

			| 技能 | 资料 | 规则策略 | Jimmer | 运行时 | 机制测试 | 待补策略 |
			| --- | --- | --- | --- | --- | --- | --- |
			| `covered-skill` 已覆盖技能 | 启用 | `effect:standard-damage`, `target:selected-target`, `hit:standard-hit`, `damage:standard-damage` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
			| `missing-skill` 待补技能 | 启用 | `effect:missing-effect` | 完整 | 支持 | — | `effect:missing-effect` |
			""".trimIndent() + "\n",
			report,
		)
	}
}
