package io.github.lishangbu.battlerules

import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationParticipantRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationSideRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.service.BattleRuntimeSnapshotService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
	classes = [BattleRulesTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"jimmer.dialect=org.babyfish.jimmer.sql.dialect.PostgresDialect",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=2",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [BattleRulesPostgresTestContainer::class])
/**
 * 验证战斗规则资料能装配为引擎运行时快照。
 */
class BattleRuntimeSnapshotServiceTests(
	@Autowired private val service: BattleRuntimeSnapshotService,
) {
	@Test
	fun `official double format assembles engine runtime snapshot`() {
		val snapshot = service.getByFormatCode("official-double")

		assertThat(snapshot.format.code).isEqualTo("official-double")
		assertThat(snapshot.format.mode).isEqualTo(BattleMode.DOUBLE)
		assertThat(snapshot.format.activeParticipantsPerSide).isEqualTo(2)
		assertThat(snapshot.format.teamSize).isEqualTo(6)
		assertThat(snapshot.format.defaultLevel).isEqualTo(50)
		assertThat(snapshot.rules.maxParticipantLevel).isEqualTo(50)
		assertThat(snapshot.rules.uniqueCreatureRequired).isTrue()
		assertThat(snapshot.rules.uniqueItemRequired).isTrue()
		assertThat(snapshot.rules.bannedCreatureIds).isEmpty()
		assertThat(snapshot.rules.bannedSkillIds).isEmpty()
		assertThat(snapshot.rules.darkElementId).isEqualTo(17)
		assertThat(snapshot.rules.electricElementId).isEqualTo(13)
		assertThat(snapshot.rules.fireElementId).isEqualTo(10)
		assertThat(snapshot.rules.grassElementId).isEqualTo(12)
		assertThat(snapshot.rules.groundElementId).isEqualTo(5)
		assertThat(snapshot.rules.iceElementId).isEqualTo(15)
		assertThat(snapshot.rules.poisonElementId).isEqualTo(4)
		assertThat(snapshot.rules.rockElementId).isEqualTo(6)
		assertThat(snapshot.rules.steelElementId).isEqualTo(9)
		assertThat(snapshot.rules.waterElementId).isEqualTo(11)
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
	fun `skill slot assembly includes explicit battle rule effects`() {
		val slots = service.skillSlotsBySkillIds(
				listOf(5, 14, 15, 20, 38, 39, 45, 49, 50, 63, 69, 71, 76, 77, 78, 79, 82, 85, 87, 94, 95, 101, 103, 105, 113, 115, 147, 162, 163, 164, 184, 191, 235, 240, 259, 261, 269, 283, 311, 319, 347, 349, 366, 390, 400, 427, 433, 446, 504, 515, 526, 564, 568, 570, 577, 580, 604, 717, 877, 883, 895, 694),
		)
			.associateBy { it.skillId }

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
	 * 空列表和非正数 ID 属于请求形状错误；不存在的正数 ID 则说明资料库没有对应基础技能。三类问题都不应该落到
	 * battle-engine 里再表现为“技能槽缺失”，否则准备校验和真实开战会得到不同错误口径。
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
		assertThat(profile.elementIds).containsExactlyInAnyOrder(12L, 4L)
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
		val initialState = service.assembleInitialState(
			BattlePreparationValidationRequest(
				formatCode = "official-double",
				sides = listOf(
					BattlePreparationSideRequest(
						sideId = "side-a",
						activeActorIds = listOf("a-1", "a-2"),
						participants = listOf(
							participant("a-1", creatureId = attackerCreatureId, level = 50, skillIds = listOf(skillId)),
							participant("a-2", creatureId = 2, level = 50, skillIds = listOf(1)),
						),
					),
					BattlePreparationSideRequest(
						sideId = "side-b",
						activeActorIds = listOf("b-1", "b-2"),
						participants = listOf(
							participant("b-1", creatureId = targetCreatureId, level = 50, skillIds = listOf(1)),
							participant("b-2", creatureId = 3, level = 50, skillIds = listOf(1)),
						),
					),
				),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(initialState),
			listOf(BattleAction.UseSkill(actorId = "a-1", skillId = skillId, targetActorId = "b-1")),
			ScriptedBattleRandom(randomValues),
		)
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
	}

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

	private fun participant(
		actorId: String,
		creatureId: Long,
		level: Int,
		itemId: Long? = null,
		skillIds: List<Long> = listOf(1, 2, 3, 4),
		abilityId: Long? = 1,
	): BattlePreparationParticipantRequest =
		BattlePreparationParticipantRequest(
			actorId = actorId,
			creatureId = creatureId,
			level = level,
			skillIds = skillIds,
			abilityId = abilityId,
			itemId = itemId,
		)
}
