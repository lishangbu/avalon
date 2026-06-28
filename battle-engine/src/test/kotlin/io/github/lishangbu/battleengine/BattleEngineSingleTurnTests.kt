package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
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
			ScriptedBattleRandom(listOf(15)),
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
			ScriptedBattleRandom(listOf(15, 15)),
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
			ScriptedBattleRandom(listOf(15)),
		)

		assertEquals(72, resolved.participant("protector")?.currentHp)
		assertEquals(9, resolved.participant("protector")?.skillSlot(2)?.remainingPp)
		assertEquals(34, resolved.participant("attacker")?.skillSlot(3)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertIs<BattleEvent.DamageApplied>(resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single())
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
			ScriptedBattleRandom(listOf(15)),
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
			ScriptedBattleRandom(listOf(15, 15)),
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
			ScriptedBattleRandom(listOf(15)),
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
			ScriptedBattleRandom(listOf(15)),
		)

		assertEquals(63, resolved.participant("defender")?.currentHp)
		assertEquals(97, resolved.participant("attacker")?.currentHp)
		assertIs<BattleEvent.RecoilDamageApplied>(resolved.events.filterIsInstance<BattleEvent.RecoilDamageApplied>().single())
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
			ScriptedBattleRandom(listOf(15)),
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
