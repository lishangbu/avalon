package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代主系列行动排序。
 *
 * 场景类型：优先度、有效速度、同速随机、速度修正和速度顺序效果 场景。
 * 参考来源类型：公开成熟对战引擎的行动队列实现、公开技能资料和公开规则说明。现代回合排序先比较有效优先度，
 * 再比较有效速度；速度会受到主要异常状态、道具、一侧场上状态、天气/场地特性和全场速度顺序效果影响。
 * 同优先度同速时必须消费可复盘随机数决定顺序。
 * 验证重点：排序只影响同一回合中 `SkillUsed` 事件顺序，不提前执行命中或伤害；优先度始终先于速度；
 * 速度修正不写回成员面板值，而是在排序时按当前状态临时计算。
 */
class BattleActionOrderingPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `positive priority acts before faster normal priority skill`() {
		val scenario = publicBattleRuleScenario(
			name = "positive-priority-acts-before-faster-normal-priority-skill",
			inputSummary = "速度较低成员使用优先度 +1 技能，速度较高成员使用普通优先度技能。",
			expectedSummary = "优先度 +1 行动先于普通优先度行动执行，速度只在同优先度内比较。",
		)
		val prioritySkill = damagingSkill(skillId = 2, name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("slow-priority", speed = 50, skill = prioritySkill),
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

		scenario.assertNamed("positive-priority-acts-before-faster-normal-priority-skill")
		assertEquals(listOf("slow-priority", "fast-normal"), usedActorIds(resolved.events))
	}

	@Test
	fun `negative priority acts after slower normal priority skill`() {
		val scenario = publicBattleRuleScenario(
			name = "negative-priority-acts-after-slower-normal-priority-skill",
			inputSummary = "速度较高成员使用优先度 -1 技能，速度较低成员使用普通优先度技能。",
			expectedSummary = "普通优先度行动先执行，负优先度行动即使速度更高也会后执行。",
		)
		val negativePrioritySkill = damagingSkill(skillId = 3, name = "后制测试", priority = -1)
		val state = engine.start(
			initialState(
				first = participant("fast-negative", speed = 120, skill = negativePrioritySkill),
				second = participant("slow-normal", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast-negative", skillId = 3, targetActorId = "slow-normal"),
				BattleAction.UseSkill("slow-normal", skillId = 1, targetActorId = "fast-negative"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("negative-priority-acts-after-slower-normal-priority-skill")
		assertEquals(listOf("slow-normal", "fast-negative"), usedActorIds(resolved.events))
	}

	@Test
	fun `higher speed acts first inside same priority bracket`() {
		val scenario = publicBattleRuleScenario(
			name = "higher-speed-acts-first-inside-same-priority-bracket",
			inputSummary = "双方都使用普通优先度技能，其中一方有效速度更高。",
			expectedSummary = "同优先度内有效速度更高的成员先产生技能使用事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("fast", speed = 100),
				second = participant("slow", speed = 60),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("slow", skillId = 1, targetActorId = "fast"),
				BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("higher-speed-acts-first-inside-same-priority-bracket")
		assertEquals(listOf("fast", "slow"), usedActorIds(resolved.events))
	}

	@Test
	fun `same speed tie consumes random order keys`() {
		val scenario = publicBattleRuleScenario(
			name = "same-speed-tie-consumes-random-order-keys",
			inputSummary = "双方同优先度且有效速度完全相同，固定同速随机键让第二个提交者先行动。",
			expectedSummary = "引擎为同速双方各消费一个排序随机数，并按随机键决定技能使用事件顺序。",
		)
		val state = engine.start(
			initialState(
				first = participant("tie-left", speed = 100),
				second = participant("tie-right", speed = 100),
			),
		)
		val random = ScriptedBattleRandom(listOf(999_999, 0, 1, 15, 1, 15))

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("tie-left", skillId = 1, targetActorId = "tie-right"),
				BattleAction.UseSkill("tie-right", skillId = 1, targetActorId = "tie-left"),
			),
			random,
		)

		scenario.assertNamed("same-speed-tie-consumes-random-order-keys")
		assertEquals(listOf("tie-right", "tie-left"), usedActorIds(resolved.events))
		assertEquals(
			listOf(
				"speed tie for tie-left",
				"speed tie for tie-right",
				"critical hit for 1",
				"damage random for 1",
				"critical hit for 1",
				"damage random for 1",
			),
			random.consumedReasons(),
		)
	}

	@Test
	fun `same positive priority bracket still orders by speed`() {
		val scenario = publicBattleRuleScenario(
			name = "same-positive-priority-bracket-still-orders-by-speed",
			inputSummary = "双方都使用优先度 +1 技能，其中一方速度更高。",
			expectedSummary = "同为 +1 优先度时，仍按有效速度决定行动先后。",
		)
		val quickSkill = damagingSkill(skillId = 2, name = "先制测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant("fast-priority", speed = 100, skill = quickSkill),
				second = participant("slow-priority", speed = 60, skill = quickSkill),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("slow-priority", skillId = 2, targetActorId = "fast-priority"),
				BattleAction.UseSkill("fast-priority", skillId = 2, targetActorId = "slow-priority"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("same-positive-priority-bracket-still-orders-by-speed")
		assertEquals(listOf("fast-priority", "slow-priority"), usedActorIds(resolved.events))
	}

	@Test
	fun `grounded terrain priority boost moves slower skill before faster opponent`() {
		val scenario = publicBattleRuleScenario(
			name = "grounded-terrain-priority-boost-moves-slower-skill-before-faster-opponent",
			inputSummary = "青草场地中，速度较低且接地的成员使用声明为青草场地接地先制 +1 的攻击技能。",
			expectedSummary = "技能获得 +1 优先度，先于速度更高的普通优先度技能执行。",
		)
		val grassyPrioritySkill = damagingSkill(
			skillId = 803,
			name = "青草滑梯",
			elementId = 12,
			groundedTerrainPriorityBoosts = mapOf(BattleTerrain.GRASSY to 1),
		)
		val state = engine.start(
			initialState(
				first = participant("grassy-priority-user", speed = 40, skill = grassyPrioritySkill),
				second = participant("faster-opponent", speed = 100),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("grassy-priority-user", skillId = 803, targetActorId = "faster-opponent"),
				BattleAction.UseSkill("faster-opponent", skillId = 1, targetActorId = "grassy-priority-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("grounded-terrain-priority-boost-moves-slower-skill-before-faster-opponent")
		assertEquals(listOf("grassy-priority-user", "faster-opponent"), usedActorIds(resolved.events))
	}

	@Test
	fun `grounded terrain priority boost does not apply to ungrounded user`() {
		val scenario = publicBattleRuleScenario(
			name = "grounded-terrain-priority-boost-does-not-apply-to-ungrounded-user",
			inputSummary = "青草场地中，速度较低但非接地的成员使用同一个场地先制技能。",
			expectedSummary = "使用者没有受到场地影响，技能保持普通优先度，速度更高的对手先行动。",
		)
		val grassyPrioritySkill = damagingSkill(
			skillId = 803,
			name = "青草滑梯",
			elementId = 12,
			groundedTerrainPriorityBoosts = mapOf(BattleTerrain.GRASSY to 1),
		)
		val state = engine.start(
			initialState(
				first = participant("ungrounded-user", speed = 40, skill = grassyPrioritySkill, grounded = false),
				second = participant("faster-opponent", speed = 100),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("ungrounded-user", skillId = 803, targetActorId = "faster-opponent"),
				BattleAction.UseSkill("faster-opponent", skillId = 1, targetActorId = "ungrounded-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("grounded-terrain-priority-boost-does-not-apply-to-ungrounded-user")
		assertEquals(listOf("faster-opponent", "ungrounded-user"), usedActorIds(resolved.events))
	}

	@Test
	fun `choice speed item changes action order`() {
		val scenario = publicBattleRuleScenario(
			name = "choice-speed-item-changes-action-order",
			inputSummary = "速度 70 的成员持有讲究类速度道具，对手速度 100，双方同优先度行动。",
			expectedSummary = "持有者有效速度按 1.5 倍变为 105，因此先于速度 100 的对手行动。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"choice-user",
					speed = 70,
					itemEffects = listOf(BattleItemEffect.ChoiceSkillLock(speedMultiplier = 1.5)),
				),
				second = participant("opponent", speed = 100),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("choice-user", skillId = 1, targetActorId = "opponent"),
				BattleAction.UseSkill("opponent", skillId = 1, targetActorId = "choice-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("choice-speed-item-changes-action-order")
		assertEquals(listOf("choice-user", "opponent"), usedActorIds(resolved.events))
	}

	@Test
	fun `paralysis speed drop changes action order`() {
		val scenario = publicBattleRuleScenario(
			name = "paralysis-speed-drop-changes-action-order",
			inputSummary = "速度 120 的成员处于麻痹状态，对手速度 80，双方同优先度行动。",
			expectedSummary = "麻痹成员有效速度减半为 60，因此速度 80 的对手先行动。",
		)
		val state = engine.start(
			initialState(
				first = participant("paralyzed-fast", speed = 120).copy(majorStatus = BattleMajorStatus.PARALYSIS),
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

		scenario.assertNamed("paralysis-speed-drop-changes-action-order")
		assertEquals(listOf("normal-mid", "paralyzed-fast"), usedActorIds(resolved.events))
	}

	@Test
	fun `side speed modifier changes action order`() {
		val scenario = publicBattleRuleScenario(
			name = "side-speed-modifier-changes-action-order",
			inputSummary = "速度 60 的成员所在一侧已有 2 倍速度修正，对手速度 100，双方同优先度行动。",
			expectedSummary = "一侧速度修正让速度 60 成员按有效速度 120 参与排序，因此先于对手行动。",
		)
		val state = engine.start(
			BattleInitialState(
				format = singleFormat(),
				rules = neutralRules(),
				sides = listOf(
					BattleSide(
						sideId = "side-a",
						activeActorIds = listOf("tailwind-user"),
						participants = listOf(participant("tailwind-user", speed = 60)),
						speedModifiers = listOf(BattleSideSpeedModifier(BattleSideSpeedModifierKind.TAILWIND)),
					),
					BattleSide(
						sideId = "side-b",
						activeActorIds = listOf("opponent"),
						participants = listOf(participant("opponent", speed = 100)),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("tailwind-user", skillId = 1, targetActorId = "opponent"),
				BattleAction.UseSkill("opponent", skillId = 1, targetActorId = "tailwind-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("side-speed-modifier-changes-action-order")
		assertEquals(listOf("tailwind-user", "opponent"), usedActorIds(resolved.events))
	}

	@Test
	fun `field speed order reversal changes same priority action order`() {
		val scenario = publicBattleRuleScenario(
			name = "field-speed-order-reversal-changes-same-priority-action-order",
			inputSummary = "全场速度顺序处于反转状态，速度 40 与速度 100 的成员同优先度行动。",
			expectedSummary = "同优先度内速度较低的成员先行动；优先度比较本身不被反转。",
		)
		val state = engine.start(
			initialState(
				first = participant("fast", speed = 100),
				second = participant("slow", speed = 40),
				environment = BattleEnvironment(
					fieldSpeedOrderEffect = BattleFieldSpeedOrderEffect(BattleFieldSpeedOrderKind.TRICK_ROOM),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("fast", skillId = 1, targetActorId = "slow"),
				BattleAction.UseSkill("slow", skillId = 1, targetActorId = "fast"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		scenario.assertNamed("field-speed-order-reversal-changes-same-priority-action-order")
		assertEquals(listOf("slow", "fast"), usedActorIds(resolved.events))
	}

	@Test
	fun `status priority ability moves status skill before faster opponent`() {
		val scenario = publicBattleRuleScenario(
			name = "status-priority-ability-changes-action-order-before-faster-opponent",
			inputSummary = "速度较低成员拥有变化技能优先度提升特性，并选择变化技能；速度较高对手使用普通攻击。",
			expectedSummary = "变化技能获得额外优先度，先于速度更高的普通攻击执行。",
		)
		val statusSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
		)
		val state = engine.start(
			initialState(
				first = participant(
					"status-priority-user",
					speed = 40,
					skill = statusSkill,
					abilityEffects = listOf(BattleAbilityEffect.StatusSkillPriorityBoost()),
				),
				second = participant("faster-opponent", speed = 100),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("status-priority-user", skillId = 1, targetActorId = "faster-opponent"),
				BattleAction.UseSkill("faster-opponent", skillId = 1, targetActorId = "status-priority-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("status-priority-ability-changes-action-order-before-faster-opponent")
		assertEquals(listOf("status-priority-user", "faster-opponent"), usedActorIds(resolved.events))
	}

	private fun usedActorIds(events: List<BattleEvent>): List<String> =
		events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId }
}
