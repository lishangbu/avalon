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
		val registeredTestClassNames = coverageGroups.flatMap { it.testClassNames }
		assertEquals(
			registeredTestClassNames.size,
			registeredTestClassNames.distinct().size,
			"每个行为测试类只能登记到一个规则族，避免同一组公开场景被多个规则族重复认领",
		)

		coverageGroups.forEach { group ->
			assertTrue(group.ruleCount > 0, "${group.code} should cover at least one rule")
			assertTrue(group.description.isNotBlank(), "${group.code} should document coverage purpose")
			assertTrue(group.testClassNames.isNotEmpty(), "${group.code} should reference behavior tests")
			assertTrue(group.minimumNamedScenarioCount > 0, "${group.code} should keep named public scenarios")
			val namedScenarioCount = group.namedScenarioCount()
			assertTrue(
				namedScenarioCount >= group.minimumNamedScenarioCount,
				"${group.code} 应至少保留 ${group.minimumNamedScenarioCount} 个命名公开规则场景，当前只有 $namedScenarioCount 个",
			)
			group.testClassNames.forEach { testClassName ->
				val sourcePath = sourcePathForTestClass(testClassName)
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
	fun `公开规则命名场景数量覆盖规则账本`() {
		val expectedRuleCount = coverageGroups.sumOf { it.ruleCount }
		val namedScenarioCount = namedScenarioRecordsForCoverageGroups().size

		assertTrue(
			namedScenarioCount >= expectedRuleCount,
			"公开规则场景应至少覆盖账本中的 $expectedRuleCount 条规则，当前只有 $namedScenarioCount 个命名场景",
		)
	}

	@Test
	fun `公开规则命名场景必须使用唯一名称`() {
		val scenarioNames = namedScenarioRecordsForCoverageGroups().map { it.name }
		val duplicatedNames = scenarioNames
			.groupingBy { it }
			.eachCount()
			.filterValues { it > 1 }
			.keys

		assertTrue(
			scenarioNames.size >= coverageGroups.sumOf { it.ruleCount },
			"公开规则场景名数量必须覆盖 312 条规则账本，当前只有 ${scenarioNames.size} 个唯一性候选",
		)
		assertTrue(
			duplicatedNames.isEmpty(),
			"公开规则场景名必须唯一，否则场景数量可能被重复名称虚增：\n${duplicatedNames.joinToString("\n")}",
		)
	}

	@Test
	fun `公开规则命名场景必须登记到唯一规则族`() {
		val registeredTestClassNames = coverageGroups.flatMap { it.testClassNames }.toSet()
		val unregisteredRecords = allNamedScenarioRecords()
			.filterNot { it.testClassName in registeredTestClassNames }

		assertTrue(
			unregisteredRecords.isEmpty(),
			"包含公开规则场景的测试类必须登记到一个规则族，避免规则账本只统计总数却不知道由哪一族兜底：\n${
				unregisteredRecords
					.map { "${it.testClassName} -> ${it.name}" }
					.distinct()
					.joinToString("\n")
			}",
		)
		assertEquals(
			allNamedScenarioRecords().map { it.name }.toSet(),
			namedScenarioRecordsForCoverageGroups().map { it.name }.toSet(),
			"规则族登记后的命名场景集合必须等于测试源码里的命名场景集合",
		)
	}

	@Test
	fun `公开规则场景不再使用历史夹具命名`() {
		val staleNames = listOf(
			"PublicBattleRule" + "Fi" + "xture",
			"publicBattleRule" + "Fi" + "xture",
			"damage" + "Fi" + "xture",
		)
		val staleHits = mutableListOf<String>()
		kotlinTestSources().forEach { sourcePath ->
			val source = Files.readString(sourcePath)

			staleNames.forEach { staleName ->
				if (source.contains(staleName)) {
					staleHits += "$sourcePath contains $staleName"
				}
			}
		}

		assertTrue(
			staleHits.isEmpty(),
			"公开规则测试已经从共享夹具命名改成场景命名，不应再出现历史名称：\n${staleHits.joinToString("\n")}",
		)
	}

	private fun CoverageGroup.namedScenarioCount(): Int =
		testClassNames.sumOf { testClassName ->
			val sourcePath = sourcePathForTestClass(testClassName)
			if (Files.exists(sourcePath)) {
				namedScenarioNamesInSource(sourcePath).size
			} else {
				0
			}
		}

	/**
	 * 提取公开规则场景的稳定名称。
	 *
	 * 账本用 `assertNamed` 而不是测试方法名来登记公开规则场景，是因为同一个 JUnit 方法经常需要在同一套初始战斗
	 * 快照下验证多个相邻规则，例如事件顺序、HP 取整和状态不变量。这里用源码级扫描读取第一个字符串参数，原因是
	 * JUnit 运行时只能看到方法，无法看到每个 `assertNamed` 调用；而场景名本身就是我们留给规则账本的最小稳定
	 * 标识。新增本校验后，312 条规则覆盖不能再靠重复场景名“凑数”，每个公开规则断言都必须有可追踪的唯一名称。
	 */
	private fun namedScenarioRecordsForCoverageGroups(): List<NamedScenarioRecord> =
		coverageGroups.flatMap { group ->
			group.testClassNames.flatMap { testClassName ->
				val sourcePath = sourcePathForTestClass(testClassName)
				if (Files.exists(sourcePath)) {
					namedScenarioNamesInSource(sourcePath).map { scenarioName ->
						NamedScenarioRecord(group.code, testClassName, scenarioName)
					}
				} else {
					emptyList()
				}
			}
		}

	/**
	 * 扫描测试源码里的所有命名公开规则场景。
	 *
	 * 这里会排除本账本测试类本身，因为账本源码中为了描述 `assertNamed` 机制会出现相同字面量，但那些不是战斗行为
	 * 场景。其它测试类只要出现真正的 `.assertNamed("场景名")`，就必须被某个规则族登记；否则新增规则时很容易让
	 * 全局数量增加，却没有人知道它应该归属于行动顺序、伤害公式、状态、道具还是其它规则族。
	 */
	private fun allNamedScenarioRecords(): List<NamedScenarioRecord> =
		kotlinTestSources()
			.filterNot { it.fileName.toString() == "${BattleRuleCoverageLedgerTests::class.simpleName}.kt" }
			.flatMap { sourcePath ->
				val testClassName = testClassNameForSourcePath(sourcePath)
				namedScenarioNamesInSource(sourcePath).map { scenarioName ->
					NamedScenarioRecord(groupCode = null, testClassName = testClassName, name = scenarioName)
				}
			}

	private fun namedScenarioNamesInSource(sourcePath: Path): List<String> {
		val scenarioNamePattern = Regex("""\.assertNamed\(\s*"([^"]+)"""")
		return scenarioNamePattern.findAll(Files.readString(sourcePath)).map { it.groupValues[1] }.toList()
	}

	private fun sourcePathForTestClass(testClassName: String): Path =
		Path.of("src/test/kotlin/${testClassName.replace('.', '/')}.kt")

	private fun testClassNameForSourcePath(sourcePath: Path): String =
		Path.of("src/test/kotlin")
			.relativize(sourcePath)
			.toString()
			.removeSuffix(".kt")
			.replace('/', '.')
			.replace('\\', '.')

	private fun kotlinTestSources(): List<Path> {
		val paths = Files.walk(Path.of("src/test/kotlin"))
		return try {
			paths
				.filter { sourcePath -> Files.isRegularFile(sourcePath) && sourcePath.toString().endsWith(".kt") }
				.toList()
		} finally {
			paths.close()
		}
	}

	private data class CoverageGroup(
		val code: String,
		val ruleCount: Int,
		val minimumNamedScenarioCount: Int,
		val description: String,
		val testClassNames: List<String>,
	)

	private data class NamedScenarioRecord(
		val groupCode: String?,
		val testClassName: String,
		val name: String,
	)

	private companion object {
		private val coverageGroups = listOf(
			CoverageGroup(
				code = "format-and-team-validation",
				ruleCount = 16,
				minimumNamedScenarioCount = 1,
				description = "回合上限、队伍数量、等级统一、重复限制、禁用列表、选择阶段和自定义格式约束。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleFormatValidationTests",
					"io.github.lishangbu.battleengine.BattlePreparationValidatorTests",
					"io.github.lishangbu.battleengine.BattleValidationPublicReferenceTests",
				),
			),
			CoverageGroup(
				code = "lifecycle-switch-faint-result",
				ruleCount = 18,
				minimumNamedScenarioCount = 30,
				description = "初始出场、替换重置、强制替换、濒死检查、胜负判定和战斗结束事件。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleLifecycleSwitchPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleFormatLifecycleBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleEnginePublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleEngineSingleTurnTests",
					"io.github.lishangbu.battleengine.BattleEntryHazardTests",
					"io.github.lishangbu.battleengine.BattleFinalRuleBoundaryPublicReferenceTests",
				),
			),
			CoverageGroup(
				code = "turn-flow-action-ordering",
				ruleCount = 26,
				minimumNamedScenarioCount = 15,
				description = "PP、锁招、多回合技能、蓄力、休整、优先度、速度、同速随机和行动取消。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleActionOrderingPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleActionValidatorTests",
					"io.github.lishangbu.battleengine.BattleActionFlowBoundaryTests",
					"io.github.lishangbu.battleengine.BattleChargeSkillTests",
					"io.github.lishangbu.battleengine.BattleLockedMoveTests",
					"io.github.lishangbu.battleengine.BattleMultiHitSkillTests",
					"io.github.lishangbu.battleengine.BattleRechargeSkillTests",
				),
			),
			CoverageGroup(
				code = "target-scope-redirection",
				ruleCount = 20,
				minimumNamedScenarioCount = 18,
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
				minimumNamedScenarioCount = 25,
				description = "命中/闪避、保护、替身、属性/状态免疫、声音穿透、粉末、抢夺、反射类变化技能和行动前目标有效性。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleHitDefenseBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleSubstituteTests",
					"io.github.lishangbu.battleengine.BattleImmunityTests",
					"io.github.lishangbu.battleengine.BattleAccuracyStatStageIgnoreAbilityTests",
					"io.github.lishangbu.battleengine.BattlePsychicTerrainTests",
					"io.github.lishangbu.battleengine.BattleSoundAbilityTests",
					"io.github.lishangbu.battleengine.BattleStatusImmunityAndGroundingTests",
				),
			),
			CoverageGroup(
				code = "damage-formula-stat-element-rounding",
				ruleCount = 42,
				minimumNamedScenarioCount = 49,
				description = "普通伤害、击中要害、属性一致加成、克制、天气/场地修正、攻防能力值修正、固定伤害、比例伤害和 HP 派生伤害。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.damage.BattleDamageFormulaBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.damage.BattleDamageCalculatorTests",
					"io.github.lishangbu.battleengine.damage.BattleDamageStatStageIgnoreAbilityTests",
					"io.github.lishangbu.battleengine.BattleCriticalHitFlowTests",
					"io.github.lishangbu.battleengine.BattleCriticalHitImmunityAbilityTests",
					"io.github.lishangbu.battleengine.BattleFixedDamageSkillTests",
					"io.github.lishangbu.battleengine.BattleHpDerivedDamageSkillTests",
					"io.github.lishangbu.battleengine.BattleProportionalDamageSkillTests",
				),
			),
			CoverageGroup(
				code = "major-volatile-persistent-status",
				ruleCount = 34,
				minimumNamedScenarioCount = 14,
				description = "灼伤、麻痹、睡眠、冰冻、中毒、剧毒、混乱、畏缩、回复封锁、挑衅、定身法、无理取闹、束缚和持续回合。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleResidualStatusTests",
					"io.github.lishangbu.battleengine.BattleVolatileStatusTests",
					"io.github.lishangbu.battleengine.BattleBindingStatusTests",
					"io.github.lishangbu.battleengine.BattleDisableTests",
					"io.github.lishangbu.battleengine.BattleFreezeStatusTests",
					"io.github.lishangbu.battleengine.BattleHealBlockTests",
					"io.github.lishangbu.battleengine.BattleParalysisStatusTests",
					"io.github.lishangbu.battleengine.BattleSleepStatusTests",
					"io.github.lishangbu.battleengine.BattleTauntTests",
					"io.github.lishangbu.battleengine.BattleTormentTests",
				),
			),
			CoverageGroup(
				code = "weather-terrain-field-side-condition",
				ruleCount = 31,
				minimumNamedScenarioCount = 26,
				description = "晴、雨、沙、雪、电气、青草、薄雾、精神、屏障、顺风、撒场、天气/场地持续时间。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleWeatherEffectTests",
					"io.github.lishangbu.battleengine.BattleTerrainEffectTests",
					"io.github.lishangbu.battleengine.BattleEnvironmentFieldBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleEnvironmentDurationTests",
					"io.github.lishangbu.battleengine.BattleSkillEnvironmentEffectTests",
					"io.github.lishangbu.battleengine.BattleWeatherElementOverrideTests",
				),
			),
			CoverageGroup(
				code = "skill-effect-family",
				ruleCount = 39,
				minimumNamedScenarioCount = 27,
				description = "能力阶级、主要状态、HP 吸取/反伤/回复、强制替换、复制、封锁、清除、交换、取反和失败条件。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleSkillEffectBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleSkillStatStageEffectTests",
					"io.github.lishangbu.battleengine.BattleSkillHpEffectTests",
					"io.github.lishangbu.battleengine.BattleForcedSwitchSkillTests",
					"io.github.lishangbu.battleengine.BattleSkillRecoilImmunityAbilityTests",
					"io.github.lishangbu.battleengine.BattleStatStageOperationSkillTests",
				),
			),
			CoverageGroup(
				code = "ability-effect-family",
				ruleCount = 36,
				minimumNamedScenarioCount = 35,
				description = "入场、攻击前、防守前、命中后、天气/场地联动、属性吸收、状态免疫、规则绕过和伤害修正。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleSwitchInAbilityTests",
					"io.github.lishangbu.battleengine.BattleAbilityItemBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleTargetAbilityIgnoreTests",
					"io.github.lishangbu.battleengine.BattleContactAbilityPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleElementAbsorbAbilityTests",
					"io.github.lishangbu.battleengine.BattleElementAbsorbStatAbilityTests",
					"io.github.lishangbu.battleengine.BattleIndirectDamageImmunityTests",
					"io.github.lishangbu.battleengine.BattlePriorityAbilityTests",
					"io.github.lishangbu.battleengine.BattleStatusPriorityAbilityTests",
				),
			),
			CoverageGroup(
				code = "item-effect-family",
				ruleCount = 18,
				minimumNamedScenarioCount = 10,
				description = "消耗、回复、状态解除、伤害增减、持续时间延长、一次性免死、锁招、蓄力跳过和抗性减伤。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.BattleHeldItemPublicReferenceTests",
					"io.github.lishangbu.battleengine.BattleConditionalDamageBoostItemTests",
					"io.github.lishangbu.battleengine.BattleDamageDealtHealingItemTests",
					"io.github.lishangbu.battleengine.BattleElementDamageBoostItemTests",
					"io.github.lishangbu.battleengine.BattleElementDamageReductionItemTests",
					"io.github.lishangbu.battleengine.BattleFatalDamageSurvivalTests",
					"io.github.lishangbu.battleengine.BattleStatusCureItemTests",
					"io.github.lishangbu.battleengine.BattleVolatileStatusCureItemTests",
				),
			),
			CoverageGroup(
				code = "random-replay-public-reference",
				ruleCount = 4,
				minimumNamedScenarioCount = 5,
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
