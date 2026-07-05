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
	fun `规则覆盖文档矩阵必须同步代码账本`() {
		val expectedRows = coverageGroups.zip(coverageGroupRuleRanges()).map { (group, range) ->
			DocumentedCoverageGroup(
				groupCode = group.code,
				ruleNumberRange = "${range.ruleNumbers.first}-${range.ruleNumbers.last}",
				ruleCount = group.ruleCount,
				testFileNames = group.testClassNames.map { testClassName -> testClassName.substringAfterLast('.') },
			)
		}

		assertEquals(
			expectedRows,
			documentedCoverageMatrix(),
			"覆盖文档里的规则族矩阵必须和 BattleRuleCoverageLedgerTests 的 coverageGroups 保持一致",
		)
	}

	@Test
	fun `规则族编号区间必须连续覆盖三百一十二条规则`() {
		val ranges = coverageGroupRuleRanges()
		val coveredRuleNumbers = ranges.flatMap { it.ruleNumbers.toList() }

		assertEquals(
			coverageGroups.map { it.code },
			ranges.map { it.groupCode },
			"规则编号区间必须保持与规则族声明顺序一致，这样新增规则时不会出现文档顺序和测试顺序漂移",
		)
		assertEquals(
			(1..312).toList(),
			coveredRuleNumbers,
			"312 条规则必须被规则族区间连续覆盖，不能出现空洞、重叠或总数看似正确但中间跳号",
		)
		coverageGroups.zip(ranges).forEach { (group, range) ->
			assertEquals(
				group.ruleCount,
				range.ruleNumbers.count(),
				"${group.code} 的规则编号区间长度必须等于该规则族声明的规则数量",
			)
		}
	}

	@Test
	fun `公开规则命名场景必须可追踪到规则族编号区间`() {
		val rangesByGroupCode = coverageGroupRuleRanges().associateBy { it.groupCode }
		val records = namedScenarioRecordsForCoverageGroups()
		val recordsWithoutRange = records.filter { record ->
			record.groupCode == null ||
				record.ruleNumberRange == null ||
				rangesByGroupCode[record.groupCode]?.ruleNumbers != record.ruleNumberRange
		}

		assertTrue(
			recordsWithoutRange.isEmpty(),
			"公开规则场景必须能追踪到所属规则族的编号区间，避免只有场景名却不知道它兜底的是哪一段规则：\n${
				recordsWithoutRange
					.map { "${it.groupCode ?: "<missing>"} -> ${it.testClassName} -> ${it.name}" }
					.distinct()
					.joinToString("\n")
			}",
		)
	}

	@Test
	fun `每条规则编号必须绑定具体公开规则场景锚点`() {
		val mappings = ruleScenarioMappings()
		val rangesByGroupCode = coverageGroupRuleRanges().associateBy { it.groupCode }
		val recordsByGroupCode = namedScenarioRecordsForCoverageGroups().groupBy { it.groupCode }

		assertEquals(
			(1..312).toList(),
			mappings.map { it.ruleNumber },
			"规则编号到公开场景的映射必须覆盖 1..312 的每一个编号，不能只证明总数为 312",
		)
		mappings.forEach { mapping ->
			assertEquals(
				rangesByGroupCode.getValue(mapping.groupCode).ruleNumbers,
				mapping.ruleNumberRange,
				"${mapping.groupCode} 的编号 ${mapping.ruleNumber} 必须保留所属规则族区间",
			)
			assertTrue(
				mapping.ruleNumber in mapping.ruleNumberRange,
				"${mapping.groupCode} 的编号 ${mapping.ruleNumber} 必须落在自己的规则族区间内",
			)
			assertEquals(
				mapping.groupCode,
				mapping.scenario.groupCode,
				"规则编号 ${mapping.ruleNumber} 的场景锚点必须来自同一个规则族，避免跨族借场景稀释覆盖含义",
			)
			assertTrue(mapping.scenario.testClassName.isNotBlank(), "规则编号 ${mapping.ruleNumber} 必须能定位测试类")
			assertTrue(mapping.scenario.name.isNotBlank(), "规则编号 ${mapping.ruleNumber} 必须能定位 assertNamed 场景名")
		}
		coverageGroupRuleRanges().forEach { range ->
			val groupMappings = mappings.filter { it.groupCode == range.groupCode }
			assertEquals(
				range.ruleNumbers.toList(),
				groupMappings.map { it.ruleNumber },
				"${range.groupCode} 的映射编号必须等于该规则族声明的连续区间",
			)
			assertEquals(
				range.ruleNumbers.count(),
				groupMappings.map { it.scenario.name }.distinct().size,
				"${range.groupCode} 的每条规则编号都必须绑定一个族内唯一公开场景，避免多个编号复用同一个锚点后掩盖缺口",
			)
		}
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
			val ruleNumberRange = coverageGroupRuleRanges()
				.single { it.groupCode == group.code }
				.ruleNumbers
			group.testClassNames.flatMap { testClassName ->
				val sourcePath = sourcePathForTestClass(testClassName)
				if (Files.exists(sourcePath)) {
					namedScenarioNamesInSource(sourcePath).map { scenarioName ->
						NamedScenarioRecord(group.code, ruleNumberRange, testClassName, scenarioName)
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
					NamedScenarioRecord(
						groupCode = null,
						ruleNumberRange = null,
						testClassName = testClassName,
						name = scenarioName,
					)
		}
	}

	/**
	 * 为 1..312 的每个规则编号生成一个具体场景锚点。
	 *
	 * 这里没有维护 312 行手写表。原因是当前规则账本按“行为族”维护，规则编号的长期维护边界也是行为族；
	 * 但每个编号仍必须落到一个可定位的 `assertNamed` 场景，不能只用总数或复用场景证明覆盖已经足够。
	 * 生成策略保持确定性：
	 * - 先按规则族声明顺序取得编号区间。
	 * - 每个编号只使用同一规则族内的 `assertNamed` 场景，不跨族借用。
	 * - 每个规则编号按源码中的场景顺序绑定一个唯一锚点；如果某个规则族场景数少于规则数，本函数会直接失败，
	 *   迫使新增真实行为断言，而不是让多个编号共享同一个场景名。
	 */
	private fun ruleScenarioMappings(): List<RuleScenarioMapping> {
		val recordsByGroupCode = namedScenarioRecordsForCoverageGroups().groupBy { requireNotNull(it.groupCode) }
		return coverageGroupRuleRanges().flatMap { range ->
			val groupRecords = recordsByGroupCode.getValue(range.groupCode)
			require(groupRecords.size >= range.ruleNumbers.count()) {
				"${range.groupCode} 只有 ${groupRecords.size} 个命名公开规则场景，少于 ${range.ruleNumbers.count()} 条规则编号"
			}
			range.ruleNumbers.mapIndexed { index, ruleNumber ->
				RuleScenarioMapping(
					ruleNumber = ruleNumber,
					groupCode = range.groupCode,
					ruleNumberRange = range.ruleNumbers,
					scenario = groupRecords[index],
				)
			}
		}
	}

	/**
	 * 由规则族顺序推导稳定规则编号区间。
	 *
	 * 账本没有把 312 条规则逐条写成硬编码编号，原因是这些规则的事实源是行为测试，规则族才是长期维护边界。
	 * 但完全只有总数也不够：总数可能正确，两个规则族之间却发生重叠或空洞。这里用声明顺序和 `ruleCount`
	 * 生成连续区间，例如第一个规则族覆盖 `1..16`，第二个从 `17` 接上。这样既不引入重复维护的大表，又能让
	 * 每个命名公开场景稳定追踪到“它属于哪一段 312 规则账本”。
	 */
	private fun coverageGroupRuleRanges(): List<CoverageGroupRuleRange> {
		var nextRuleNumber = 1
		return coverageGroups.map { group ->
			val ruleNumbers = nextRuleNumber until nextRuleNumber + group.ruleCount
			nextRuleNumber += group.ruleCount
			CoverageGroupRuleRange(group.code, ruleNumbers)
		}
	}

	private fun namedScenarioNamesInSource(sourcePath: Path): List<String> {
		val scenarioNamePattern = Regex("""\.assertNamed\(\s*"([^"]+)"""")
		return scenarioNamePattern.findAll(Files.readString(sourcePath)).map { it.groupValues[1] }.toList()
	}

	/**
	 * 读取人工维护的规则族矩阵。
	 *
	 * 文档不是事实源，但它是开发时定位规则族的入口；这里用很窄的 Markdown 表格解析，只识别
	 * “规则族 code / 规则编号区间 / 规则数 / 主要测试文件”这张表，避免再维护一份额外数据结构。
	 */
	private fun documentedCoverageMatrix(): List<DocumentedCoverageGroup> {
		val matrixRowPattern = Regex("""^\| `([^`]+)` \| ([0-9]+-[0-9]+) \| ([0-9]+) \| (.+) \|$""")
		val testFilePattern = Regex("""`([^`]+)`""")
		val documentPath = Path.of("../docs/superpowers/plans/2026-06-29-battle-rule-final-coverage-ledger.md")

		return Files.readAllLines(documentPath).mapNotNull { line ->
			matrixRowPattern.matchEntire(line)?.let { match ->
				DocumentedCoverageGroup(
					groupCode = match.groupValues[1],
					ruleNumberRange = match.groupValues[2],
					ruleCount = match.groupValues[3].toInt(),
					testFileNames = testFilePattern.findAll(match.groupValues[4])
						.map { fileMatch -> fileMatch.groupValues[1].substringAfterLast('/') }
						.toList(),
				)
			}
		}
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

	private data class CoverageGroupRuleRange(
		val groupCode: String,
		val ruleNumbers: IntRange,
	)

	private data class RuleScenarioMapping(
		val ruleNumber: Int,
		val groupCode: String,
		val ruleNumberRange: IntRange,
		val scenario: NamedScenarioRecord,
	)

	private data class NamedScenarioRecord(
		val groupCode: String?,
		val ruleNumberRange: IntRange?,
		val testClassName: String,
		val name: String,
	)

	private data class DocumentedCoverageGroup(
		val groupCode: String,
		val ruleNumberRange: String,
		val ruleCount: Int,
		val testFileNames: List<String>,
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
					"io.github.lishangbu.battleengine.BattleStruggleTests",
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
				description = "普通伤害、击中要害、属性一致加成、克制、天气/场地修正、攻防能力值修正、固定伤害、比例伤害、HP 派生伤害、已受伤害反打和一击必杀直接伤害。",
				testClassNames = listOf(
					"io.github.lishangbu.battleengine.damage.BattleDamageFormulaBoundaryPublicReferenceTests",
					"io.github.lishangbu.battleengine.damage.BattleDamageCalculatorTests",
					"io.github.lishangbu.battleengine.damage.BattleDamageStatStageIgnoreAbilityTests",
					"io.github.lishangbu.battleengine.BattleCriticalHitFlowTests",
					"io.github.lishangbu.battleengine.BattleCriticalHitImmunityAbilityTests",
					"io.github.lishangbu.battleengine.BattleFixedDamageSkillTests",
					"io.github.lishangbu.battleengine.BattleFormulaDamageSafetyTests",
					"io.github.lishangbu.battleengine.BattleHpDerivedDamageSkillTests",
					"io.github.lishangbu.battleengine.BattleOneHitKnockOutSkillTests",
					"io.github.lishangbu.battleengine.BattleProportionalDamageSkillTests",
					"io.github.lishangbu.battleengine.BattleReceivedDamageSkillTests",
					"io.github.lishangbu.battleengine.BattleSpeedRatioPowerSkillTests",
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
					"io.github.lishangbu.battleengine.BattleScreenBreakingSkillTests",
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
					"io.github.lishangbu.battleengine.BattlePostDamageStatusCureSkillTests",
					"io.github.lishangbu.battleengine.BattleUserElementRemovalSkillTests",
					"io.github.lishangbu.battleengine.BattleForcedSwitchSkillTests",
					"io.github.lishangbu.battleengine.BattleSkillRecoilImmunityAbilityTests",
					"io.github.lishangbu.battleengine.BattleStatStageOperationSkillTests",
					"io.github.lishangbu.battleengine.BattleSkillWeightEffectTests",
					"io.github.lishangbu.battleengine.BattleNonFaintingDamageSkillTests",
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
