package io.github.lishangbu.battleengine

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 战斗引擎规则覆盖账本。
 *
 * 数据库场景管理已移除，规则正确性的事实源回到单元测试本身。本测试不执行战斗逻辑；
 * 它固定 V1 的 312 条现代主系列规则行为拆分，并确保每个规则族至少绑定一组真实存在的行为测试类。
 *
 * 新增普通资料时不需要修改本账本；只有新增触发时机、事件顺序、取整位置、随机消费或状态不变量时，
 * 才需要新增行为测试并调整对应规则族的 `ruleCount` 或测试文件。
 */
class BattleRuleCoverageLedgerTests {
	@Test
	fun `modern battle rule ledger keeps all covered kotlin test files documented`() {
		assertEquals(312, coverageGroups.sumOf { it.ruleCount })
		assertEquals(12, coverageGroups.size)
		assertEquals(coverageGroups.size, coverageGroups.map { it.code }.toSet().size)
		assertEquals(35, coverageGroups.flatMap { it.testClassNames }.distinct().size)

		coverageGroups.forEach { group ->
			assertTrue(group.ruleCount > 0, "${group.code} should cover at least one rule")
			assertTrue(group.description.isNotBlank(), "${group.code} should document coverage purpose")
			assertTrue(group.testClassNames.isNotEmpty(), "${group.code} should reference behavior tests")
			group.testClassNames.forEach { testClassName ->
				val sourcePath = Path.of("src/test/kotlin/${testClassName.replace('.', '/')}.kt")
				assertTrue(Files.exists(sourcePath), "$sourcePath should exist")
				val testClass = Class.forName(testClassName)
				assertTrue(
					testClass.declaredMethods.any { it.isAnnotationPresent(Test::class.java) },
					"$testClassName should contain at least one JUnit test",
				)
			}
		}
	}

	@Test
	fun `公开规则场景不再使用历史夹具命名`() {
		val staleNames = listOf(
			"PublicBattleRule" + "Fi" + "xture",
			"publicBattleRule" + "Fi" + "xture",
			"damage" + "Fi" + "xture",
		)
		val staleHits = mutableListOf<String>()
		val paths = Files.walk(Path.of("src/test/kotlin"))

		try {
			paths
				.filter { sourcePath -> Files.isRegularFile(sourcePath) && sourcePath.toString().endsWith(".kt") }
				.forEach { sourcePath ->
					val source = Files.readString(sourcePath)

					staleNames.forEach { staleName ->
						if (source.contains(staleName)) {
							staleHits += "$sourcePath contains $staleName"
						}
					}
				}
		} finally {
			paths.close()
		}

		assertTrue(
			staleHits.isEmpty(),
			"公开规则测试已经从共享夹具命名改成场景命名，不应再出现历史名称：\n${staleHits.joinToString("\n")}",
		)
	}

	private data class CoverageGroup(
		val code: String,
		val ruleCount: Int,
		val description: String,
		val testClassNames: List<String>,
	)

	private companion object {
		private val coverageGroups = listOf(
			CoverageGroup(
				code = "format-and-team-validation",
				ruleCount = 16,
				description = "回合上限、队伍数量、等级统一、重复限制、禁用列表、选择阶段和自定义格式约束。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleFormatValidationTests",
					"io.github.lishangbu.battleengine.BattlePreparationValidatorTests",
				),
			),
			CoverageGroup(
				code = "lifecycle-switch-faint-result",
				ruleCount = 18,
				description = "初始出场、替换重置、强制替换、濒死检查、胜负判定和战斗结束事件。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleLifecycleSwitchPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleFormatLifecycleBoundaryPublicReferenceTests",
				),
			),
			CoverageGroup(
				code = "turn-flow-action-ordering",
				ruleCount = 26,
				description = "PP、锁招、多回合技能、蓄力、休整、优先度、速度、同速随机和行动取消。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleActionOrderingPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleActionValidatorTests",
					"io.github.lishangbu.battleengine.BattleActionFlowBoundaryTests",
				),
			),
			CoverageGroup(
				code = "target-scope-redirection",
				ruleCount = 20,
				description = "单体、相邻、全场、己方、随机目标、目标失效重定向和范围伤害。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleTargetScopePublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleTargetRedirectionPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleRandomTargetPublicReferenceTests",
				),
			),
			CoverageGroup(
				code = "hit-protect-substitute-immunity-reflect",
				ruleCount = 28,
				description = "命中/闪避、保护、替身、属性/状态免疫、声音穿透、粉末、抢夺、反射类变化技能和行动前目标有效性。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleHitDefenseBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleSubstituteTests",
					"io.github.lishangbu.battleengine.BattleImmunityTests",
				),
			),
			CoverageGroup(
				code = "damage-formula-stat-element-rounding",
				ruleCount = 42,
				description = "普通伤害、击中要害、属性一致加成、克制、天气/场地修正、攻防能力值修正、固定伤害、比例伤害和 HP 派生伤害。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.damage.BattleDamageFormulaBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.damage.BattleDamageCalculatorTests",
					"io.github.lishangbu.battleengine.BattleCriticalHitFlowTests",
				),
			),
			CoverageGroup(
				code = "major-volatile-persistent-status",
				ruleCount = 34,
				description = "灼伤、麻痹、睡眠、冰冻、中毒、剧毒、混乱、畏缩、回复封锁、挑衅、定身法、无理取闹、束缚和持续回合。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleResidualStatusTests",
					"io.github.lishangbu.battleengine.BattleVolatileStatusTests",
					"io.github.lishangbu.battleengine.BattleBindingStatusTests",
					"io.github.lishangbu.battleengine.BattleDisableTests",
				),
			),
			CoverageGroup(
				code = "weather-terrain-field-side-condition",
				ruleCount = 31,
				description = "晴、雨、沙、雪、电气、青草、薄雾、精神、屏障、顺风、撒场、天气/场地持续时间。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleWeatherEffectTests",
					"io.github.lishangbu.battleengine.BattleTerrainEffectTests",
					"io.github.lishangbu.battleengine.BattleEnvironmentFieldBoundaryPublicReferenceTests",
				),
			),
			CoverageGroup(
				code = "skill-effect-family",
				ruleCount = 39,
				description = "能力阶级、主要状态、HP 吸取/反伤/回复、强制替换、复制、封锁、清除、交换、取反和失败条件。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleSkillEffectBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleSkillStatStageEffectTests",
					"io.github.lishangbu.battleengine.BattleSkillHpEffectTests",
				),
			),
			CoverageGroup(
				code = "ability-effect-family",
				ruleCount = 36,
				description = "入场、攻击前、防守前、命中后、天气/场地联动、属性吸收、状态免疫、规则绕过和伤害修正。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleSwitchInAbilityTests",
					"io.github.lishangbu.battleengine.BattleAbilityItemBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleTargetAbilityIgnoreTests",
				),
			),
			CoverageGroup(
				code = "item-effect-family",
				ruleCount = 18,
				description = "消耗、回复、状态解除、伤害增减、持续时间延长、一次性免死、锁招、蓄力跳过和抗性减伤。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleHeldItemPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleElementDamageReductionItemTests",
					"io.github.lishangbu.battleengine.BattleStatusCureItemTests",
				),
			),
			CoverageGroup(
				code = "random-replay-public-reference",
				ruleCount = 4,
				description = "固定随机序列、事件流稳定、回放复算和公开对照测试元数据。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.random.ScriptedBattleRandomTests",
					"io.github.lishangbu.battleengine.BattleReplayRecorderTests",
					"io.github.lishangbu.battleengine.BattleReplayPublicReferenceTests",
				),
			),
		)
	}
}
