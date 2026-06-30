package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证技能命中后的能力阶级特殊操作。
 *
 * 场景类型：技能能力阶级操作 fixture。
 * 参考来源类型：公开成熟模拟器技能资料和公开规则说明；测试只保存行为输入、事件和状态断言，不复制外部实现代码。
 * 验证重点：清除、复制、交换和取反都按当前战斗运行态结算，并使用专门事件表达特殊规则语义。
 */
class BattleStatStageOperationSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `damaging skill clears target stat stages after hit`() {
		val fixture = publicBattleRuleFixture(
			name = "damaging-skill-clears-target-stat-stages-after-hit",
			inputSummary = "目标已有正负能力阶级，使用者用命中后清除目标阶级的伤害技能攻击。",
			expectedSummary = "技能先按普通伤害流程命中，再把目标指定能力阶级清为 0，并记录清除事件。",
		)
		val skill = damagingSkill(
			skillId = 499,
			name = "清除之烟",
			statStageOperations = listOf(
				clearTarget(BattleStat.ATTACK),
				clearTarget(BattleStat.DEFENSE),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to 3, BattleStat.DEFENSE to -2),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 499, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("damaging-skill-clears-target-stat-stages-after-hit")
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.DEFENSE))
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.StatStageCleared>().size)
		val damageIndex = resolved.events.indexOfFirst { it is BattleEvent.DamageApplied }
		val clearIndex = resolved.events.indexOfFirst { it is BattleEvent.StatStageCleared }
		assertTrue(damageIndex >= 0)
		assertTrue(clearIndex > damageIndex)
	}

	@Test
	fun `status skill clears every active participant stat stages`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-clears-every-active-participant-stat-stages",
			inputSummary = "双打中多个当前上场成员拥有能力阶级变化，使用者发动全场清除能力阶级的变化技能。",
			expectedSummary = "所有当前上场且仍可战斗成员的指定能力阶级清为 0，后备成员不受影响。",
		)
		val skill = damagingSkill(
			skillId = 114,
			name = "黑雾",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			affectedByProtect = false,
			targetScope = BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS,
			statStageOperations = listOf(
				clearAllActive(BattleStat.ATTACK),
				clearAllActive(BattleStat.SPEED),
			),
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("user", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
				firstB = participant("ally", speed = 90).copy(
					statStages = mapOf(BattleStat.SPEED to -1),
				),
				secondA = participant("opponent-a", speed = 80).copy(
					statStages = mapOf(BattleStat.ATTACK to -3, BattleStat.SPEED to 1),
				),
				secondB = participant("opponent-b", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 114, targetActorId = "opponent-a")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-clears-every-active-participant-stat-stages")
		listOf("user", "ally", "opponent-a").forEach { actorId ->
			assertEquals(0, resolved.participant(actorId)?.statStage(BattleStat.ATTACK))
			assertEquals(0, resolved.participant(actorId)?.statStage(BattleStat.SPEED))
		}
		assertEquals(4, resolved.events.filterIsInstance<BattleEvent.StatStageCleared>().size)
	}

	@Test
	fun `status skill copies target stat stages to user`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-copies-target-stat-stages-to-user",
			inputSummary = "使用者和目标拥有不同能力阶级，使用者发动复制目标阶级的变化技能。",
			expectedSummary = "使用者指定能力阶级被改写为目标当前阶级，目标自身阶级保持不变。",
		)
		val skill = damagingSkill(
			skillId = 244,
			name = "自我暗示",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			affectedByProtect = false,
			statStageOperations = listOf(
				copyTargetToUser(BattleStat.ATTACK),
				copyTargetToUser(BattleStat.EVASION),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.ATTACK to -1, BattleStat.EVASION to 1),
				),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to 4, BattleStat.EVASION to -2),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 244, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-copies-target-stat-stages-to-user")
		assertEquals(4, resolved.participant("user")?.statStage(BattleStat.ATTACK))
		assertEquals(-2, resolved.participant("user")?.statStage(BattleStat.EVASION))
		assertEquals(4, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(-2, resolved.participant("target")?.statStage(BattleStat.EVASION))
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.StatStageCopied>().size)
	}

	@Test
	fun `status skill swaps attack stat stages between user and target`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-swaps-attack-stat-stages-between-user-and-target",
			inputSummary = "使用者和目标拥有不同攻击与特攻阶级，使用者发动攻击组阶级互换技能。",
			expectedSummary = "双方攻击与特攻阶级互换，防御类阶级不变化。",
		)
		val skill = swapSkill(
			skillId = 384,
			name = "力量互换",
			stats = listOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK),
		)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.ATTACK to 2, BattleStat.SPECIAL_ATTACK to -1, BattleStat.DEFENSE to 3),
				),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to -3, BattleStat.SPECIAL_ATTACK to 4, BattleStat.DEFENSE to -2),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 384, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-swaps-attack-stat-stages-between-user-and-target")
		assertEquals(-3, resolved.participant("user")?.statStage(BattleStat.ATTACK))
		assertEquals(4, resolved.participant("user")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(-1, resolved.participant("target")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(3, resolved.participant("user")?.statStage(BattleStat.DEFENSE))
		assertEquals(-2, resolved.participant("target")?.statStage(BattleStat.DEFENSE))
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.StatStageSwapped>().size)
	}

	@Test
	fun `status skill swaps defense stat stages between user and target`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-swaps-defense-stat-stages-between-user-and-target",
			inputSummary = "使用者和目标拥有不同防御与特防阶级，使用者发动防御组阶级互换技能。",
			expectedSummary = "双方防御与特防阶级互换，攻击类阶级不变化。",
		)
		val skill = swapSkill(
			skillId = 385,
			name = "防守互换",
			stats = listOf(BattleStat.DEFENSE, BattleStat.SPECIAL_DEFENSE),
		)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, skill = skill).copy(
					statStages = mapOf(BattleStat.DEFENSE to 2, BattleStat.SPECIAL_DEFENSE to -1, BattleStat.ATTACK to 3),
				),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.DEFENSE to -3, BattleStat.SPECIAL_DEFENSE to 4, BattleStat.ATTACK to -2),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 385, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-swaps-defense-stat-stages-between-user-and-target")
		assertEquals(-3, resolved.participant("user")?.statStage(BattleStat.DEFENSE))
		assertEquals(4, resolved.participant("user")?.statStage(BattleStat.SPECIAL_DEFENSE))
		assertEquals(2, resolved.participant("target")?.statStage(BattleStat.DEFENSE))
		assertEquals(-1, resolved.participant("target")?.statStage(BattleStat.SPECIAL_DEFENSE))
		assertEquals(3, resolved.participant("user")?.statStage(BattleStat.ATTACK))
		assertEquals(-2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.StatStageSwapped>().size)
	}

	@Test
	fun `status skill swaps all stat stages between user and target`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-swaps-all-stat-stages-between-user-and-target",
			inputSummary = "使用者和目标拥有多项不同能力阶级，使用者发动全部能力阶级互换技能。",
			expectedSummary = "双方所有配置的能力阶级互换，未变化的 0 阶级不产生事件。",
		)
		val skill = swapSkill(
			skillId = 391,
			name = "心灵互换",
			stats = BattleStat.entries.toList(),
		)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, skill = skill).copy(
					statStages = mapOf(
						BattleStat.ATTACK to 2,
						BattleStat.SPECIAL_ATTACK to -1,
						BattleStat.SPEED to 3,
					),
				),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(
						BattleStat.ATTACK to -3,
						BattleStat.SPECIAL_ATTACK to 4,
						BattleStat.SPEED to -2,
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 391, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-swaps-all-stat-stages-between-user-and-target")
		assertEquals(-3, resolved.participant("user")?.statStage(BattleStat.ATTACK))
		assertEquals(4, resolved.participant("user")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(-2, resolved.participant("user")?.statStage(BattleStat.SPEED))
		assertEquals(2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(-1, resolved.participant("target")?.statStage(BattleStat.SPECIAL_ATTACK))
		assertEquals(3, resolved.participant("target")?.statStage(BattleStat.SPEED))
		assertEquals(3, resolved.events.filterIsInstance<BattleEvent.StatStageSwapped>().size)
	}

	@Test
	fun `status skill inverts target stat stages`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-inverts-target-stat-stages",
			inputSummary = "目标拥有正负能力阶级，使用者发动取反目标能力阶级的变化技能。",
			expectedSummary = "目标指定能力阶级取相反数，0 阶级不产生事件。",
		)
		val skill = damagingSkill(
			skillId = 576,
			name = "颠倒",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = 100,
			statStageOperations = listOf(
				invertTarget(BattleStat.ATTACK),
				invertTarget(BattleStat.DEFENSE),
				invertTarget(BattleStat.SPEED),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, skill = skill),
				second = participant("target", speed = 50).copy(
					statStages = mapOf(BattleStat.ATTACK to 2, BattleStat.DEFENSE to -3),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 576, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-inverts-target-stat-stages")
		assertEquals(-2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(3, resolved.participant("target")?.statStage(BattleStat.DEFENSE))
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.SPEED))
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.StatStageInverted>().size)
	}

	private fun clearTarget(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.CLEAR,
			stat = stat,
			target = BattleStatStageOperationTarget.TARGET,
			chancePercent = 100,
		)

	private fun clearAllActive(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.CLEAR,
			stat = stat,
			target = BattleStatStageOperationTarget.ALL_ACTIVE,
			chancePercent = 100,
		)

	private fun copyTargetToUser(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.COPY,
			stat = stat,
			target = BattleStatStageOperationTarget.USER,
			source = BattleStatStageOperationTarget.TARGET,
			chancePercent = 100,
		)

	private fun swapSkill(skillId: Long, name: String, stats: List<BattleStat>) =
		damagingSkill(
			skillId = skillId,
			name = name,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			affectedByProtect = false,
			statStageOperations = stats.map { stat ->
				BattleStatStageOperation(
					kind = BattleStatStageOperationKind.SWAP,
					stat = stat,
					target = BattleStatStageOperationTarget.USER,
					source = BattleStatStageOperationTarget.TARGET,
					chancePercent = 100,
				)
			},
		)

	private fun invertTarget(stat: BattleStat): BattleStatStageOperation =
		BattleStatStageOperation(
			kind = BattleStatStageOperationKind.INVERT,
			stat = stat,
			target = BattleStatStageOperationTarget.TARGET,
			chancePercent = 100,
		)
}
