package io.github.lishangbu.battlerules

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleOneHitKnockOut
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationParticipantRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationSideRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnRequest
import io.github.lishangbu.battlerules.service.BattleRuntimeSnapshotService
import io.github.lishangbu.battlerules.service.isBattleAbilityRuntimePolicySupported
import io.github.lishangbu.battlerules.service.isBattleItemRuntimePolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeDamagePolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeEffectPolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeHitPolicySupported
import io.github.lishangbu.battlerules.service.isBattleSkillRuntimeTargetPolicySupported
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate

@BattleRulesIntegrationTest
/**
 * 验证战斗规则资料能装配为引擎运行时快照。
 */
class BattleRuntimeSnapshotServiceTests(
	@Autowired private val service: BattleRuntimeSnapshotService,
	@Autowired private val jdbcTemplate: JdbcTemplate,
) {
	private val objectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()

	@Test
	fun `official double format assembles engine runtime snapshot`() {
		val snapshot = service.getByFormatCode("official-double")

		assertThat(snapshot.format.code).isEqualTo("official-double")
		assertThat(snapshot.format.mode).isEqualTo(BattleMode.DOUBLE)
		assertThat(snapshot.format.activeParticipantsPerSide).isEqualTo(2)
		assertThat(snapshot.format.teamSize).isEqualTo(4)
		assertThat(snapshot.format.defaultLevel).isEqualTo(50)
		assertThat(snapshot.rules.maxParticipantLevel).isEqualTo(50)
		assertThat(snapshot.rules.uniqueCreatureRequired).isTrue()
		assertThat(snapshot.rules.uniqueItemRequired).isTrue()
		assertThat(snapshot.rules.bannedCreatureIds).isEmpty()
		assertThat(snapshot.rules.bannedSkillIds).isEmpty()
		assertThat(snapshot.rules.elementId("dark")).isEqualTo(17)
		assertThat(snapshot.rules.elementId("electric")).isEqualTo(13)
		assertThat(snapshot.rules.elementId("fire")).isEqualTo(10)
		assertThat(snapshot.rules.elementId("grass")).isEqualTo(12)
		assertThat(snapshot.rules.elementId("ground")).isEqualTo(5)
		assertThat(snapshot.rules.elementId("ice")).isEqualTo(15)
		assertThat(snapshot.rules.elementId("poison")).isEqualTo(4)
		assertThat(snapshot.rules.elementId("rock")).isEqualTo(6)
		assertThat(snapshot.rules.elementId("steel")).isEqualTo(9)
		assertThat(snapshot.rules.elementId("water")).isEqualTo(11)
		assertThat(snapshot.rules.elementChart.multiplier(10, setOf(12))).isEqualTo(2.0)
		assertThat(snapshot.rules.elementChart.multiplier(11, setOf(10))).isEqualTo(2.0)
		assertThat(snapshot.rules.elementChart.multiplier(1, setOf(8))).isEqualTo(0.0)
		assertThat(snapshot.rules.elementChart.multiplier(12, setOf(11, 5))).isEqualTo(4.0)
	}

	@Test
	fun `runtime snapshot rejects unknown format code`() {
		val exception = assertThrows<ApiException> {
			service.getByFormatCode("missing-format")
		}

		assertThat(exception.status).isEqualTo(HttpStatus.NOT_FOUND)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND)
		assertThat(exception.field).isEqualTo("formatCode")
	}

	@Test
	fun `runtime snapshot rejects unsupported format battle mode as api error`() {
		jdbcTemplate.update("update battle_format set battle_mode = ? where code = ?", "TRIPLE", "official-double")
		try {
			val exception = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}

			assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
			assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
			assertThat(exception.field).isEqualTo("battleMode")
			assertThat(exception.message).isEqualTo("不支持的战斗模式: TRIPLE")
		} finally {
			/**
			 * 本测试故意破坏共享集成测试数据库中的赛制资料来模拟生产误配置；必须在 finally 中恢复，
			 * 否则后续运行时装配测试会在读取 `official-double` 时命中同一条坏数据。
			 */
			jdbcTemplate.update("update battle_format set battle_mode = ? where code = ?", "DOUBLE", "official-double")
		}
	}

	@Test
	fun `runtime snapshot rejects malformed format restriction operands`() {
		withTemporaryFormatRestriction(
			code = "runtime-invalid-level-cap",
			restrictionType = "LEVEL",
			restrictionOperator = "MAX",
			operandText = null,
			operandNumber = null,
		) {
			val missingLevelCap = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}
			assertThat(missingLevelCap.field).isEqualTo("operandNumber")
			assertThat(missingLevelCap.message).isEqualTo("赛制限制 runtime-invalid-level-cap 必须配置正数操作数")
		}

		withTemporaryFormatRestriction(
			code = "runtime-invalid-ban-text",
			restrictionType = "SKILL",
			restrictionOperator = "BAN",
			operandText = "1, bad-token",
			operandNumber = null,
		) {
			val invalidBanText = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}
			assertThat(invalidBanText.field).isEqualTo("operandText")
			assertThat(invalidBanText.message).isEqualTo("禁用限制 runtime-invalid-ban-text 包含非数字操作数: bad-token")
		}

		withTemporaryFormatRestriction(
			code = "runtime-non-positive-ban-text",
			restrictionType = "ITEM",
			restrictionOperator = "BAN",
			operandText = "0",
			operandNumber = null,
		) {
			val nonPositiveBanText = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}
			assertThat(nonPositiveBanText.field).isEqualTo("operandText")
			assertThat(nonPositiveBanText.message).isEqualTo("禁用限制 runtime-non-positive-ban-text 操作数必须大于 0: 0")
		}

		withTemporaryFormatRestriction(
			code = "runtime-conflicting-select-count",
			restrictionType = "TEAM",
			restrictionOperator = "SELECT_COUNT",
			operandText = null,
			operandNumber = 5,
		) {
			val conflictingSelectCount = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}
			assertThat(conflictingSelectCount.field).isEqualTo("operandNumber")
			assertThat(conflictingSelectCount.message).isEqualTo("队伍出战人数不能配置多个不同操作数: 4, 5")
		}
	}

	@Test
	fun `runtime snapshot rejects unsupported enabled format rules`() {
		withTemporaryFormatClause("runtime-unknown-format-clause") {
			val unknownClause = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}
			assertThat(unknownClause.field).isEqualTo("clauseCode")
			assertThat(unknownClause.message).isEqualTo("不支持的赛制条款: runtime-unknown-format-clause")
		}

		withTemporaryFormatRestriction(
			code = "runtime-unknown-format-restriction",
			restrictionType = "TEAM",
			restrictionOperator = "UNKNOWN",
			operandText = null,
			operandNumber = 1,
		) {
			val unknownRestriction = assertThrows<ApiException> {
				service.getByFormatCode("official-double")
			}
			assertThat(unknownRestriction.field).isEqualTo("restrictionOperator")
			assertThat(unknownRestriction.message)
				.isEqualTo("不支持的赛制限制: runtime-unknown-format-restriction (TEAM/UNKNOWN)")
		}
	}

	@Test
	fun `skill slot assembly includes explicit battle rule effects`() {
		val slots = service.skillSlotsBySkillIds(
			listOf(
				2, 3, 5, 7, 12, 14, 15, 20, 23, 28, 32, 36, 37, 38, 39, 40, 45, 47, 49, 50, 57, 63, 69,
				67, 71, 74, 76, 77, 78, 79, 80, 81, 82, 83, 85, 87, 90, 92, 94, 95, 101, 103, 105, 113,
				115, 129, 138, 147, 157, 162, 163, 164, 170, 184, 189, 191, 199, 200, 235, 240, 252, 259, 261, 263, 265, 269,
				283, 305, 311, 319, 329, 344, 347, 349, 358, 360, 362, 366, 386, 390, 400, 427, 433, 435, 446, 447, 456, 457,
				464, 474, 475, 484, 486, 500, 504, 505, 506, 512, 515, 526, 535, 564, 568, 570, 577, 580, 604, 611, 659, 664, 666, 668, 681, 682, 685, 694, 717, 733, 803, 804, 805, 819, 875, 877,
				883, 892, 895,
			),
		)
			.associateBy { it.skillId }

		assertThat(slots.getValue(2).criticalHitStage).isEqualTo(1)
		val doubleSlap = slots.getValue(3)
		assertThat(doubleSlap.minHits).isEqualTo(2)
		assertThat(doubleSlap.maxHits).isEqualTo(5)
		assertThat(slots.getValue(37).targetScope).isEqualTo(BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT)
		assertThat(slots.getValue(37).lockMoveTurnsMin).isEqualTo(2)
		assertThat(slots.getValue(37).lockMoveTurnsMax).isEqualTo(3)
		assertThat(slots.getValue(37).confusesUserAfterLock).isTrue()
		assertThat(slots.getValue(80).targetScope).isEqualTo(BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT)
		assertThat(slots.getValue(80).lockMoveTurnsMin).isEqualTo(2)
		assertThat(slots.getValue(80).lockMoveTurnsMax).isEqualTo(3)
		assertThat(slots.getValue(80).confusesUserAfterLock).isTrue()
		assertThat(slots.getValue(170).locksAccuracyOnTarget).isTrue()
		assertThat(slots.getValue(199).locksAccuracyOnTarget).isTrue()
		assertThat(slots.getValue(200).targetScope).isEqualTo(BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT)
		assertThat(slots.getValue(200).lockMoveTurnsMin).isEqualTo(2)
		assertThat(slots.getValue(200).lockMoveTurnsMax).isEqualTo(3)
		assertThat(slots.getValue(200).confusesUserAfterLock).isTrue()
		assertThat(slots.getValue(57).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		assertThat(slots.getValue(74).targetScope).isEqualTo(BattleSkillTargetScope.SELF)
		assertThat(slots.getValue(129).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(slots.getValue(7).statusApplications.single().status).isEqualTo(BattleMajorStatus.BURN)
		assertThat(slots.getValue(7).statusApplications.single().chancePercent).isEqualTo(10)
		assertThat(slots.getValue(47).statusApplications.single().status).isEqualTo(BattleMajorStatus.SLEEP)
		assertThat(slots.getValue(47).statusApplications.single().chancePercent).isEqualTo(100)
		assertThat(slots.getValue(40).statusApplications.single().status).isEqualTo(BattleMajorStatus.POISON)
		assertThat(slots.getValue(40).statusApplications.single().chancePercent).isEqualTo(30)
		assertThat(slots.getValue(92).statusApplications.single().status).isEqualTo(BattleMajorStatus.BAD_POISON)
		assertThat(slots.getValue(92).statusApplications.single().chancePercent).isEqualTo(100)
		assertThat(slots.getValue(305).statusApplications.single().status).isEqualTo(BattleMajorStatus.BAD_POISON)
		assertThat(slots.getValue(305).statusApplications.single().chancePercent).isEqualTo(50)
		assertThat(slots.getValue(23).volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.FLINCH)
		assertThat(slots.getValue(23).volatileStatusApplications.single().chancePercent).isEqualTo(30)
		assertThat(slots.getValue(157).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(slots.getValue(157).volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.FLINCH)
		assertThat(slots.getValue(157).volatileStatusApplications.single().chancePercent).isEqualTo(30)
		assertThat(slots.getValue(252).volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.FLINCH)
		assertThat(slots.getValue(252).volatileStatusApplications.single().chancePercent).isEqualTo(100)
		assertThat(slots.getValue(344).statusApplications.single().status).isEqualTo(BattleMajorStatus.PARALYSIS)
		assertThat(slots.getValue(344).statusApplications.single().chancePercent).isEqualTo(10)
		assertThat(slots.getValue(435).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		assertThat(slots.getValue(435).statusApplications.single().status).isEqualTo(BattleMajorStatus.PARALYSIS)
		assertThat(slots.getValue(435).statusApplications.single().chancePercent).isEqualTo(30)
		assertThat(slots.getValue(464).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(slots.getValue(464).statusApplications.single().status).isEqualTo(BattleMajorStatus.SLEEP)
		assertThat(slots.getValue(464).statusApplications.single().chancePercent).isEqualTo(100)
		assertThat(slots.getValue(28).statStageEffects.single().stat).isEqualTo(BattleStat.ACCURACY)
		assertThat(slots.getValue(28).statStageEffects.single().target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(slots.getValue(28).statStageEffects.single().stageDelta).isEqualTo(-1)
		assertThat(slots.getValue(28).statStageEffects.single().chancePercent).isEqualTo(100)
		assertThat(slots.getValue(74).statStageEffects)
			.anySatisfy {
				assertThat(it.target).isEqualTo(BattleEffectTarget.USER)
				assertThat(it.stat).isEqualTo(BattleStat.ATTACK)
				assertThat(it.stageDelta).isEqualTo(1)
				assertThat(it.chancePercent).isEqualTo(100)
			}
		assertThat(slots.getValue(74).statStageEffects)
			.anySatisfy {
				assertThat(it.target).isEqualTo(BattleEffectTarget.USER)
				assertThat(it.stat).isEqualTo(BattleStat.SPECIAL_ATTACK)
				assertThat(it.stageDelta).isEqualTo(1)
				assertThat(it.chancePercent).isEqualTo(100)
			}
		assertThat(slots.getValue(81).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(slots.getValue(81).statStageEffects.single().stat).isEqualTo(BattleStat.SPEED)
		assertThat(slots.getValue(81).statStageEffects.single().target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(slots.getValue(81).statStageEffects.single().stageDelta).isEqualTo(-2)
		assertThat(slots.getValue(81).statStageEffects.single().chancePercent).isEqualTo(100)
		assertThat(slots.getValue(189).statStageEffects.single().stat).isEqualTo(BattleStat.ACCURACY)
		assertThat(slots.getValue(189).statStageEffects.single().chancePercent).isEqualTo(100)

		val growl = slots.getValue(45)
		assertThat(growl.targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(growl.statStageEffects)
			.anySatisfy {
				assertThat(it.target).isEqualTo(BattleEffectTarget.TARGET)
				assertThat(it.stat).isEqualTo(BattleStat.ATTACK)
				assertThat(it.stageDelta).isEqualTo(-1)
				assertThat(it.chancePercent).isEqualTo(100)
			}

		val tailWhip = slots.getValue(39)
		assertThat(tailWhip.targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(tailWhip.statStageEffects)
			.anySatisfy {
				assertThat(it.target).isEqualTo(BattleEffectTarget.TARGET)
				assertThat(it.stat).isEqualTo(BattleStat.DEFENSE)
				assertThat(it.stageDelta).isEqualTo(-1)
			}

		val absorb = slots.getValue(71)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.DrainDamage>()
			.single()
		assertThat(absorb.numerator).isEqualTo(1)
		assertThat(absorb.denominator).isEqualTo(2)

		val recoil = slots.getValue(38)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.RecoilByDamageDealt>()
			.single()
		assertThat(recoil.numerator).isEqualTo(1)
		assertThat(recoil.denominator).isEqualTo(3)

		val quarterRecoil = slots.getValue(36)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.RecoilByDamageDealt>()
			.single()
		assertThat(quarterRecoil.numerator).isEqualTo(1)
		assertThat(quarterRecoil.denominator).isEqualTo(4)

		val halfRecoil = slots.getValue(457)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.RecoilByDamageDealt>()
			.single()
		assertThat(halfRecoil.numerator).isEqualTo(1)
		assertThat(halfRecoil.denominator).isEqualTo(2)

		val fullDrain = slots.getValue(733)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.DrainDamage>()
			.single()
		assertThat(fullDrain.numerator).isEqualTo(1)
		assertThat(fullDrain.denominator).isEqualTo(1)

		val derivedSelfHeal = slots.getValue(456)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.SelfHealMaxHpFraction>()
			.single()
		assertThat(derivedSelfHeal.numerator).isEqualTo(1)
		assertThat(derivedSelfHeal.denominator).isEqualTo(2)

		val strengthSap = slots.getValue(668)
		assertThat(strengthSap.damageClass).isEqualTo(BattleDamageClass.STATUS)
		assertThat(strengthSap.hpEffects).containsExactly(BattleSkillHpEffect.SelfHealByTargetCurrentAttack)
		val strengthSapStatStage = strengthSap.statStageEffects.single()
		assertThat(strengthSapStatStage.target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(strengthSapStatStage.stat).isEqualTo(BattleStat.ATTACK)
		assertThat(strengthSapStatStage.stageDelta).isEqualTo(-1)
		assertThat(strengthSapStatStage.chancePercent).isEqualTo(100)

		val purify = slots.getValue(685)
		assertThat(purify.damageClass).isEqualTo(BattleDamageClass.STATUS)
		val purifyHpEffect = purify.hpEffects
			.filterIsInstance<BattleSkillHpEffect.SelfHealAfterTargetMajorStatusCure>()
			.single()
		assertThat(purifyHpEffect.numerator).isEqualTo(1)
		assertThat(purifyHpEffect.denominator).isEqualTo(2)

		val facade = slots.getValue(263)
		assertThat(facade.ignoresUserBurnAttackReduction).isTrue()
		val facadePower = facade.conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.UserMajorStatus>()
			.single()
		assertThat(facadePower.statuses)
			.containsExactlyInAnyOrder(
				BattleMajorStatus.BURN,
				BattleMajorStatus.PARALYSIS,
				BattleMajorStatus.POISON,
				BattleMajorStatus.BAD_POISON,
			)
		assertThat(facadePower.multiplier).isEqualTo(2.0)

		val brinePower = slots.getValue(362).conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.TargetCurrentHpAtMostFraction>()
			.single()
		assertThat(brinePower.numerator).isEqualTo(1)
		assertThat(brinePower.denominator).isEqualTo(2)
		assertThat(brinePower.multiplier).isEqualTo(2.0)

		val venoshockPower = slots.getValue(474).conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.TargetMajorStatus>()
			.single()
		assertThat(venoshockPower.statuses).containsExactlyInAnyOrder(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON)
		assertThat(venoshockPower.multiplier).isEqualTo(2.0)

		val hexPower = slots.getValue(506).conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.TargetMajorStatus>()
			.single()
		assertThat(hexPower.statuses).containsExactlyInAnyOrderElementsOf(BattleMajorStatus.entries.toSet())
		assertThat(hexPower.multiplier).isEqualTo(2.0)

		val acrobaticsPower = slots.getValue(512).conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.UserHasNoHeldItem>()
			.single()
		assertThat(acrobaticsPower.multiplier).isEqualTo(2.0)

		val risingVoltagePower = slots.getValue(804).conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.TargetGroundedTerrain>()
			.single()
		assertThat(risingVoltagePower.terrain).isEqualTo(BattleTerrain.ELECTRIC)
		assertThat(risingVoltagePower.multiplier).isEqualTo(2.0)

		val psybladePower = slots.getValue(875).conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.ActiveTerrain>()
			.single()
		assertThat(psybladePower.terrain).isEqualTo(BattleTerrain.ELECTRIC)
		assertThat(psybladePower.multiplier).isEqualTo(1.5)

		assertThat(slots.getValue(803).groundedTerrainPriorityBoosts).containsExactlyEntriesOf(
			mapOf(BattleTerrain.GRASSY to 1),
		)

		val autotomize = slots.getValue(475)
		assertThat(autotomize.damageClass).isEqualTo(BattleDamageClass.STATUS)
		assertThat(autotomize.power).isNull()
		assertThat(autotomize.targetScope).isEqualTo(BattleSkillTargetScope.SELF)
		assertThat(autotomize.statStageEffects.single()).isEqualTo(
			BattleStatStageEffect(
				target = BattleEffectTarget.USER,
				stat = BattleStat.SPEED,
				stageDelta = 2,
				chancePercent = 100,
			),
		)
		assertThat(autotomize.weightEffects).containsExactly(
			BattleSkillWeightEffect(
				target = BattleEffectTarget.USER,
				reduction = 1000,
				minimumWeight = 1,
				requiredChangedStat = BattleStat.SPEED,
			),
		)

		val smellingSalts = slots.getValue(265)
		val smellingSaltsPower = smellingSalts.conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.TargetMajorStatus>()
			.single()
		assertThat(smellingSaltsPower.statuses).containsExactlyInAnyOrder(BattleMajorStatus.PARALYSIS)
		assertThat(smellingSaltsPower.multiplier).isEqualTo(2.0)
		assertThat(smellingSalts.postDamageStatusCures.single().statuses)
			.containsExactlyInAnyOrder(BattleMajorStatus.PARALYSIS)

		val wakeUpSlap = slots.getValue(358)
		val wakeUpSlapPower = wakeUpSlap.conditionalPowerMultipliers
			.filterIsInstance<BattleSkillPowerMultiplier.TargetMajorStatus>()
			.single()
		assertThat(wakeUpSlapPower.statuses).containsExactlyInAnyOrder(BattleMajorStatus.SLEEP)
		assertThat(wakeUpSlapPower.multiplier).isEqualTo(2.0)
		assertThat(wakeUpSlap.postDamageStatusCures.single().statuses)
			.containsExactlyInAnyOrder(BattleMajorStatus.SLEEP)

		val sparklingAria = slots.getValue(664)
		assertThat(sparklingAria.conditionalPowerMultipliers).isEmpty()
		assertThat(sparklingAria.postDamageStatusCures.single().statuses)
			.containsExactlyInAnyOrder(BattleMajorStatus.BURN)

		val storedPowerDynamicPower = slots.getValue(500).dynamicPower
		assertThat(storedPowerDynamicPower)
			.isEqualTo(
				BattleSkillDynamicPower.PositiveStatStageSum(
					source = BattleEffectTarget.USER,
					basePower = 20,
					powerPerPositiveStage = 20,
				),
			)
		assertThat(slots.getValue(681).dynamicPower).isEqualTo(storedPowerDynamicPower)
		assertThat(slots.getValue(386).dynamicPower)
			.isEqualTo(
				BattleSkillDynamicPower.PositiveStatStageSum(
					source = BattleEffectTarget.TARGET,
					basePower = 60,
					powerPerPositiveStage = 20,
					maxPower = 200,
				),
			)

		assertThat(slots.getValue(486).dynamicPower)
			.isEqualTo(
				BattleSkillDynamicPower.UserSpeedRatioThresholds(
					thresholds = listOf(
						BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 4, power = 150),
						BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 3, power = 120),
						BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 2, power = 80),
						BattleSkillDynamicPower.SpeedPowerThreshold(minimumRatio = 1, power = 60),
					),
					fallbackPower = 40,
				),
			)
		assertThat(slots.getValue(360).dynamicPower)
			.isEqualTo(
				BattleSkillDynamicPower.TargetToUserSpeedRatio(
					multiplier = 25,
					additivePower = 1,
					maxPower = 150,
				),
			)

		val targetWeightPower = BattleSkillDynamicPower.TargetWeightThresholds(
			thresholds = listOf(
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 100, power = 20),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 250, power = 40),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 500, power = 60),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 1000, power = 80),
				BattleSkillDynamicPower.WeightPowerThreshold(maxWeightInclusive = 2000, power = 100),
			),
			fallbackPower = 120,
		)
		assertThat(slots.getValue(67).dynamicPower).isEqualTo(targetWeightPower)
		assertThat(slots.getValue(447).dynamicPower).isEqualTo(targetWeightPower)
		val userTargetWeightPower = BattleSkillDynamicPower.UserTargetWeightRatioThresholds(
			thresholds = listOf(
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 5, power = 120),
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 4, power = 100),
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 3, power = 80),
				BattleSkillDynamicPower.WeightRatioPowerThreshold(minimumUserToTargetRatio = 2, power = 60),
			),
			fallbackPower = 40,
		)
		assertThat(slots.getValue(484).dynamicPower).isEqualTo(userTargetWeightPower)
		assertThat(slots.getValue(535).dynamicPower).isEqualTo(userTargetWeightPower)

		assertThat(slots.getValue(682).removesUserElementAfterDamage).isTrue()
		assertThat(slots.getValue(892).removesUserElementAfterDamage).isTrue()

		assertThat(slots.getValue(49).fixedDamage).isEqualTo(BattleFixedDamage.FixedAmount(20))
		assertThat(slots.getValue(82).fixedDamage).isEqualTo(BattleFixedDamage.FixedAmount(40))
		assertThat(slots.getValue(69).fixedDamage).isEqualTo(BattleFixedDamage.UserLevel)
		assertThat(slots.getValue(101).fixedDamage).isEqualTo(BattleFixedDamage.UserLevel)
		assertThat(slots.getValue(162).proportionalDamage)
			.isEqualTo(BattleProportionalDamage.TargetCurrentHpFraction(numerator = 1, denominator = 2))
		assertThat(slots.getValue(717).proportionalDamage)
			.isEqualTo(BattleProportionalDamage.TargetCurrentHpFraction(numerator = 1, denominator = 2))
		assertThat(slots.getValue(877).proportionalDamage)
			.isEqualTo(BattleProportionalDamage.TargetCurrentHpFraction(numerator = 1, denominator = 2))
		assertThat(slots.getValue(283).hpDerivedDamage)
			.isEqualTo(BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp)
		assertThat(slots.getValue(515).hpDerivedDamage)
			.isEqualTo(BattleHpDerivedDamage.UserCurrentHpAndUserFaints)
		assertThat(slots.getValue(12).oneHitKnockOut).isEqualTo(BattleOneHitKnockOut())
		assertThat(slots.getValue(32).oneHitKnockOut).isEqualTo(BattleOneHitKnockOut())
		assertThat(slots.getValue(90).oneHitKnockOut).isEqualTo(BattleOneHitKnockOut())
		assertThat(slots.getValue(329).oneHitKnockOut)
			.isEqualTo(
				BattleOneHitKnockOut(
					baseAccuracyPercent = 20,
					sameElementUserBaseAccuracyPercent = 30,
					blocksSameElementTarget = true,
				),
			)

		assertThat(slots.getValue(63).rechargesAfterUse).isTrue()

		val solarBeam = slots.getValue(76)
		assertThat(solarBeam.chargesBeforeUse).isTrue()
		assertThat(solarBeam.chargeSkippedByWeathers).containsExactly(BattleWeather.SUN)
		assertThat(solarBeam.powerMultipliersByWeather[BattleWeather.RAIN]).isEqualTo(0.5)
		assertThat(solarBeam.powerMultipliersByWeather[BattleWeather.SANDSTORM]).isEqualTo(0.5)
		assertThat(solarBeam.powerMultipliersByWeather[BattleWeather.SNOW]).isEqualTo(0.5)

		val thunderShock = slots.getValue(85)
		assertThat(thunderShock.statusApplications)
			.anySatisfy {
				assertThat(it.status).isEqualTo(BattleMajorStatus.PARALYSIS)
				assertThat(it.chancePercent).isEqualTo(10)
			}

		val poisonPowder = slots.getValue(77)
		assertThat(poisonPowder.powderBased).isTrue()
		val poisonPowderStatus = poisonPowder.statusApplications.single()
		assertThat(poisonPowderStatus.status).isEqualTo(BattleMajorStatus.POISON)
		assertThat(poisonPowderStatus.target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(poisonPowderStatus.chancePercent).isEqualTo(100)

		val stunSpore = slots.getValue(78)
		assertThat(stunSpore.powderBased).isTrue()
		assertThat(stunSpore.statusApplications.single().status).isEqualTo(BattleMajorStatus.PARALYSIS)

		val sleepPowder = slots.getValue(79)
		assertThat(sleepPowder.powderBased).isTrue()
		assertThat(sleepPowder.statusApplications.single().status).isEqualTo(BattleMajorStatus.SLEEP)

		val hypnosis = slots.getValue(95)
		assertThat(hypnosis.powderBased).isFalse()
		assertThat(hypnosis.statusApplications.single().status).isEqualTo(BattleMajorStatus.SLEEP)

		val spore = slots.getValue(147)
		assertThat(spore.powderBased).isTrue()
		assertThat(spore.statusApplications.single().status).isEqualTo(BattleMajorStatus.SLEEP)

		val willOWisp = slots.getValue(261)
		assertThat(willOWisp.powderBased).isFalse()
		assertThat(willOWisp.statusApplications.single().status).isEqualTo(BattleMajorStatus.BURN)

		val taunt = slots.getValue(269)
		assertThat(taunt.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.TAUNT)
		assertThat(taunt.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)

		val disable = slots.getValue(50)
		assertThat(disable.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.DISABLE)
		assertThat(disable.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)

		val torment = slots.getValue(259)
		assertThat(torment.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.TORMENT)
		assertThat(torment.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)

		val bind = slots.getValue(20)
		assertThat(bind.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.BINDING)
		assertThat(bind.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)

		val fireSpin = slots.getValue(83)
		assertThat(fireSpin.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.BINDING)
		assertThat(fireSpin.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(fireSpin.volatileStatusApplications.single().chancePercent).isEqualTo(100)

		val infestation = slots.getValue(611)
		assertThat(infestation.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.BINDING)
		assertThat(infestation.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(infestation.volatileStatusApplications.single().chancePercent).isEqualTo(100)

		val thunderCage = slots.getValue(819)
		assertThat(thunderCage.volatileStatusApplications.single().status).isEqualTo(BattleVolatileStatus.BINDING)
		assertThat(thunderCage.volatileStatusApplications.single().target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(thunderCage.volatileStatusApplications.single().chancePercent).isEqualTo(100)

		val thunder = slots.getValue(87)
		assertThat(thunder.accuracyOverridesByWeather).containsKey(BattleWeather.RAIN)
		assertThat(thunder.accuracyOverridesByWeather[BattleWeather.RAIN]).isNull()
		assertThat(thunder.accuracyOverridesByWeather[BattleWeather.SUN]).isEqualTo(50)

		val psychic = slots.getValue(94)
		assertThat(psychic.statStageEffects)
			.anySatisfy {
				assertThat(it.stat).isEqualTo(BattleStat.SPECIAL_DEFENSE)
				assertThat(it.stageDelta).isEqualTo(-1)
				assertThat(it.chancePercent).isEqualTo(10)
			}

		val swordsDance = slots.getValue(14)
			.statStageEffects
			.single()
		assertThat(swordsDance.target).isEqualTo(BattleEffectTarget.USER)
		assertThat(swordsDance.stat).isEqualTo(BattleStat.ATTACK)
		assertThat(swordsDance.stageDelta).isEqualTo(2)

		val calmMindStats = slots.getValue(347)
			.statStageEffects
			.associate { it.stat to it.stageDelta }
		assertThat(calmMindStats).containsEntry(BattleStat.SPECIAL_ATTACK, 1)
		assertThat(calmMindStats).containsEntry(BattleStat.SPECIAL_DEFENSE, 1)

		val dragonDanceStats = slots.getValue(349)
			.statStageEffects
			.associate { it.stat to it.stageDelta }
		assertThat(dragonDanceStats).containsEntry(BattleStat.ATTACK, 1)
		assertThat(dragonDanceStats).containsEntry(BattleStat.SPEED, 1)

		val shellSmashStats = slots.getValue(504)
			.statStageEffects
			.associate { it.stat to it.stageDelta }
		assertThat(shellSmashStats).containsEntry(BattleStat.DEFENSE, -1)
		assertThat(shellSmashStats).containsEntry(BattleStat.SPECIAL_DEFENSE, -1)
		assertThat(shellSmashStats).containsEntry(BattleStat.ATTACK, 2)
		assertThat(shellSmashStats).containsEntry(BattleStat.SPECIAL_ATTACK, 2)
		assertThat(shellSmashStats).containsEntry(BattleStat.SPEED, 2)

		val workUpStats = slots.getValue(526)
			.statStageEffects
			.associate { it.stat to it.stageDelta }
		assertThat(workUpStats).containsEntry(BattleStat.ATTACK, 1)
		assertThat(workUpStats).containsEntry(BattleStat.SPECIAL_ATTACK, 1)

		val screech = slots.getValue(103)
			.statStageEffects
			.single()
		assertThat(screech.target).isEqualTo(BattleEffectTarget.TARGET)
		assertThat(screech.stat).isEqualTo(BattleStat.DEFENSE)
		assertThat(screech.stageDelta).isEqualTo(-2)

		val scaryFace = slots.getValue(184)
			.statStageEffects
			.single()
		assertThat(scaryFace.stat).isEqualTo(BattleStat.SPEED)
		assertThat(scaryFace.stageDelta).isEqualTo(-2)

		val metalSound = slots.getValue(319)
			.statStageEffects
			.single()
		assertThat(slots.getValue(319).soundBased).isTrue()
		assertThat(metalSound.stat).isEqualTo(BattleStat.SPECIAL_DEFENSE)
		assertThat(metalSound.stageDelta).isEqualTo(-2)

		assertThat(slots.getValue(5).punchBased).isTrue()
		assertThat(slots.getValue(5).slicingBased).isFalse()
		assertThat(slots.getValue(15).slicingBased).isTrue()
		assertThat(slots.getValue(163).slicingBased).isTrue()
		assertThat(slots.getValue(163).criticalHitStage).isEqualTo(1)
		assertThat(slots.getValue(400).slicingBased).isTrue()
		assertThat(slots.getValue(400).criticalHitStage).isEqualTo(1)
		assertThat(slots.getValue(427).slicingBased).isTrue()
		assertThat(slots.getValue(427).makesContact).isFalse()
		assertThat(slots.getValue(895).slicingBased).isTrue()
		assertThat(slots.getValue(895).makesContact).isFalse()

		val nobleRoarStats = slots.getValue(568)
			.statStageEffects
			.associate { it.stat to it.stageDelta }
		assertThat(slots.getValue(568).targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS)
		assertThat(nobleRoarStats).containsEntry(BattleStat.ATTACK, -1)
		assertThat(nobleRoarStats).containsEntry(BattleStat.SPECIAL_ATTACK, -1)

		val recover = slots.getValue(105)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.SelfHealMaxHpFraction>()
			.single()
		assertThat(recover.numerator).isEqualTo(1)
		assertThat(recover.denominator).isEqualTo(2)

		val substitute = slots.getValue(164)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.CreateSubstitute>()
			.single()
		assertThat(substitute.numerator).isEqualTo(1)
		assertThat(substitute.denominator).isEqualTo(4)

		val weatherHealing = slots.getValue(235)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.SelfHealMaxHpByWeather>()
			.single()
		assertThat(weatherHealing.defaultFraction.numerator).isEqualTo(1)
		assertThat(weatherHealing.defaultFraction.denominator).isEqualTo(2)
		assertThat(weatherHealing.weatherFractions.getValue(BattleWeather.SUN).numerator).isEqualTo(2)
		assertThat(weatherHealing.weatherFractions.getValue(BattleWeather.SUN).denominator).isEqualTo(3)
		assertThat(weatherHealing.weatherFractions.getValue(BattleWeather.RAIN).numerator).isEqualTo(1)
		assertThat(weatherHealing.weatherFractions.getValue(BattleWeather.RAIN).denominator).isEqualTo(4)

		val targetHealing = slots.getValue(505)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.TargetHealMaxHpFraction>()
			.single()
		assertThat(targetHealing.numerator).isEqualTo(1)
		assertThat(targetHealing.denominator).isEqualTo(2)

		val terrainTargetHealing = slots.getValue(666)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.TargetHealMaxHpByTerrain>()
			.single()
		assertThat(terrainTargetHealing.defaultFraction.numerator).isEqualTo(1)
		assertThat(terrainTargetHealing.defaultFraction.denominator).isEqualTo(2)
		assertThat(terrainTargetHealing.terrainFractions.getValue(BattleTerrain.GRASSY).numerator).isEqualTo(2)
		assertThat(terrainTargetHealing.terrainFractions.getValue(BattleTerrain.GRASSY).denominator).isEqualTo(3)

		val sandstormHealing = slots.getValue(659)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.SelfHealMaxHpByWeather>()
			.single()
		assertThat(sandstormHealing.defaultFraction.numerator).isEqualTo(1)
		assertThat(sandstormHealing.defaultFraction.denominator).isEqualTo(2)
		assertThat(sandstormHealing.weatherFractions.getValue(BattleWeather.SANDSTORM).numerator).isEqualTo(2)
		assertThat(sandstormHealing.weatherFractions.getValue(BattleWeather.SANDSTORM).denominator).isEqualTo(3)

		val rainDance = slots.getValue(240)
			.environmentEffects
			.filterIsInstance<BattleSkillEnvironmentEffect.SetWeather>()
			.single()
		assertThat(rainDance.weather).isEqualTo(BattleWeather.RAIN)
		assertThat(rainDance.turnsRemaining).isEqualTo(5)

		val weatherBall = slots.getValue(311)
		assertThat(weatherBall.powerMultipliersByWeather[BattleWeather.SUN]).isEqualTo(2.0)
		assertThat(weatherBall.powerMultipliersByWeather[BattleWeather.RAIN]).isEqualTo(2.0)
		assertThat(weatherBall.elementOverridesByWeather[BattleWeather.SUN]).isEqualTo(10)
		assertThat(weatherBall.elementOverridesByWeather[BattleWeather.RAIN]).isEqualTo(11)
		assertThat(weatherBall.elementOverridesByWeather[BattleWeather.SANDSTORM]).isEqualTo(6)
		assertThat(weatherBall.elementOverridesByWeather[BattleWeather.SNOW]).isEqualTo(15)

		val terrainPulse = slots.getValue(805)
		assertThat(terrainPulse.groundedPowerMultipliersByTerrain).containsExactlyEntriesOf(
			mapOf(
				BattleTerrain.ELECTRIC to 2.0,
				BattleTerrain.GRASSY to 2.0,
				BattleTerrain.MISTY to 2.0,
				BattleTerrain.PSYCHIC to 2.0,
			),
		)
		assertThat(terrainPulse.elementOverridesByTerrain).containsExactlyEntriesOf(
			mapOf(
				BattleTerrain.ELECTRIC to 13,
				BattleTerrain.GRASSY to 12,
				BattleTerrain.MISTY to 18,
				BattleTerrain.PSYCHIC to 14,
			),
		)

		val lightScreen = slots.getValue(113).sideConditionApplications.single()
		assertThat(lightScreen.targetSide).isEqualTo(BattleSideConditionTarget.USER_SIDE)
		assertThat(lightScreen.damageReduction.kind).isEqualTo(BattleSideDamageReductionKind.SPECIAL)
		assertThat(lightScreen.damageReduction.turnsRemaining).isEqualTo(5)
		assertThat(lightScreen.requiredWeather).isNull()

		val reflect = slots.getValue(115).sideConditionApplications.single()
		assertThat(reflect.targetSide).isEqualTo(BattleSideConditionTarget.USER_SIDE)
		assertThat(reflect.damageReduction.kind).isEqualTo(BattleSideDamageReductionKind.PHYSICAL)
		assertThat(reflect.damageReduction.turnsRemaining).isEqualTo(5)

		val auroraVeil = slots.getValue(694).sideConditionApplications.single()
		assertThat(auroraVeil.targetSide).isEqualTo(BattleSideConditionTarget.USER_SIDE)
		assertThat(auroraVeil.damageReduction.kind).isEqualTo(BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE)
		assertThat(auroraVeil.damageReduction.turnsRemaining).isEqualTo(5)
		assertThat(auroraVeil.requiredWeather).isEqualTo(BattleWeather.SNOW)

		val tailwind = slots.getValue(366).sideSpeedModifierApplications.single()
		assertThat(tailwind.targetSide).isEqualTo(BattleSideConditionTarget.USER_SIDE)
		assertThat(tailwind.speedModifier.kind).isEqualTo(BattleSideSpeedModifierKind.TAILWIND)
		assertThat(tailwind.speedModifier.multiplier).isEqualTo(2.0)
		assertThat(tailwind.speedModifier.turnsRemaining).isEqualTo(4)

		val trickRoom = slots.getValue(433).fieldSpeedOrderApplications.single()
		assertThat(trickRoom.speedOrderEffect.kind).isEqualTo(BattleFieldSpeedOrderKind.TRICK_ROOM)
		assertThat(trickRoom.speedOrderEffect.turnsRemaining).isEqualTo(5)

		val spikes = slots.getValue(191).sideEntryHazardApplications.single()
		assertThat(spikes.targetSide).isEqualTo(BattleSideConditionTarget.TARGET_SIDE)
		assertThat(spikes.hazard.kind).isEqualTo(BattleSideEntryHazardKind.SPIKES)
		assertThat(spikes.hazard.maxLayers).isEqualTo(3)

		val toxicSpikes = slots.getValue(390).sideEntryHazardApplications.single()
		assertThat(toxicSpikes.targetSide).isEqualTo(BattleSideConditionTarget.TARGET_SIDE)
		assertThat(toxicSpikes.hazard.kind).isEqualTo(BattleSideEntryHazardKind.TOXIC_SPIKES)
		assertThat(toxicSpikes.hazard.maxLayers).isEqualTo(2)

		val stealthRock = slots.getValue(446).sideEntryHazardApplications.single()
		assertThat(stealthRock.targetSide).isEqualTo(BattleSideConditionTarget.TARGET_SIDE)
		assertThat(stealthRock.hazard.kind).isEqualTo(BattleSideEntryHazardKind.STEALTH_ROCK)
		assertThat(stealthRock.hazard.maxLayers).isEqualTo(1)

		val stickyWeb = slots.getValue(564).sideEntryHazardApplications.single()
		assertThat(stickyWeb.targetSide).isEqualTo(BattleSideConditionTarget.TARGET_SIDE)
		assertThat(stickyWeb.hazard.kind).isEqualTo(BattleSideEntryHazardKind.STICKY_WEB)
		assertThat(stickyWeb.hazard.maxLayers).isEqualTo(1)

		val parabolicCharge = slots.getValue(570)
		assertThat(parabolicCharge.targetScope).isEqualTo(BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS)
		val highDrain = slots.getValue(577)
			.hpEffects
			.filterIsInstance<BattleSkillHpEffect.DrainDamage>()
			.single()
		assertThat(highDrain.numerator).isEqualTo(3)
		assertThat(highDrain.denominator).isEqualTo(4)

		val snowscape = slots.getValue(883)
			.environmentEffects
			.filterIsInstance<BattleSkillEnvironmentEffect.SetWeather>()
			.single()
		assertThat(snowscape.weather).isEqualTo(BattleWeather.SNOW)
		assertThat(snowscape.turnsRemaining).isEqualTo(5)

		val grassyTerrain = slots.getValue(580)
			.environmentEffects
			.filterIsInstance<BattleSkillEnvironmentEffect.SetTerrain>()
			.single()
		assertThat(grassyTerrain.terrain).isEqualTo(BattleTerrain.GRASSY)
		assertThat(grassyTerrain.turnsRemaining).isEqualTo(5)

		val electricTerrain = slots.getValue(604)
			.environmentEffects
			.filterIsInstance<BattleSkillEnvironmentEffect.SetTerrain>()
			.single()
		assertThat(electricTerrain.terrain).isEqualTo(BattleTerrain.ELECTRIC)
		assertThat(electricTerrain.turnsRemaining).isEqualTo(5)
	}

	/**
	 * 技能运行时装配的输入错误要在适配层被直接定位。
	 *
	 * 空列表和非正数 ID 属于请求形状错误；不存在的正数 ID 说明资料库没有对应基础技能；基础技能存在但没有
	 * 战斗规则则说明 Liquibase 种子或后台维护漏配。四类问题都不应该落到 battle-engine 里再表现为“技能槽缺失”，
	 * 否则准备校验和真实开战会得到不同错误口径。
	 */
	@Test
	fun `skill slot assembly rejects invalid and missing skill data`() {
		val emptySkillIds = assertThrows<ApiException> {
			service.skillSlotsBySkillIds(emptyList())
		}
		assertThat(emptySkillIds.field).isEqualTo("skillIds")
		assertThat(emptySkillIds.message).isEqualTo("skillIds 不能为空")

		val nonPositiveSkillId = assertThrows<ApiException> {
			service.skillSlotsBySkillIds(listOf(0))
		}
		assertThat(nonPositiveSkillId.field).isEqualTo("skillIds")
		assertThat(nonPositiveSkillId.message).isEqualTo("skillIds 只能包含正数 ID")

		val missingSkill = assertThrows<ApiException> {
			service.skillSlotsBySkillIds(listOf(999_999))
		}
		assertThat(missingSkill.field).isEqualTo("skillIds")
		assertThat(missingSkill.message).isEqualTo("技能不存在: 999999")

		withTemporarySkillWithoutRule { skillId ->
			val missingRule = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(missingRule.field).isEqualTo("skillIds")
			assertThat(missingRule.message).isEqualTo("技能缺少战斗规则: $skillId")
		}
	}

	@Test
	fun `creature runtime profile uses game data stats and elements`() {
		val profile = service.creatureRuntimeProfile(creatureId = 1, level = 50)

		assertThat(profile.maxHp).isEqualTo(120)
		assertThat(profile.attack).isEqualTo(69)
		assertThat(profile.defense).isEqualTo(69)
		assertThat(profile.specialAttack).isEqualTo(85)
		assertThat(profile.specialDefense).isEqualTo(85)
		assertThat(profile.speed).isEqualTo(65)
		assertThat(profile.weight).isEqualTo(69)
		assertThat(profile.elementIds).containsExactlyInAnyOrder(12L, 4L)
	}

	/**
	 * 验证参与方显式能力配置会进入初始状态装配，并且不会被同种类、同等级成员的画像缓存串用。
	 *
	 * 生产对战里最常见的真实差异并不是资料表中的基础能力，而是玩家配置的个体值、努力值和性格。同一个成员资料 ID
	 * 在同一场战斗中可能出现两次，但两者速度、攻击等最终能力完全不同；因此单请求缓存必须把能力配置纳入 key。
	 * 这里让双方都使用同一个资料 ID 和等级，但给出相反的性格与努力值，确保装配结果分别按现代能力公式计算。
	 */
	@Test
	fun `initial state assembly applies participant stat config without cache collision`() {
		val initialState = service.assembleInitialState(
			BattlePreparationValidationRequest(
				formatCode = "standard-single",
				sides = listOf(
					BattlePreparationSideRequest(
						sideId = "side-a",
						activeActorIds = listOf("a-1"),
						participants = listOf(
							participant(
								actorId = "a-1",
								creatureId = 1,
								level = 50,
								skillIds = listOf(1),
								individualValues = mapOf("speed" to 0),
								natureIncreasedStat = "attack",
								natureDecreasedStat = "speed",
							),
						),
					),
					BattlePreparationSideRequest(
						sideId = "side-b",
						activeActorIds = listOf("b-1"),
						participants = listOf(
							participant(
								actorId = "b-1",
								creatureId = 1,
								level = 50,
								skillIds = listOf(1),
								effortValues = mapOf("speed" to 252),
								natureIncreasedStat = "speed",
								natureDecreasedStat = "attack",
							),
						),
					),
				),
			),
		)

		val participants = initialState.sides
			.flatMap { it.participants }
			.associateBy { it.actorId }
		val slowPhysical = participants.getValue("a-1")
		val fastSpecial = participants.getValue("b-1")

		assertThat(slowPhysical.maxHp).isEqualTo(120)
		assertThat(slowPhysical.attack).isEqualTo(75)
		assertThat(slowPhysical.speed).isEqualTo(45)
		assertThat(fastSpecial.maxHp).isEqualTo(120)
		assertThat(fastSpecial.attack).isEqualTo(62)
		assertThat(fastSpecial.speed).isEqualTo(106)
	}

	/**
	 * 成员运行时画像必须保证属性和基础能力完整。
	 *
	 * 资料缺属性或缺基础能力时，引擎无法安全计算伤害、接地、属性免疫和速度排序；因此读取器需要在装配阶段抛出
	 * 结构化异常，而不是用 0 或 1 这类占位值继续往下跑。
	 */
	@Test
	fun `creature runtime profile rejects invalid level and missing creature data`() {
		val invalidCreatureId = assertThrows<ApiException> {
			service.creatureRuntimeProfile(creatureId = 0, level = 50)
		}
		assertThat(invalidCreatureId.field).isEqualTo("creatureId")
		assertThat(invalidCreatureId.message).isEqualTo("creatureId 必须大于 0")

		val invalidLevel = assertThrows<ApiException> {
			service.creatureRuntimeProfile(creatureId = 1, level = 101)
		}
		assertThat(invalidLevel.field).isEqualTo("level")
		assertThat(invalidLevel.message).isEqualTo("level 必须在 1 到 100 之间")

		val missingCreature = assertThrows<ApiException> {
			service.creatureRuntimeProfile(creatureId = 999_999, level = 50)
		}
		assertThat(missingCreature.status).isEqualTo(HttpStatus.NOT_FOUND)
		assertThat(missingCreature.field).isEqualTo("creatureId")
		assertThat(missingCreature.message).isEqualTo("成员属性资料不存在: 999999")
	}

	/**
	 * 验证个体值、努力值和性格在应用层边界就被拒绝。
	 *
	 * 这些字段会直接进入速度排序、伤害计算和 HP 上限；如果让非法值进入 battle-engine，后续表现会像规则错误而不是
	 * 输入错误，排查成本很高。测试覆盖单项范围、努力值总和、性格成对填写和性格不能影响 HP 四类最容易手填出错的
	 * 场景，确保所有入口复用同一套结构化 `ApiException`。
	 */
	@Test
	fun `preparation validation rejects invalid participant stat config before engine assembly`() {
		val invalidIndividualValue = assertThrows<ApiException> {
			service.validatePreparation(
				preparationRequest(
					participant(
						actorId = "a-1",
						creatureId = 1,
						level = 50,
						individualValues = mapOf("speed" to 32),
					),
				),
			)
		}
		assertThat(invalidIndividualValue.field).isEqualTo("individualValues")
		assertThat(invalidIndividualValue.message).isEqualTo("individualValues.speed 必须在 0 到 31 之间")

		val invalidEffortTotal = assertThrows<ApiException> {
			service.validatePreparation(
				preparationRequest(
					participant(
						actorId = "a-1",
						creatureId = 1,
						level = 50,
						effortValues = mapOf("attack" to 252, "defense" to 252, "speed" to 7),
					),
				),
			)
		}
		assertThat(invalidEffortTotal.field).isEqualTo("effortValues")
		assertThat(invalidEffortTotal.message).isEqualTo("effortValues 总和不能超过 510")

		val incompleteNature = assertThrows<ApiException> {
			service.validatePreparation(
				preparationRequest(
					participant(
						actorId = "a-1",
						creatureId = 1,
						level = 50,
						natureIncreasedStat = "speed",
					),
				),
			)
		}
		assertThat(incompleteNature.field).isEqualTo("natureIncreasedStat")
		assertThat(incompleteNature.message).isEqualTo("natureIncreasedStat 和 natureDecreasedStat 必须同时填写或同时留空")

		val hpNature = assertThrows<ApiException> {
			service.validatePreparation(
				preparationRequest(
					participant(
						actorId = "a-1",
						creatureId = 1,
						level = 50,
						natureIncreasedStat = "hp",
						natureDecreasedStat = "speed",
					),
				),
			)
		}
		assertThat(hpNature.field).isEqualTo("natureIncreasedStat")
		assertThat(hpNature.message).isEqualTo("natureIncreasedStat 只支持 attack、defense、special-attack、special-defense、speed")
	}

	@Test
	fun `ability and item rule assembly includes supported engine effects`() {
		val grassBoost = service.abilityEffectsByAbilityId(65)
			.filterIsInstance<BattleAbilityEffect.LowHpElementDamageBoost>()
			.single()
		assertThat(grassBoost.elementId).isEqualTo(12)
		assertThat(grassBoost.multiplier).isEqualTo(1.5)
		val bugBoost = service.abilityEffectsByAbilityId(68)
			.filterIsInstance<BattleAbilityEffect.LowHpElementDamageBoost>()
			.single()
		assertThat(bugBoost.elementId).isEqualTo(7)
		assertThat(bugBoost.multiplier).isEqualTo(1.5)
		assertThat(service.abilityEffectsByAbilityId(200))
			.containsExactly(BattleAbilityEffect.ElementSkillDamageBoost(setOf(9), multiplier = 1.5))
		assertThat(service.abilityEffectsByAbilityId(262))
			.containsExactly(BattleAbilityEffect.ElementSkillDamageBoost(setOf(13), multiplier = 1.3))
		assertThat(service.abilityEffectsByAbilityId(263))
			.containsExactly(BattleAbilityEffect.ElementSkillDamageBoost(setOf(16), multiplier = 1.5))
		assertThat(service.abilityEffectsByAbilityId(276))
			.containsExactly(BattleAbilityEffect.ElementSkillDamageBoost(setOf(6), multiplier = 1.5))
		val punchBoost = service.abilityEffectsByAbilityId(89)
			.filterIsInstance<BattleAbilityEffect.PunchBasedSkillDamageBoost>()
			.single()
		assertThat(punchBoost.multiplier).isEqualTo(1.2)
		val slicingBoost = service.abilityEffectsByAbilityId(292)
			.filterIsInstance<BattleAbilityEffect.SlicingBasedSkillDamageBoost>()
			.single()
		assertThat(slicingBoost.multiplier).isEqualTo(1.5)
		val contactBoost = service.abilityEffectsByAbilityId(181)
			.filterIsInstance<BattleAbilityEffect.ContactBasedSkillDamageBoost>()
			.single()
		assertThat(contactBoost.multiplier).isEqualTo(1.3)
		val sandForceEffects = service.abilityEffectsByAbilityId(159)
		val weatherElementBoost = sandForceEffects
			.filterIsInstance<BattleAbilityEffect.WeatherElementDamageBoost>()
			.single()
		assertThat(weatherElementBoost.weather).isEqualTo(BattleWeather.SANDSTORM)
		assertThat(weatherElementBoost.elementIds).containsExactlyInAnyOrder(5L, 6L, 9L)
		assertThat(weatherElementBoost.multiplier).isEqualTo(1.3)
		assertThat(sandForceEffects).contains(BattleAbilityEffect.WeatherDamageImmunity(setOf(BattleWeather.SANDSTORM)))
		assertThat(service.abilityEffectsByAbilityId(244))
			.containsExactly(
				BattleAbilityEffect.SoundBasedSkillDamageBoost(),
				BattleAbilityEffect.SoundBasedSkillDamageReduction(),
			)
		assertThat(service.abilityEffectsByAbilityId(111))
			.containsExactly(BattleAbilityEffect.SuperEffectiveDamageReduction())
		assertThat(service.abilityEffectsByAbilityId(116))
			.containsExactly(BattleAbilityEffect.SuperEffectiveDamageReduction())
		assertThat(service.abilityEffectsByAbilityId(232))
			.containsExactly(BattleAbilityEffect.SuperEffectiveDamageReduction())
		assertThat(service.abilityEffectsByAbilityId(136))
			.containsExactly(BattleAbilityEffect.FullHpDamageReduction())
		assertThat(service.abilityEffectsByAbilityId(231))
			.containsExactly(BattleAbilityEffect.FullHpDamageReduction())
		assertThat(service.abilityEffectsByAbilityId(246))
			.containsExactly(
				BattleAbilityEffect.DamageClassDamageReduction(
					damageClasses = setOf(BattleDamageClass.SPECIAL),
				),
			)
		assertThat(service.abilityEffectsByAbilityId(169))
			.containsExactly(
				BattleAbilityEffect.DefendingStatMultiplier(
					stat = BattleStat.DEFENSE,
					multiplier = 2.0,
				),
			)
		assertThat(service.abilityEffectsByAbilityId(179))
			.containsExactly(
				BattleAbilityEffect.DefendingStatMultiplier(
					stat = BattleStat.DEFENSE,
					multiplier = 1.5,
					requiredTerrain = BattleTerrain.GRASSY,
				),
			)
		assertThat(service.abilityEffectsByAbilityId(37))
			.containsExactly(
				BattleAbilityEffect.AttackingStatMultiplier(
					stat = BattleStat.ATTACK,
					multiplier = 2.0,
				),
			)
		assertThat(service.abilityEffectsByAbilityId(74))
			.containsExactly(
				BattleAbilityEffect.AttackingStatMultiplier(
					stat = BattleStat.ATTACK,
					multiplier = 2.0,
				),
			)
		assertThat(service.abilityEffectsByAbilityId(62))
			.containsExactly(
				BattleAbilityEffect.AttackingStatMultiplier(
					stat = BattleStat.ATTACK,
					multiplier = 1.5,
					requiresMajorStatus = true,
					ignoresBurnAttackReduction = true,
				),
			)
		assertThat(service.abilityEffectsByAbilityId(91))
			.containsExactly(BattleAbilityEffect.SameElementBonusOverride(multiplier = 2.0))

		val contactParalysis = service.abilityEffectsByAbilityId(9)
			.filterIsInstance<BattleAbilityEffect.ContactStatusOnAttacker>()
			.single()
		assertThat(contactParalysis.status).isEqualTo(BattleMajorStatus.PARALYSIS)
		assertThat(contactParalysis.chancePercent).isEqualTo(30)
		val switchInAttackDrop = service.abilityEffectsByAbilityId(22)
			.filterIsInstance<BattleAbilityEffect.SwitchInStatStageChange>()
			.single()
		assertThat(switchInAttackDrop.stat).isEqualTo(BattleStat.ATTACK)
		assertThat(switchInAttackDrop.stageDelta).isEqualTo(-1)

		assertThat(service.switchInWeatherByAbilityId(2)).isEqualTo(BattleWeather.RAIN)
		assertThat(service.switchInWeatherByAbilityId(45)).isEqualTo(BattleWeather.SANDSTORM)
		assertThat(service.switchInWeatherByAbilityId(70)).isEqualTo(BattleWeather.SUN)
		assertThat(service.switchInWeatherByAbilityId(117)).isEqualTo(BattleWeather.SNOW)

		assertThat(service.switchInTerrainByAbilityId(226)).isEqualTo(BattleTerrain.ELECTRIC)
		assertThat(service.switchInTerrainByAbilityId(227)).isEqualTo(BattleTerrain.PSYCHIC)
		assertThat(service.switchInTerrainByAbilityId(228)).isEqualTo(BattleTerrain.MISTY)
		assertThat(service.switchInTerrainByAbilityId(229)).isEqualTo(BattleTerrain.GRASSY)

		assertThat(service.weatherSpeedByAbilityId(33)).isEqualTo(BattleWeather.RAIN to 2.0)
		assertThat(service.weatherSpeedByAbilityId(34)).isEqualTo(BattleWeather.SUN to 2.0)
		assertThat(service.weatherSpeedByAbilityId(146)).isEqualTo(BattleWeather.SANDSTORM to 2.0)
		assertThat(service.weatherSpeedByAbilityId(202)).isEqualTo(BattleWeather.SNOW to 2.0)

		assertThat(service.terrainSpeedByAbilityId(207)).isEqualTo(BattleTerrain.ELECTRIC to 2.0)

		assertThat(service.abilityEffectsByAbilityId(134))
			.containsExactly(BattleAbilityEffect.WeightMultiplier(numerator = 2, denominator = 1))
		assertThat(service.abilityEffectsByAbilityId(135))
			.containsExactly(BattleAbilityEffect.WeightMultiplier(numerator = 1, denominator = 2))

		assertThat(service.weatherHealByAbilityId(44)).isEqualTo(setOf(BattleWeather.RAIN) to 16)
		assertThat(service.weatherHealByAbilityId(115)).isEqualTo(setOf(BattleWeather.SNOW) to 16)

		assertThat(service.abilityEffectsByAbilityId(5))
			.hasExactlyElementsOfTypes(BattleAbilityEffect.SurviveFatalDamageAtFullHp::class.java)
		assertThat(service.abilityEffectsByAbilityId(4))
			.containsExactly(BattleAbilityEffect.CriticalHitImmunity)
		assertThat(service.abilityEffectsByAbilityId(75))
			.containsExactly(BattleAbilityEffect.CriticalHitImmunity)
		assertThat(service.abilityEffectsByAbilityId(69))
			.containsExactly(BattleAbilityEffect.SkillRecoilDamageImmunity)
		assertThat(service.abilityEffectsByAbilityId(98))
			.containsExactly(BattleAbilityEffect.IndirectDamageImmunity)
		assertThat(service.abilityEffectsByAbilityId(109))
			.containsExactly(
				BattleAbilityEffect.IgnoreOpponentDamageStatStages,
				BattleAbilityEffect.IgnoreOpponentAccuracyStatStages,
			)
		assertThat(service.abilityEffectsByAbilityId(104))
			.containsExactly(BattleAbilityEffect.IgnoreTargetAbilityEffects)
		assertThat(service.abilityEffectsByAbilityId(163))
			.containsExactly(BattleAbilityEffect.IgnoreTargetAbilityEffects)
		assertThat(service.abilityEffectsByAbilityId(164))
			.containsExactly(BattleAbilityEffect.IgnoreTargetAbilityEffects)
		assertThat(service.abilityEffectsByAbilityId(43))
			.containsExactly(BattleAbilityEffect.SoundBasedSkillImmunity)

		assertThat(service.abilityEffectsByAbilityId(214))
			.hasExactlyElementsOfTypes(BattleAbilityEffect.PriorityMoveImmunityForSide::class.java)
		assertThat(service.abilityEffectsByAbilityId(219))
			.hasExactlyElementsOfTypes(BattleAbilityEffect.PriorityMoveImmunityForSide::class.java)
		assertThat(service.abilityEffectsByAbilityId(296))
			.hasExactlyElementsOfTypes(BattleAbilityEffect.PriorityMoveImmunityForSide::class.java)
		assertThat(service.abilityEffectsByAbilityId(158))
			.hasExactlyElementsOfTypes(BattleAbilityEffect.StatusSkillPriorityBoost::class.java)

		val electricAbsorb = service.abilityEffectsByAbilityId(10)
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbHeal>()
			.single()
		assertThat(electricAbsorb.elementId).isEqualTo(13)
		assertThat(electricAbsorb.healDenominator).isEqualTo(4)
		val waterAbsorb = service.abilityEffectsByAbilityId(11)
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbHeal>()
			.single()
		assertThat(waterAbsorb.elementId).isEqualTo(11)
		val groundAbsorb = service.abilityEffectsByAbilityId(297)
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbHeal>()
			.single()
		assertThat(groundAbsorb.elementId).isEqualTo(5)

		val electricAbsorbSpeed = service.abilityEffectsByAbilityId(78)
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbStatStage>()
			.single()
		assertThat(electricAbsorbSpeed.elementId).isEqualTo(13)
		assertThat(electricAbsorbSpeed.stat).isEqualTo(BattleStat.SPEED)
		assertThat(electricAbsorbSpeed.stageDelta).isEqualTo(1)
		val grassAbsorbAttack = service.abilityEffectsByAbilityId(157)
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbStatStage>()
			.single()
		assertThat(grassAbsorbAttack.elementId).isEqualTo(12)
		assertThat(grassAbsorbAttack.stat).isEqualTo(BattleStat.ATTACK)
		assertThat(grassAbsorbAttack.stageDelta).isEqualTo(1)
		val fireAbsorbDefense = service.abilityEffectsByAbilityId(273)
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbStatStage>()
			.single()
		assertThat(fireAbsorbDefense.elementId).isEqualTo(10)
		assertThat(fireAbsorbDefense.stat).isEqualTo(BattleStat.DEFENSE)
		assertThat(fireAbsorbDefense.stageDelta).isEqualTo(2)

		assertThat(service.groundedByAbilityId(26)).isFalse()
		assertThat(service.groundedByAbilityId(null)).isTrue()

		val leftovers = service.itemEffectsByItemId(211)
			.filterIsInstance<BattleItemEffect.HeldEndTurnHeal>()
			.single()
		assertThat(leftovers.healDenominator).isEqualTo(16)

		val lifeOrb = service.itemEffectsByItemId(247)
			.filterIsInstance<BattleItemEffect.DamageBoostWithRecoil>()
			.single()
		assertThat(lifeOrb.multiplier).isEqualTo(1.3)
		assertThat(lifeOrb.recoilDenominator).isEqualTo(10)

		val shellBell = service.itemEffectsByItemId(230)
			.filterIsInstance<BattleItemEffect.DamageDealtHeal>()
			.single()
		assertThat(shellBell.healDenominator).isEqualTo(8)

		val physicalPowerBoost = service.itemEffectsByItemId(243)
			.filterIsInstance<BattleItemEffect.DamageClassPowerBoost>()
			.single()
		assertThat(physicalPowerBoost.damageClasses).containsExactly(BattleDamageClass.PHYSICAL)
		assertThat(physicalPowerBoost.multiplier).isEqualTo(1.1)

		val specialPowerBoost = service.itemEffectsByItemId(244)
			.filterIsInstance<BattleItemEffect.DamageClassPowerBoost>()
			.single()
		assertThat(specialPowerBoost.damageClasses).containsExactly(BattleDamageClass.SPECIAL)
		assertThat(specialPowerBoost.multiplier).isEqualTo(1.1)

		val superEffectiveBoost = service.itemEffectsByItemId(245)
			.filterIsInstance<BattleItemEffect.SuperEffectiveDamageBoost>()
			.single()
		assertThat(superEffectiveBoost.multiplier).isEqualTo(1.2)

		mapOf(
			199L to 7L,
			210L to 9L,
			214L to 5L,
			215L to 6L,
			216L to 12L,
			217L to 17L,
			218L to 2L,
			219L to 13L,
			220L to 11L,
			221L to 3L,
			222L to 4L,
			223L to 15L,
			224L to 8L,
			225L to 14L,
			226L to 10L,
			227L to 16L,
			228L to 1L,
			2105L to 18L,
		).forEach { (itemId, elementId) ->
			val elementBoost = service.itemEffectsByItemId(itemId)
				.filterIsInstance<BattleItemEffect.ElementDamageBoost>()
				.single()
			assertThat(elementBoost.elementId).isEqualTo(elementId)
			assertThat(elementBoost.multiplier).isEqualTo(1.2)
		}

		mapOf(
			161L to 10L,
			162L to 11L,
			163L to 13L,
			164L to 12L,
			165L to 15L,
			166L to 2L,
			167L to 4L,
			168L to 5L,
			169L to 3L,
			170L to 14L,
			171L to 7L,
			172L to 6L,
			173L to 8L,
			174L to 16L,
			175L to 17L,
			176L to 9L,
			177L to 1L,
			723L to 18L,
		).forEach { (itemId, elementId) ->
			val elementReduction = service.itemEffectsByItemId(itemId)
				.filterIsInstance<BattleItemEffect.ElementDamageReduction>()
				.single()
			assertThat(elementReduction.elementId).isEqualTo(elementId)
			assertThat(elementReduction.multiplier).isEqualTo(0.5)
			assertThat(elementReduction.consumesItem).isTrue()
			assertThat(elementReduction.requiresSuperEffective).isEqualTo(itemId != 177L)
		}

		val smallBerry = service.itemEffectsByItemId(132)
			.filterIsInstance<BattleItemEffect.LowHpHeal>()
			.single()
		assertThat(smallBerry.fixedHealAmount).isEqualTo(10)
		assertThat(smallBerry.healDenominator).isNull()

		val mediumBerry = service.itemEffectsByItemId(135)
			.filterIsInstance<BattleItemEffect.LowHpHeal>()
			.single()
		assertThat(mediumBerry.fixedHealAmount).isNull()
		assertThat(mediumBerry.healDenominator).isEqualTo(4)

		val choiceSpeedLock = service.itemEffectsByItemId(264)
			.filterIsInstance<BattleItemEffect.ChoiceSkillLock>()
			.single()
		assertThat(choiceSpeedLock.speedMultiplier).isEqualTo(1.5)
		assertThat(service.itemEffectsByItemId(582))
			.containsExactly(BattleItemEffect.WeightMultiplier(numerator = 1, denominator = 2))

		assertThat(service.itemEffectsByItemId(126).filterIsInstance<BattleItemEffect.MajorStatusCure>().single().statuses)
			.containsExactly(BattleMajorStatus.PARALYSIS)
		assertThat(service.itemEffectsByItemId(127).filterIsInstance<BattleItemEffect.MajorStatusCure>().single().statuses)
			.containsExactly(BattleMajorStatus.SLEEP)
		assertThat(service.itemEffectsByItemId(128).filterIsInstance<BattleItemEffect.MajorStatusCure>().single().statuses)
			.containsExactlyInAnyOrder(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON)
		assertThat(service.itemEffectsByItemId(129).filterIsInstance<BattleItemEffect.MajorStatusCure>().single().statuses)
			.containsExactly(BattleMajorStatus.BURN)
		assertThat(service.itemEffectsByItemId(130).filterIsInstance<BattleItemEffect.MajorStatusCure>().single().statuses)
			.containsExactly(BattleMajorStatus.FREEZE)

		val confusionCure = service.itemEffectsByItemId(133)
			.filterIsInstance<BattleItemEffect.VolatileStatusCure>()
			.single()
		assertThat(confusionCure.statuses).containsExactly(BattleVolatileStatus.CONFUSION)
		assertThat(confusionCure.consumesItem).isTrue()

		val majorStatusCure = service.itemEffectsByItemId(134)
			.filterIsInstance<BattleItemEffect.MajorStatusCure>()
			.single()
		assertThat(majorStatusCure.statuses)
			.containsExactlyInAnyOrder(
				BattleMajorStatus.BURN,
				BattleMajorStatus.PARALYSIS,
				BattleMajorStatus.POISON,
				BattleMajorStatus.BAD_POISON,
				BattleMajorStatus.SLEEP,
				BattleMajorStatus.FREEZE,
			)
		assertThat(majorStatusCure.consumesItem).isTrue()

		assertThat(service.itemEffectsByItemId(248))
			.hasExactlyElementsOfTypes(BattleItemEffect.ChargeSkipOnce::class.java)

		val fatalDamageSurvival = service.itemEffectsByItemId(252)
			.filterIsInstance<BattleItemEffect.SurviveFatalDamageAtFullHp>()
			.single()
		assertThat(fatalDamageSurvival.consumesItem).isTrue()

		val screenDuration = service.itemEffectsByItemId(246)
			.filterIsInstance<BattleItemEffect.SideDamageReductionDurationExtension>()
			.single()
		assertThat(screenDuration.kinds)
			.containsExactlyInAnyOrder(
				BattleSideDamageReductionKind.PHYSICAL,
				BattleSideDamageReductionKind.SPECIAL,
				BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE,
			)
		assertThat(screenDuration.turnsRemaining).isEqualTo(8)

		val rainDuration = service.itemEffectsByItemId(262)
			.filterIsInstance<BattleItemEffect.WeatherDurationExtension>()
			.single()
		assertThat(rainDuration.weathers).containsExactly(BattleWeather.RAIN)
		assertThat(rainDuration.turnsRemaining).isEqualTo(8)

		val sandstormDuration = service.itemEffectsByItemId(260)
			.filterIsInstance<BattleItemEffect.WeatherDurationExtension>()
			.single()
		assertThat(sandstormDuration.weathers).containsExactly(BattleWeather.SANDSTORM)
		assertThat(sandstormDuration.turnsRemaining).isEqualTo(8)

		val terrainDuration = service.itemEffectsByItemId(896)
			.filterIsInstance<BattleItemEffect.TerrainDurationExtension>()
			.single()
		assertThat(terrainDuration.terrains)
			.containsExactlyInAnyOrder(
				BattleTerrain.ELECTRIC,
				BattleTerrain.GRASSY,
				BattleTerrain.MISTY,
				BattleTerrain.PSYCHIC,
			)
		assertThat(terrainDuration.turnsRemaining).isEqualTo(8)
	}

	/**
	 * 特性/道具规则可以暂时没有运行时 hook，但非法 ID 不能被静默接受。
	 *
	 * 空策略列表表示“资料存在性由资料维护侧保证，但当前没有引擎需要执行的效果”；非正数 ID 则是请求错误，应立即
	 * 拒绝。这个差异能避免后续新增普通资料时被迫同步新增空规则行。
	 */
	@Test
	fun `ability and item effect assembly separates empty policies from invalid identifiers`() {
		assertThat(service.abilityEffectsByAbilityId(null)).isEmpty()
		assertThat(service.itemEffectsByItemId(null)).isEmpty()
		assertThat(service.groundedByAbilityId(null)).isTrue()
		assertThat(service.abilityEffectsByAbilityId(999_999)).isEmpty()
		assertThat(service.itemEffectsByItemId(999_999)).isEmpty()

		val invalidAbility = assertThrows<ApiException> {
			service.abilityEffectsByAbilityId(0)
		}
		assertThat(invalidAbility.field).isEqualTo("abilityId")
		assertThat(invalidAbility.message).isEqualTo("abilityId 必须大于 0")

		val invalidGroundingAbility = assertThrows<ApiException> {
			service.groundedByAbilityId(-1)
		}
		assertThat(invalidGroundingAbility.field).isEqualTo("abilityId")
		assertThat(invalidGroundingAbility.message).isEqualTo("abilityId 必须大于 0")

		val invalidItem = assertThrows<ApiException> {
			service.itemEffectsByItemId(0)
		}
		assertThat(invalidItem.field).isEqualTo("itemId")
		assertThat(invalidItem.message).isEqualTo("itemId 必须大于 0")
	}

	@Test
	fun `runtime assembly rejects enabled unknown policies instead of dropping them`() {
		withTemporaryAbilityPolicy("unknown-ability-policy") {
			val abilityEffect = assertThrows<ApiException> {
				service.abilityEffectsByAbilityId(65)
			}
			assertThat(abilityEffect.field).isEqualTo("effectPolicy")
			assertThat(abilityEffect.message).isEqualTo("不支持的特性战斗效果策略: unknown-ability-policy")

			val grounding = assertThrows<ApiException> {
				service.groundedByAbilityId(65)
			}
			assertThat(grounding.field).isEqualTo("effectPolicy")
			assertThat(grounding.message).isEqualTo("不支持的特性战斗效果策略: unknown-ability-policy")
		}

		withTemporaryItemPolicy("unknown-item-policy") {
			val itemEffect = assertThrows<ApiException> {
				service.itemEffectsByItemId(211)
			}
			assertThat(itemEffect.field).isEqualTo("effectPolicy")
			assertThat(itemEffect.message).isEqualTo("不支持的道具战斗效果策略: unknown-item-policy")
		}

		withTemporarySkillPolicy("unknown-skill-policy") { skillId ->
			val skillRule = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(skillRule.field).isEqualTo("effectPolicy")
			assertThat(skillRule.message).isEqualTo("不支持的技能主效果策略: unknown-skill-policy")
		}

		withTemporarySideFieldEffectPolicy("unknown-side-field-policy") { skillId ->
			val fieldEffect = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(fieldEffect.field).isEqualTo("effectPolicy")
			assertThat(fieldEffect.message).isEqualTo("不支持的一侧场地效果策略: unknown-side-field-policy")
		}

		withTemporaryGlobalFieldEffectPolicy("unknown-global-field-policy") { skillId ->
			val globalFieldEffect = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(globalFieldEffect.field).isEqualTo("effectPolicy")
			assertThat(globalFieldEffect.message).isEqualTo("不支持的全场速度顺序效果策略: unknown-global-field-policy")
		}

		withTemporaryStatusEffectTargetScope("UNKNOWN_TARGET_SCOPE") { skillId ->
			val statusTarget = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(statusTarget.field).isEqualTo("targetScope")
			assertThat(statusTarget.message).isEqualTo("不支持的状态效果目标作用域: UNKNOWN_TARGET_SCOPE")
		}

		withTemporaryStatStageEffectTargetScope("UNKNOWN_TARGET_SCOPE") { skillId ->
			val statTarget = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(statTarget.field).isEqualTo("targetScope")
			assertThat(statTarget.message).isEqualTo("不支持的能力阶级效果目标作用域: UNKNOWN_TARGET_SCOPE")
		}

		withTemporaryStatusKind("UNKNOWN_STATUS_KIND") { skillId ->
			val statusKind = assertThrows<ApiException> {
				service.skillSlotsBySkillIds(listOf(skillId))
			}
			assertThat(statusKind.field).isEqualTo("statusKind")
			assertThat(statusKind.message).isEqualTo("不支持的状态效果类型: UNKNOWN_STATUS_KIND")
		}
	}

	/**
	 * 验证数据库中已经启用的战斗规则资料都能被运行时适配层装配。
	 *
	 * 这个测试不逐条断言具体效果数值，原因是具体数值已经由技能、特性、道具的专项测试覆盖；这里锁定的是另一类
	 * 更容易被遗漏的资料完整性问题：Liquibase 新增了启用中的规则行，但运行时读取器或 policy mapper 没有跟上。
	 * 测试直接从三范式规则表读取启用中的 policy 和基础资料 ID，再走正式的 [BattleRuntimeSnapshotService] 装配入口：
	 * - 技能 `effect_policy` 必须要么映射成强类型效果，要么被子表/布尔字段承载为结构型规则。
	 * - 技能 `target_policy`、`hit_policy`、`damage_policy` 必须落在运行时显式支持集合内，不能依赖默认兜底。
	 * - 技能规则必须能装配成 [io.github.lishangbu.battleengine.model.BattleSkillSlot]。
	 * - 特性规则必须能装配结构化效果或接地事实，`ground-immunity` 这种非效果型 policy 也会通过接地装配暴露。
	 * - 道具规则必须能装配成结构化携带道具效果。
	 *
	 * 这样做比在测试里复制 mapper 字典更稳：真实入口如果因为字段名、外键、禁用状态、policy 拼写或基础资料缺失而
	 * 失败，这里会直接报错；而 mapper 自己公开的内部支持判定则保证 `mapNotNull` 不会把启用中的策略静默吞掉。
	 */
	@Test
	fun `enabled battle rule rows can be assembled by runtime adapters`() {
		val elementIds = elementIdsByCode()
		val skillIds = enabledIds("battle_skill_rule", "skill_id")
		val abilityIds = enabledIds("battle_ability_rule", "ability_id")
		val itemIds = enabledIds("battle_item_rule", "item_id")
		val unsupportedSkillEffectPolicies = enabledPolicies("battle_skill_rule", "effect_policy")
			.filterNot { it.isBattleSkillRuntimeEffectPolicySupported() }
		val unsupportedSkillTargetPolicies = enabledPolicies("battle_skill_rule", "target_policy")
			.filterNot { it.isBattleSkillRuntimeTargetPolicySupported() }
		val unsupportedSkillHitPolicies = enabledPolicies("battle_skill_rule", "hit_policy")
			.filterNot { it.isBattleSkillRuntimeHitPolicySupported() }
		val unsupportedSkillDamagePolicies = enabledPolicies("battle_skill_rule", "damage_policy")
			.filterNot { it.isBattleSkillRuntimeDamagePolicySupported() }
		val unsupportedAbilityPolicies = enabledPolicies("battle_ability_rule", "effect_policy")
			.filterNot { it.isBattleAbilityRuntimePolicySupported(elementIds) }
		val unsupportedItemPolicies = enabledPolicies("battle_item_rule", "effect_policy")
			.filterNot { it.isBattleItemRuntimePolicySupported(elementIds) }

		assertThat(skillIds).isNotEmpty
		assertThat(abilityIds).isNotEmpty
		assertThat(itemIds).isNotEmpty
		assertThat(unsupportedSkillEffectPolicies)
			.describedAs("启用中的技能 effect_policy 必须被运行时装配层承载")
			.isEmpty()
		assertThat(unsupportedSkillTargetPolicies)
			.describedAs("启用中的技能 target_policy 必须显式可识别，不能依赖默认单体目标兜底")
			.isEmpty()
		assertThat(unsupportedSkillHitPolicies)
			.describedAs("启用中的技能 hit_policy 必须显式可识别")
			.isEmpty()
		assertThat(unsupportedSkillDamagePolicies)
			.describedAs("启用中的技能 damage_policy 必须显式可识别")
			.isEmpty()
		assertThat(unsupportedAbilityPolicies)
			.describedAs("启用中的特性 effect_policy 必须被运行时装配层承载")
			.isEmpty()
		assertThat(unsupportedItemPolicies)
			.describedAs("启用中的道具 effect_policy 必须被运行时装配层承载")
			.isEmpty()
		assertThat(service.skillSlotsBySkillIds(skillIds).map { it.skillId }).containsExactlyElementsOf(skillIds)
		abilityIds.forEach { abilityId ->
			service.abilityEffectsByAbilityId(abilityId)
			service.groundedByAbilityId(abilityId)
		}
		itemIds.forEach { itemId ->
			assertThat(service.itemEffectsByItemId(itemId))
				.describedAs("道具规则必须装配为运行时效果: itemId=$itemId")
				.isNotEmpty
		}
	}

	@Test
	fun `preparation validation uses assembled official double rules`() {
		val response = service.validatePreparation(
			BattlePreparationValidationRequest(
				formatCode = "official-double",
				sides = listOf(
					BattlePreparationSideRequest(
						sideId = "side-a",
						activeActorIds = listOf("a-1", "a-2"),
						participants = listOf(
							participant("a-1", creatureId = 1, level = 60, itemId = 10),
							participant("a-2", creatureId = 1, level = 50, itemId = 10),
						),
					),
					BattlePreparationSideRequest(
						sideId = "side-b",
						activeActorIds = listOf("b-1", "b-2"),
						participants = listOf(
							participant("b-1", creatureId = 1, level = 50, itemId = 10),
							participant("b-2", creatureId = 2, level = 50, itemId = 11),
						),
					),
				),
			),
		)

		assertThat(response.valid).isFalse()
		assertThat(response.violations.map { it.code }).containsExactlyInAnyOrder(
			"level-too-high",
			"duplicate-creature",
			"duplicate-creature",
			"duplicate-item",
			"duplicate-item",
		)
		assertThat(response.violations.map { it.actorId }.toSet()).contains("a-1", "a-2")
		assertThat(response.violations.map { it.actorId }).doesNotContain("b-1")
	}

	@Test
	fun `preparation validation rejects invalid skill slot input before engine assembly`() {
		val tooManySkills = assertThrows<ApiException> {
			service.validatePreparation(
				preparationRequest(
					participant("a-1", creatureId = 1, level = 50, itemId = 10).copy(
						skillIds = listOf(1, 2, 3, 4, 5),
					),
				),
			)
		}
		assertThat(tooManySkills.field).isEqualTo("skillIds")
		assertThat(tooManySkills.message).isEqualTo("skillIds 最多只能包含 4 个技能")

		val duplicateSkill = assertThrows<ApiException> {
			service.validatePreparation(
				preparationRequest(
					participant("a-1", creatureId = 1, level = 50, itemId = 10).copy(
						skillIds = listOf(1, 1),
					),
				),
			)
		}
		assertThat(duplicateSkill.field).isEqualTo("skillIds")
		assertThat(duplicateSkill.message).isEqualTo("skillIds 不能包含重复技能")
	}

	/**
	 * 验证准备阶段装配器会在进入 battle-engine 模型前拒绝畸形队伍骨架。
	 *
	 * `BattleInitialState` 自身有完整 `require` 不变量，但这些异常只适合纯领域层和测试使用；生产 API 入口必须把
	 * 请求层面的错误翻译成稳定 `ApiException`。这里集中覆盖最容易从管理端手填 JSON 里出现的三类问题：
	 * 少传一侧队伍、不同队伍复用同一个 actorId、双打赛制下只声明一个上场成员。
	 */
	@Test
	fun `preparation validation rejects malformed team skeleton before engine model`() {
		val missingSide = assertThrows<ApiException> {
			service.validatePreparation(
				BattlePreparationValidationRequest(
					formatCode = "official-double",
					sides = validDoubleSides().take(1),
				),
			)
		}
		assertThat(missingSide.field).isEqualTo("sides")
		assertThat(missingSide.message).isEqualTo("sides 必须包含两侧队伍")

		val duplicateActor = assertThrows<ApiException> {
			service.validatePreparation(
				BattlePreparationValidationRequest(
					formatCode = "official-double",
					sides = duplicateActorDoubleSides(),
				),
			)
		}
		assertThat(duplicateActor.field).isEqualTo("actorId")
		assertThat(duplicateActor.message).isEqualTo("actorId 不能跨队伍重复")

		val activeCountDrift = assertThrows<ApiException> {
			service.validatePreparation(
				BattlePreparationValidationRequest(
					formatCode = "official-double",
					sides = activeCountDriftDoubleSides(),
				),
			)
		}
		assertThat(activeCountDrift.field).isEqualTo("activeActorIds")
		assertThat(activeCountDrift.message).isEqualTo("activeActorIds 数量必须符合赛制")
	}

	@Test
	fun `action validation uses assembled runtime snapshot`() {
		val response = service.validateActions(
			actionValidationRequest(
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 999,
						targetActorId = "b-1",
					),
					BattleActionRequest(
						type = "SWITCH_PARTICIPANT",
						actorId = "a-2",
						targetActorId = "b-2",
					),
				),
			),
		)

		assertThat(response.valid).isFalse()
		assertThat(response.violations.map { it.code }).containsExactly("skill-not-found", "switch-target-opponent")
		assertThat(response.violations.first().resourceId).isEqualTo(999)
		assertThat(response.violations[1].targetActorId).isEqualTo("b-2")
	}

	/**
	 * 验证行动校验入口不会把准备阶段违规交给核心引擎启动。
	 *
	 * `BattleEngine.start` 会用 `require` 保护非法初始状态；这是纯引擎内部正确的 fail-fast 行为。
	 * 但 HTTP/API 应用层必须把同一类问题转换成稳定字段错误，否则生产接口会把用户提交的非法队伍误报成 500。
	 */
	@Test
	fun `action validation rejects preparation violations before engine start`() {
		val error = assertThrows<ApiException> {
			service.validateActions(
				BattleActionValidationRequest(
					formatCode = "official-double",
					sides = invalidLevelDoubleSides(),
					actions = listOf(
						BattleActionRequest(
							type = "USE_SKILL",
							actorId = "a-1",
							skillId = 1,
							targetActorId = "b-1",
						),
					),
				),
			)
		}

		assertThat(error.field).isEqualTo("sides")
		assertThat(error.message).contains("准备阶段队伍不合法")
		assertThat(error.message).contains("成员等级 60 超过上限 50")
	}

	@Test
	fun `action validation accepts placeholder target for self target skill`() {
		val sides = preparationRequest(
			participant(
				actorId = "a-1",
				creatureId = 1,
				level = 50,
				itemId = 10,
				skillIds = listOf(182, 1, 2, 3),
			),
		).sides

		val response = service.validateActions(
			BattleActionValidationRequest(
				formatCode = "official-double",
				sides = sides,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 182,
						targetActorId = "missing-placeholder",
					),
				),
			),
		)

		assertThat(response.valid).isTrue()
		assertThat(response.violations).isEmpty()
	}

	/**
	 * 验证沙盒首回合也复用同一层准备阶段保护。
	 *
	 * 沙盒是管理端最容易直接手填请求的入口，非法等级、禁用资料或重复条款都应以业务校验错误返回。
	 * 这条测试防止未来只修 `validateActions`、却让 `/api/battle-sandbox/turn` 再次把准备阶段错误冒泡成 500。
	 */
	@Test
	fun `sandbox turn rejects preparation violations before engine start`() {
		val error = assertThrows<ApiException> {
			service.resolveSandboxTurn(
				BattleSandboxTurnRequest(
					formatCode = "official-double",
					sides = invalidLevelDoubleSides(),
					randomSeed = 0,
					actions = listOf(
						BattleActionRequest(
							type = "USE_SKILL",
							actorId = "a-1",
							skillId = 1,
							targetActorId = "b-1",
						),
					),
				),
			)
		}

		assertThat(error.field).isEqualTo("sides")
		assertThat(error.message).contains("准备阶段队伍不合法")
		assertThat(error.message).contains("成员等级 60 超过上限 50")
	}

	@Test
	fun `sandbox turn resolves assembled runtime state and returns inspectable event stream`() {
		val response = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "official-double",
				sides = validDoubleSides(),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val target = response.sides
			.flatMap { it.participants }
			.single { it.actorId == "b-1" }

		assertThat(response.resolved).isTrue()
		assertThat(response.violations).isEmpty()
		assertThat(response.events.map { it.type }).contains("BattleStarted", "TurnStarted", "SkillUsed", "DamageApplied")
		assertThat(response.events.single { it.type == "DamageApplied" }.message).contains("b-1", "伤害")
		assertThat(response.randomTrace).isNotEmpty
		assertThat(target.currentHp).isLessThan(target.maxHp)
		assertThat(response.state.turnNumber).isEqualTo(1)
		assertThat(response.state.events).hasSize(response.events.size)
		assertThat(response.state.turns).hasSize(1)
		assertThat(response.state.turns.single().actions.map { it.actorId }).containsExactly("a-1")
		assertThat(response.state.turns.single().randomTrace).containsExactlyElementsOf(response.randomTrace)
		assertThat(response.state.turns.single().events.map { it.turnNumber }.toSet()).containsExactly(1)
	}

	@Test
	fun `sandbox turn renders skill failure reason in chinese`() {
		val response = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "official-double",
				sides = listOf(
					BattlePreparationSideRequest(
						sideId = "side-a",
						activeActorIds = listOf("a-1", "a-2"),
						participants = listOf(
							participant("a-1", creatureId = 1, level = 49, skillIds = listOf(12)),
							participant("a-2", creatureId = 2, level = 50),
						),
					),
					BattlePreparationSideRequest(
						sideId = "side-b",
						activeActorIds = listOf("b-1", "b-2"),
						participants = listOf(
							participant("b-1", creatureId = 3, level = 50),
							participant("b-2", creatureId = 4, level = 50),
						),
					),
				),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 12,
						targetActorId = "b-1",
					),
				),
			),
		)

		assertThat(response.resolved).isTrue()
		assertThat(response.events.single { it.type == "SkillFailed" }.message)
			.isEqualTo("a-1 的技能 12 失败：目标等级高于使用者。")
	}

	@Test
	fun `sandbox turn renders fallback event type in chinese`() {
		val response = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "official-double",
				sides = listOf(
					BattlePreparationSideRequest(
						sideId = "side-a",
						activeActorIds = listOf("a-1", "a-2"),
						participants = listOf(
							participant("a-1", creatureId = 1, level = 50, skillIds = listOf(164)),
							participant("a-2", creatureId = 2, level = 50),
						),
					),
					BattlePreparationSideRequest(
						sideId = "side-b",
						activeActorIds = listOf("b-1", "b-2"),
						participants = listOf(
							participant("b-1", creatureId = 3, level = 50),
							participant("b-2", creatureId = 4, level = 50),
						),
					),
				),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 164,
						targetActorId = "a-1",
					),
				),
			),
		)

		assertThat(response.resolved).isTrue()
		assertThat(response.events.single { it.type == "SubstituteStarted" }.message).isEqualTo("替身开始")
	}

	@Test
	fun `sandbox turn can continue from previous response state snapshot`() {
		val first = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "official-double",
				sides = validDoubleSides(),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val second = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "official-double",
				sides = validDoubleSides(),
				randomSeed = 1,
				state = first.state,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val firstTarget = first.sides.flatMap { it.participants }.single { it.actorId == "b-1" }
		val secondTarget = second.sides.flatMap { it.participants }.single { it.actorId == "b-1" }
		val firstAttackerSlot = first.sides.flatMap { it.participants }.single { it.actorId == "a-1" }.skillSlots.single { it.skillId == 1L }
		val secondAttackerSlot = second.sides.flatMap { it.participants }.single { it.actorId == "a-1" }.skillSlots.single { it.skillId == 1L }

		assertThat(second.resolved).isTrue()
		assertThat(second.turnNumber).isEqualTo(2)
		assertThat(secondTarget.currentHp).isLessThan(firstTarget.currentHp)
		assertThat(secondAttackerSlot.remainingPp).isEqualTo(firstAttackerSlot.remainingPp - 1)
		assertThat(second.events.count { it.type == "BattleStarted" }).isEqualTo(1)
		assertThat(second.events.map { it.turnNumber }).contains(0, 1, 2)
		assertThat(second.state.turnNumber).isEqualTo(2)
		assertThat(second.state.events).hasSize(second.events.size)
		assertThat(second.state.turns.map { it.turnNumber }).containsExactly(1, 2)
		assertThat(second.state.turns.flatMap { it.actions }.map { it.actorId }).containsExactly("a-1", "a-1")
		assertThat(second.state.turns.last().events.map { it.turnNumber }.toSet()).containsExactly(2)
	}

	@Test
	fun `sandbox state snapshot can continue after json round trip`() {
		val first = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "standard-single",
				sides = validSingleSides(),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val secondRequestJson = objectMapper.writeValueAsString(
			BattleSandboxTurnRequest(
				formatCode = "standard-single",
				sides = validSingleSides(),
				randomSeed = 1,
				state = first.state,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val secondRequest = objectMapper.readValue(secondRequestJson, BattleSandboxTurnRequest::class.java)
		val second = service.resolveSandboxTurn(secondRequest)

		assertThat(secondRequest.state?.sides?.map { it.sideId }).containsExactly("side-a", "side-b")
		assertThat(secondRequest.state?.turns?.map { it.turnNumber }).containsExactly(1)
		assertThat(secondRequest.state?.turns?.single()?.randomTrace?.map { it.sequence })
			.containsExactlyElementsOf((1..first.randomTrace.size).toList())
		assertThat(second.resolved).isTrue()
		assertThat(second.turnNumber).isEqualTo(2)
		assertThat(second.state.events).hasSize(second.events.size)
		assertThat(second.state.turns.map { it.turnNumber }).containsExactly(1, 2)
	}

	/**
	 * 验证沙盒续算会在恢复状态前拒绝被破坏的回合记录。
	 *
	 * 管理端沙盒不是正式对局存档，状态仍由前端原样带回；因此后端必须在恢复引擎状态前做最基本的自洽检查。
	 * 这里把第一回合记录删掉但保留 `turnNumber=1`，模拟浏览器或调试工具误改 JSON 的情况。期望返回字段校验
	 * 错误，而不是让状态机以“已过一回合但没有回放片段”的材料继续运行。
	 */
	@Test
	fun `sandbox state snapshot rejects missing settled turn record`() {
		val first = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "standard-single",
				sides = validSingleSides(),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val tamperedState = first.state.copy(turns = emptyList())

		val error = assertThrows<ApiException> {
			service.resolveSandboxTurn(
				BattleSandboxTurnRequest(
					formatCode = "standard-single",
					sides = validSingleSides(),
					randomSeed = 1,
					state = tamperedState,
					actions = listOf(
						BattleActionRequest(
							type = "USE_SKILL",
							actorId = "a-1",
							skillId = 1,
							targetActorId = "b-1",
						),
					),
				),
			)
		}

		assertThat(error.field).isEqualTo("state")
		assertThat(error.message).isEqualTo("state 回合记录必须从 1 连续到当前回合")
	}

	/**
	 * 验证沙盒续算会拒绝和当前赛制站位不一致的 state。
	 *
	 * 沙盒 state 由管理端原样带回，不能被当成可信存档。这里用双打首回合响应作为合法基线，再把 side-a 的
	 * `activeActorIds` 手动缩成一个成员，模拟浏览器调试工具或错误前端把双打运行态改成了单打形状。服务端必须
	 * 在恢复 [io.github.lishangbu.battleengine.model.BattleState] 前拒绝它，否则后续范围技能、同速排序、
	 * 入场特性和行动提交校验都会在“赛制是双打、实际只有一个上场席位”的矛盾状态下运行。
	 */
	@Test
	fun `sandbox state snapshot rejects active participant count drift`() {
		val first = service.resolveSandboxTurn(
			BattleSandboxTurnRequest(
				formatCode = "official-double",
				sides = validDoubleSides(),
				randomSeed = 0,
				actions = listOf(
					BattleActionRequest(
						type = "USE_SKILL",
						actorId = "a-1",
						skillId = 1,
						targetActorId = "b-1",
					),
				),
			),
		)
		val tamperedState = first.state.copy(
			sides = first.state.sides.map { side ->
				if (side.sideId == "side-a") {
					side.copy(activeActorIds = listOf("a-1"))
				} else {
					side
				}
			},
		)

		val error = assertThrows<ApiException> {
			service.resolveSandboxTurn(
				BattleSandboxTurnRequest(
					formatCode = "official-double",
					sides = validDoubleSides(),
					randomSeed = 1,
					state = tamperedState,
					actions = listOf(
						BattleActionRequest(
							type = "USE_SKILL",
							actorId = "a-1",
							skillId = 1,
							targetActorId = "b-1",
						),
					),
				),
			)
		}

		assertThat(error.field).isEqualTo("state")
		assertThat(error.message).isEqualTo("state 上场成员数量必须符合赛制")
	}

	/**
	 * 验证 battle-rules 装配出的运行时快照可以直接驱动 battle-engine 完成最小一回合。
	 *
	 * 这个测试刻意不手写 [io.github.lishangbu.battleengine.model.BattleRuleSnapshot]、技能槽、能力值或属性 ID：
	 * 这些事实全部来自 Liquibase 种子数据和 [BattleRuntimeSnapshotService.assembleInitialState]。如果后续资料表
	 * 字段、策略映射或默认技能装配发生偏移，这里会在引擎真正结算时失败，而不是只在 CRUD 层展示出一份看似完整
	 * 的 JSON。固定随机脚本只覆盖本次普通技能需要的要害和伤害浮动随机数，避免测试依赖不可重复随机。
	 */
	@Test
	fun `assembled runtime snapshot can drive battle engine minimum damage turn`() {
		val initialState = service.assembleInitialState(
			BattlePreparationValidationRequest(
				formatCode = "official-double",
				sides = validDoubleSides(),
			),
		)
		val targetHpBeforeTurn = initialState.sides
			.flatMap { it.participants }
			.single { it.actorId == "b-1" }
			.currentHp

		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(initialState),
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = 1, targetActorId = "b-1")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		assertThat(initialState.format.code).isEqualTo("official-double")
		assertThat(damage.actorId).isEqualTo("a-1")
		assertThat(damage.targetActorId).isEqualTo("b-1")
		assertThat(damage.skillId).isEqualTo(1)
		assertThat(damage.amount).isGreaterThan(0)
		assertThat(resolved.participant("b-1")?.currentHp).isLessThan(targetHpBeforeTurn)
	}

	/**
	 * 验证数据库中的暴走锁招字段会进入真实引擎状态机。
	 *
	 * 技能 37 的锁定回合与结束混乱来自 `battle_skill_rule`，测试不手写 [io.github.lishangbu.battleengine.model.BattleSkillSlot]。
	 * 第一回合脚本把总持续回合固定为 2；第二回合即使提交普通技能，引擎也必须继续使用 37，并且结束锁招后附加疲劳混乱。
	 */
	@Test
	fun `assembled runtime snapshot applies database rampage lock in engine turn`() {
		val initialState = assembledState(
			firstSideFirst = participant("a-1", creatureId = 1, level = 50, skillIds = listOf(37, 1)),
			secondSideFirst = participant("b-1", creatureId = 150, level = 50, skillIds = listOf(1)),
		)
		val engine = BattleEngine()
		val firstRandom = ScriptedBattleRandom(listOf(0, 1, 15, 0))
		val afterFirst = engine.resolveTurn(
			engine.start(initialState),
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = 37, targetActorId = "b-1")),
			firstRandom,
		)
		val ppAfterFirstUse = afterFirst.participant("a-1")
			?.skillSlots
			?.single { it.skillId == 37L }
			?.remainingPp
		val secondRandom = ScriptedBattleRandom(listOf(0, 1, 15, 0))
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = 1, targetActorId = "b-1")),
			secondRandom,
		)

		assertThat(firstRandom.consumedReasons()).containsExactly(
			"random adjacent opponent target for 37",
			"critical hit for 37",
			"damage random for 37",
			"locked move duration for 37",
		)
		assertThat(secondRandom.consumedReasons()).containsExactly(
			"random adjacent opponent target for 37",
			"critical hit for 37",
			"damage random for 37",
			"locked move confusion duration for 37",
		)
		assertThat(afterFirst.participant("a-1")?.lockedMoveTurnsRemaining).isEqualTo(1)
		assertThat(afterSecond.participant("a-1")?.lockedMoveTurnsRemaining).isZero()
		assertThat(afterSecond.participant("a-1")?.confusionTurnsRemaining).isGreaterThan(0)
		assertThat(
			afterSecond.participant("a-1")
				?.skillSlots
				?.single { it.skillId == 37L }
				?.remainingPp,
		).isEqualTo(ppAfterFirstUse)
		assertThat(
			afterSecond.events
				.filterIsInstance<BattleEvent.SkillUsed>()
				.filter { it.actorId == "a-1" }
				.map { it.skillId },
		).containsExactly(37L, 37L)
		assertThat(
			afterSecond.events
				.filterIsInstance<BattleEvent.LockedMoveEnded>()
				.single { it.actorId == "a-1" }
				.confusesUser,
		).isTrue()
		assertThat(
			afterSecond.events
				.filterIsInstance<BattleEvent.VolatileStatusApplied>()
				.single { it.targetActorId == "a-1" }
				.status,
		).isEqualTo(BattleVolatileStatus.CONFUSION)
	}

	/**
	 * 验证固定伤害技能规则不是只停留在技能槽断言，而是可以经数据库装配后进入真实伤害事件。
	 *
	 * 技能 49 的固定 20 点伤害来自 Liquibase 中的技能战斗规则资料。测试不手写 [BattleFixedDamage]，只通过
	 * [BattleRuntimeSnapshotService.assembleInitialState] 读取资料后交给引擎结算；如果以后资料表或映射丢失了
	 * 固定伤害字段，这里会看到普通伤害公式产出的数值而失败。
	 */
	@Test
	fun `assembled runtime snapshot applies database fixed damage rule in engine turn`() {
		val damage = assembledDamageEvent(
			skillId = 49,
			attackerCreatureId = 1,
			targetCreatureId = 4,
			randomValues = listOf(0),
		)

		assertThat(damage.amount).isEqualTo(20)
		assertThat(damage.effectiveness).isEqualTo(1.0)
		assertThat(damage.criticalHit).isFalse()
	}

	/**
	 * 验证数据库中的一击必杀规则不只是能装进技能槽，还能在真实状态机中使用专用命中率并造成目标当前 HP 伤害。
	 *
	 * 技能 12 的 effect_policy 来自 Liquibase 推导；测试不手写 [BattleOneHitKnockOut]。随机数 30 命中 50 级对
	 * 50 级的 30% 基础命中率，命中后伤害应等于装配出来的目标当前 HP，而不是普通公式或固定数值。
	 */
	@Test
	fun `assembled runtime snapshot applies database one hit knock out rule in engine turn`() {
		val initialState = assembledState(
			firstSideFirst = participant("a-1", creatureId = 1, level = 50, skillIds = listOf(12)),
			secondSideFirst = participant("b-1", creatureId = 4, level = 50, skillIds = listOf(1)),
		)
		val random = ScriptedBattleRandom(listOf(29))
		val engine = BattleEngine()
		val started = engine.start(initialState)
		val targetCurrentHp = started.participant("b-1")?.currentHp

		val resolved = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = 12, targetActorId = "b-1")),
			random,
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		assertThat(resolved.participant("b-1")?.currentHp).isEqualTo(0)
		assertThat(damage.amount).isEqualTo(targetCurrentHp)
		assertThat(damage.effectiveness).isEqualTo(1.0)
		assertThat(random.consumedReasons()).containsExactly("accuracy for 12")
	}

	/**
	 * 验证数据库中的替身技能规则可以完整驱动“建立替身 -> 后续伤害打到替身”流程。
	 *
	 * 第一回合只让资料中的技能 164 建立替身；第二回合由对手使用资料中的普通伤害技能打向该成员。断言重点不是
	 * 复制伤害公式，而是确认后续伤害进入 [BattleEvent.SubstituteDamageApplied]，目标本体 HP 保持在替身支付后的
	 * 数值，且不会同时产生打到本体的 [BattleEvent.DamageApplied]。
	 */
	@Test
	fun `assembled runtime snapshot drives database substitute skill and substitute damage`() {
		val initialState = service.assembleInitialState(
			preparationRequest(
				participant("a-1", creatureId = 1, level = 50, skillIds = listOf(164)),
			),
		)
		val engine = BattleEngine()
		val afterSubstitute = engine.resolveTurn(
			engine.start(initialState),
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = 164, targetActorId = "a-1")),
			ScriptedBattleRandom(emptyList()),
		)
		val substituteStarted = afterSubstitute.events.filterIsInstance<BattleEvent.SubstituteStarted>().single()
		val hpAfterSubstitute = afterSubstitute.participant("a-1")?.currentHp
		val substituteHpBeforeHit = afterSubstitute.participant("a-1")?.substituteHp ?: 0

		val afterHit = engine.resolveTurn(
			afterSubstitute,
			listOf(BattleAction.UseSkill(actorId = "b-1", skillId = 1, targetActorId = "a-1")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val secondTurnEvents = afterHit.events.drop(afterSubstitute.events.size)
		val substituteDamage = secondTurnEvents.filterIsInstance<BattleEvent.SubstituteDamageApplied>().single()

		assertThat(substituteStarted.skillId).isEqualTo(164)
		assertThat(substituteStarted.hpCost).isGreaterThan(0)
		assertThat(substituteStarted.substituteHp).isEqualTo(substituteHpBeforeHit)
		assertThat(afterHit.participant("a-1")?.currentHp).isEqualTo(hpAfterSubstitute)
		assertThat(substituteDamage.targetActorId).isEqualTo("a-1")
		assertThat(substituteDamage.amount).isGreaterThan(0)
		assertThat(substituteDamage.substituteHpRemaining).isLessThan(substituteHpBeforeHit)
		assertThat(secondTurnEvents.filterIsInstance<BattleEvent.DamageApplied>().map { it.targetActorId })
			.doesNotContain("a-1")
	}

	/**
	 * 验证满 HP 保命特性和一次性保命道具都能从数据库 policy 装配成引擎效果。
	 *
	 * 这里用同一个高伤害技能分别命中两种目标：一个目标带特性 5，一个目标携带道具 252。测试关注事件来源、是否
	 * 消耗和最终 HP，而不手写 [BattleAbilityEffect.SurviveFatalDamageAtFullHp] 或
	 * [BattleItemEffect.SurviveFatalDamageAtFullHp]，确保真实资料链路没有退回旧共享场景。
	 */
	@Test
	fun `assembled runtime snapshot applies database full hp survival effects in engine turn`() {
		val abilitySurvival = resolvedFatalSurvivalTurn(
			target = participant("b-1", creatureId = 4, level = 1, skillIds = listOf(1), abilityId = 5),
		)
		val abilityEvent = abilitySurvival.events.filterIsInstance<BattleEvent.FatalDamageSurvived>().single()

		assertThat(abilitySurvival.participant("b-1")?.currentHp).isEqualTo(1)
		assertThat(abilityEvent.source).isEqualTo(BattleFatalDamageSurvivalSource.ABILITY)
		assertThat(abilityEvent.sourceId).isEqualTo(5)
		assertThat(abilityEvent.consumed).isFalse()

		val itemSurvival = resolvedFatalSurvivalTurn(
			target = participant("b-1", creatureId = 4, level = 1, skillIds = listOf(1), abilityId = 1, itemId = 252),
		)
		val itemEvent = itemSurvival.events.filterIsInstance<BattleEvent.FatalDamageSurvived>().single()

		assertThat(itemSurvival.participant("b-1")?.currentHp).isEqualTo(1)
		assertThat(itemSurvival.participant("b-1")?.itemId).isNull()
		assertThat(itemSurvival.participant("b-1")?.itemEffects).isEmpty()
		assertThat(itemEvent.source).isEqualTo(BattleFatalDamageSurvivalSource.ITEM)
		assertThat(itemEvent.sourceId).isEqualTo(252)
		assertThat(itemEvent.consumed).isTrue()
	}

	/**
	 * 验证出场环境特性不是只被装配成列表，而是在引擎启动阶段真实写入天气和场地。
	 *
	 * 本用例同时放置一个天气特性和一个场地特性，原因是二者都在 `start` 的出场钩子触发，但写入不同的环境槽。
	 * 如果后续资料读取只保留其中一种，或者特性 ID 到结构化效果的映射发生漂移，事件流会直接缺少对应的
	 * [BattleEvent.WeatherStarted] 或 [BattleEvent.TerrainStarted]。这里不手写特性效果，全部通过数据库中的
	 * ability policy 装配，固定的是现代规则默认 5 回合持续时间。
	 */
	@Test
	fun `assembled runtime snapshot applies database switch in weather and terrain abilities`() {
		val initialState = assembledState(
			firstSideFirst = participant("a-1", creatureId = 7, level = 50, skillIds = listOf(1), abilityId = 2),
			secondSideFirst = participant("b-1", creatureId = 4, level = 50, skillIds = listOf(1), abilityId = 226),
		)
		val started = BattleEngine().start(initialState)
		val weather = started.events.filterIsInstance<BattleEvent.WeatherStarted>().single()
		val terrain = started.events.filterIsInstance<BattleEvent.TerrainStarted>().single()

		assertThat(weather.actorId).isEqualTo("a-1")
		assertThat(weather.weather).isEqualTo(BattleWeather.RAIN)
		assertThat(weather.turnsRemaining).isEqualTo(5)
		assertThat(terrain.actorId).isEqualTo("b-1")
		assertThat(terrain.terrain).isEqualTo(BattleTerrain.ELECTRIC)
		assertThat(terrain.turnsRemaining).isEqualTo(5)
	}

	/**
	 * 验证数据库中的属性吸收特性会在命中前门禁阶段中断技能。
	 *
	 * 技能 85 的电属性、特性 10 的电属性吸收回复都来自资料表；测试只把二者放在同一击里。期望结果是技能已经
	 * 被使用，但不会进入普通伤害事件，也不会继续触发技能自带的后续状态概率。因为目标满 HP，所以回复量可以是
	 * 0；关键事实是 [BattleEvent.SkillAbsorbedByAbility] 记录了实际拦截该技能的特性和属性。
	 */
	@Test
	fun `assembled runtime snapshot applies database element absorb ability in engine turn`() {
		val resolved = resolvedAssembledTurn(
			firstSideFirst = participant("a-1", creatureId = 25, level = 50, skillIds = listOf(85)),
			secondSideFirst = participant("b-1", creatureId = 7, level = 50, skillIds = listOf(1), abilityId = 10),
			action = BattleAction.UseSkill(actorId = "a-1", skillId = 85, targetActorId = "b-1"),
		)
		val absorbed = resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single()

		assertThat(absorbed.actorId).isEqualTo("a-1")
		assertThat(absorbed.targetActorId).isEqualTo("b-1")
		assertThat(absorbed.abilityHolderActorId).isEqualTo("b-1")
		assertThat(absorbed.abilityId).isEqualTo(10)
		assertThat(absorbed.elementId).isEqualTo(13)
		assertThat(resolved.events.filterIsInstance<BattleEvent.DamageApplied>()).isEmpty()
	}

	/**
	 * 验证数据库中的主要异常治愈道具会在状态已经写入后立刻清除并消费。
	 *
	 * 技能 261 是资料中的灼伤变化技能，道具 129 是只解除灼伤的一次性携带道具。现代规则要求这类道具不拦截状态
	 * 赋予本身，而是在 [BattleEvent.StatusApplied] 后追加 [BattleEvent.StatusCleared]；这个顺序对 replay 和
	 * 规则调试很重要，因此测试显式比较两个事件在同一事件流中的先后位置。
	 */
	@Test
	fun `assembled runtime snapshot applies database major status cure item in engine turn`() {
		val resolved = resolvedAssembledTurn(
			firstSideFirst = participant("a-1", creatureId = 4, level = 50, skillIds = listOf(261)),
			secondSideFirst = participant("b-1", creatureId = 7, level = 50, itemId = 129, skillIds = listOf(1)),
			action = BattleAction.UseSkill(actorId = "a-1", skillId = 261, targetActorId = "b-1"),
			randomValues = listOf(0),
		)
		val applied = resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single()
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()

		assertThat(applied.targetActorId).isEqualTo("b-1")
		assertThat(applied.status).isEqualTo(BattleMajorStatus.BURN)
		assertThat(cleared.actorId).isEqualTo("b-1")
		assertThat(cleared.status).isEqualTo(BattleMajorStatus.BURN)
		assertThat(resolved.events.indexOf(applied)).isLessThan(resolved.events.indexOf(cleared))
		assertThat(resolved.participant("b-1")?.majorStatus).isNull()
		assertThat(resolved.participant("b-1")?.itemId).isNull()
		assertThat(resolved.participant("b-1")?.itemEffects).isEmpty()
	}

	/**
	 * 验证基础技能优先度和阻止先制技能的特性能跨资料模块组合生效。
	 *
	 * 技能 98 没有额外 battle rule 时仍从 `game_skill.priority` 获得先制优先度；特性 214 则来自 battle ability
	 * policy。测试把这两份资料装成真实成员后结算一回合，确保先制技能在命中、伤害、附加效果前被特性拥有者阻挡。
	 */
	@Test
	fun `assembled runtime snapshot applies database priority immunity ability in engine turn`() {
		val resolved = resolvedAssembledTurn(
			firstSideFirst = participant("a-1", creatureId = 25, level = 50, skillIds = listOf(98)),
			secondSideFirst = participant("b-1", creatureId = 4, level = 50, skillIds = listOf(1), abilityId = 214),
			action = BattleAction.UseSkill(actorId = "a-1", skillId = 98, targetActorId = "b-1"),
		)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().single()

		assertThat(blocked.actorId).isEqualTo("a-1")
		assertThat(blocked.targetActorId).isEqualTo("b-1")
		assertThat(blocked.abilityHolderActorId).isEqualTo("b-1")
		assertThat(blocked.abilityId).isEqualTo(214)
		assertThat(resolved.events.filterIsInstance<BattleEvent.DamageApplied>()).isEmpty()
	}

	/**
	 * 验证抗性减伤道具从数据库装配后能进入真实伤害公式链。
	 *
	 * 目标携带道具 161，资料声明它在本体受到火属性且效果绝佳的技能伤害时消费并乘以 0.5；技能 52 和目标双属性
	 * 都来自 game data。测试不复刻完整伤害公式，只确认减伤事件出现、道具被消费、随后仍有实际伤害事件，覆盖
	 * “资料装配 -> 伤害前道具 hook -> HP 写入”的跨模块链路。
	 */
	@Test
	fun `assembled runtime snapshot applies database element damage reduction item in engine turn`() {
		val resolved = resolvedAssembledTurn(
			firstSideFirst = participant("a-1", creatureId = 4, level = 50, skillIds = listOf(52)),
			secondSideFirst = participant("b-1", creatureId = 1, level = 50, itemId = 161, skillIds = listOf(1)),
			action = BattleAction.UseSkill(actorId = "a-1", skillId = 52, targetActorId = "b-1"),
			randomValues = listOf(1, 15, 99),
		)
		val reduced = resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>().single()
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		assertThat(reduced.actorId).isEqualTo("a-1")
		assertThat(reduced.targetActorId).isEqualTo("b-1")
		assertThat(reduced.skillId).isEqualTo(52)
		assertThat(reduced.itemId).isEqualTo(161)
		assertThat(reduced.elementId).isEqualTo(10)
		assertThat(reduced.multiplier).isEqualTo(0.5)
		assertThat(reduced.consumed).isTrue()
		assertThat(damage.targetActorId).isEqualTo("b-1")
		assertThat(damage.effectiveness).isGreaterThan(1.0)
		assertThat(resolved.participant("b-1")?.itemId).isNull()
		assertThat(resolved.participant("b-1")?.itemEffects).isEmpty()
	}

	/**
	 * 验证数据库属性克制表不只是被装进快照，也确实驱动 battle-engine 的真实伤害事件。
	 *
	 * 这里不重复实现伤害公式，只断言事件中的 `effectiveness` 和是否扣血：
	 * - 水属性技能打火属性目标应为 2 倍。
	 * - 一般属性技能打幽灵属性目标应为 0 倍且不扣 HP。
	 * - 草属性技能打岩石/地面双属性目标应为 4 倍。
	 *
	 * 如果以后 `game_element_damage_relation` 的读取方向写反，或运行时快照退回全中性表，这个测试会直接失败。
	 */
	@Test
	fun `assembled runtime snapshot applies database element chart during real damage calculation`() {
		val waterAgainstFire = assembledDamageEvent(
			skillId = 55,
			attackerCreatureId = 7,
			targetCreatureId = 4,
			randomValues = listOf(1, 15),
		)
		assertThat(waterAgainstFire.effectiveness).isEqualTo(2.0)
		assertThat(waterAgainstFire.amount).isGreaterThan(0)

		val normalAgainstGhost = assembledDamageEvent(
			skillId = 33,
			attackerCreatureId = 1,
			targetCreatureId = 92,
			randomValues = emptyList(),
		)
		assertThat(normalAgainstGhost.effectiveness).isEqualTo(0.0)
		assertThat(normalAgainstGhost.amount).isEqualTo(0)

		val grassAgainstRockGround = assembledDamageEvent(
			skillId = 22,
			attackerCreatureId = 1,
			targetCreatureId = 74,
			randomValues = listOf(1, 15),
		)
		assertThat(grassAgainstRockGround.effectiveness).isEqualTo(4.0)
		assertThat(grassAgainstRockGround.amount).isGreaterThan(0)
	}

	@Test
	fun `action validation rejects unsupported action type`() {
		val exception = assertThrows<ApiException> {
			service.validateActions(
				actionValidationRequest(
					actions = listOf(
						BattleActionRequest(
							type = "USE_ITEM",
							actorId = "a-1",
							targetActorId = "a-1",
						),
					),
				),
			)
		}

		assertThat(exception.field).isEqualTo("type")
		assertThat(exception.message).isEqualTo("type 只支持 USE_SKILL 或 SWITCH_PARTICIPANT")
	}

	private fun assembledDamageEvent(
		skillId: Long,
		attackerCreatureId: Long,
		targetCreatureId: Long,
		randomValues: List<Int>,
	): BattleEvent.DamageApplied {
		val initialState = assembledState(
			firstSideFirst = participant("a-1", creatureId = attackerCreatureId, level = 50, skillIds = listOf(skillId)),
			secondSideFirst = participant("b-1", creatureId = targetCreatureId, level = 50, skillIds = listOf(1)),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(initialState),
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = skillId, targetActorId = "b-1")),
			ScriptedBattleRandom(randomValues),
		)
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
	}

	private fun resolvedFatalSurvivalTurn(target: BattlePreparationParticipantRequest) =
		BattleEngine().let { engine ->
			val initialState = assembledState(
				firstSideFirst = participant("a-1", creatureId = 150, level = 50, skillIds = listOf(63)),
				secondSideFirst = target,
			)
			engine.resolveTurn(
				engine.start(initialState),
				listOf(BattleAction.UseSkill(actorId = "a-1", skillId = 63, targetActorId = "b-1")),
				ScriptedBattleRandom(listOf(0, 1, 15)),
			)
		}

	private fun resolvedAssembledTurn(
		firstSideFirst: BattlePreparationParticipantRequest,
		secondSideFirst: BattlePreparationParticipantRequest,
		action: BattleAction.UseSkill,
		randomValues: List<Int> = emptyList(),
	) = BattleEngine().let { engine ->
		engine.resolveTurn(
			engine.start(
				assembledState(
					firstSideFirst = firstSideFirst,
					secondSideFirst = secondSideFirst,
				),
			),
			listOf(action),
			ScriptedBattleRandom(randomValues),
		)
	}

	/**
	 * 为跨模块运行时测试装配一场固定双方结构的双打快照。
	 *
	 * 每个测试只替换双方首发一号位，二号位保留合法占位成员，目的是走真实 `official-double` 赛制和
	 * [BattleRuntimeSnapshotService.assembleInitialState]，同时避免在每个测试里重复维护四个 active 成员。
	 */
	private fun assembledState(
		firstSideFirst: BattlePreparationParticipantRequest,
		secondSideFirst: BattlePreparationParticipantRequest,
	) = service.assembleInitialState(
		BattlePreparationValidationRequest(
			formatCode = "official-double",
			sides = listOf(
				BattlePreparationSideRequest(
					sideId = "side-a",
					activeActorIds = listOf("a-1", "a-2"),
					participants = listOf(
						firstSideFirst,
						participant("a-2", creatureId = 2, level = 50, skillIds = listOf(1)),
					),
				),
				BattlePreparationSideRequest(
					sideId = "side-b",
					activeActorIds = listOf("b-1", "b-2"),
					participants = listOf(
						secondSideFirst,
						participant("b-2", creatureId = 3, level = 50, skillIds = listOf(1)),
					),
				),
			),
		),
	)

	private fun BattleRuntimeSnapshotService.switchInWeatherByAbilityId(abilityId: Long): BattleWeather =
		abilityEffectsByAbilityId(abilityId)
			.filterIsInstance<BattleAbilityEffect.SwitchInWeatherChange>()
			.single()
			.weather

	private fun BattleRuntimeSnapshotService.switchInTerrainByAbilityId(abilityId: Long): BattleTerrain =
		abilityEffectsByAbilityId(abilityId)
			.filterIsInstance<BattleAbilityEffect.SwitchInTerrainChange>()
			.single()
			.terrain

	private fun BattleRuntimeSnapshotService.weatherSpeedByAbilityId(abilityId: Long): Pair<BattleWeather, Double> =
		abilityEffectsByAbilityId(abilityId)
			.filterIsInstance<BattleAbilityEffect.WeatherSpeedMultiplier>()
			.single()
			.let { it.weather to it.multiplier }

	private fun BattleRuntimeSnapshotService.terrainSpeedByAbilityId(abilityId: Long): Pair<BattleTerrain, Double> =
		abilityEffectsByAbilityId(abilityId)
			.filterIsInstance<BattleAbilityEffect.TerrainSpeedMultiplier>()
			.single()
			.let { it.terrain to it.multiplier }

	private fun BattleRuntimeSnapshotService.weatherHealByAbilityId(abilityId: Long): Pair<Set<BattleWeather>, Int> =
		abilityEffectsByAbilityId(abilityId)
			.filterIsInstance<BattleAbilityEffect.WeatherEndTurnHeal>()
			.single()
			.let { it.weathers to it.healDenominator }

	private fun enabledIds(tableName: String, idColumn: String): List<Long> =
		jdbcTemplate.query(
			"""
			select distinct $idColumn
			from $tableName
			where enabled = true
			order by $idColumn
			""".trimIndent(),
			{ rs, _ -> rs.getLong(idColumn) },
		)

	private fun enabledPolicies(tableName: String, policyColumn: String): List<String> =
		jdbcTemplate.query(
			"""
			select distinct $policyColumn
			from $tableName
			where enabled = true
			order by $policyColumn
			""".trimIndent(),
			{ rs, _ -> rs.getString(policyColumn) },
		)

	private fun elementIdsByCode(): Map<String, Long> =
		jdbcTemplate.query(
			"""
			select code, id
			from game_element
			order by code
			""".trimIndent(),
			{ rs, _ -> rs.getString("code") to rs.getLong("id") },
		).toMap()

	private fun withTemporaryFormatRestriction(
		code: String,
		restrictionType: String,
		restrictionOperator: String,
		operandText: String?,
		operandNumber: Int?,
		block: () -> Unit,
	) {
		jdbcTemplate.update("delete from battle_format_restriction where id = ?", TEMP_FORMAT_RESTRICTION_ID)
		jdbcTemplate.update(
			"""
			insert into battle_format_restriction (
				id, format_id, code, name, restriction_type, restriction_operator, operand_text, operand_number, description, enabled, sort_order
			) values (?, 3, ?, '运行时限制操作数测试', ?, ?, ?, ?, '运行时限制操作数测试', true, 999999)
			""".trimIndent(),
			TEMP_FORMAT_RESTRICTION_ID,
			code,
			restrictionType,
			restrictionOperator,
			operandText,
			operandNumber,
		)
		try {
			block()
		} finally {
			jdbcTemplate.update("delete from battle_format_restriction where id = ?", TEMP_FORMAT_RESTRICTION_ID)
		}
	}

	private fun withTemporaryFormatClause(code: String, block: () -> Unit) {
		jdbcTemplate.update("delete from battle_format_clause_binding where id = ?", TEMP_FORMAT_CLAUSE_BINDING_ID)
		jdbcTemplate.update("delete from battle_format_clause where id = ?", TEMP_FORMAT_CLAUSE_ID)
		jdbcTemplate.update(
			"""
			insert into battle_format_clause (
				id, code, name, clause_type, description, enabled, sort_order
			) values (?, ?, '运行时未知条款测试', 'TEAM', '运行时未知条款测试', true, 999999)
			""".trimIndent(),
			TEMP_FORMAT_CLAUSE_ID,
			code,
		)
		jdbcTemplate.update(
			"""
			insert into battle_format_clause_binding (
				id, format_id, clause_id, required, sort_order
			) values (?, 3, ?, true, 999999)
			""".trimIndent(),
			TEMP_FORMAT_CLAUSE_BINDING_ID,
			TEMP_FORMAT_CLAUSE_ID,
		)
		try {
			block()
		} finally {
			jdbcTemplate.update("delete from battle_format_clause_binding where id = ?", TEMP_FORMAT_CLAUSE_BINDING_ID)
			jdbcTemplate.update("delete from battle_format_clause where id = ?", TEMP_FORMAT_CLAUSE_ID)
		}
	}

	private fun withTemporaryAbilityPolicy(effectPolicy: String, block: () -> Unit) {
		jdbcTemplate.update("delete from battle_ability_rule where id = ?", TEMP_ABILITY_RULE_ID)
		jdbcTemplate.update(
			"""
			insert into battle_ability_rule (
				id, ability_id, trigger_timing, effect_policy, trigger_order, description, enabled, sort_order
			) values (?, 65, 'BEFORE_DAMAGE', ?, 999, '未知特性策略测试', true, 999999)
			""".trimIndent(),
			TEMP_ABILITY_RULE_ID,
			effectPolicy,
		)
		try {
			block()
		} finally {
			jdbcTemplate.update("delete from battle_ability_rule where id = ?", TEMP_ABILITY_RULE_ID)
		}
	}

	private fun withTemporaryItemPolicy(effectPolicy: String, block: () -> Unit) {
		jdbcTemplate.update("delete from battle_item_rule where id = ?", TEMP_ITEM_RULE_ID)
		jdbcTemplate.update(
			"""
			insert into battle_item_rule (
				id, item_id, trigger_timing, effect_policy, consumable, trigger_order, description, enabled, sort_order
			) values (?, 211, 'HELD_END_TURN', ?, false, 999, '未知道具策略测试', true, 999999)
			""".trimIndent(),
			TEMP_ITEM_RULE_ID,
			effectPolicy,
		)
		try {
			block()
		} finally {
			jdbcTemplate.update("delete from battle_item_rule where id = ?", TEMP_ITEM_RULE_ID)
		}
	}

	private fun withTemporarySkillPolicy(effectPolicy: String, block: (Long) -> Unit) {
		insertTemporarySkill()
		jdbcTemplate.update("delete from battle_skill_rule where id = ? or skill_id = ?", TEMP_SKILL_RULE_ID, TEMP_SKILL_ID)
		jdbcTemplate.update(
			"""
			insert into battle_skill_rule (
				id,
				skill_id,
				effect_policy,
				target_policy,
				hit_policy,
				damage_policy,
				makes_contact,
				affected_by_protect,
				sound_based,
				powder_based,
				punch_based,
				slicing_based,
				description,
				enabled,
				sort_order
			) values (
				?,
				?,
				?,
				'selected-target',
				'standard-hit',
				'standard-damage',
				false,
				true,
				false,
				false,
				false,
				false,
				'未知技能策略测试',
				true,
				999999
			)
			""".trimIndent(),
			TEMP_SKILL_RULE_ID,
			TEMP_SKILL_ID,
			effectPolicy,
		)
		try {
			block(TEMP_SKILL_ID)
		} finally {
			jdbcTemplate.update("delete from battle_skill_rule where id = ?", TEMP_SKILL_RULE_ID)
			deleteTemporarySkill()
		}
	}

	private fun withTemporarySideFieldEffectPolicy(effectPolicy: String, block: (Long) -> Unit) {
		withTemporarySkillPolicy("side-condition") { skillId ->
			deleteTemporaryFieldEffects()
			insertTemporaryFieldRule(effectPolicy = effectPolicy, effectScope = "SIDE")
			jdbcTemplate.update(
				"""
				insert into battle_skill_field_effect (
					id, skill_rule_id, field_rule_id, target_side, effect_timing, chance_percent, enabled, sort_order
				) values (?, ?, ?, 'USER_SIDE', 'AFTER_HIT', 100, true, 999999)
				""".trimIndent(),
				TEMP_SKILL_FIELD_EFFECT_ID,
				TEMP_SKILL_RULE_ID,
				TEMP_FIELD_RULE_ID,
			)
			try {
				block(skillId)
			} finally {
				deleteTemporaryFieldEffects()
			}
		}
	}

	private fun withTemporaryGlobalFieldEffectPolicy(effectPolicy: String, block: (Long) -> Unit) {
		withTemporarySkillPolicy("field-condition") { skillId ->
			deleteTemporaryFieldEffects()
			insertTemporaryFieldRule(effectPolicy = effectPolicy, effectScope = "FIELD")
			jdbcTemplate.update(
				"""
				insert into battle_skill_global_field_effect (
					id, skill_rule_id, field_rule_id, effect_timing, chance_percent, enabled, sort_order
				) values (?, ?, ?, 'AFTER_HIT', 100, true, 999999)
				""".trimIndent(),
				TEMP_SKILL_GLOBAL_FIELD_EFFECT_ID,
				TEMP_SKILL_RULE_ID,
				TEMP_FIELD_RULE_ID,
			)
			try {
				block(skillId)
			} finally {
				deleteTemporaryFieldEffects()
			}
		}
	}

	private fun insertTemporaryFieldRule(effectPolicy: String, effectScope: String) {
		jdbcTemplate.update(
			"""
			insert into battle_field_rule (
				id, code, name, effect_scope, effect_policy, min_turns, max_turns, max_layers, description, enabled, sort_order
			) values (?, 'runtime-unknown-field-policy-test', '运行时未知场地策略测试', ?, ?, 5, 5, null, '未知场地策略测试', true, 999999)
			""".trimIndent(),
			TEMP_FIELD_RULE_ID,
			effectScope,
			effectPolicy,
		)
	}

	private fun deleteTemporaryFieldEffects() {
		jdbcTemplate.update("delete from battle_skill_field_effect where id = ?", TEMP_SKILL_FIELD_EFFECT_ID)
		jdbcTemplate.update("delete from battle_skill_global_field_effect where id = ?", TEMP_SKILL_GLOBAL_FIELD_EFFECT_ID)
		jdbcTemplate.update("delete from battle_field_rule where id = ?", TEMP_FIELD_RULE_ID)
	}

	private fun withTemporaryStatusEffectTargetScope(targetScope: String, block: (Long) -> Unit) {
		withTemporarySkillPolicy("status-effect") { skillId ->
			deleteTemporarySkillChildEffects()
			jdbcTemplate.update(
				"""
				insert into battle_skill_status_effect (
					id, skill_rule_id, status_rule_id, target_scope, effect_timing, chance_percent, enabled, sort_order
				) values (?, ?, 1, ?, 'AFTER_HIT', 100, true, 999999)
				""".trimIndent(),
				TEMP_SKILL_STATUS_EFFECT_ID,
				TEMP_SKILL_RULE_ID,
				targetScope,
			)
			try {
				block(skillId)
			} finally {
				deleteTemporarySkillChildEffects()
			}
		}
	}

	private fun withTemporaryStatusKind(statusKind: String, block: (Long) -> Unit) {
		withTemporarySkillPolicy("status-effect") { skillId ->
			deleteTemporaryStatusRule()
			jdbcTemplate.update(
				"""
				insert into battle_status_rule (
					id, code, name, status_kind, effect_policy, min_turns, max_turns, description, enabled, sort_order
				) values (?, 'runtime-unknown-status-kind-test', '运行时未知状态族测试', ?, 'runtime-unknown-status-kind', null, null, '未知状态族测试', true, 999999)
				""".trimIndent(),
				TEMP_STATUS_RULE_ID,
				statusKind,
			)
			jdbcTemplate.update(
				"""
				insert into battle_skill_status_effect (
					id, skill_rule_id, status_rule_id, target_scope, effect_timing, chance_percent, enabled, sort_order
				) values (?, ?, ?, 'TARGET', 'AFTER_HIT', 100, true, 999999)
				""".trimIndent(),
				TEMP_SKILL_STATUS_EFFECT_ID,
				TEMP_SKILL_RULE_ID,
				TEMP_STATUS_RULE_ID,
			)
			try {
				block(skillId)
			} finally {
				deleteTemporaryStatusRule()
			}
		}
	}

	private fun withTemporaryStatStageEffectTargetScope(targetScope: String, block: (Long) -> Unit) {
		withTemporarySkillPolicy("stat-stage-change") { skillId ->
			deleteTemporarySkillChildEffects()
			jdbcTemplate.update(
				"""
				insert into battle_skill_stat_stage_effect (
					id, skill_rule_id, stat_id, target_scope, effect_timing, stage_delta, chance_percent, enabled, sort_order
				) values (?, ?, 2, ?, 'AFTER_HIT', 1, 100, true, 999999)
				""".trimIndent(),
				TEMP_SKILL_STAT_STAGE_EFFECT_ID,
				TEMP_SKILL_RULE_ID,
				targetScope,
			)
			try {
				block(skillId)
			} finally {
				deleteTemporarySkillChildEffects()
			}
		}
	}

	private fun deleteTemporarySkillChildEffects() {
		jdbcTemplate.update("delete from battle_skill_status_effect where id = ?", TEMP_SKILL_STATUS_EFFECT_ID)
		jdbcTemplate.update("delete from battle_skill_stat_stage_effect where id = ?", TEMP_SKILL_STAT_STAGE_EFFECT_ID)
	}

	private fun deleteTemporaryStatusRule() {
		deleteTemporarySkillChildEffects()
		jdbcTemplate.update("delete from battle_status_rule where id = ?", TEMP_STATUS_RULE_ID)
	}

	private fun withTemporarySkillWithoutRule(block: (Long) -> Unit) {
		insertTemporarySkill()
		try {
			block(TEMP_SKILL_ID)
		} finally {
			deleteTemporarySkill()
		}
	}

	private fun insertTemporarySkill() {
		jdbcTemplate.update("delete from battle_skill_rule where skill_id = ?", TEMP_SKILL_ID)
		deleteTemporarySkill()
		jdbcTemplate.update(
			"""
			insert into game_skill (
				id,
				code,
				name,
				element_id,
				damage_class_id,
				accuracy,
				power,
				pp,
				priority,
				effect_chance,
				enabled
			) values (
				?,
				'runtime-unknown-policy-test',
				'运行时未知策略测试',
				1,
				3,
				100,
				40,
				5,
				0,
				null,
				true
			)
			""".trimIndent(),
			TEMP_SKILL_ID,
		)
	}

	private fun deleteTemporarySkill() {
		jdbcTemplate.update("delete from game_skill where id = ?", TEMP_SKILL_ID)
	}

	private fun preparationRequest(firstParticipant: BattlePreparationParticipantRequest): BattlePreparationValidationRequest =
		BattlePreparationValidationRequest(
			formatCode = "official-double",
			sides = listOf(
				BattlePreparationSideRequest(
					sideId = "side-a",
					activeActorIds = listOf("a-1", "a-2"),
					participants = listOf(
						firstParticipant,
						participant("a-2", creatureId = 2, level = 50, itemId = 11),
					),
				),
				BattlePreparationSideRequest(
					sideId = "side-b",
					activeActorIds = listOf("b-1", "b-2"),
					participants = listOf(
						participant("b-1", creatureId = 3, level = 50, itemId = 12),
						participant("b-2", creatureId = 4, level = 50, itemId = 13),
					),
				),
			),
		)

	private fun actionValidationRequest(actions: List<BattleActionRequest>): BattleActionValidationRequest =
		BattleActionValidationRequest(
			formatCode = "official-double",
			sides = validDoubleSides(),
			actions = actions,
		)

	private fun validDoubleSides(): List<BattlePreparationSideRequest> =
		listOf(
			BattlePreparationSideRequest(
				sideId = "side-a",
				activeActorIds = listOf("a-1", "a-2"),
				participants = listOf(
					participant("a-1", creatureId = 1, level = 50, itemId = 10),
					participant("a-2", creatureId = 2, level = 50, itemId = 11),
				),
			),
			BattlePreparationSideRequest(
				sideId = "side-b",
				activeActorIds = listOf("b-1", "b-2"),
				participants = listOf(
					participant("b-1", creatureId = 3, level = 50, itemId = 12),
					participant("b-2", creatureId = 4, level = 50, itemId = 13),
				),
			),
		)

	private fun validSingleSides(): List<BattlePreparationSideRequest> =
		listOf(
			BattlePreparationSideRequest(
				sideId = "side-a",
				activeActorIds = listOf("a-1"),
				participants = listOf(
					participant("a-1", creatureId = 1, level = 50, itemId = 10),
					participant("a-2", creatureId = 2, level = 50, itemId = 11),
				),
			),
			BattlePreparationSideRequest(
				sideId = "side-b",
				activeActorIds = listOf("b-1"),
				participants = listOf(
					participant("b-1", creatureId = 3, level = 50, itemId = 12),
					participant("b-2", creatureId = 4, level = 50, itemId = 13),
				),
			),
		)

	private fun invalidLevelDoubleSides(): List<BattlePreparationSideRequest> =
		validDoubleSides().map { side ->
			if (side.sideId == "side-a") {
				side.copy(
					participants = side.participants.map { participant ->
						if (participant.actorId == "a-1") {
							participant.copy(level = 60)
						} else {
							participant
						}
					},
				)
			} else {
				side
			}
		}

	private fun duplicateActorDoubleSides(): List<BattlePreparationSideRequest> =
		validDoubleSides().map { side ->
			if (side.sideId == "side-b") {
				side.copy(
					activeActorIds = listOf("a-1", "b-2"),
					participants = side.participants.map { participant ->
						if (participant.actorId == "b-1") {
							participant.copy(actorId = "a-1")
						} else {
							participant
						}
					},
				)
			} else {
				side
			}
		}

	private fun activeCountDriftDoubleSides(): List<BattlePreparationSideRequest> =
		validDoubleSides().map { side ->
			if (side.sideId == "side-a") {
				side.copy(activeActorIds = listOf("a-1"))
			} else {
				side
			}
		}

	private fun participant(
		actorId: String,
		creatureId: Long,
		level: Int,
		itemId: Long? = null,
		skillIds: List<Long> = listOf(1, 2, 3, 4),
		abilityId: Long? = 1,
		individualValues: Map<String, Int> = emptyMap(),
		effortValues: Map<String, Int> = emptyMap(),
		natureIncreasedStat: String? = null,
		natureDecreasedStat: String? = null,
	): BattlePreparationParticipantRequest =
		BattlePreparationParticipantRequest(
			actorId = actorId,
			creatureId = creatureId,
			level = level,
			skillIds = skillIds,
			abilityId = abilityId,
			itemId = itemId,
			individualValues = individualValues,
			effortValues = effortValues,
			natureIncreasedStat = natureIncreasedStat,
			natureDecreasedStat = natureDecreasedStat,
		)

	private companion object {
		private const val TEMP_FORMAT_RESTRICTION_ID = 9_900_000L
		private const val TEMP_ABILITY_RULE_ID = 9_900_001L
		private const val TEMP_ITEM_RULE_ID = 9_900_002L
		private const val TEMP_SKILL_RULE_ID = 9_900_003L
		private const val TEMP_SKILL_ID = 9_900_004L
		private const val TEMP_FIELD_RULE_ID = 9_900_005L
		private const val TEMP_SKILL_FIELD_EFFECT_ID = 9_900_006L
		private const val TEMP_SKILL_GLOBAL_FIELD_EFFECT_ID = 9_900_007L
		private const val TEMP_SKILL_STATUS_EFFECT_ID = 9_900_008L
		private const val TEMP_SKILL_STAT_STAGE_EFFECT_ID = 9_900_009L
		private const val TEMP_STATUS_RULE_ID = 9_900_010L
		private const val TEMP_FORMAT_CLAUSE_ID = 9_900_011L
		private const val TEMP_FORMAT_CLAUSE_BINDING_ID = 9_900_012L
	}
}
