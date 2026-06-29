package io.github.lishangbu.battlerules

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
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
			listOf(14, 38, 39, 45, 63, 71, 76, 77, 78, 79, 85, 87, 94, 95, 103, 105, 113, 115, 147, 164, 184, 191, 235, 240, 261, 311, 319, 347, 349, 366, 390, 433, 446, 504, 526, 564, 568, 570, 577, 580, 604, 883, 694),
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
		itemId: Long,
	): BattlePreparationParticipantRequest =
		BattlePreparationParticipantRequest(
			actorId = actorId,
			creatureId = creatureId,
			level = level,
			skillIds = listOf(1, 2, 3, 4),
			abilityId = 1,
			itemId = itemId,
		)
}
