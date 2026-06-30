package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 场地相关战斗规则测试。
 *
 * 场景类型：全场环境对行动顺序和后续规则的影响。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。该文件只验证已经结构化进入引擎的场地效果，
 * 不通过特性名称、资料 ID 或本地化文本驱动状态机。
 */
class BattleTerrainEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `terrain speed ability changes skill action order`() {
		val fixture = publicBattleRuleFixture(
			name = "terrain-speed-ability-changes-skill-action-order",
			inputSummary = "电气场地环境下，低速成员拥有场地速度翻倍特性，高速成员没有场地速度特性。",
			expectedSummary = "低速成员的有效速度翻倍后先行动，事件流中的技能使用顺序随之改变。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"terrain-boosted",
					speed = 60,
					abilityEffects = listOf(
						BattleAbilityEffect.TerrainSpeedMultiplier(
							terrain = BattleTerrain.ELECTRIC,
							multiplier = 2.0,
						),
					),
				),
				second = participant("naturally-fast", speed = 100),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("terrain-boosted", skillId = 1, targetActorId = "naturally-fast"),
				BattleAction.UseSkill("naturally-fast", skillId = 1, targetActorId = "terrain-boosted"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		fixture.assertNamed("terrain-speed-ability-changes-skill-action-order")
		assertEquals(
			listOf("terrain-boosted", "naturally-fast"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
	}
}
