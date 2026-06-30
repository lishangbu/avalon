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
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.fixtureId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.runCode
import io.github.lishangbu.battlerules.entity.runStatus
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
 * 覆盖状态来自代码、事件流和公开 fixture；运行在 Spring 容器中时，会顺带读取 fixture 与最新 test-run，
 * 让管理端能发现覆盖清单和数据库测试资料是否已经脱节。
 * 管理端使用它快速判断当前战斗引擎哪些规则已有公开对照、哪些只是部分接入、哪些仍在计划中。
 *
 * 清单中的 code 是稳定报告标识，不参与权限、路由或数据库主键。每次实现新规则并补充公开 fixture 后，
 * 应在 [BattleRuleCoverageCatalog] 同步把状态从 `PLANNED` 或 `PARTIAL` 推进到更准确的状态。
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
		val staticItems = BattleRuleCoverageCatalog.coverageItems()
		val runtime = fixtureRuntime(staticItems.flatMap { it.fixtureNames }.distinct())
		val items = staticItems.map { item ->
			item.copy(fixtures = item.fixtureNames.map { runtime.fixture(it) })
		}
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

	private fun fixtureRuntime(fixtureCodes: List<String>): FixtureRuntime {
		val sqlClient = sqlClientProvider?.getIfAvailable() ?: return FixtureRuntime.unavailable()
		if (fixtureCodes.isEmpty()) {
			return FixtureRuntime(available = true, fixturesByCode = emptyMap(), latestRunsByFixtureId = emptyMap())
		}
		val fixtures = sqlClient.createQuery(BattleRuleFixture::class) {
			where(table.code valueIn fixtureCodes)
			select(table)
		}.execute()
		val fixtureIds = fixtures.map { it.id }
		val latestRuns = if (fixtureIds.isEmpty()) {
			emptyMap()
		} else {
			// ponytail: all coverage fixtures are a small admin report; switch to a DB window query if this grows large.
			sqlClient.createQuery(BattleRuleTestRun::class) {
				where(table.fixtureId valueIn fixtureIds)
				orderBy(table.fixtureId.asc(), table.startedAt.desc(), table.id.desc())
				select(table)
			}.execute().groupBy { it.fixtureId }.mapValues { (_, runs) -> runs.first() }
		}
		return FixtureRuntime(
			available = true,
			fixturesByCode = fixtures.associateBy { it.code },
			latestRunsByFixtureId = latestRuns,
		)
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
					referenceCount = categoryItems.sumOf { it.referenceUrls.size },
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
		val duplicateCodes = items.groupingBy { it.code }.eachCount().filterValues { it > 1 }.keys.sorted()
		val unknownStatuses = items.filterNot { it.status in RULE_STATUSES }.map { it.code }
		val blankCategoryCodes = items.filter { it.category.isBlank() }.map { it.code }
		val implementedWithoutFixtures = items.filter { it.status == IMPLEMENTED && it.fixtureNames.isEmpty() }.map { it.code }
		val implementedWithoutReferences = items.filter { it.status == IMPLEMENTED && it.referenceUrls.isEmpty() }.map { it.code }
		val allFixtures = items.flatMap { it.fixtures }
		val missingFixtures = allFixtures.filter { it.missing }.map { it.code }
		val fixturesWithoutRun = allFixtures.filter { !it.missing && it.latestRunStatus == null }.map { it.code }
		val goldenReplayCovered = items.any {
			it.code == GOLDEN_REPLAY_COVERAGE_CODE &&
				it.status == IMPLEMENTED &&
				it.fixtureNames.any { fixture -> fixture.contains("replay") }
		}
		val staticChecks = listOf(
			check(
				code = "target-count",
				name = "最终目标数量",
				passed = FINAL_TARGET_RULE_COUNT == REQUIRED_TARGET_RULE_COUNT &&
					FINAL_COVERED_RULE_COUNT == REQUIRED_TARGET_RULE_COUNT &&
					FINAL_COVERED_RULE_COUNT <= FINAL_TARGET_RULE_COUNT,
				success = "最终目标 $FINAL_TARGET_RULE_COUNT 条，已覆盖 $FINAL_COVERED_RULE_COUNT 条。",
				failure = "最终目标或已覆盖数量偏离 $REQUIRED_TARGET_RULE_COUNT 条。",
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
				name = "已实现项有 fixture",
				passed = implementedWithoutFixtures.isEmpty(),
				success = "所有已实现规则项均绑定至少一个公开对照 fixture。",
				failure = "已实现但缺少 fixture 的规则项: ${implementedWithoutFixtures.joinToString()}。",
			),
			check(
				code = "implemented-references",
				name = "已实现项有来源",
				passed = implementedWithoutReferences.isEmpty(),
				success = "所有已实现规则项均记录至少一个公开来源。",
				failure = "已实现但缺少公开来源的规则项: ${implementedWithoutReferences.joinToString()}。",
			),
			check(
				code = "golden-replay",
				name = "Golden Replay 对照",
				passed = goldenReplayCovered,
				success = "严格 replay 已纳入覆盖报告，并绑定公开对照 fixture。",
				failure = "严格 replay 未纳入覆盖报告或缺少公开对照 fixture。",
			),
		)
		if (!runtime.available) {
			return staticChecks
		}
		return staticChecks + listOf(
			check(
				code = "fixture-data",
				name = "Fixture 数据一致",
				passed = missingFixtures.isEmpty(),
				success = "覆盖清单中的 fixture 均已登记到数据库。",
				failure = "覆盖清单中存在未登记 fixture: ${missingFixtures.joinToString()}。",
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
		val fixturesByCode: Map<String, BattleRuleFixture>,
		val latestRunsByFixtureId: Map<Long, BattleRuleTestRun>,
	) {
		fun fixture(code: String): BattleRuleCoverageFixtureResponse {
			if (!available) {
				return BattleRuleCoverageFixtureResponse(
					code = code,
					fixtureId = null,
					name = null,
					enabled = null,
					latestRunCode = null,
					latestRunStatus = null,
					latestRunStartedAt = null,
					missing = false,
				)
			}
			val fixture = fixturesByCode[code]
				?: return BattleRuleCoverageFixtureResponse(
					code = code,
					fixtureId = null,
					name = null,
					enabled = null,
					latestRunCode = null,
					latestRunStatus = null,
					latestRunStartedAt = null,
					missing = true,
				)
			val latestRun = latestRunsByFixtureId[fixture.id]
			return BattleRuleCoverageFixtureResponse(
				code = code,
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
					fixturesByCode = emptyMap(),
					latestRunsByFixtureId = emptyMap(),
				)
		}
	}

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
		private const val GOLDEN_REPLAY_COVERAGE_CODE = "replay.deterministic-random-trace"
		private val RULE_STATUSES = setOf(IMPLEMENTED, PARTIAL, PLANNED)
		private const val FINAL_TARGET_BASIS =
			"按可复用规则行为族统计，详见 docs/superpowers/plans/2026-06-29-battle-rule-final-coverage-ledger.md。"
	}
}
