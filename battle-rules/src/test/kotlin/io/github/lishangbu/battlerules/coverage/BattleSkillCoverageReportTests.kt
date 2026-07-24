package io.github.lishangbu.battlerules.coverage

import io.github.lishangbu.battlerules.BattleRulesIntegrationTest
import io.github.lishangbu.battlerules.entity.BattleSkillRule
import io.github.lishangbu.battlerules.entity.enabled as ruleEnabled
import io.github.lishangbu.battlerules.service.BattleSkillRuntimeLookup
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeDamagePolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeEffectPolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeHitPolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeTargetPolicySupported
import io.github.lishangbu.gamedata.entity.GameSkill
import io.github.lishangbu.gamedata.entity.enabled as skillEnabled
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path

/** 验证启用技能资料、Jimmer 规则装配、运行时支持与纯引擎行为测试之间的覆盖关系。 */
@BattleRulesIntegrationTest
@Tag("battle-skill-coverage")
class BattleSkillCoverageReportTests(
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val runtimeLookup: BattleSkillRuntimeLookup,
) {
	@Test
	fun `checked in battle skill coverage report matches runtime data and behavior tests`() {
		val projectRoot = Path.of(requireNotNull(System.getProperty(PROJECT_ROOT_PROPERTY)))
		val report = BattleSkillCoverageMatrix().render(coverageEntries(projectRoot))
		BattleCoverageReportSnapshot(projectRoot).verifyOrWrite(
			REPORT_PATH,
			report,
			WRITE_REPORT_PROPERTY,
			"战斗技能覆盖矩阵已过期，请运行 ./gradlew :battle-rules:generateBattleSkillCoverage",
		)
	}

	private fun coverageEntries(projectRoot: Path): List<BattleSkillCoverageEntry> {
		val skills = sqlClient.createQuery(GameSkill::class) {
			where(table.skillEnabled eq true)
			select(table)
		}.execute()
		val rulesBySkillId = sqlClient.createQuery(BattleSkillRule::class) {
			where(table.ruleEnabled eq true)
			select(table)
		}.execute().associateBy(BattleSkillRule::skillId)
		val evidence = BattleSkillBehaviorEvidence(BattleBehaviorTestSourceIndex(projectRoot))

		return skills.map { skill ->
			val rule = rulesBySkillId[skill.id]
			val policies = rule?.labeledPolicies().orEmpty()
			val unsupportedPolicies = rule?.unsupportedLabeledPolicies().orEmpty()
			val runtimeSlot = runCatching { runtimeLookup.skillSlotBySkillId(skill.id) }
			val behaviorTestClasses = rule?.let { enabledRule ->
				runtimeSlot.getOrNull()?.let { slot -> evidence.classesFor(skill.code, enabledRule.effectPolicy, slot) }
			}.orEmpty()
			val unverifiedPolicies = buildList {
				addAll(unsupportedPolicies)
				if (rule == null) add(MISSING_RULE)
				if (rule != null && behaviorTestClasses.isEmpty()) add("effect:${rule.effectPolicy}")
			}

			BattleSkillCoverageEntry(
				code = skill.code,
				name = skill.name,
				enabled = true,
				policies = policies,
				jimmerLoaded = rule != null,
				runtimeSupported = rule != null && unsupportedPolicies.isEmpty() && runtimeSlot.isSuccess,
				behaviorTestClasses = behaviorTestClasses,
				unverifiedPolicies = unverifiedPolicies.distinct(),
			)
		}
	}

	private fun BattleSkillRule.labeledPolicies(): List<String> =
		listOf(
			"effect:$effectPolicy",
			"target:$targetPolicy",
			"hit:$hitPolicy",
			"damage:$damagePolicy",
		)

	private fun BattleSkillRule.unsupportedLabeledPolicies(): List<String> =
		buildList {
			if (!effectPolicy.isBattleSkillRuntimeEffectPolicySupported()) add("effect:$effectPolicy")
			if (!targetPolicy.isBattleSkillRuntimeTargetPolicySupported()) add("target:$targetPolicy")
			if (!hitPolicy.isBattleSkillRuntimeHitPolicySupported()) add("hit:$hitPolicy")
			if (!damagePolicy.isBattleSkillRuntimeDamagePolicySupported()) add("damage:$damagePolicy")
		}

	private companion object {
		private const val PROJECT_ROOT_PROPERTY = "avalon.project-root"
		private const val WRITE_REPORT_PROPERTY = "battleSkillCoverage.write"
		private const val REPORT_PATH = "docs/battle-skill-coverage.md"
		private const val MISSING_RULE = "missing-rule"
	}
}
