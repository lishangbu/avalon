package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证现代主系列击中要害流程。
 *
 * 场景类型：命中后伤害流程、随机轨迹和防守方屏障交互 场景。
 * 参考来源类型：公开成熟对战引擎的行动结算实现、公开伤害计算器实现，以及中文公开规则资料。现代规则中，
 * 技能侧要害等级会映射到固定概率表：普通为 1/24，+1 为 1/8，+2 为 1/2，+3 及以上必定要害；
 * 必定要害不再消费要害随机数。击中要害后，伤害公式会忽略攻击方不利攻击/特攻阶级和防守方有利防御/特防阶级，
 * 并绕过反射壁、光墙等一侧伤害减免。
 * 验证重点：不同要害等级消费正确随机上界；必定要害保持随机轨迹稳定；要害标记传入普通伤害公式；
 * 能力阶级与一侧屏障按要害规则被绕过。
 */
class BattleCriticalHitFlowTests {
	private val engine = BattleEngine()

	@Test
	fun `critical hit stage item combines with high critical hit skill`() {
		val random = ScriptedBattleRandom(listOf(15))
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(criticalHitStage = 2),
					itemId = 232,
					itemEffects = listOf(BattleItemEffect.CriticalHitStageBoost(stageDelta = 1)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		assertEquals(true, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().criticalHit)
		assertEquals(listOf("damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `critical hit stage ability combines with the skill stage`() {
		val random = ScriptedBattleRandom(listOf(15))
		val state = engine.start(
			initialState(
				first = participant(
					"super-luck-holder",
					speed = 100,
					skill = damagingSkill(criticalHitStage = 2),
					abilityEffects = listOf(BattleAbilityEffect.CriticalHitStageBoost(1)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("super-luck-holder", 1, "defender")),
			random,
		)

		assertEquals(true, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().criticalHit)
		assertEquals(listOf("damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `stage one critical hit uses one eighth chance`() {
		val scenario = publicBattleRuleScenario(
			name = "stage-one-critical-hit-uses-one-eighth-chance",
			inputSummary = "使用者使用基础要害等级 +1 的物理技能，要害随机数掷到 0。",
			expectedSummary = "引擎以 1/8 上界消费要害随机数，本次伤害标记为击中要害并按 1.5 倍要害倍率结算。",
		)
		val random = ScriptedBattleRandom(listOf(0, 15))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(criticalHitStage = 1)),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		scenario.assertNamed("stage-one-critical-hit-uses-one-eighth-chance")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(true, damage.criticalHit)
		assertEquals(42, damage.amount)
		assertEquals(58, resolved.participant("defender")?.currentHp)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `stage two critical hit uses one half chance`() {
		val scenario = publicBattleRuleScenario(
			name = "stage-two-critical-hit-uses-one-half-chance",
			inputSummary = "使用者使用基础要害等级 +2 的物理技能，要害随机数掷到 0。",
			expectedSummary = "引擎以 1/2 上界消费要害随机数，本次伤害标记为击中要害并按现代要害倍率结算。",
		)
		val random = ScriptedBattleRandom(listOf(0, 15))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(criticalHitStage = 2)),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		scenario.assertNamed("stage-two-critical-hit-uses-one-half-chance")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(true, damage.criticalHit)
		assertEquals(42, damage.amount)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `stage three critical hit is guaranteed without critical random consumption`() {
		val scenario = publicBattleRuleScenario(
			name = "stage-three-critical-hit-is-guaranteed-without-critical-random-consumption",
			inputSummary = "使用者使用基础要害等级 +3 的物理技能。",
			expectedSummary = "引擎把 +3 及以上视为必定要害，不消费要害随机数，只继续消费伤害浮动随机数。",
		)
		val random = ScriptedBattleRandom(listOf(15))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(criticalHitStage = 3)),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		scenario.assertNamed("stage-three-critical-hit-is-guaranteed-without-critical-random-consumption")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(true, damage.criticalHit)
		assertEquals(42, damage.amount)
		assertEquals(listOf("damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `critical hit ignores unfavorable attack and favorable defense stages`() {
		val scenario = publicBattleRuleScenario(
			name = "critical-hit-ignores-unfavorable-attack-and-favorable-defense-stages",
			inputSummary = "使用者攻击阶级为 -6，目标防御阶级为 +6，使用必定要害物理技能命中。",
			expectedSummary = "击中要害进入伤害公式后忽略攻击方不利攻击阶级和防守方有利防御阶级，仍按中性攻防造成要害伤害。",
		)
		val random = ScriptedBattleRandom(listOf(15))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(criticalHitStage = 3)).copy(
					statStages = mapOf(BattleStat.ATTACK to -6),
				),
				second = participant("defender", speed = 50).copy(
					statStages = mapOf(BattleStat.DEFENSE to 6),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		scenario.assertNamed("critical-hit-ignores-unfavorable-attack-and-favorable-defense-stages")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(true, damage.criticalHit)
		assertEquals(42, damage.amount)
		assertEquals(58, resolved.participant("defender")?.currentHp)
		assertEquals(listOf("damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `critical hit ignores side damage reduction`() {
		val scenario = publicBattleRuleScenario(
			name = "critical-hit-ignores-side-damage-reduction",
			inputSummary = "目标一侧存在物理伤害减免屏障，使用者用必定要害物理技能命中目标。",
			expectedSummary = "伤害事件标记为击中要害；普通物理伤害不应用目标一侧屏障减免，按完整要害伤害结算。",
		)
		val random = ScriptedBattleRandom(listOf(15))
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(criticalHitStage = 3)),
				second = participant("defender", speed = 50),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		scenario.assertNamed("critical-hit-ignores-side-damage-reduction")
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()
		assertEquals(true, damage.criticalHit)
		assertEquals(42, damage.amount)
		assertEquals(58, resolved.participant("defender")?.currentHp)
		assertEquals(listOf("damage random for 1"), random.consumedReasons())
	}
}
