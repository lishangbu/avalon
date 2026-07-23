package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
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
		"bug" to 7L,
		"normal" to 1L,
		"flying" to 3L,
		"fighting" to 2L,
		"poison" to 4L,
		"fire" to 10L,
		"water" to 11L,
		"grass" to 12L,
		"electric" to 13L,
		"ice" to 14L,
		"ghost" to 8L,
		"steel" to 9L,
		"psychic" to 14L,
		"dragon" to 16L,
		"dark" to 17L,
		"fairy" to 18L,
		"ground" to 5L,
	)

	@Test
	fun `received damage stat item policies map element and weakness triggers`() {
		assertThat("berry-marker".toBattleItemEffect(elementIds)).isEqualTo(BattleItemEffect.BerryMarker())
		val expected = mapOf(
			"received-water-special-attack-stage-plus-one" to BattleItemEffect.ReceivedDamageStatStageBoost(
				elementId = 11,
				stageChanges = mapOf(BattleStat.SPECIAL_ATTACK to 1),
			),
			"received-electric-attack-stage-plus-one" to BattleItemEffect.ReceivedDamageStatStageBoost(
				elementId = 13,
				stageChanges = mapOf(BattleStat.ATTACK to 1),
			),
			"received-water-special-defense-stage-plus-one" to BattleItemEffect.ReceivedDamageStatStageBoost(
				elementId = 11,
				stageChanges = mapOf(BattleStat.SPECIAL_DEFENSE to 1),
			),
			"received-ice-attack-stage-plus-one" to BattleItemEffect.ReceivedDamageStatStageBoost(
				elementId = 14,
				stageChanges = mapOf(BattleStat.ATTACK to 1),
			),
			"received-super-effective-attack-special-attack-stage-plus-two" to
				BattleItemEffect.ReceivedDamageStatStageBoost(
					requiresSuperEffective = true,
					stageChanges = mapOf(BattleStat.ATTACK to 2, BattleStat.SPECIAL_ATTACK to 2),
				),
		)

		expected.forEach { (policy, effect) ->
			assertThat(policy.toBattleItemEffect(elementIds)).isEqualTo(effect)
		}
	}

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
	fun `ability shield policy maps ability ignore protection`() {
		assertThat("ability-ignore-protection".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AbilityIgnoreProtection())
	}

	@Test
	fun `utility umbrella policy maps sun and rain effect immunity`() {
		assertThat("sun-rain-effect-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.SunRainEffectImmunity())
	}

	@Test
	fun `reactive stat item policies map ability reduction boost and opponent copy`() {
		assertThat("ability-stat-reduction-speed-stage-plus-one".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AbilityStatReductionReactiveBoost(BattleStat.SPEED, 1))
		assertThat("opponent-positive-stat-stage-copy".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.OpponentPositiveStatStageCopy())
	}

	@Test
	fun `forced switch item policies map damage and stat reduction triggers`() {
		assertThat("damaged-force-self-switch".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.DamagedForceSelfSwitch())
		assertThat("damaged-force-attacker-switch".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.DamagedForceAttackerSwitch())
		assertThat("negative-stat-stage-force-self-switch".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.NegativeStatStageForceSelfSwitch())
	}

	@Test
	fun `mental herb and destiny knot policies map volatile status lifecycle effects`() {
		assertThat("volatile-status-cure-mental-herb".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.VolatileStatusCure(
					setOf(
						BattleVolatileStatus.HEAL_BLOCK,
						BattleVolatileStatus.TAUNT,
						BattleVolatileStatus.DISABLE,
						BattleVolatileStatus.TORMENT,
						BattleVolatileStatus.INFATUATION,
					),
				),
			)
		assertThat("infatuation-reflect-to-source".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.InfatuationReflectToSource())
	}

	@Test
	fun `booster energy policy maps matching abilities highest stat activation`() {
		assertThat("highest-stat-booster-abilities-protosynthesis-quark-drive".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.HighestStatBoosterActivation(setOf(281, 282)))
	}

	@Test
	fun `ability immunity policies map status and stat stage protection`() {
		assertThat("stat-drop-immunity-defense".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentStatStageReductionImmunity(setOf(BattleStat.DEFENSE)))
		assertThat("major-status-immunity-poison".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON)))
		assertThat("volatile-status-immunity-infatuation-taunt".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.VolatileStatusImmunity(
					setOf(BattleVolatileStatus.INFATUATION, BattleVolatileStatus.TAUNT),
				),
			)
	}

	@Test
	fun `conditional stat ability policies retain status environment and hp conditions`() {
		assertThat("burn-special-attack-stat-one-and-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.AttackingStatMultiplier(
					BattleStat.SPECIAL_ATTACK,
					1.5,
					requiredMajorStatuses = setOf(BattleMajorStatus.BURN),
				),
			)
		assertThat("major-status-speed-one-and-half-ignore-paralysis".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.MajorStatusSpeedMultiplier(1.5, true))
		assertThat("half-hp-attack-stat-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.AttackingStatMultiplier(BattleStat.ATTACK, 0.5, maximumHpFraction = 0.5))
	}

	@Test
	fun `received damage ability policies retain hit conditions and stage changes`() {
		assertThat("contact-attacker-speed-minus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.ReceivedDamageStatStageChange(
					mapOf(BattleStat.SPEED to -1),
					requiresContact = true,
					changesAttacker = true,
				),
			)
		assertThat("contact-random-poison-paralysis-sleep".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.RandomContactStatusOnAttacker(
					listOf(BattleMajorStatus.POISON, BattleMajorStatus.PARALYSIS, BattleMajorStatus.SLEEP),
					30,
				),
			)
		assertThat("received-fire-water-speed-plus-six".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.ReceivedDamageStatStageChange(
					mapOf(BattleStat.SPEED to 6),
					elementIds = setOf(10, 11),
				),
			)
	}

	@Test
	fun `element and effectiveness ability policies map damage multipliers`() {
		assertThat("received-fire-ice-damage-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ElementSkillDamageReduction(setOf(10, 14), 0.5))
		assertThat("super-effective-damage-boost-quarter".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EffectivenessDamageBoost(1.25, requiresSuperEffective = true))
		assertThat("critical-hit-damage-boost-one-and-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.CriticalHitDamageBoost(1.5))
		assertThat("element-dark-damage-boost-four-thirds".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FieldElementSkillDamageAura(17, 4.0 / 3.0, 3.0 / 4.0))
		assertThat("field-damage-aura-reversal".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FieldDamageAuraReversal())
	}

	@Test
	fun `accuracy ability policies map user target and cap modifiers`() {
		assertThat("physical-accuracy-multiplier-four-fifths".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.AccuracyMultiplier(0.8, setOf(BattleDamageClass.PHYSICAL)))
		assertThat("opponent-accuracy-sandstorm-four-fifths".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentAccuracyMultiplier(0.8, BattleWeather.SANDSTORM))
		assertThat("accuracy-always-hit".toBattleAbilityEffect(elementIds)).isEqualTo(BattleAbilityEffect.AlwaysHit())
		assertThat("status-skill-accuracy-cap-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.StatusSkillAccuracyCap(50))
	}

	@Test
	fun `switch in stat policies distinguish self and opponents`() {
		assertThat("switch-in-self-attack-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.SwitchInStatStageChange(
					BattleStat.ATTACK,
					1,
					BattleEffectTarget.USER,
				),
			)
		assertThat("switch-in-opponents-evasion-minus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInStatStageChange(BattleStat.EVASION, -1))
	}

	@Test
	fun `faint boost policies distinguish killer any faint and highest stat`() {
		assertThat("caused-faint-attack-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FaintStatStageBoost(BattleStat.ATTACK, 1, true))
		assertThat("any-faint-special-attack-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FaintStatStageBoost(BattleStat.SPECIAL_ATTACK, 1, false))
		assertThat("caused-faint-highest-stat-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FaintHighestStatBoost())
	}

	@Test
	fun `end turn ability policies map damage healing cure and stat changes`() {
		assertThat("end-turn-speed-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EndTurnStatStageChange(BattleStat.SPEED, 1))
		assertThat("end-turn-major-status-cure-rain".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EndTurnMajorStatusCure(requiredWeathers = setOf(BattleWeather.RAIN)))
		assertThat("poison-status-end-turn-heal-eighth".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.MajorStatusEndTurnHeal(
					setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
					8,
				),
			)
		assertThat("sun-end-turn-damage-eighth".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.WeatherEndTurnDamage(setOf(BattleWeather.SUN), 8))
	}

	@Test
	fun `stat transform ability policies map multipliers and reactive boosts`() {
		assertThat("stat-stage-delta-reverse".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.StatStageDeltaMultiplier(-1))
		assertThat("stat-stage-delta-double".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.StatStageDeltaMultiplier(2))
		assertThat("opponent-stat-drop-attack-plus-two".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentStatReductionReactiveBoost(BattleStat.ATTACK, 2))
	}

	@Test
	fun `passive and switch ability policies map executable effects`() {
		assertThat("powder-skill-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.PowderSkillImmunity())
		assertThat("damaging-skill-secondary-effect-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.DamagingSkillSecondaryEffectImmunity())
		assertThat("contact-suppression".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ContactSuppression())
		assertThat("critical-hit-stage-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.CriticalHitStageBoost(1))
		assertThat("switch-out-heal-third".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchOutHeal(3))
	}

	@Test
	fun `damage threshold ability policies map critical and half hp reactions`() {
		assertThat("critical-damage-set-attack-plus-six".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.CriticalDamageSetStatStage(BattleStat.ATTACK, 6))
		assertThat("cross-half-hp-special-attack-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.DamageCrossedHpThresholdStatStageChange(
					mapOf(BattleStat.SPECIAL_ATTACK to 1),
				),
			)
	}

	@Test
	fun `received damage environment policies map weather and terrain`() {
		assertThat("received-damage-weather-sandstorm".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ReceivedDamageWeatherChange(BattleWeather.SANDSTORM))
		assertThat("received-damage-terrain-grassy".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ReceivedDamageTerrainChange(BattleTerrain.GRASSY))
	}

	@Test
	fun `ruin and fluffy policies map formula modifiers`() {
		assertThat("opponent-attack-stat-three-quarters".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentAttackingStatMultiplier(BattleStat.ATTACK, 0.75))
		assertThat("opponent-defense-stat-three-quarters".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentDefendingStatMultiplier(BattleStat.DEFENSE, 0.75))
		assertThat("received-contact-damage-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ContactBasedSkillDamageReduction(0.5))
	}

	@Test
	fun `skill shape ability policies map hit count and damage conditions`() {
		assertThat("multi-hit-maximum".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.MultiHitMaximum())
		assertThat("base-power-at-most-sixty-damage-one-and-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.BasePowerAtMostDamageBoost(60, 1.5))
		assertThat("recoil-skill-damage-six-fifths".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.RecoilSkillDamageBoost(1.2))
	}

	@Test
	fun `status offense and defense policies map conditional rules`() {
		assertThat("poison-element-status-bypass".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.PoisonElementStatusBypass())
		assertThat("opponent-status-skill-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentStatusSkillImmunity())
		assertThat("poisoned-target-guaranteed-critical-hit".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.MajorStatusGuaranteedCriticalHit(
					setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON),
				),
			)
	}

	@Test
	fun `download and elemental redirection policies map executable effects`() {
		assertThat("switch-in-opponent-defense-comparison-attack-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInOpponentDefenseComparisonBoost())
		assertThat("element-electric-absorb-special-attack-up".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.ElementSkillAbsorbStatStage(
					elementIds.getValue("electric"),
					BattleStat.SPECIAL_ATTACK,
					1,
				),
			)
	}

	@Test
	fun `pp sleep and side status policies map executable effects`() {
		assertThat("opponent-skill-pp-cost-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentSkillPpCostIncrease(1))
		assertThat("sleep-duration-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SleepDurationDivisor(2))
		assertThat("side-major-status-immunity-sleep".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SideMajorStatusImmunity(setOf(BattleMajorStatus.SLEEP)))
	}

	@Test
	fun `specialized priority policies map element and healing conditions`() {
		assertThat("full-hp-flying-skill-priority-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ElementSkillPriorityBoost(elementIds.getValue("flying"), 1, true))
		assertThat("healing-skill-priority-plus-three".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.HealingSkillPriorityBoost(3))
	}

	@Test
	fun `weather suppression policy maps continuous effect`() {
		assertThat("weather-effect-suppression".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.WeatherEffectSuppression())
	}

	@Test
	fun `switch restriction policies map target conditions and immunity`() {
		assertThat("grounded-opponent-switch-restriction".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentSwitchRestriction(requiresGroundedTarget = true))
		assertThat("steel-opponent-switch-restriction".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentSwitchRestriction(requiredTargetElementId = elementIds.getValue("steel")))
		assertThat("forced-switch-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ForcedSwitchImmunity())
	}

	@Test
	fun `unburden policy maps item loss speed multiplier`() {
		assertThat("item-lost-speed-double".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ItemLostSpeedMultiplier(2.0))
	}

	@Test
	fun `type immunity policies map bypass and wonder guard`() {
		assertThat("normal-fighting-type-immunity-bypass".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ElementTypeImmunityBypass(setOf(1, 2)))
		assertThat("non-super-effective-damage-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.NonSuperEffectiveDamageImmunity())
	}

	@Test
	fun `faint retaliation policies map contact and damage taken sources`() {
		assertThat("contact-faint-attacker-max-hp-quarter-damage".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.FaintAttackerDamage(
					requiresContact = true,
					attackerMaxHpDenominator = 4,
					suppressedByExplosionSuppression = true,
				),
			)
		assertThat("faint-attacker-damage-taken".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FaintAttackerDamage(usesDamageTaken = true))
	}

	@Test
	fun `threshold switch policy maps half hp crossing`() {
		assertThat("cross-half-hp-force-self-switch".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.DamageCrossedHpThresholdForceSelfSwitch())
	}

	@Test
	fun `disable cotton and steadfast policies map reactions`() {
		assertThat("received-damage-disable-attacker-skill-thirty-percent".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ReceivedDamageDisableAttackerSkill(30, 4))
		assertThat("received-damage-all-other-speed-minus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ReceivedDamageAllOtherStatStageChange(BattleStat.SPEED, -1))
		assertThat("flinch-speed-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FlinchStatStageBoost(BattleStat.SPEED, 1))
	}

	@Test
	fun `switch in ally and field policies map effects`() {
		assertThat("switch-in-ally-heal-quarter".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInAllyHeal(4))
		assertThat("switch-in-ally-stat-stage-copy".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInAllyStatStageCopy())
		assertThat("switch-in-clear-all-side-damage-reductions".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInClearAllSideDamageReductions())
		assertThat("fainted-ally-ability-copy".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FaintedAllyAbilityCopy())
		assertThat("opponent-major-status-reflection".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentMajorStatusReflection())
		assertThat("first-skill-element-change-since-switch-in".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.FirstSkillElementChangeSinceSwitchIn())
		assertThat("single-target-second-hit-quarter-damage".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SingleTargetSecondHit())
		assertThat("poison-application-confusion".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.PoisonApplicationConfusion())
		assertThat("terastallization-environment-clear".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.TerastallizationEnvironmentClear())
		assertThat("opponent-stat-stage-increase-copy".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentStatStageIncreaseCopy())
		assertThat("low-hp-item-trigger-threshold-half".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.LowHpItemTriggerThresholdHalf())
		assertThat("berry-consumption-heal-third".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.BerryConsumptionHeal(3))
		assertThat("received-contact-shared-perish-countdown-three".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ContactSharedPerishCountdown(3))
		assertThat("status-skill-moves-last-ignore-target-ability".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.StatusSkillMovesLastAndIgnoresTargetAbility())
		assertThat("terrain-element-identity".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.TerrainElementIdentity(
					mapOf(
						BattleTerrain.ELECTRIC to elementIds.getValue("electric"),
						BattleTerrain.GRASSY to elementIds.getValue("grass"),
						BattleTerrain.MISTY to elementIds.getValue("fairy"),
						BattleTerrain.PSYCHIC to elementIds.getValue("psychic"),
					),
				),
			)
		assertThat("end-turn-consumed-berry-restore-half-sun-guaranteed".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EndTurnConsumedBerryRestore(50, BattleWeather.SUN))
		assertThat("end-turn-pickup-last-consumed-item".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EndTurnPickupConsumedItem())
		assertThat("berry-effect-double".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.BerryEffectMultiplier(2.0))
		assertThat("sun-highest-stat-boost".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EnvironmentHighestStatMultiplier(requiredWeather = BattleWeather.SUN))
		assertThat("electric-terrain-highest-stat-boost".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EnvironmentHighestStatMultiplier(requiredTerrain = BattleTerrain.ELECTRIC))
		assertThat("caused-faint-once-attack-special-attack-speed-plus-one".toBattleAbilityEffect(elementIds))
			.isEqualTo(
				BattleAbilityEffect.OncePerBattleCausedFaintMultiStatBoost(
					setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK, BattleStat.SPEED),
					1,
				),
			)
		assertThat("switch-in-reveal-opponent-held-items".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInRevealOpponentHeldItems())
		assertThat("switch-in-reveal-opponent-highest-power-skill".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInRevealOpponentHighestPowerSkill())
		assertThat("opponent-stat-stage-reduction-reflection".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentStatStageReductionReflection())
		assertThat("switch-in-transform-into-opponent".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInTransformIntoOpponent())
		assertThat("switch-in-detect-dangerous-opponent-skill".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInDetectDangerousOpponentSkill())
		assertThat("received-damage-next-electric-damage-double".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ReceivedDamageNextElementDamageBoost(13, 2.0))
		assertThat("always-treated-asleep-major-status-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.AlwaysTreatedAsleep())
		assertThat("held-item-removal-immunity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.HeldItemRemovalImmunity())
		assertThat("held-item-element-identity".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.HeldItemElementIdentity())
		assertThat("opponent-berry-consumption-prevention".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.OpponentBerryConsumptionPrevention())
		assertThat("ally-item-consumption-transfer".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.AllyItemConsumptionTransfer())
		assertThat("end-turn-next-turn-consumed-berry-replay".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.EndTurnConsumedBerryReplay(1))
	}

	@Test
	fun `rusted item policies map matching crowned form overrides`() {
		assertThat("creature-form-override-zacian-crowned".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.CreatureFormOverride(sourceCreatureId = 888, targetCreatureId = 10188))
		assertThat("creature-form-override-zamazenta-crowned".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.CreatureFormOverride(sourceCreatureId = 889, targetCreatureId = 10189))
	}

	@Test
	fun `eviolite policy maps evolvable defense multipliers`() {
		assertThat("evolvable-defense-special-defense-one-and-half".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.EvolvableDefendingStatMultiplier(
					setOf(BattleStat.DEFENSE, BattleStat.SPECIAL_DEFENSE),
					1.5,
				),
			)
	}

	@Test
	fun `metronome item policy maps consecutive skill damage boost`() {
		assertThat("consecutive-skill-damage-boost-twenty-percent".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ConsecutiveSkillDamageBoost())
	}

	@Test
	fun `covert cloak policy maps damaging skill secondary immunity`() {
		assertThat("damaging-skill-secondary-effect-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.DamagingSkillSecondaryEffectImmunity())
	}

	@Test
	fun `stat protection item policies map immunity and reset`() {
		assertThat("opponent-stat-stage-reduction-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.OpponentStatStageReductionImmunity())
		assertThat("negative-stat-stage-reset".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.NegativeStatStageReset())
	}

	@Test
	fun `micle berry policy maps next skill accuracy boost`() {
		assertThat("low-hp-next-skill-accuracy-six-fifths".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.LowHpNextSkillAccuracyBoost(1.2))
	}

	@Test
	fun `starf berry policy maps random battle stat boost`() {
		assertThat("low-hp-random-battle-stat-stage-plus-two".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.LowHpRandomStatStageBoost(
					setOf(
						BattleStat.ATTACK,
						BattleStat.DEFENSE,
						BattleStat.SPECIAL_ATTACK,
						BattleStat.SPECIAL_DEFENSE,
						BattleStat.SPEED,
					),
					2,
				),
			)
	}

	@Test
	fun `flavor berry policies retain disliked nature stat`() {
		val expected = mapOf(
			"quarter-hp-third-heal-confuse-attack" to BattleStat.ATTACK,
			"quarter-hp-third-heal-confuse-defense" to BattleStat.DEFENSE,
			"quarter-hp-third-heal-confuse-special-attack" to BattleStat.SPECIAL_ATTACK,
			"quarter-hp-third-heal-confuse-special-defense" to BattleStat.SPECIAL_DEFENSE,
			"quarter-hp-third-heal-confuse-speed" to BattleStat.SPEED,
		)

		expected.forEach { (policy, stat) ->
			assertThat(policy.toBattleItemEffect(elementIds)).isEqualTo(
				BattleItemEffect.LowHpHeal(1, 4, healDenominator = 3, confusesIfNatureDecreases = stat),
			)
		}
	}

	@Test
	fun `zoom lens policy maps target acted accuracy boost`() {
		assertThat("accuracy-multiplier-six-fifths-after-target-acted".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AccuracyMultiplierAfterTargetActed(1.2))
	}

	@Test
	fun `grounding item policies map forced grounding speed and immunity suppression`() {
		assertThat("airborne-until-damaged".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AirborneUntilDamaged())
		assertThat("force-grounded".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.GroundingOverride())
		assertThat("speed-multiplier-half".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.SpeedMultiplier(0.5))
		assertThat("type-immunity-suppression".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.TypeImmunitySuppression())
	}

	@Test
	fun `room service policy maps trick room speed reduction`() {
		assertThat("field-speed-order-trick-room-speed-stage-minus-one".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.FieldSpeedOrderActivatedStatStageChange(
					BattleFieldSpeedOrderKind.TRICK_ROOM,
					BattleStat.SPEED,
					-1,
				),
			)
	}

	@Test
	fun `normal gem policy maps consumable normal power boost`() {
		assertThat("consumable-element-damage-boost-normal-thirty-percent".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ConsumableElementDamageBoost(1, 1.3))
	}

	@Test
	fun `species item policies retain creature stat and element restrictions`() {
		assertThat("species-pikachu-attack-special-attack-double".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.CreatureStatMultiplier(setOf(25), setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK), 2.0))
		assertThat("species-dialga-steel-dragon-power-one-fifth".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.CreatureElementDamageBoost(setOf(483), setOf(9, 16), 1.2))
	}

	@Test
	fun `focus band policy maps random fatal damage survival`() {
		assertThat("random-fatal-damage-survival-ten-percent".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.RandomFatalDamageSurvival(10))
	}

	@Test
	fun `action order item policies map random forced last and low hp boosts`() {
		assertThat("random-action-order-boost-twenty-percent".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.RandomActionOrderBoost(20))
		assertThat("forced-last-action-order".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ForcedLastActionOrder())
		assertThat("low-hp-action-order-boost-quarter".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.LowHpActionOrderBoost())
	}

	@Test
	fun `drain and binding item policies map lifecycle modifiers`() {
		assertThat("drain-healing-multiplier-thirteen-tenths".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.DrainHealingMultiplier(13, 10))
		assertThat("binding-duration-seven".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.BindingDurationOverride(7))
		assertThat("binding-damage-denominator-six".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.BindingDamageDenominator(6))
	}

	@Test
	fun `flinch held item policy maps ten percent additional chance`() {
		assertThat("additional-flinch-chance-ten-percent".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AdditionalFlinchChance(chancePercent = 10))
	}

	@Test
	fun `blunder policy maps accuracy miss speed boost`() {
		assertThat("accuracy-miss-speed-stage-plus-two".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AccuracyMissStatStageBoost(BattleStat.SPEED, stageDelta = 2))
	}

	@Test
	fun `throat spray policy maps successful sound skill special attack boost`() {
		assertThat("successful-sound-skill-special-attack-stage-plus-one".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.SuccessfulSkillStatStageBoost(
					requiresSoundBased = true,
					stat = BattleStat.SPECIAL_ATTACK,
					stageDelta = 1,
				),
			)
	}

	@Test
	fun `terrain seed policies map grounded matching terrain stat boosts`() {
		val expected = mapOf(
			"terrain-electric-defense-stage-plus-one" to
				BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.ELECTRIC, BattleStat.DEFENSE, 1),
			"terrain-grassy-defense-stage-plus-one" to
				BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.GRASSY, BattleStat.DEFENSE, 1),
			"terrain-misty-special-defense-stage-plus-one" to
				BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.MISTY, BattleStat.SPECIAL_DEFENSE, 1),
			"terrain-psychic-special-defense-stage-plus-one" to
				BattleItemEffect.TerrainActivatedStatStageBoost(BattleTerrain.PSYCHIC, BattleStat.SPECIAL_DEFENSE, 1),
		)

		expected.forEach { (policy, effect) ->
			assertThat(policy.toBattleItemEffect(elementIds)).isEqualTo(effect)
		}
	}

	@Test
	fun `assault vest policies map special defense boost and status restriction`() {
		assertThat("special-defense-stat-one-and-half".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.DefendingStatMultiplier(
					stats = setOf(BattleStat.SPECIAL_DEFENSE),
					multiplier = 1.5,
				),
			)
		assertThat("status-skill-selection-restriction".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.StatusSkillRestriction())
	}

	@Test
	fun `lansat berry policy maps low hp critical boost`() {
		assertThat("low-hp-critical-hit-stage-plus-two".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.LowHpCriticalHitStageBoost(stageBonus = 2))
	}

	@Test
	fun `low hp stat berry policies map quarter hp one stage boosts`() {
		val expectedStats = mapOf(
			"low-hp-attack-stage-plus-one" to BattleStat.ATTACK,
			"low-hp-defense-stage-plus-one" to BattleStat.DEFENSE,
			"low-hp-speed-stage-plus-one" to BattleStat.SPEED,
			"low-hp-special-attack-stage-plus-one" to BattleStat.SPECIAL_ATTACK,
			"low-hp-special-defense-stage-plus-one" to BattleStat.SPECIAL_DEFENSE,
		)

		expectedStats.forEach { (policy, stat) ->
			assertThat(policy.toBattleItemEffect(elementIds))
				.isEqualTo(BattleItemEffect.LowHpStatStageBoost(stat = stat, stageDelta = 1))
		}
	}

	@Test
	fun `shed shell policy maps opponent switch restriction immunity`() {
		assertThat("switch-restriction-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.SwitchRestrictionImmunity())
	}

	@Test
	fun `accuracy item policies map user boost and opponent reduction`() {
		assertThat("accuracy-multiplier-eleven-tenths".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.AccuracyMultiplier(multiplier = 1.1))
		assertThat("opponent-accuracy-multiplier-nine-tenths".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.OpponentAccuracyMultiplier(multiplier = 0.9))
	}

	@Test
	fun `black sludge policies split poison healing and non poison damage`() {
		assertThat("held-end-turn-heal-poison-sixteenth".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.HeldEndTurnHealForElement(elementId = 4, healDenominator = 16))
		assertThat("held-end-turn-damage-non-poison-eighth".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.HeldEndTurnDamageWithoutElement(elementId = 4, damageDenominator = 8))
	}

	@Test
	fun `loaded dice policy narrows standard multi hit count`() {
		assertThat("standard-multi-hit-count-four-to-five".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.MultiHitCountRangeOverride(minHits = 4, maxHits = 5))
	}

	@Test
	fun `heavy duty boots policy maps entry hazard immunity`() {
		assertThat("entry-hazard-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.EntryHazardImmunity())
	}

	@Test
	fun `safety goggles policies map powder and sandstorm immunity`() {
		assertThat("powder-skill-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.PowderSkillImmunity())
		assertThat("weather-damage-immunity-sandstorm".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.WeatherDamageImmunity(setOf(BattleWeather.SANDSTORM)))
	}

	@Test
	fun `critical hit held item policy maps one stage boost`() {
		assertThat("critical-hit-stage-plus-one".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.CriticalHitStageBoost(stageDelta = 1))
	}

	@Test
	fun `status orb policies map to end turn self status effects`() {
		assertThat("held-end-turn-major-status-burn".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.HeldEndTurnMajorStatus(BattleMajorStatus.BURN))
		assertThat("held-end-turn-major-status-bad-poison".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.HeldEndTurnMajorStatus(BattleMajorStatus.BAD_POISON))
	}

	@Test
	fun `choice band and specs policies compose skill lock with matching damage boost`() {
		assertThat("choice-skill-lock".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.0))
		assertThat("damage-class-power-boost-physical-50".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.DamageClassPowerBoost(
					damageClasses = setOf(BattleDamageClass.PHYSICAL),
					multiplier = 1.5,
				),
			)
		assertThat("damage-class-power-boost-special-50".toBattleItemEffect(elementIds))
			.isEqualTo(
				BattleItemEffect.DamageClassPowerBoost(
					damageClasses = setOf(BattleDamageClass.SPECIAL),
					multiplier = 1.5,
				),
			)
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
	fun `ability fact and format no effect policies are supported without creating effect object`() {
		assertThat("ground-immunity".toBattleAbilityEffect(elementIds)).isNull()
		assertThat("ground-immunity".isBattleAbilityRuntimePolicySupported(elementIds)).isTrue()
		assertThat("single-battle-no-effect".toBattleAbilityEffect(elementIds)).isNull()
		assertThat("single-battle-no-effect".isBattleAbilityRuntimePolicySupported(elementIds)).isTrue()
		assertThat("missing-policy".isBattleAbilityRuntimePolicySupported(elementIds)).isFalse()
	}

	@Test
	fun `target gender damage policy maps to rivalry multiplier`() {
		assertThat("target-gender-damage-five-quarters-three-quarters".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.TargetGenderDamageMultiplier())
	}

	@Test
	fun `contact opposite gender infatuation policy maps to cute charm effect`() {
		assertThat("contact-opposite-gender-infatuation-thirty-percent".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ContactInfatuationOnAttacker())
	}

	@Test
	fun `switch in disguise policy maps to illusion effect`() {
		assertThat("switch-in-disguise-as-last-healthy-ally".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.SwitchInDisguiseAsLastHealthyAlly())
	}

	@Test
	fun `contact protection bypass and contact item policies map to runtime effects`() {
		assertThat("contact-skill-protection-bypass".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ContactSkillProtectionBypass())
		assertThat("contact-skill-protection-bypass".isBattleAbilityRuntimePolicySupported(elementIds)).isTrue()
		assertThat("contact-damage-to-attacker-eighth".toBattleAbilityEffect(elementIds))
			.isEqualTo(BattleAbilityEffect.ContactDamageToAttacker(damageDenominator = 8))
		assertThat("contact-damage-to-attacker-eighth".isBattleAbilityRuntimePolicySupported(elementIds)).isTrue()

		assertThat("contact-damage-to-attacker-sixth".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ContactDamageToAttacker(damageDenominator = 6))
		assertThat("contact-transfer-to-attacker".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ContactTransferToAttacker())
		assertThat("held-end-turn-damage-eighth".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.HeldEndTurnDamage(damageDenominator = 8))
		assertThat("punch-based-skill-power-boost".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.PunchBasedSkillPowerBoost())
		assertThat("punch-based-contact-suppression".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.PunchBasedContactSuppression())
		assertThat("contact-side-effect-immunity".toBattleItemEffect(elementIds))
			.isEqualTo(BattleItemEffect.ContactSideEffectImmunity())
		assertThat("contact-damage-to-attacker-sixth".isBattleItemRuntimePolicySupported(elementIds)).isTrue()
		assertThat("contact-transfer-to-attacker".isBattleItemRuntimePolicySupported(elementIds)).isTrue()
		assertThat("held-end-turn-damage-eighth".isBattleItemRuntimePolicySupported(elementIds)).isTrue()
		assertThat("punch-based-skill-power-boost".isBattleItemRuntimePolicySupported(elementIds)).isTrue()
		assertThat("punch-based-contact-suppression".isBattleItemRuntimePolicySupported(elementIds)).isTrue()
		assertThat("contact-side-effect-immunity".isBattleItemRuntimePolicySupported(elementIds)).isTrue()
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
	fun `skill protection breaking policy maps hit after protection removal flag`() {
		assertThat("break-target-protection-damage".breaksProtection()).isTrue()
		assertThat("break-target-protection-damage".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill target last skill pp reduction policy maps reduction amount`() {
		assertThat("target-last-skill-pp-reduction-four".targetLastSkillPpReduction()).isEqualTo(4)
		assertThat("target-last-skill-pp-reduction-four".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill leech seed policy maps persistent target drain flag`() {
		assertThat("apply-leech-seed".plantsLeechSeed()).isTrue()
		assertThat("apply-leech-seed".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill spin cleanup policy maps user side hazard and trap cleanup flag`() {
		assertThat("clear-user-side-hazards-and-traps".clearsUserSideHazardsAndTraps()).isTrue()
		assertThat("clear-user-side-hazards-and-traps".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill field cleanup policy maps all field hazards and substitutes cleanup flag`() {
		assertThat("clear-field-hazards-and-substitutes".clearsFieldHazardsAndSubstitutes()).isTrue()
		assertThat("clear-field-hazards-and-substitutes".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill target side field cleanup policy maps barriers hazards and substitute bypass flags`() {
		assertThat("clear-target-side-barriers-and-field-hazards".bypassesSubstitute()).isTrue()
		assertThat("clear-target-side-barriers-and-field-hazards".clearsTargetSideBarriersAndFieldHazards()).isTrue()
		assertThat("clear-target-side-barriers-and-field-hazards".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
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
	fun `skill user side guard policies map explicit protection flags`() {
		assertThat("user-side-multi-target-skill-protection".protectsUserSideFromMultiTargetSkills()).isTrue()
		assertThat("user-side-priority-skill-protection".protectsUserSideFromPrioritySkills()).isTrue()
		assertThat("user-side-multi-target-skill-protection".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
		assertThat("user-side-priority-skill-protection".isBattleSkillRuntimeEffectPolicySupported()).isTrue()
	}

	@Test
	fun `skill direct status hp policies map explicit hp effects`() {
		assertThat("maximize-user-attack-half-max-hp-cost".toBattleSkillHpEffects())
			.containsExactly(BattleSkillHpEffect.MaximizeUserAttackWithHalfMaxHpCost())
		assertThat("average-user-target-current-hp".toBattleSkillHpEffects())
			.containsExactly(BattleSkillHpEffect.AverageUserAndTargetCurrentHp())
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
