package io.github.lishangbu.battlerules.coverage

import io.github.lishangbu.battlerules.BattleRulesIntegrationTest
import io.github.lishangbu.battlerules.entity.BattleItemRule
import io.github.lishangbu.battlerules.entity.enabled as ruleEnabled
import io.github.lishangbu.battlerules.service.BattleEffectPolicyRuntimeLookup
import io.github.lishangbu.battlerules.service.BattleRuntimeDataLookup
import io.github.lishangbu.battlerules.service.isBattleItemRuntimePolicySupported
import io.github.lishangbu.battlerules.service.toBattleItemEffect
import io.github.lishangbu.gamedata.entity.GameItem
import io.github.lishangbu.gamedata.entity.ItemUsageType
import io.github.lishangbu.gamedata.entity.enabled as itemEnabled
import io.github.lishangbu.gamedata.entity.usageType
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

/** 验证携带道具资料、Jimmer 规则、引擎实现与行为测试之间的覆盖关系。 */
@BattleRulesIntegrationTest
@Tag("battle-item-coverage")
class BattleItemCoverageReportTests(
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val runtimeLookup: BattleEffectPolicyRuntimeLookup,
	@Autowired private val runtimeDataLookup: BattleRuntimeDataLookup,
) {
	@Test
	fun `checked in battle item coverage report matches runtime data and behavior tests`() {
		val projectRoot = Path.of(requireNotNull(System.getProperty(PROJECT_ROOT_PROPERTY)))
		val reportPath = projectRoot.resolve(REPORT_PATH)
		val report = BattleItemCoverageMatrix().render(coverageEntries(projectRoot))

		if (System.getProperty(WRITE_REPORT_PROPERTY).toBoolean()) {
			Files.createDirectories(reportPath.parent)
			Files.writeString(reportPath, report)
		} else {
			assertEquals(
				Files.readString(reportPath),
				report,
				"战斗道具覆盖矩阵已过期，请运行 ./gradlew :battle-rules:generateBattleItemCoverage",
			)
		}
	}

	private fun coverageEntries(projectRoot: Path): List<BattleItemCoverageEntry> {
		val items = sqlClient.createQuery(GameItem::class) {
			where(table.usageType eq ItemUsageType.HELD, table.itemEnabled eq true)
			select(table)
		}.execute()
		val rulesByItemId = sqlClient.createQuery(BattleItemRule::class) {
			where(table.ruleEnabled eq true)
			select(table)
		}.execute().groupBy(BattleItemRule::itemId)
		val elementIds = runtimeDataLookup.coreElementIds()
		val testSourceIndex = BattleBehaviorTestSourceIndex(projectRoot)

		return items.map { item ->
			val directPolicies = rulesByItemId[item.id]
				.orEmpty()
				.sortedWith(compareBy(BattleItemRule::triggerOrder, BattleItemRule::sortOrder, BattleItemRule::id))
				.map(BattleItemRule::effectPolicy)
			val runtimePolicies = runtimeLookup.enabledItemPolicies(item.id)
			val evidenceByPolicy = runtimePolicies.associateWith { policy ->
				behaviorTestClasses(policy, elementIds, testSourceIndex)
			}
			val unverifiedPolicies = runtimePolicies.filter { policy ->
				!policy.isBattleItemRuntimePolicySupported(elementIds) || evidenceByPolicy.getValue(policy).isEmpty()
			}

			BattleItemCoverageEntry(
				code = item.code,
				name = item.name,
				enabled = item.enabled == true,
				policies = runtimePolicies,
				jimmerLoaded = directPolicies == runtimePolicies,
				runtimeSupported = runtimePolicies.all { it.isBattleItemRuntimePolicySupported(elementIds) },
				behaviorTestClasses = evidenceByPolicy.values.flatten().toSet(),
				unverifiedPolicies = unverifiedPolicies,
			)
		}
	}

	private fun behaviorTestClasses(
		policy: String,
		elementIds: Map<String, Long>,
		testSourceIndex: BattleBehaviorTestSourceIndex,
	): Set<String> {
		val effectType = policy.toBattleItemEffect(elementIds)?.javaClass?.simpleName
		val scannedEvidence = effectType?.let {
			testSourceIndex.classesInstantiating("BattleItemEffect", it)
		}.orEmpty()
		return scannedEvidence + EXPLICIT_POLICY_EVIDENCE[policy].orEmpty()
	}

	private companion object {
		private const val PROJECT_ROOT_PROPERTY = "avalon.project-root"
		private const val WRITE_REPORT_PROPERTY = "battleItemCoverage.write"
		private const val REPORT_PATH = "docs/battle-item-coverage.md"
		private val EXPLICIT_POLICY_EVIDENCE = mapOf(
			"creature-form-override-zacian-crowned" to setOf("BattleRuntimeSnapshotServiceTests"),
			"creature-form-override-zamazenta-crowned" to setOf("BattleRuntimeSnapshotServiceTests"),
		)
	}
}
