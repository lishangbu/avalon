package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证回合末主要异常状态伤害。
 *
 * 场景类型：持续状态 场景。
 * 参考来源类型：现代主系列灼伤、中毒和剧毒回合末伤害规则；本测试重点覆盖剧毒的递增计数，
 * 普通中毒与灼伤的固定比例由原有单回合测试覆盖。
 * 验证重点：剧毒首次按 1/16 扣血，之后在仍然上场且存活时逐回合递增。
 */
class BattleResidualStatusTests {
	private val engine = BattleEngine()

	@Test
	fun `bad poison residual damage increases each active turn`() {
		val toxicSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.BAD_POISON,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("status-user", speed = 100, skill = toxicSkill),
				second = participant("defender", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("status-user", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			emptyList(),
			ScriptedBattleRandom(emptyList()),
		)

		val residualEvents = afterSecond.events.filterIsInstance<BattleEvent.ResidualDamageApplied>()
		assertEquals(BattleMajorStatus.BAD_POISON, afterFirst.participant("defender")?.majorStatus)
		assertEquals(94, afterFirst.participant("defender")?.currentHp)
		assertEquals(2, afterFirst.participant("defender")?.badPoisonCounter)
		assertEquals(82, afterSecond.participant("defender")?.currentHp)
		assertEquals(3, afterSecond.participant("defender")?.badPoisonCounter)
		assertEquals(listOf(6, 12), residualEvents.map { it.amount })
	}
}
