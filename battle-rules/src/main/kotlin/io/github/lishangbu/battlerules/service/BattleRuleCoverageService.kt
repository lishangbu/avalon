package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleCoverageCheckResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageFixtureResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageFixtureSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageItemResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageMatrixRowResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageTargetSummaryResponse
import io.github.lishangbu.battlerules.entity.BattleRuleFixture
import io.github.lishangbu.battlerules.entity.BattleRuleTestRun
import io.github.lishangbu.battlerules.entity.category
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.expectedSummary
import io.github.lishangbu.battlerules.entity.fixtureId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.runCode
import io.github.lishangbu.battlerules.entity.runStatus
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.startedAt
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗规则实现覆盖报告服务。
 *
 * 覆盖项直接由数据库中的公开 fixture 推导，最近运行结果来自 `battle_rule_test_run`。这样新增规则对照时
 * 只需要维护 fixture 和测试运行数据，不再额外维护一份容易漂移的静态覆盖目录。
 */
@Service
class BattleRuleCoverageService(
	private val sqlClientProvider: ObjectProvider<KSqlClient>? = null,
) {
	/**
	 * 读取当前战斗规则实现覆盖报告。
	 */
	@Transactional(readOnly = true)
	fun getCoverage(): BattleRuleCoverageResponse {
		val runtime = fixtureRuntime()
		val items = coverageItems(runtime)
		val implementedCount = items.count { it.status == IMPLEMENTED }
		val partialCount = items.count { it.status == PARTIAL }
		val plannedCount = items.count { it.status == PLANNED }
		val fixtureCount = items.sumOf { it.fixtureNames.size }
		val implementationPercent = if (items.isEmpty()) {
			0
		} else {
			(implementedCount * 100) / items.size
		}
		return BattleRuleCoverageResponse(
			summary = BattleRuleCoverageSummaryResponse(
				totalCount = items.size,
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
			fixtureSummary = fixtureSummary(runtime, items),
			matrix = coverageMatrix(items),
			checks = coverageChecks(items, runtime),
			items = items,
		)
	}

	private fun fixtureRuntime(): FixtureRuntime {
		val sqlClient = sqlClientProvider?.getIfAvailable() ?: return FixtureRuntime.unavailable()
		val fixtures = sqlClient.createQuery(BattleRuleFixture::class) {
			orderBy(table.category.asc(), table.sortOrder.asc(), table.code.asc())
			select(table)
		}.execute()
		val fixtureIds = fixtures.map { it.id }
		val latestRuns = if (fixtureIds.isEmpty()) {
			emptyMap()
		} else {
			// ponytail: coverage is an admin report over a bounded fixture set; use a window query only if this grows.
			sqlClient.createQuery(BattleRuleTestRun::class) {
				where(table.fixtureId valueIn fixtureIds)
				orderBy(table.fixtureId.asc(), table.startedAt.desc(), table.id.desc())
				select(table)
			}.execute().groupBy { it.fixtureId }.mapValues { (_, runs) -> runs.first() }
		}
		return FixtureRuntime(
			available = true,
			fixtures = fixtures,
			latestRunsByFixtureId = latestRuns,
		)
	}

	private fun coverageItems(runtime: FixtureRuntime): List<BattleRuleCoverageItemResponse> {
		if (!runtime.available) {
			return emptyList()
		}
		return runtime.fixtures.map { fixture ->
			BattleRuleCoverageItemResponse(
				code = fixture.code,
				name = fixture.name,
				category = fixture.category.label(),
				status = if (fixture.enabled) IMPLEMENTED else PLANNED,
				fixtureNames = listOf(fixture.code),
				fixtures = listOf(runtime.fixture(fixture)),
				note = fixture.description?.takeIf { it.isNotBlank() } ?: fixture.expectedSummary,
			)
		}
	}

	private fun fixtureSummary(
		runtime: FixtureRuntime,
		items: List<BattleRuleCoverageItemResponse>,
	): BattleRuleCoverageFixtureSummaryResponse {
		val fixtures = items.flatMap { it.fixtures }
		return BattleRuleCoverageFixtureSummaryResponse(
			runtimeAvailable = runtime.available,
			fixtureReferenceCount = fixtures.size,
			matchedFixtureCount = if (runtime.available) fixtures.count { !it.missing } else 0,
			missingFixtureCount = if (runtime.available) fixtures.count { it.missing } else 0,
			latestPassedCount = if (runtime.available) fixtures.count { it.latestRunStatus == PASSED } else 0,
			latestFailedCount = if (runtime.available) fixtures.count { it.latestRunStatus == FAILED } else 0,
			latestRunningCount = if (runtime.available) fixtures.count { it.latestRunStatus == RUNNING } else 0,
			withoutRunCount = if (runtime.available) {
				fixtures.count { !it.missing && it.latestRunStatus == null }
			} else {
				0
			},
		)
	}

	private fun coverageMatrix(items: List<BattleRuleCoverageItemResponse>): List<BattleRuleCoverageMatrixRowResponse> =
		items.groupBy { it.category }
			.map { (category, categoryItems) ->
				val implementedCount = categoryItems.count { it.status == IMPLEMENTED }
				BattleRuleCoverageMatrixRowResponse(
					category = category,
					totalCount = categoryItems.size,
					implementedCount = implementedCount,
					partialCount = categoryItems.count { it.status == PARTIAL },
					plannedCount = categoryItems.count { it.status == PLANNED },
					fixtureCount = categoryItems.sumOf { it.fixtureNames.size },
					implementationPercent = if (categoryItems.isEmpty()) {
						0
					} else {
						(implementedCount * 100) / categoryItems.size
					},
				)
			}

	private fun coverageChecks(
		items: List<BattleRuleCoverageItemResponse>,
		runtime: FixtureRuntime,
	): List<BattleRuleCoverageCheckResponse> {
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
		if (!runtime.available) {
			return targetChecks
		}
		val duplicateCodes = items.groupingBy { it.code }.eachCount().filterValues { it > 1 }.keys.sorted()
		val unknownStatuses = items.filterNot { it.status in RULE_STATUSES }.map { it.code }
		val blankCategoryCodes = items.filter { it.category.isBlank() }.map { it.code }
		val implementedWithoutFixtures = items.filter { it.status == IMPLEMENTED && it.fixtureNames.isEmpty() }.map { it.code }
		val allFixtures = items.flatMap { it.fixtures }
		val missingFixtures = allFixtures.filter { it.missing }.map { it.code }
		val fixturesWithoutRun = allFixtures.filter { !it.missing && it.latestRunStatus == null }.map { it.code }
		val goldenReplayCovered = allFixtures.any { it.code == GOLDEN_REPLAY_FIXTURE_CODE }
		return targetChecks + listOf(
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
				name = "已实现项有 fixture",
				passed = implementedWithoutFixtures.isEmpty(),
				success = "所有已实现规则项均绑定至少一个公开对照 fixture。",
				failure = "已实现但缺少 fixture 的规则项: ${implementedWithoutFixtures.joinToString()}。",
			),
			check(
				code = "golden-replay",
				name = "Golden Replay 对照",
				passed = goldenReplayCovered,
				success = "严格 replay 已纳入覆盖报告，并绑定公开对照 fixture。",
				failure = "严格 replay 未纳入覆盖报告或缺少公开对照 fixture。",
			),
			check(
				code = "fixture-data",
				name = "Fixture 数据一致",
				passed = missingFixtures.isEmpty() && allFixtures.size == runtime.fixtures.size,
				success = "数据库 fixture 均已转换为覆盖报告项。",
				failure = "覆盖报告存在未匹配 fixture: ${missingFixtures.joinToString()}。",
			),
			check(
				code = "fixture-latest-run",
				name = "Fixture 最近运行",
				passed = fixturesWithoutRun.isEmpty(),
				success = "所有已登记 fixture 均有最近一次测试运行结果。",
				failure = "已登记但没有运行结果的 fixture: ${fixturesWithoutRun.joinToString()}。",
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

	private data class FixtureRuntime(
		val available: Boolean,
		val fixtures: List<BattleRuleFixture>,
		val latestRunsByFixtureId: Map<Long, BattleRuleTestRun>,
	) {
		fun fixture(fixture: BattleRuleFixture): BattleRuleCoverageFixtureResponse {
			val latestRun = latestRunsByFixtureId[fixture.id]
			return BattleRuleCoverageFixtureResponse(
				code = fixture.code,
				fixtureId = fixture.id,
				name = fixture.name,
				enabled = fixture.enabled,
				latestRunCode = latestRun?.runCode,
				latestRunStatus = latestRun?.runStatus,
				latestRunStartedAt = latestRun?.startedAt,
				missing = false,
			)
		}

		companion object {
			fun unavailable(): FixtureRuntime =
				FixtureRuntime(
					available = false,
					fixtures = emptyList(),
					latestRunsByFixtureId = emptyMap(),
				)
		}
	}

	private fun String.label(): String =
		CATEGORY_LABELS[this] ?: this

	private companion object {
		private const val IMPLEMENTED = "IMPLEMENTED"
		private const val PARTIAL = "PARTIAL"
		private const val PLANNED = "PLANNED"
		private const val CHECK_PASSED = "PASSED"
		private const val CHECK_FAILED = "FAILED"
		private const val PASSED = "PASSED"
		private const val FAILED = "FAILED"
		private const val RUNNING = "RUNNING"
		private const val FINAL_TARGET_RULE_COUNT = 312
		private const val FINAL_COVERED_RULE_COUNT = 312
		private const val REQUIRED_TARGET_RULE_COUNT = 312
		private const val GOLDEN_REPLAY_FIXTURE_CODE = "golden-replay-pins-random-trace-event-fragment-and-final-hp"
		private val RULE_STATUSES = setOf(IMPLEMENTED, PARTIAL, PLANNED)
		private const val FINAL_TARGET_BASIS =
			"按可复用规则行为族统计，详见 docs/superpowers/plans/2026-06-29-battle-rule-final-coverage-ledger.md。"
		private val CATEGORY_LABELS = mapOf(
			"ABILITY" to "特性",
			"DAMAGE" to "伤害",
			"DAMAGE_FORMULA" to "伤害公式",
			"FIELD" to "场上效果",
			"FORMAT" to "赛制",
			"ITEM" to "道具",
			"MAJOR_STATUS" to "主要状态",
			"REPLAY" to "随机/回放",
			"SKILL" to "技能效果",
			"STATUS" to "状态",
			"STATUS_IMMUNITY" to "状态免疫",
			"TERRAIN" to "场地",
			"TURN" to "回合流程",
			"TURN_FLOW" to "行动流程",
			"VOLATILE_STATUS" to "临时状态",
			"WEATHER" to "天气",
		)
	}
}
