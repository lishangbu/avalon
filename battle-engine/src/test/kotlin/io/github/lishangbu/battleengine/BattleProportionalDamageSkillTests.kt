package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证按目标当前 HP 比例造成伤害的技能。
 *
 * 场景类型：状态机级 fixture。
 * 参考来源类型：公开对战引擎技能资料中的 `damageCallback`，只用来对照比例和取整，不复制外部实现代码。
 * 固定随机序列意图：比例伤害不消费击中要害或伤害浮动随机数，因此成功命中场景使用空随机脚本即可复现。
 * 验证重点：目标当前 HP 作为计算基数、向下取整且至少 1 点，属性免疫仍阻止比例伤害造成 HP 变化。
 */
class BattleProportionalDamageSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `proportional damage skill uses target current hp fraction`() {
		val fixture = publicBattleRuleFixture(
			name = "proportional-damage-skill-halves-target-current-hp",
			inputSummary = "使用者用目标当前 HP 一半伤害技能命中当前 HP 为 99 和 1 的目标。",
			expectedSummary = "目标当前 HP 为 99 时损失 49；当前 HP 为 1 时仍造成至少 1 点实际伤害。",
		)
		val skill = proportionalDamageSkill()

		val oddHp = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, skill = skill),
					second = participant("target", speed = 80, currentHp = 99),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 162, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val minimumHp = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, skill = skill),
					second = participant("target", speed = 80, currentHp = 1),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 162, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("proportional-damage-skill-halves-target-current-hp")
		assertEquals(50, oddHp.participant("target")?.currentHp)
		assertEquals(49, oddHp.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(0, minimumHp.participant("target")?.currentHp)
		assertEquals(1, minimumHp.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `proportional damage still respects element immunity`() {
		val fixture = publicBattleRuleFixture(
			name = "proportional-damage-skill-respects-element-immunity",
			inputSummary = "目标当前 HP 一半伤害技能命中属性相性为 0 的目标。",
			expectedSummary = "目标 HP 保持不变，引擎只记录 0 伤害事件，比例伤害不会绕过属性免疫。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"user",
					speed = 100,
					elementId = 1,
					skill = proportionalDamageSkill(elementId = 1),
				),
				second = participant("immune-target", speed = 80, currentHp = 100, elementId = 2),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(
						mapOf(1L to mapOf(2L to 0.0)),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", skillId = 162, targetActorId = "immune-target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("proportional-damage-skill-respects-element-immunity")
		assertEquals(100, resolved.participant("immune-target")?.currentHp)
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	private fun proportionalDamageSkill(
		elementId: Long = 1,
	) = damagingSkill(
		skillId = 162,
		name = "愤怒门牙",
		elementId = elementId,
		damageClass = BattleDamageClass.PHYSICAL,
		power = null,
		proportionalDamage = BattleProportionalDamage.TargetCurrentHpFraction(
			numerator = 1,
			denominator = 2,
		),
	)
}
