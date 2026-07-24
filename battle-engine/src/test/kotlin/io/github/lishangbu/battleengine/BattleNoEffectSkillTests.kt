package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证资料明确声明为“没有战斗效果”的变化技能仍遵循正常使用流程。
 *
 * 跃起、庆祝和欢乐时光在对战引擎中没有需要结算的状态、能力阶级或场地效果，但成功选择后仍应消耗 PP、记录
 * 技能使用事件，并且不应隐式修改任一参战成员的 HP 或主要状态。
 */
class BattleNoEffectSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `no effect status skill only consumes pp and records normal use`() {
		val noEffectSkill = damagingSkill(
			skillId = 150,
			name = "无效果变化技能测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
		).copy(remainingPp = 40, maxPp = 40)
		val state = engine.start(
			initialState(
				first = participant("user", speed = 100, skill = noEffectSkill),
				second = participant("opponent", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 150, targetActorId = "user")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(39, resolved.participant("user")?.skillSlot(150)?.remainingPp)
		assertEquals(100, resolved.participant("user")?.currentHp)
		assertEquals(100, resolved.participant("opponent")?.currentHp)
		assertEquals(150, resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().skillId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
	}
}
