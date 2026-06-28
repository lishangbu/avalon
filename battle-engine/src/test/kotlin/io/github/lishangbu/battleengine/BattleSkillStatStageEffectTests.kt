package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证技能成功后造成能力阶级变化的效果。
 *
 * 场景类型：技能能力阶级效果 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，变化技能成功后可以提升或降低使用者自身多项能力阶级。
 * 验证重点：同一个技能槽可以携带多条结构化阶级变化，并按事件流记录每一项实际变化。
 */
class BattleSkillStatStageEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill applies multiple user stat stage changes`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-applies-multiple-user-stat-stage-changes",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Dragon_Dance_(move)",
				"https://bulbapedia.bulbagarden.net/wiki/Shell_Smash_(move)",
			),
			inputSummary = "使用者成功使用会提升自身多项能力阶级的变化技能。",
			expectedSummary = "使用者自身对应能力阶级逐项改变，并为每一项实际变化产生能力阶级事件。",
		)
		val skill = damagingSkill(
			name = "自我强化测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.USER,
					stageDelta = 1,
					chancePercent = 100,
				),
				BattleStatStageEffect(
					stat = BattleStat.SPEED,
					target = BattleEffectTarget.USER,
					stageDelta = 1,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("stage-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("stage-user", skillId = 1, targetActorId = "stage-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-applies-multiple-user-stat-stage-changes")
		val user = resolved.participant("stage-user")
		assertEquals(1, user?.statStage(BattleStat.ATTACK))
		assertEquals(1, user?.statStage(BattleStat.SPEED))
		val events = resolved.events.filterIsInstance<BattleEvent.StatStageChanged>()
		assertEquals(2, events.size)
		assertEquals(listOf(BattleStat.ATTACK, BattleStat.SPEED), events.map { it.stat })
		assertEquals(setOf("stage-user"), events.map { it.targetActorId }.toSet())
	}

	@Test
	fun `all opponents status skill applies stat stage change to each active opponent`() {
		val fixture = publicBattleRuleFixture(
			name = "all-opponents-status-skill-applies-stat-stage-change-to-each-active-opponent",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Growl_(move)",
				"https://bulbapedia.bulbagarden.net/wiki/Noble_Roar_(move)",
			),
			inputSummary = "双打中使用者成功使用影响所有相邻对手的变化技能。",
			expectedSummary = "两个当前对手分别降低对应能力阶级，己方队友不受影响，并为每个实际目标记录事件。",
		)
		val skill = damagingSkill(
			name = "全体降攻测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS,
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
			doubleInitialState(
				firstA = participant("stage-user", speed = 100, skill = skill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-a", speed = 80),
				secondB = participant("opponent-b", speed = 70),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("stage-user", skillId = 1, targetActorId = "opponent-a")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("all-opponents-status-skill-applies-stat-stage-change-to-each-active-opponent")
		assertEquals(0, resolved.participant("ally")?.statStage(BattleStat.ATTACK))
		assertEquals(-1, resolved.participant("opponent-a")?.statStage(BattleStat.ATTACK))
		assertEquals(-1, resolved.participant("opponent-b")?.statStage(BattleStat.ATTACK))
		val events = resolved.events.filterIsInstance<BattleEvent.StatStageChanged>()
		assertEquals(listOf("opponent-a", "opponent-b"), events.map { it.targetActorId }.sorted())
	}
}
