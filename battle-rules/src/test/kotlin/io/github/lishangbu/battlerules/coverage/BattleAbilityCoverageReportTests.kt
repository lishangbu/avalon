package io.github.lishangbu.battlerules.coverage

import io.github.lishangbu.battlerules.BattleRulesIntegrationTest
import io.github.lishangbu.battlerules.entity.BattleAbilityRule
import io.github.lishangbu.battlerules.service.BattleEffectPolicyRuntimeLookup
import io.github.lishangbu.battlerules.service.BattleRuntimeDataLookup
import io.github.lishangbu.battlerules.service.isBattleAbilityRuntimePolicySupported
import io.github.lishangbu.battlerules.service.toBattleAbilityEffect
import io.github.lishangbu.gamedata.entity.GameAbility
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path

/** 验证数据库特性、Jimmer 装配、引擎实现与行为测试之间的覆盖关系。 */
@BattleRulesIntegrationTest
@Tag("battle-ability-coverage")
class BattleAbilityCoverageReportTests(
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val runtimeLookup: BattleEffectPolicyRuntimeLookup,
	@Autowired private val runtimeDataLookup: BattleRuntimeDataLookup,
) {
	@Test
	fun `checked in battle ability coverage report matches runtime data and behavior tests`() {
		val projectRoot = Path.of(requireNotNull(System.getProperty(PROJECT_ROOT_PROPERTY)))
		val report = BattleAbilityCoverageMatrix().render(coverageEntries(projectRoot))
		BattleCoverageReportSnapshot(projectRoot).verifyOrWrite(
			REPORT_PATH,
			report,
			WRITE_REPORT_PROPERTY,
			"战斗特性覆盖矩阵已过期，请运行 ./gradlew :battle-rules:generateBattleAbilityCoverage",
		)
	}

	private fun coverageEntries(projectRoot: Path): List<BattleAbilityCoverageEntry> {
		val abilities = sqlClient.createQuery(GameAbility::class) { select(table) }
			.execute()
			.filter { it.mainSeries == true }
		val rulesByAbilityId = sqlClient.createQuery(BattleAbilityRule::class) { select(table) }
			.execute()
			.filter(BattleAbilityRule::enabled)
			.groupBy(BattleAbilityRule::abilityId)
		val elementIds = runtimeDataLookup.coreElementIds()
		val testSourceIndex = BattleBehaviorTestSourceIndex(projectRoot)

		return abilities.map { ability ->
			val directPolicies = rulesByAbilityId[ability.id]
				.orEmpty()
				.sortedWith(compareBy(BattleAbilityRule::triggerOrder, BattleAbilityRule::sortOrder, BattleAbilityRule::id))
				.map(BattleAbilityRule::effectPolicy)
			val runtimePolicies = runtimeLookup.enabledAbilityPolicies(ability.id)
			val evidenceByPolicy = runtimePolicies.associateWith { policy ->
				behaviorTestClasses(policy, elementIds, testSourceIndex)
			}
			val unverifiedPolicies = runtimePolicies.filter { policy ->
				!policy.isBattleAbilityRuntimePolicySupported(elementIds) || (
					evidenceByPolicy.getValue(policy).isEmpty() && policy !in INTENTIONAL_NO_EFFECT_POLICIES
				)
			}

			BattleAbilityCoverageEntry(
				code = ability.code,
				name = ability.name,
				enabled = ability.enabled == true,
				policies = runtimePolicies,
				jimmerLoaded = directPolicies == runtimePolicies,
				runtimeSupported = runtimePolicies.all { it.isBattleAbilityRuntimePolicySupported(elementIds) },
				behaviorTestClasses = evidenceByPolicy.values.flatten().toSet(),
				unverifiedPolicies = unverifiedPolicies,
				intentionalNoEffectPolicies = runtimePolicies.filter { it in INTENTIONAL_NO_EFFECT_POLICIES },
			)
		}
	}

	private fun behaviorTestClasses(
		policy: String,
		elementIds: Map<String, Long>,
		testSourceIndex: BattleBehaviorTestSourceIndex,
	): Set<String> {
		val effectType = policy.toBattleAbilityEffect(elementIds)?.javaClass?.simpleName
		val evidenceToken = effectType?.let { "BattleAbilityEffect.$it" }
		return when {
			evidenceToken != null -> testSourceIndex.classesContaining(evidenceToken)
			policy == "ground-immunity" -> testSourceIndex.classesContaining("grounded = false")
			else -> emptySet()
		}
	}

	private companion object {
		private const val PROJECT_ROOT_PROPERTY = "avalon.project-root"
		private const val WRITE_REPORT_PROPERTY = "battleAbilityCoverage.write"
		private const val REPORT_PATH = "docs/battle-ability-coverage.md"
		private val INTENTIONAL_NO_EFFECT_POLICIES = setOf("single-battle-no-effect")
	}
}
