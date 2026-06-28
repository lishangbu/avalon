package io.github.lishangbu.battlerules

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleWeather
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
		val slots = service.skillSlotsBySkillIds(listOf(45, 76, 85, 87, 94, 113, 115, 311, 366, 694))
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

		val solarBeam = slots.getValue(76)
		assertThat(solarBeam.powerMultipliersByWeather[BattleWeather.RAIN]).isEqualTo(0.5)
		assertThat(solarBeam.powerMultipliersByWeather[BattleWeather.SANDSTORM]).isEqualTo(0.5)
		assertThat(solarBeam.powerMultipliersByWeather[BattleWeather.SNOW]).isEqualTo(0.5)

		val thunderShock = slots.getValue(85)
		assertThat(thunderShock.statusApplications)
			.anySatisfy {
				assertThat(it.status).isEqualTo(BattleMajorStatus.PARALYSIS)
				assertThat(it.chancePercent).isEqualTo(10)
			}

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

		val contactParalysis = service.abilityEffectsByAbilityId(9)
			.filterIsInstance<BattleAbilityEffect.ContactStatusOnAttacker>()
			.single()
		assertThat(contactParalysis.status).isEqualTo(BattleMajorStatus.PARALYSIS)
		assertThat(contactParalysis.chancePercent).isEqualTo(30)
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
