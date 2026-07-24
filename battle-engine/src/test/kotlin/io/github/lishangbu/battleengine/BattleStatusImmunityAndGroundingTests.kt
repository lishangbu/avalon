package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证主要异常状态免疫和接地判定。
 *
 * 场景类型：状态附加前置条件 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，部分属性天然免疫对应主要异常状态；
 * 电气场地、薄雾场地和青草场地只影响当前上场且接地的成员。
 * 验证重点：被免疫的状态不会消耗状态私有随机数，非接地成员不会被场地错误影响。
 */
class BattleStatusImmunityAndGroundingTests {
	private val engine = BattleEngine()

	@Test
	fun `element immunities block matching major statuses`() {
		val scenario = publicBattleRuleScenario(
			name = "element-immunities-block-major-statuses",
			inputSummary = "分别尝试给火、电、毒、钢、冰属性目标附加其天然免疫的主要异常状态。",
			expectedSummary = "目标不会获得对应状态，事件流以属性免疫作为阻止原因。",
		)
		val cases = listOf(
			StatusImmunityCase("fire-target", BattleMajorStatus.BURN, FIRE_ELEMENT_ID),
			StatusImmunityCase("electric-target", BattleMajorStatus.PARALYSIS, ELECTRIC_ELEMENT_ID),
			StatusImmunityCase("poison-target", BattleMajorStatus.POISON, POISON_ELEMENT_ID),
			StatusImmunityCase("steel-target", BattleMajorStatus.BAD_POISON, STEEL_ELEMENT_ID),
			StatusImmunityCase("ice-target", BattleMajorStatus.FREEZE, ICE_ELEMENT_ID),
		)

		scenario.assertNamed("element-immunities-block-major-statuses")
		cases.forEach { case ->
			val statusSkill = statusSkill(case.status)
			val state = engine.start(
				initialState(
					first = participant("status-user-${case.actorId}", speed = 100, skill = statusSkill),
					second = participant(case.actorId, speed = 50, elementId = case.elementId),
				),
			)

			val resolved = engine.resolveTurn(
				state,
				listOf(BattleAction.UseSkill("status-user-${case.actorId}", skillId = 1, targetActorId = case.actorId)),
				ScriptedBattleRandom(emptyList()),
			)

			assertEquals(null, resolved.participant(case.actorId)?.majorStatus)
			assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
			val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
			assertEquals(case.status, blocked.status)
			assertEquals(BattleStatusBlockReason.ELEMENT, blocked.reason)
		}
	}

	@Test
	fun `electric terrain does not block sleep for ungrounded target`() {
		val scenario = publicBattleRuleScenario(
			name = "electric-terrain-does-not-block-sleep-for-ungrounded-target",
			inputSummary = "电气场地存在时，非接地上场目标被睡眠技能命中。",
			expectedSummary = "目标仍会获得睡眠状态，并正常消费睡眠持续随机数。",
		)
		val random = ScriptedBattleRandom(listOf(1))
		val state = engine.start(
			initialState(
				first = participant("sleep-user", speed = 100, skill = statusSkill(BattleMajorStatus.SLEEP)),
				second = participant("ungrounded-target", speed = 50, grounded = false),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sleep-user", skillId = 1, targetActorId = "ungrounded-target")),
			random,
		)

		scenario.assertNamed("electric-terrain-does-not-block-sleep-for-ungrounded-target")
		assertEquals(listOf("sleep duration for 1"), random.consumedReasons())
		assertEquals(BattleMajorStatus.SLEEP, resolved.participant("ungrounded-target")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>())
	}

	@Test
	fun `misty terrain blocks major status for grounded target`() {
		val scenario = publicBattleRuleScenario(
			name = "misty-terrain-blocks-major-status-for-grounded-target",
			inputSummary = "薄雾场地存在时，接地上场目标被灼伤技能命中。",
			expectedSummary = "目标不会获得灼伤状态，事件流以场地作为阻止原因。",
		)
		val state = engine.start(
			initialState(
				first = participant("status-user", speed = 100, skill = statusSkill(BattleMajorStatus.BURN)),
				second = participant("grounded-target", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.MISTY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-user", skillId = 1, targetActorId = "grounded-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("misty-terrain-blocks-major-status-for-grounded-target")
		assertEquals(null, resolved.participant("grounded-target")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
		assertEquals(BattleMajorStatus.BURN, blocked.status)
		assertEquals(BattleStatusBlockReason.TERRAIN, blocked.reason)
	}

	@Test
	fun `powder immunity item blocks powder skill before accuracy and status random`() {
		val powderSleep = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			powderBased = true,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("powder-user", speed = 100, skill = powderSleep),
				second = participant(
					"goggles-target",
					speed = 50,
					itemId = 650,
					itemEffects = listOf(BattleItemEffect.PowderSkillImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("powder-user", skillId = 1, targetActorId = "goggles-target")),
			random,
		)

		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(null, resolved.participant("goggles-target")?.majorStatus)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByItem>().single()
		assertEquals(650, blocked.itemId)
		assertEquals("goggles-target", blocked.targetActorId)
	}

	@Test
	fun `corrosion bypasses poison element status immunity`() {
		val poisonSkill = statusSkill(BattleMajorStatus.POISON)
		val state = engine.start(
			initialState(
				first = participant(
					"corrosion-user",
					speed = 100,
					skill = poisonSkill,
					abilityEffects = listOf(BattleAbilityEffect.PoisonElementStatusBypass()),
				),
				second = participant("poison-target", speed = 50, elementId = POISON_ELEMENT_ID),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("corrosion-user", poisonSkill.skillId, "poison-target")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(BattleMajorStatus.POISON, resolved.participant("poison-target")?.majorStatus)
	}

	@Test
	fun `powder immunity ability blocks powder skills before private random`() {
		val powderSleep = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			powderBased = true,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.SLEEP, BattleEffectTarget.TARGET, 100),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("powder-user", speed = 100, skill = powderSleep),
				second = participant(
					"overcoat-holder",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.PowderSkillImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("powder-user", powderSleep.skillId, "overcoat-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(null, resolved.participant("overcoat-holder")?.majorStatus)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().size)
	}

	@Test
	fun `ignore target ability effects bypasses powder immunity ability`() {
		val powderSleep = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			powderBased = true,
			statusApplications = listOf(
				BattleStatusApplication(BattleMajorStatus.SLEEP, BattleEffectTarget.TARGET, 100),
			),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"mold-breaker-user",
					speed = 100,
					skill = powderSleep,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
				),
				second = participant(
					"overcoat-holder",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.PowderSkillImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("mold-breaker-user", powderSleep.skillId, "overcoat-holder")),
			ScriptedBattleRandom(listOf(1)),
		)

		assertEquals(BattleMajorStatus.SLEEP, resolved.participant("overcoat-holder")?.majorStatus)
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().size)
	}

	@Test
	fun `side status immunity protects an active ally`() {
		val poisonSkill = statusSkill(BattleMajorStatus.POISON)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("status-user", speed = 100, skill = poisonSkill),
				firstB = participant("user-ally", speed = 90),
				secondA = participant(
					"veil-holder",
					speed = 80,
					abilityEffects = listOf(
						BattleAbilityEffect.SideMajorStatusImmunity(setOf(BattleMajorStatus.POISON)),
					),
				),
				secondB = participant("protected-ally", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-user", poisonSkill.skillId, "protected-ally")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(null, resolved.participant("protected-ally")?.majorStatus)
		assertEquals(BattleStatusBlockReason.ABILITY, resolved.events
			.filterIsInstance<BattleEvent.StatusApplicationBlocked>()
			.single().reason)
	}

	@Test
	fun `grass target blocks powder based status skill before status random`() {
		val scenario = publicBattleRuleScenario(
			name = "grass-target-blocks-powder-based-status-skill",
			inputSummary = "草属性目标被粉末类睡眠技能选中。",
			expectedSummary = "技能使用并消耗 PP，但被目标草属性免疫阻挡，不消费睡眠持续随机数。",
		)
		val powderSleep = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			powderBased = true,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("powder-user", speed = 100, skill = powderSleep),
				second = participant("grass-target", speed = 50, elementId = GRASS_ELEMENT_ID),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("powder-user", skillId = 1, targetActorId = "grass-target")),
			random,
		)

		scenario.assertNamed("grass-target-blocks-powder-based-status-skill")
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(34, resolved.participant("powder-user")?.skillSlot(1)?.remainingPp)
		assertEquals(null, resolved.participant("grass-target")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByElement>().single()
		assertEquals(GRASS_ELEMENT_ID, blocked.elementId)
		assertEquals("grass-target", blocked.targetActorId)
	}

	@Test
	fun `ability and item immunities block matching major statuses before private random`() {
		val scenario = publicBattleRuleScenario(
			name = "ability-and-item-immunities-block-matching-major-statuses",
			inputSummary = "目标分别通过特性免疫中毒、通过携带道具免疫睡眠。",
			expectedSummary = "状态不会写入，事件流分别记录特性和道具作为阻止原因，且睡眠持续随机数不会被消费。",
		)
		val cases = listOf(
			StatusEffectImmunityCase(
				actorId = "ability-immune-target",
				status = BattleMajorStatus.POISON,
				target = participant("ability-immune-target", speed = 50).replaceAbilityEffects(
					listOf(
						BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.POISON)),
					),
				),
				expectedReason = BattleStatusBlockReason.ABILITY,
			),
			StatusEffectImmunityCase(
				actorId = "item-immune-target",
				status = BattleMajorStatus.SLEEP,
				target = participant("item-immune-target", speed = 50).replaceItemEffects(
					listOf(
						BattleItemEffect.MajorStatusImmunity(setOf(BattleMajorStatus.SLEEP)),
					),
				),
				expectedReason = BattleStatusBlockReason.ITEM,
			),
		)

		scenario.assertNamed("ability-and-item-immunities-block-matching-major-statuses")
		cases.forEach { case ->
			val random = ScriptedBattleRandom(emptyList())
			val state = engine.start(
				initialState(
					first = participant("status-user-${case.actorId}", speed = 100, skill = statusSkill(case.status)),
					second = case.target,
				),
			)

			val resolved = engine.resolveTurn(
				state,
				listOf(BattleAction.UseSkill("status-user-${case.actorId}", skillId = 1, targetActorId = case.actorId)),
				random,
			)

			assertEquals(emptyList(), random.consumedReasons())
			assertEquals(null, resolved.participant(case.actorId)?.majorStatus)
			assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
			val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
			assertEquals(case.status, blocked.status)
			assertEquals(case.expectedReason, blocked.reason)
		}
	}

	@Test
	fun `existing major status blocks new major status without private random`() {
		val scenario = publicBattleRuleScenario(
			name = "existing-major-status-blocks-new-major-status",
			inputSummary = "目标已经处于麻痹状态，随后被 100% 附加睡眠的变化技能命中。",
			expectedSummary = "目标仍保持原有麻痹状态，不获得睡眠；事件流记录 EXISTING_STATUS 阻止原因，且不消费睡眠持续随机数。",
		)
		val random = ScriptedBattleRandom(emptyList())
		val state = engine.start(
			initialState(
				first = participant("sleep-user", speed = 100, skill = statusSkill(BattleMajorStatus.SLEEP)),
				second = participant("already-statused-target", speed = 50)
					.applyMajorStatus(BattleMajorStatus.PARALYSIS),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sleep-user", skillId = 1, targetActorId = "already-statused-target")),
			random,
		)

		scenario.assertNamed("existing-major-status-blocks-new-major-status")
		assertEquals(emptyList(), random.consumedReasons())
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("already-statused-target")?.majorStatus)
		assertEquals(0, resolved.participant("already-statused-target")?.sleepTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
		assertEquals(BattleMajorStatus.SLEEP, blocked.status)
		assertEquals(BattleStatusBlockReason.EXISTING_STATUS, blocked.reason)
	}

	@Test
	fun `terrain ability and item immunities block confusion before duration random`() {
		val scenario = publicBattleRuleScenario(
			name = "terrain-ability-and-item-immunities-block-confusion-before-duration-random",
			inputSummary = "目标分别因薄雾场地、特性和携带道具免疫混乱。",
			expectedSummary = "混乱不会写入，事件流记录对应阻止原因，且混乱持续时间随机数不会被消费。",
		)
		val cases = listOf(
			VolatileEffectImmunityCase(
				actorId = "misty-target",
				target = participant("misty-target", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.MISTY),
				expectedReason = BattleStatusBlockReason.TERRAIN,
			),
			VolatileEffectImmunityCase(
				actorId = "ability-confusion-immune-target",
				target = participant("ability-confusion-immune-target", speed = 50).replaceAbilityEffects(
					listOf(
						BattleAbilityEffect.VolatileStatusImmunity(setOf(BattleVolatileStatus.CONFUSION)),
					),
				),
				expectedReason = BattleStatusBlockReason.ABILITY,
			),
			VolatileEffectImmunityCase(
				actorId = "item-confusion-immune-target",
				target = participant("item-confusion-immune-target", speed = 50).replaceItemEffects(
					listOf(
						BattleItemEffect.VolatileStatusImmunity(setOf(BattleVolatileStatus.CONFUSION)),
					),
				),
				expectedReason = BattleStatusBlockReason.ITEM,
			),
		)

		scenario.assertNamed("terrain-ability-and-item-immunities-block-confusion-before-duration-random")
		cases.forEach { case ->
			val random = ScriptedBattleRandom(emptyList())
			val state = engine.start(
				initialState(
					first = participant("confusion-user-${case.actorId}", speed = 100, skill = confusionSkill()),
					second = case.target,
					environment = case.environment,
				),
			)

			val resolved = engine.resolveTurn(
				state,
				listOf(BattleAction.UseSkill("confusion-user-${case.actorId}", skillId = 1, targetActorId = case.actorId)),
				random,
			)

			assertEquals(emptyList(), random.consumedReasons())
			assertEquals(0, resolved.participant(case.actorId)?.confusionTurnsRemaining)
			assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>())
			val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
			assertEquals(BattleVolatileStatus.CONFUSION, blocked.status)
			assertEquals(case.expectedReason, blocked.reason)
		}
	}

	@Test
	fun `ability and item immunities block flinch so target still acts`() {
		val scenario = publicBattleRuleScenario(
			name = "ability-and-item-immunities-block-flinch-before-action",
			inputSummary = "较快成员使用 100% 附加畏缩的接触外普通技能命中目标；目标分别通过特性和携带道具免疫畏缩。",
			expectedSummary = "畏缩不会写入目标运行态，事件流记录对应阻止原因；目标随后仍能在同回合正常行动并造成伤害。",
		)
		val cases = listOf(
			VolatileEffectImmunityCase(
				actorId = "ability-flinch-immune-target",
				target = participant("ability-flinch-immune-target", speed = 50).replaceAbilityEffects(
					listOf(
						BattleAbilityEffect.VolatileStatusImmunity(setOf(BattleVolatileStatus.FLINCH)),
					),
				),
				expectedReason = BattleStatusBlockReason.ABILITY,
			),
			VolatileEffectImmunityCase(
				actorId = "item-flinch-immune-target",
				target = participant("item-flinch-immune-target", speed = 50).replaceItemEffects(
					listOf(
						BattleItemEffect.VolatileStatusImmunity(setOf(BattleVolatileStatus.FLINCH)),
					),
				),
				expectedReason = BattleStatusBlockReason.ITEM,
			),
		)

		scenario.assertNamed("ability-and-item-immunities-block-flinch-before-action")
		cases.forEach { case ->
			val random = ScriptedBattleRandom(listOf(1, 15, 1, 15))
			val state = engine.start(
				initialState(
					first = participant("flinch-user-${case.actorId}", speed = 100, skill = flinchSkill()),
					second = case.target,
					environment = case.environment,
				),
			)

			val resolved = engine.resolveTurn(
				state,
				listOf(
					BattleAction.UseSkill("flinch-user-${case.actorId}", skillId = 1, targetActorId = case.actorId),
					BattleAction.UseSkill(case.actorId, skillId = 1, targetActorId = "flinch-user-${case.actorId}"),
				),
				random,
			)

			assertEquals(
				listOf("critical hit for 1", "damage random for 1", "critical hit for 1", "damage random for 1"),
				random.consumedReasons(),
			)
			assertEquals(72, resolved.participant(case.actorId)?.currentHp)
			assertEquals(72, resolved.participant("flinch-user-${case.actorId}")?.currentHp)
			assertEquals(34, resolved.participant(case.actorId)?.skillSlot(1)?.remainingPp)
			assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>())
			assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillPrevented>().filter { it.reason == SkillPreventionReason.VOLATILE_STATUS })
			val blocked = resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
			assertEquals(BattleVolatileStatus.FLINCH, blocked.status)
			assertEquals(case.expectedReason, blocked.reason)
		}
	}

	@Test
	fun `grassy terrain heals only grounded active participants`() {
		val scenario = publicBattleRuleScenario(
			name = "grassy-terrain-heals-only-grounded-active-participants",
			inputSummary = "青草场地存在时，双方当前上场成员都损失 HP，其中一方不接地。",
			expectedSummary = "只有接地成员在回合末获得青草场地回复。",
		)
		val state = engine.start(
			initialState(
				first = participant("grounded", speed = 100, currentHp = 80),
				second = participant("ungrounded", speed = 50, currentHp = 80, grounded = false),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("grassy-terrain-heals-only-grounded-active-participants")
		assertEquals(86, resolved.participant("grounded")?.currentHp)
		assertEquals(80, resolved.participant("ungrounded")?.currentHp)
		assertEquals(listOf("grounded"), resolved.events.filterIsInstance<BattleEvent.TerrainHealingApplied>().map { it.actorId })
	}

	private fun statusSkill(status: BattleMajorStatus) =
		damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = status,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private fun confusionSkill() =
		damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private fun flinchSkill() =
		damagingSkill(
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.FLINCH,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private data class StatusImmunityCase(
		val actorId: String,
		val status: BattleMajorStatus,
		val elementId: Long,
	)

	private data class StatusEffectImmunityCase(
		val actorId: String,
		val status: BattleMajorStatus,
		val target: BattleParticipant,
		val expectedReason: BattleStatusBlockReason,
	)

	private data class VolatileEffectImmunityCase(
		val actorId: String,
		val target: BattleParticipant,
		val environment: BattleEnvironment = BattleEnvironment(),
		val expectedReason: BattleStatusBlockReason,
	)

	private companion object {
		private const val ELECTRIC_ELEMENT_ID = 13L
		private const val FIRE_ELEMENT_ID = 10L
		private const val GRASS_ELEMENT_ID = 12L
		private const val ICE_ELEMENT_ID = 15L
		private const val POISON_ELEMENT_ID = 4L
		private const val STEEL_ELEMENT_ID = 9L
	}
}
