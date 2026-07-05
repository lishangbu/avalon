package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

/**
 * 验证运行时 policy mapper 的纯字符串映射边界。
 *
 * 集成测试已经会从 Liquibase 种子数据读取启用 policy，并走正式快照装配入口；本测试只覆盖不需要数据库的 mapper
 * 细节，尤其是前缀解析和“启用规则必须显式声明目标”的约束。这样拆分 mapper 文件或新增同构 policy 时，可以用
 * 最小测试快速发现字符串解析被改坏。
 */
class BattleRuntimePolicyMapperTests {
	private val elementIds = mapOf(
		"normal" to 1L,
		"fire" to 10L,
		"water" to 11L,
		"grass" to 12L,
	)

	@Test
	fun `required element id reports api validation instead of internal error`() {
		val exception = assertThrows<ApiException> {
			emptyMap<String, Long>().requiredElementId("fire")
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("elementCode")
		assertThat(exception.message).isEqualTo("核心属性资料缺失: fire")
	}

	@Test
	fun `skill target mapper requires explicit policy and rejects unknown policy`() {
		assertThat("selected-target".toBattleSkillTargetScope()).isEqualTo(BattleSkillTargetScope.SELECTED_TARGET)
		assertThat("self".toBattleSkillTargetScope()).isEqualTo(BattleSkillTargetScope.SELF)
		assertThat("user-side-active".toBattleSkillTargetScope()).isEqualTo(BattleSkillTargetScope.USER_SIDE_ACTIVE)
		assertThat("unknown-target".isBattleSkillRuntimeTargetPolicySupported()).isFalse()
		assertThat("all-opponents".isBattleSkillRuntimeTargetPolicySupported()).isTrue()
		assertThat("user-side-active".isBattleSkillRuntimeTargetPolicySupported()).isTrue()

		val exception = assertThrows<ApiException> {
			"unknown-target".toBattleSkillTargetScope()
		}

		assertThat(exception.field).isEqualTo("targetPolicy")
		assertThat(exception.message).isEqualTo("不支持的技能目标策略: unknown-target")
	}

	@Test
	fun `skill accuracy lock policy is supported by runtime mapper`() {
		assertThat("accuracy-lock-on-target".locksAccuracyOnTarget()).isTrue()
		assertThat("accuracy-lock-on-target".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `item element boost policy is parsed from element suffix`() {
		val effect = "element-damage-boost-fire".toBattleItemEffect(elementIds)

		assertThat(effect).isInstanceOf(BattleItemEffect.ElementDamageBoost::class.java)
		effect as BattleItemEffect.ElementDamageBoost
		assertThat(effect.elementId).isEqualTo(10L)
		assertThat(effect.multiplier).isEqualTo(1.2)
	}

	@Test
	fun `item element reduction keeps normal element exception`() {
		val normalReduction = "element-damage-reduction-normal".toBattleItemEffect(elementIds)
		val waterReduction = "element-damage-reduction-water".toBattleItemEffect(elementIds)

		assertThat(normalReduction).isInstanceOf(BattleItemEffect.ElementDamageReduction::class.java)
		assertThat((normalReduction as BattleItemEffect.ElementDamageReduction).requiresSuperEffective).isFalse()
		assertThat(waterReduction).isInstanceOf(BattleItemEffect.ElementDamageReduction::class.java)
		assertThat((waterReduction as BattleItemEffect.ElementDamageReduction).requiresSuperEffective).isTrue()
	}

	@Test
	fun `ability grounding policy is supported without creating effect object`() {
		assertThat("ground-immunity".toBattleAbilityEffect(elementIds)).isNull()
		assertThat("ground-immunity".isBattleAbilityRuntimePolicySupported(elementIds)).isTrue()
		assertThat("missing-policy".isBattleAbilityRuntimePolicySupported(elementIds)).isFalse()
	}

	@Test
	fun `weight multiplier policies keep rational scale`() {
		assertThat("weight-double".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.WeightMultiplier(numerator = 2, denominator = 1))
		assertThat("weight-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.WeightMultiplier(numerator = 1, denominator = 2))
		assertThat("weight-half".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.WeightMultiplier(numerator = 1, denominator = 2))
	}

	@Test
	fun `skill temporary weight reduction policy requires successful speed change`() {
		assertThat("self-weight-reduction-100kg-after-speed-change".toBattleSkillWeightEffects()).containsExactly(
			BattleSkillWeightEffect(
				target = BattleEffectTarget.USER,
				reduction = 1000,
				minimumWeight = 1,
				requiredChangedStat = BattleStat.SPEED,
			),
		)
		assertThat("self-weight-reduction-100kg-after-speed-change".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill target grounded terrain power policy keeps terrain condition explicit`() {
		assertThat("power-one-and-half-if-electric-terrain".toBattleSkillPowerMultipliers()).containsExactly(
			BattleSkillPowerMultiplier.ActiveTerrain(
				terrain = BattleTerrain.ELECTRIC,
				multiplier = 1.5,
			),
		)
		assertThat("power-double-if-target-grounded-electric-terrain".toBattleSkillPowerMultipliers()).containsExactly(
			BattleSkillPowerMultiplier.TargetGroundedTerrain(
				terrain = BattleTerrain.ELECTRIC,
				multiplier = 2.0,
			),
		)
		assertThat("power-one-and-half-if-electric-terrain".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("power-double-if-target-grounded-electric-terrain".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill grounded terrain priority policy maps to priority boost`() {
		assertThat("priority-plus-one-if-user-grounded-grassy-terrain".toBattleSkillGroundedTerrainPriorityBoosts())
			.containsExactlyEntriesOf(mapOf(BattleTerrain.GRASSY to 1))
		assertThat("priority-plus-one-if-user-grounded-grassy-terrain".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill special damage target defense policy maps defender stat override`() {
		assertThat("special-damage-target-defense".toBattleSkillDefendingStatOverride()).isEqualTo(BattleStat.DEFENSE)
		assertThat("special-damage-target-defense".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill non fainting damage policy maps target one hp floor`() {
		assertThat("leave-target-at-one-hp".leavesTargetAtOneHp()).isTrue()
		assertThat("leave-target-at-one-hp".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill screen breaking policy maps pre damage side condition removal`() {
		assertThat("break-target-side-damage-reductions".breaksTargetSideDamageReductions()).isTrue()
		assertThat("break-target-side-damage-reductions".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill target last skill pp reduction policy maps reduction amount`() {
		assertThat("target-last-skill-pp-reduction-four".targetLastSkillPpReduction()).isEqualTo(4)
		assertThat("target-last-skill-pp-reduction-four".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill self critical hit boost policy maps focus energy bonus`() {
		assertThat("self-critical-hit-stage-plus-two".criticalHitStageBoost()).isEqualTo(2)
		assertThat("self-critical-hit-stage-plus-two".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill rest and team status cure policies map explicit flags`() {
		assertThat("self-rest-full-heal".restoresUserBySleeping()).isTrue()
		assertThat("self-rest-full-heal".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("self-major-status-cure".curesUserMajorStatus()).isTrue()
		assertThat("self-major-status-cure".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("user-side-major-status-cure".curesUserSideMajorStatuses()).isTrue()
		assertThat("user-side-major-status-cure".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("target-heal-quarter-max-hp-user-side-active-major-status-cure".curesUserSideActiveMajorStatuses())
			.isTrue()
		assertThat("target-heal-quarter-max-hp-user-side-active-major-status-cure".isBattleSkillRuntimeEffectPolicySupported())
			.isTrue()
	}

	@Test
	fun `skill endure policy maps fatal damage survival flag`() {
		assertThat("endure-fatal-damage".enduresFatalDamage()).isTrue()
		assertThat("endure-fatal-damage".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill direct status hp policies map explicit hp effects`() {
		assertThat("maximize-user-attack-half-max-hp-cost".toBattleSkillHpEffects())
			.containsExactly(BattleSkillHpEffect.MaximizeUserAttackWithHalfMaxHpCost)
		assertThat("average-user-target-current-hp".toBattleSkillHpEffects())
			.containsExactly(BattleSkillHpEffect.AverageUserAndTargetCurrentHp)
		assertThat("target-heal-quarter-max-hp".toBattleSkillHpEffects())
			.containsExactly(BattleSkillHpEffect.TargetHealMaxHpFraction(1, 4))
		assertThat("target-heal-quarter-max-hp-user-side-active-major-status-cure".toBattleSkillHpEffects())
			.containsExactly(BattleSkillHpEffect.TargetHealMaxHpFraction(1, 4))
		assertThat("maximize-user-attack-half-max-hp-cost".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("average-user-target-current-hp".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("target-heal-quarter-max-hp".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `side protection field policies map to protection kinds`() {
		assertThat("side-stat-stage-reduction-protection".toBattleSideProtectionKind())
			.isEqualTo(BattleSideProtectionKind.STAT_STAGE_REDUCTION)
		assertThat("side-status-condition-protection".toBattleSideProtectionKind())
			.isEqualTo(BattleSideProtectionKind.STATUS_CONDITION)
		assertThat("side-unknown-protection".toBattleSideProtectionKind()).isNull()
	}

	@Test
	fun `skill user hp dynamic power policy maps six step thresholds`() {
		assertThat("power-by-user-current-hp-ratio".toBattleSkillDynamicPower()).isEqualTo(
			BattleSkillDynamicPower.UserHpFractionThresholds(
				scale = 48,
				thresholds = listOf(
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 1, power = 200),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 4, power = 150),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 9, power = 100),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 16, power = 80),
					BattleSkillDynamicPower.HpPowerThreshold(maxScaledHpInclusive = 32, power = 40),
				),
				fallbackPower = 20,
			),
		)
		assertThat("power-by-user-current-hp-ratio".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}
}
