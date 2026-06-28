package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * 验证单打第一版状态机。
 *
 * 场景类型：状态机级 fixture。
 * 参考来源类型：公开主系列回合流程的通用阶段拆解；本测试固定随机序列，只验证行动排序、技能使用、
 * 伤害事件、倒下事件和终局事件，不覆盖尚未实现的状态、天气、特性、道具或替换。
 * 验证重点：同一初始状态、行动序列和随机脚本得到稳定事件流。
 */
class BattleEngineSingleTurnTests {
	private val engine = BattleEngine()

	@Test
	fun `start emits battle started event`() {
		val state = engine.start(initialState())

		assertEquals(0, state.turnNumber)
		assertNull(state.result)
		assertIs<BattleEvent.BattleStarted>(state.events.single())
	}

	@Test
	fun `faster participant deals damage and ends battle when target faints`() {
		val state = engine.start(
			initialState(
				first = participant("fast", speed = 100),
				second = participant("slow", speed = 50, currentHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow"),
				BattleAction.UseSkill("slow", skillId = 1, targetActorId = "fast"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals("side-a", resolved.result?.winningSideId)
		assertEquals(0, resolved.participant("slow")?.currentHp)
		assertIs<BattleEvent.BattleEnded>(resolved.events.last())
		assertEquals(
			listOf(
				BattleEvent.BattleStarted::class,
				BattleEvent.TurnStarted::class,
				BattleEvent.SkillUsed::class,
				BattleEvent.DamageApplied::class,
				BattleEvent.ParticipantFainted::class,
				BattleEvent.BattleEnded::class,
			),
			resolved.events.map { it::class },
		)
	}

	@Test
	fun `priority acts before speed`() {
		val slowPrioritySkill = damagingSkill(skillId = 2, name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("slow-priority", speed = 50, skill = slowPrioritySkill),
				second = participant("fast-normal", speed = 100),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast-normal", skillId = 1, targetActorId = "slow-priority"),
				BattleAction.UseSkill("slow-priority", skillId = 2, targetActorId = "fast-normal"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		val usedEvents = resolved.events.filterIsInstance<BattleEvent.SkillUsed>()
		assertEquals(listOf("slow-priority", "fast-normal"), usedEvents.map { it.actorId })
		assertEquals(72, resolved.participant("fast-normal")?.currentHp)
		assertEquals(72, resolved.participant("slow-priority")?.currentHp)
	}

	@Test
	fun `protection skill blocks affected damage in the same turn`() {
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals(9, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(1)?.remainingPp)
		assertIs<BattleEvent.ProtectionStarted>(resolved.events.filterIsInstance<BattleEvent.ProtectionStarted>().single())
		assertIs<BattleEvent.SkillBlockedByProtection>(
			resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single(),
		)
		assertEquals(
			listOf(
				BattleEvent.BattleStarted::class,
				BattleEvent.TurnStarted::class,
				BattleEvent.SkillUsed::class,
				BattleEvent.ProtectionStarted::class,
				BattleEvent.SkillUsed::class,
				BattleEvent.SkillBlockedByProtection::class,
				BattleEvent.TurnEnded::class,
			),
			resolved.events.map { it::class },
		)
	}

	@Test
	fun `skill marked as unaffected by protection bypasses protection`() {
		val bypassSkill = damagingSkill(skillId = 3, name = "破防测试", affectedByProtect = false)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100, skill = bypassSkill),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 3, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(72, resolved.participant("protector")?.currentHp)
		assertEquals(9, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(3)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertIs<BattleEvent.DamageApplied>(resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single())
	}

	@Test
	fun `second consecutive protection can succeed on one third roll`() {
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100),
			),
		)
		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker")),
			ScriptedBattleRandom(emptyList()),
		)

		val resolved = engine.resolveTurn(
			afterFirst,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(0)),
		)

		assertEquals(2, resolved.participant("protector")?.protectionChain)
		assertEquals(100, resolved.participant("protector")?.currentHp)
		assertEquals(8, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertIs<BattleEvent.SkillBlockedByProtection>(
			resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single(),
		)
	}

	@Test
	fun `second consecutive protection can fail and leave user unprotected`() {
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("attacker", speed = 100),
			),
		)
		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker")),
			ScriptedBattleRandom(emptyList()),
		)

		val resolved = engine.resolveTurn(
			afterFirst,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(1, 1, 15)),
		)

		assertEquals(0, resolved.participant("protector")?.protectionChain)
		assertEquals(72, resolved.participant("protector")?.currentHp)
		assertEquals(8, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertIs<BattleEvent.ProtectionFailed>(resolved.events.filterIsInstance<BattleEvent.ProtectionFailed>().single())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
	}

	@Test
	fun `protection chain resets when user does not protect this turn`() {
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 50, skill = protectionSkill()),
				second = participant("observer", speed = 100),
			),
		)
		val afterProtection = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("protector", skillId = 2, targetActorId = "observer")),
			ScriptedBattleRandom(emptyList()),
		)

		val resolved = engine.resolveTurn(
			afterProtection,
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(1, afterProtection.participant("protector")?.protectionChain)
		assertEquals(0, resolved.participant("protector")?.protectionChain)
	}

	@Test
	fun `miss consumes accuracy random and does not apply damage`() {
		val inaccurateSkill = damagingSkill(accuracy = 50)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = inaccurateSkill),
				second = participant("defender", speed = 50),
			),
		)
		val random = ScriptedBattleRandom(listOf(99))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		assertEquals(100, resolved.participant("defender")?.currentHp)
		assertIs<BattleEvent.SkillMissed>(resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single())
		assertEquals(listOf("accuracy for 1"), random.consumedReasons())
	}

	@Test
	fun `evasion stage can make accurate skill miss`() {
		val accurateSkill = damagingSkill(accuracy = 100)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = accurateSkill),
				second = participant("defender", speed = 50).copy(
					statStages = mapOf(BattleStat.EVASION to 1),
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(75))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		assertEquals(100, resolved.participant("defender")?.currentHp)
		assertEquals(76, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(listOf("accuracy for 1"), random.consumedReasons())
	}

	@Test
	fun `guaranteed critical hit marks damage event and increases damage`() {
		val criticalSkill = damagingSkill(criticalHitStage = 3)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = criticalSkill),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(15)),
		)

		val damageEvent = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(true, damageEvent.criticalHit)
		assertEquals(42, damageEvent.amount)
		assertEquals(58, resolved.participant("defender")?.currentHp)
	}

	@Test
	fun `status skill applies burn and end turn residual damage`() {
		val burnSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.BURN,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = burnSkill),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(BattleMajorStatus.BURN, resolved.participant("defender")?.majorStatus)
		assertEquals(94, resolved.participant("defender")?.currentHp)
		assertIs<BattleEvent.StatusApplied>(resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single())
		assertIs<BattleEvent.ResidualDamageApplied>(resolved.events.filterIsInstance<BattleEvent.ResidualDamageApplied>().single())
	}

	@Test
	fun `stat stage effect changes later action damage in the same turn`() {
		val attackDropSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("stage-user", speed = 100, skill = attackDropSkill),
				second = participant("damager", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("stage-user", skillId = 1, targetActorId = "damager"),
				BattleAction.UseSkill("damager", skillId = 1, targetActorId = "stage-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(-1, resolved.participant("damager")?.statStage(BattleStat.ATTACK))
		assertEquals(81, resolved.participant("stage-user")?.currentHp)
		assertIs<BattleEvent.StatStageChanged>(resolved.events.filterIsInstance<BattleEvent.StatStageChanged>().single())
	}

	@Test
	fun `paralysis halves speed for action ordering`() {
		val state = engine.start(
			initialState(
				first = participant("paralyzed-fast", speed = 100).copy(majorStatus = BattleMajorStatus.PARALYSIS),
				second = participant("normal-mid", speed = 80),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("paralyzed-fast", skillId = 1, targetActorId = "normal-mid"),
				BattleAction.UseSkill("normal-mid", skillId = 1, targetActorId = "paralyzed-fast"),
			),
			ScriptedBattleRandom(listOf(1, 15, 99, 1, 15)),
		)

		val usedEvents = resolved.events.filterIsInstance<BattleEvent.SkillUsed>()
		assertEquals(listOf("normal-mid", "paralyzed-fast"), usedEvents.map { it.actorId })
	}

	@Test
	fun `contact ability can apply status to attacker`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(makesContact = true)),
				second = participant(
					"defender",
					speed = 50,
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.PARALYSIS,
							chancePercent = 100,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("attacker")?.majorStatus)
		val statusEvent = resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single()
		assertEquals("defender", statusEvent.actorId)
		assertEquals("attacker", statusEvent.targetActorId)
	}

	@Test
	fun `damage boost item applies recoil after damage`() {
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					itemEffects = listOf(BattleItemEffect.DamageBoostWithRecoil(multiplier = 1.3, recoilDenominator = 10)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(63, resolved.participant("defender")?.currentHp)
		assertEquals(97, resolved.participant("attacker")?.currentHp)
		assertIs<BattleEvent.RecoilDamageApplied>(resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().single())
	}

	@Test
	fun `low hp fixed healing item triggers after damage and is consumed`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant(
					"defender",
					speed = 50,
					currentHp = 60,
					itemId = 132,
					itemEffects = listOf(BattleItemEffect.LowHpHeal(fixedHealAmount = 10)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val defender = requireNotNull(resolved.participant("defender"))

		assertEquals(42, defender.currentHp)
		assertNull(defender.itemId)
		assertEquals(emptyList(), defender.itemEffects)
		assertEquals(10, resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single().amount)
	}

	@Test
	fun `low hp fractional healing item restores max hp fraction`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant(
					"defender",
					speed = 50,
					currentHp = 60,
					itemId = 135,
					itemEffects = listOf(BattleItemEffect.LowHpHeal(healDenominator = 4)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val defender = requireNotNull(resolved.participant("defender"))

		assertEquals(57, defender.currentHp)
		assertNull(defender.itemId)
		assertEquals(25, resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single().amount)
	}

	@Test
	fun `physical side damage reduction halves single battle physical damage`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant("defender", speed = 50),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 3),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(86, resolved.participant("defender")?.currentHp)
		assertEquals(2, resolved.sideOf("defender")?.damageReductions?.single()?.turnsRemaining)
	}

	@Test
	fun `critical hit ignores side damage reduction`() {
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant("defender", speed = 50),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(0, 15)),
		)

		assertEquals(58, resolved.participant("defender")?.currentHp)
		assertEquals(true, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().criticalHit)
	}

	@Test
	fun `side damage reduction uses weaker double battle multiplier when target side has two active participants`() {
		val state = engine.start(
			doubleInitialState(
				firstA = participant("attacker", speed = 100),
				firstB = participant("ally", speed = 90),
				secondA = participant("defender-left", speed = 50),
				secondB = participant("defender-right", speed = 40),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender-left")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(81, resolved.participant("defender-left")?.currentHp)
	}

	@Test
	fun `side damage reduction expires after final turn`() {
		val state = engine.start(
			initialState(
				first = participant("observer-a", speed = 100),
				second = participant("observer-b", speed = 50),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 1),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(emptyList(), resolved.sideOf("observer-b")?.damageReductions)
	}

	@Test
	fun `choice speed lock item boosts speed and locks first selected skill`() {
		val firstSkill = damagingSkill(skillId = 1, name = "一号技能")
		val secondSkill = damagingSkill(skillId = 2, name = "二号技能")
		val choiceUser = participant(
			"choice-user",
			speed = 60,
			skill = firstSkill,
			itemId = 264,
			itemEffects = listOf(BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)),
		).copy(skillSlots = listOf(firstSkill, secondSkill))
		val state = engine.start(
			initialState(
				first = choiceUser,
				firstBench = listOf(participant("reserve", speed = 50)),
				second = participant("opponent", speed = 80),
			),
		)

		val firstTurn = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("choice-user", skillId = 1, targetActorId = "opponent"),
				BattleAction.UseSkill("opponent", skillId = 1, targetActorId = "choice-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		assertEquals("choice-user", firstTurn.events.filterIsInstance<BattleEvent.DamageApplied>().first().actorId)
		assertEquals(1, firstTurn.participant("choice-user")?.choiceLockedSkillId)
		assertFailsWith<IllegalArgumentException> {
			engine.resolveTurn(
				firstTurn,
				listOf(BattleAction.UseSkill("choice-user", skillId = 2, targetActorId = "opponent")),
				ScriptedBattleRandom(emptyList()),
			)
		}

		val afterSwitch = engine.resolveTurn(
			firstTurn,
			listOf(BattleAction.SwitchParticipant("choice-user", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		assertNull(afterSwitch.participant("choice-user")?.choiceLockedSkillId)
	}

	@Test
	fun `end turn healing item restores hp`() {
		val state = engine.start(
			initialState(
				first = participant(
					"holder",
					speed = 100,
					currentHp = 80,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(86, resolved.participant("holder")?.currentHp)
		assertIs<BattleEvent.HealingApplied>(resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single())
	}

	@Test
	fun `grassy terrain heals active participants at end turn`() {
		val state = engine.start(
			initialState(
				first = participant("grounded", speed = 100, currentHp = 80),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(86, resolved.participant("grounded")?.currentHp)
		val event = resolved.events.filterIsInstance<BattleEvent.TerrainHealingApplied>().single()
		assertEquals(BattleTerrain.GRASSY, event.terrain)
		assertEquals(6, event.amount)
	}

	@Test
	fun `spread skill keeps full damage when only one target can battle`() {
		val spreadSkill = damagingSkill(
			name = "范围攻击",
			targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS,
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("spread-user", speed = 100, skill = spreadSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70, currentHp = 0),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spread-user", skillId = 1, targetActorId = "opponent-left")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damageEvent = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		assertEquals("opponent-left", damageEvent.targetActorId)
		assertEquals(28, damageEvent.amount)
		assertEquals(1.0, damageEvent.targetMultiplier)
		assertEquals(72, resolved.participant("opponent-left")?.currentHp)
		assertEquals(0, resolved.participant("opponent-right")?.currentHp)
	}

	@Test
	fun `switch action happens before attacks and redirects target slot to replacement`() {
		val state = engine.start(
			initialState(
				first = participant("starter", speed = 100),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("attacker", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.SwitchParticipant("starter", targetActorId = "reserve"),
				BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "starter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		assertEquals(100, resolved.participant("starter")?.currentHp)
		assertEquals(72, resolved.participant("reserve")?.currentHp)
		val switchEvent = resolved.events.filterIsInstance<BattleEvent.ParticipantSwitched>().single()
		val skillEvent = resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single()
		assertEquals(false, switchEvent.forced)
		assertEquals("reserve", skillEvent.targetActorId)
		assertEquals(
			listOf(
				BattleEvent.BattleStarted::class,
				BattleEvent.TurnStarted::class,
				BattleEvent.ParticipantSwitched::class,
				BattleEvent.SkillUsed::class,
				BattleEvent.DamageApplied::class,
				BattleEvent.TurnEnded::class,
			),
			resolved.events.map { it::class },
		)
	}

	@Test
	fun `fainted active participant can be replaced by a reserve`() {
		val state = engine.start(
			initialState(
				first = participant("fainted-active", speed = 100, currentHp = 0),
				firstBench = listOf(participant("reserve", speed = 80)),
				second = participant("observer", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("fainted-active", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		assertNull(resolved.result)
		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		val switchEvent = resolved.events.filterIsInstance<BattleEvent.ParticipantSwitched>().single()
		assertEquals(true, switchEvent.forced)
	}

	@Test
	fun `switch target must belong to same side and be able to battle`() {
		val state = engine.start(
			initialState(
				first = participant("starter", speed = 100),
				firstBench = listOf(participant("fainted-reserve", speed = 80, currentHp = 0)),
				second = participant("opponent", speed = 60),
			),
		)

		assertFailsWith<IllegalArgumentException> {
			engine.resolveTurn(
				state,
				listOf(BattleAction.SwitchParticipant("starter", targetActorId = "opponent")),
				ScriptedBattleRandom(emptyList()),
			)
		}
		assertFailsWith<IllegalArgumentException> {
			engine.resolveTurn(
				state,
				listOf(BattleAction.SwitchParticipant("starter", targetActorId = "fainted-reserve")),
				ScriptedBattleRandom(emptyList()),
			)
		}
	}
}
