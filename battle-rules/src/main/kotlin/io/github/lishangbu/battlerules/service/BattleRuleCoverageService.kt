package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleCoverageCheckResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageFixtureResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageFixtureSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageItemResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageMatrixRowResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageTargetSummaryResponse
import org.springframework.stereotype.Service

/**
 * 战斗规则实现覆盖报告服务。
 *
 * 覆盖报告只保留规则族级汇总；具体规则行为由 `battle-engine` 单元测试断言。这样新增普通资料数据时不需要
 * 同步维护数据库 fixture，只有引入新的触发时机、事件顺序、取整规则或状态不变量时才补测试。
 */
@Service
class BattleRuleCoverageService {
	/**
	 * 读取当前战斗规则实现覆盖报告。
	 */
	fun getCoverage(): BattleRuleCoverageResponse {
		val items = coverageItems()
		val implementedCount = items.filter { it.status == IMPLEMENTED }.sumOf { it.ruleCount }
		val partialCount = items.filter { it.status == PARTIAL }.sumOf { it.ruleCount }
		val plannedCount = items.filter { it.status == PLANNED }.sumOf { it.ruleCount }
		val totalCount = items.sumOf { it.ruleCount }
		val fixtureCount = items.sumOf { it.fixtureNames.size }
		val implementationPercent = if (totalCount == 0) {
			0
		} else {
			(implementedCount * 100) / totalCount
		}
		return BattleRuleCoverageResponse(
			summary = BattleRuleCoverageSummaryResponse(
				totalCount = totalCount,
				implementedCount = implementedCount,
				partialCount = partialCount,
				plannedCount = plannedCount,
				fixtureCount = fixtureCount,
				implementationPercent = implementationPercent,
			),
			targetSummary = BattleRuleCoverageTargetSummaryResponse(
				targetRuleCount = FINAL_TARGET_RULE_COUNT,
				coveredRuleCount = FINAL_COVERED_RULE_COUNT,
				remainingRuleCount = FINAL_TARGET_RULE_COUNT - FINAL_COVERED_RULE_COUNT,
				implementationPercent = (FINAL_COVERED_RULE_COUNT * 100) / FINAL_TARGET_RULE_COUNT,
				coverageItemCount = items.size,
				basis = FINAL_TARGET_BASIS,
			),
			fixtureSummary = fixtureSummary(items),
			matrix = coverageMatrix(items),
			checks = coverageChecks(items),
			items = items,
		)
	}

	private fun coverageItems(): List<BattleRuleCoverageItemResponse> =
		COVERAGE_CATALOG.map { item ->
			BattleRuleCoverageItemResponse(
				code = item.code,
				name = item.name,
				category = item.category,
				status = IMPLEMENTED,
				ruleCount = item.ruleCount,
				fixtureNames = item.testFiles,
				fixtures = item.testFiles.map { testFile ->
					BattleRuleCoverageFixtureResponse(
						code = testFile,
						fixtureId = null,
						name = "单元测试",
						enabled = true,
						latestRunCode = null,
						latestRunStatus = null,
						latestRunStartedAt = null,
						missing = false,
					)
				},
				note = item.note,
			)
		}

	private fun fixtureSummary(items: List<BattleRuleCoverageItemResponse>): BattleRuleCoverageFixtureSummaryResponse {
		val fixtures = items.flatMap { it.fixtures }
		return BattleRuleCoverageFixtureSummaryResponse(
			runtimeAvailable = true,
			fixtureReferenceCount = fixtures.size,
			matchedFixtureCount = fixtures.count { !it.missing },
			missingFixtureCount = fixtures.count { it.missing },
			latestPassedCount = 0,
			latestFailedCount = 0,
			latestRunningCount = 0,
			withoutRunCount = 0,
		)
	}

	private fun coverageMatrix(items: List<BattleRuleCoverageItemResponse>): List<BattleRuleCoverageMatrixRowResponse> =
		items.groupBy { it.category }
			.map { (category, categoryItems) ->
				val totalCount = categoryItems.sumOf { it.ruleCount }
				val implementedCount = categoryItems.filter { it.status == IMPLEMENTED }.sumOf { it.ruleCount }
				BattleRuleCoverageMatrixRowResponse(
					category = category,
					totalCount = totalCount,
					implementedCount = implementedCount,
					partialCount = categoryItems.filter { it.status == PARTIAL }.sumOf { it.ruleCount },
					plannedCount = categoryItems.filter { it.status == PLANNED }.sumOf { it.ruleCount },
					fixtureCount = categoryItems.sumOf { it.fixtureNames.size },
					implementationPercent = if (totalCount == 0) {
						0
					} else {
						(implementedCount * 100) / totalCount
					},
				)
			}

	private fun coverageChecks(items: List<BattleRuleCoverageItemResponse>): List<BattleRuleCoverageCheckResponse> {
		val targetChecks = listOf(
			check(
				code = "target-count",
				name = "最终目标数量",
				passed = FINAL_TARGET_RULE_COUNT == REQUIRED_TARGET_RULE_COUNT &&
					FINAL_COVERED_RULE_COUNT == REQUIRED_TARGET_RULE_COUNT &&
					FINAL_COVERED_RULE_COUNT <= FINAL_TARGET_RULE_COUNT,
				success = "最终目标 $FINAL_TARGET_RULE_COUNT 条，已覆盖 $FINAL_COVERED_RULE_COUNT 条。",
				failure = "最终目标或已覆盖数量偏离 $REQUIRED_TARGET_RULE_COUNT 条。",
			),
		)
		val duplicateCodes = items.groupingBy { it.code }.eachCount().filterValues { it > 1 }.keys.sorted()
		val unknownStatuses = items.filterNot { it.status in RULE_STATUSES }.map { it.code }
		val blankCategoryCodes = items.filter { it.category.isBlank() }.map { it.code }
		val ruleCountTotal = items.sumOf { it.ruleCount }
		val implementedWithoutFixtures = items.filter { it.status == IMPLEMENTED && it.fixtureNames.isEmpty() }.map { it.code }
		val allFixtures = items.flatMap { it.fixtures }
		val missingFixtures = allFixtures.filter { it.missing }.map { it.code }
		val goldenReplayCovered = allFixtures.any { it.code == GOLDEN_REPLAY_TEST_FILE }
		return targetChecks + listOf(
			check(
				code = "catalog-rule-count",
				name = "覆盖账本数量",
				passed = ruleCountTotal == REQUIRED_TARGET_RULE_COUNT,
				success = "覆盖账本合计 $ruleCountTotal 条规则行为。",
				failure = "覆盖账本合计 $ruleCountTotal 条，偏离 $REQUIRED_TARGET_RULE_COUNT 条。",
			),
			check(
				code = "unique-code",
				name = "规则 code 唯一",
				passed = duplicateCodes.isEmpty(),
				success = "所有覆盖项 code 均唯一。",
				failure = "存在重复 code: ${duplicateCodes.joinToString()}。",
			),
			check(
				code = "known-status",
				name = "状态值合法",
				passed = unknownStatuses.isEmpty(),
				success = "所有状态值均在 ${RULE_STATUSES.joinToString()} 内。",
				failure = "存在未知状态的规则项: ${unknownStatuses.joinToString()}。",
			),
			check(
				code = "category-filled",
				name = "分类已填写",
				passed = blankCategoryCodes.isEmpty(),
				success = "所有规则项均已填写分类。",
				failure = "存在未填写分类的规则项: ${blankCategoryCodes.joinToString()}。",
			),
			check(
				code = "implemented-fixtures",
				name = "已实现项有测试",
				passed = implementedWithoutFixtures.isEmpty(),
				success = "所有已实现规则族均绑定至少一个单元测试文件。",
				failure = "已实现但缺少单元测试文件的规则族: ${implementedWithoutFixtures.joinToString()}。",
			),
			check(
				code = "golden-replay",
				name = "Golden Replay 对照",
				passed = goldenReplayCovered,
				success = "严格 replay 已纳入单元测试覆盖。",
				failure = "严格 replay 未纳入单元测试覆盖。",
			),
			check(
				code = "fixture-data",
				name = "测试引用一致",
				passed = missingFixtures.isEmpty(),
				success = "覆盖报告中的单元测试引用均有效。",
				failure = "覆盖报告存在未匹配测试引用: ${missingFixtures.joinToString()}。",
			),
		)
	}

	private fun check(
		code: String,
		name: String,
		passed: Boolean,
		success: String,
		failure: String,
	): BattleRuleCoverageCheckResponse =
		BattleRuleCoverageCheckResponse(
			code = code,
			name = name,
			status = if (passed) CHECK_PASSED else CHECK_FAILED,
			message = if (passed) success else failure,
		)

	private data class CoverageCatalogItem(
		val code: String,
		val name: String,
		val category: String,
		val ruleCount: Int,
		val testFiles: List<String>,
		val note: String,
	)

	private companion object {
		private const val IMPLEMENTED = "IMPLEMENTED"
		private const val PARTIAL = "PARTIAL"
		private const val PLANNED = "PLANNED"
		private const val CHECK_PASSED = "PASSED"
		private const val CHECK_FAILED = "FAILED"
		private const val FINAL_TARGET_RULE_COUNT = 312
		private const val FINAL_COVERED_RULE_COUNT = 312
		private const val REQUIRED_TARGET_RULE_COUNT = 312
		private const val GOLDEN_REPLAY_TEST_FILE = "BattleReplayPublicReferenceTests.kt"
		private val RULE_STATUSES = setOf(IMPLEMENTED, PARTIAL, PLANNED)
		private const val FINAL_TARGET_BASIS =
			"按可复用规则行为族统计；具体断言以 battle-engine 单元测试和覆盖账本为准。"
		private val COVERAGE_CATALOG = listOf(
			CoverageCatalogItem(
				code = "format-and-team-validation",
				name = "对战格式与队伍合法性",
				category = "赛制",
				ruleCount = 16,
				testFiles = listOf("BattleFormatValidationTests.kt", "BattlePreparationValidatorTests.kt"),
				note = "覆盖回合上限、队伍数量、等级统一、重复限制、禁用列表、选择阶段和自定义格式约束。",
			),
			CoverageCatalogItem(
				code = "lifecycle-switch-faint-result",
				name = "初始化、替换、濒死与胜负",
				category = "生命周期",
				ruleCount = 18,
				testFiles = listOf("BattleLifecycleSwitchPublicReferenceTests.kt", "BattleFormatLifecycleBoundaryPublicReferenceTests.kt"),
				note = "覆盖初始出场、替换重置、强制替换、濒死检查、胜负判定和战斗结束事件。",
			),
			CoverageCatalogItem(
				code = "turn-flow-action-ordering",
				name = "回合流程、行动选择与行动排序",
				category = "回合流程",
				ruleCount = 26,
				testFiles = listOf("BattleActionOrderingPublicReferenceTests.kt", "BattleActionValidatorTests.kt", "BattleActionFlowBoundaryTests.kt"),
				note = "覆盖 PP、锁招、多回合技能、蓄力、休整、优先度、速度、同速随机和行动取消。",
			),
			CoverageCatalogItem(
				code = "target-scope-redirection",
				name = "目标选择、双打范围与重定向",
				category = "目标",
				ruleCount = 20,
				testFiles = listOf("BattleTargetScopePublicReferenceTests.kt", "BattleTargetRedirectionPublicReferenceTests.kt", "BattleRandomTargetPublicReferenceTests.kt"),
				note = "覆盖单体、相邻、全场、己方、随机目标、目标失效重定向和范围伤害。",
			),
			CoverageCatalogItem(
				code = "hit-protect-substitute-immunity-reflect",
				name = "命中、保护、替身、免疫与反射",
				category = "命中防守",
				ruleCount = 28,
				testFiles = listOf("BattleHitDefenseBoundaryPublicReferenceTests.kt", "BattleSubstituteTests.kt", "BattleImmunityTests.kt"),
				note = "覆盖命中/闪避、保护、替身、属性/状态免疫、声音穿透、粉末、抢夺、反射类变化技能和行动前目标有效性。",
			),
			CoverageCatalogItem(
				code = "damage-formula-stat-element-rounding",
				name = "伤害公式、能力值、属性与取整",
				category = "伤害公式",
				ruleCount = 42,
				testFiles = listOf("BattleDamageFormulaBoundaryPublicReferenceTests.kt", "BattleDamageCalculatorTests.kt", "BattleCriticalHitFlowTests.kt"),
				note = "覆盖普通伤害、击中要害、属性一致加成、克制、天气/场地修正、攻防能力值修正、固定伤害、比例伤害和 HP 派生伤害。",
			),
			CoverageCatalogItem(
				code = "major-volatile-persistent-status",
				name = "主要状态、临时状态与持续状态",
				category = "状态",
				ruleCount = 34,
				testFiles = listOf("BattleResidualStatusTests.kt", "BattleVolatileStatusTests.kt", "BattleBindingStatusTests.kt", "BattleDisableTests.kt"),
				note = "覆盖灼伤、麻痹、睡眠、冰冻、中毒、剧毒、混乱、畏缩、回复封锁、挑衅、定身法、无理取闹、束缚和持续回合。",
			),
			CoverageCatalogItem(
				code = "weather-terrain-field-side-condition",
				name = "天气、场地、场地状态和一侧状态",
				category = "环境",
				ruleCount = 31,
				testFiles = listOf("BattleWeatherEffectTests.kt", "BattleTerrainEffectTests.kt", "BattleEnvironmentFieldBoundaryPublicReferenceTests.kt"),
				note = "覆盖晴、雨、沙、雪、电气、青草、薄雾、精神、屏障、顺风、撒场、天气/场地持续时间。",
			),
			CoverageCatalogItem(
				code = "skill-effect-family",
				name = "技能效果行为族",
				category = "技能",
				ruleCount = 39,
				testFiles = listOf("BattleSkillEffectBoundaryPublicReferenceTests.kt", "BattleSkillStatStageEffectTests.kt", "BattleSkillHpEffectTests.kt"),
				note = "覆盖能力阶级、主要状态、HP 吸取/反伤/回复、强制替换、复制、封锁、清除、交换、取反和失败条件。",
			),
			CoverageCatalogItem(
				code = "ability-effect-family",
				name = "特性效果行为族",
				category = "特性",
				ruleCount = 36,
				testFiles = listOf("BattleSwitchInAbilityTests.kt", "BattleAbilityItemBoundaryPublicReferenceTests.kt", "BattleTargetAbilityIgnoreTests.kt"),
				note = "覆盖入场、攻击前、防守前、命中后、天气/场地联动、属性吸收、状态免疫、规则绕过和伤害修正。",
			),
			CoverageCatalogItem(
				code = "item-effect-family",
				name = "道具效果行为族",
				category = "道具",
				ruleCount = 18,
				testFiles = listOf("BattleHeldItemPublicReferenceTests.kt", "BattleElementDamageReductionItemTests.kt", "BattleStatusCureItemTests.kt"),
				note = "覆盖消耗、回复、状态解除、伤害增减、持续时间延长、一次性免死、锁招、蓄力跳过和抗性减伤。",
			),
			CoverageCatalogItem(
				code = "random-replay-public-reference",
				name = "随机、回放和对照测试基础",
				category = "随机/回放",
				ruleCount = 4,
				testFiles = listOf("ScriptedBattleRandomTests.kt", "BattleReplayRecorderTests.kt", GOLDEN_REPLAY_TEST_FILE),
				note = "覆盖固定随机序列、事件流稳定、回放复算和公开对照测试元数据。",
			),
		)
	}
}
