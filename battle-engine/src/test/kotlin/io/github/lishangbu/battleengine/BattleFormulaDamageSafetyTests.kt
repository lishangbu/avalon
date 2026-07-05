package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证普通伤害公式入口对资料缺口的生产兜底。
 *
 * 场景类型：伤害公式安全边界 场景。
 * 参考来源类型：公开规则说明与当前三范式资料共同确认的边界。现代规则里存在“伤害分类是物理/特殊，但基础威力为空”
 * 的技能，这类技能通常读取本回合或上一段受到的伤害记录，例如反击类规则。它们不能进入普通伤害公式，也不能被
 * 简化成 0 威力伤害；在完整伤害记忆模型接入前，引擎必须稳定失败，避免生产对局因为缺威力抛出 500。
 */
class BattleFormulaDamageSafetyTests {
	private val engine = BattleEngine()

	@Test
	fun `damage class skill without power or dynamic power fails before formula damage`() {
		val scenario = publicBattleRuleScenario(
			name = "damage-class-skill-without-power-or-dynamic-power-fails-before-formula-damage",
			inputSummary = "使用者发动一个物理伤害分类、但没有基础威力和动态威力模型的技能。",
			expectedSummary = "技能使用后稳定失败，不进入普通伤害公式，不产生伤害事件，目标生命值保持不变。",
		)
		val skill = damagingSkill(name = "缺威力伤害测试", power = null)
		val state = engine.start(
			initialState(
				first = participant("counter-user", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("counter-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("damage-class-skill-without-power-or-dynamic-power-fails-before-formula-damage")
		assertEquals(100, resolved.participant("target")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		val event = resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single()
		assertEquals("counter-user", event.actorId)
		assertEquals("target", event.targetActorId)
		assertEquals(1, event.skillId)
		assertEquals("damage-power-unavailable", event.reason)
	}
}
