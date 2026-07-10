package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证固定伤害类技能不会进入普通伤害公式。
 *
 * 场景类型：状态机级 场景。
 * 参考来源类型：公开对战引擎技能资料中的 `damage` 字段；本测试只保存输入、行动和期望事件，不复制外部实现。
 * 固定随机序列意图：固定伤害不消费击中要害或伤害浮动随机数，因此使用空随机脚本即可完成成功命中场景。
 * 验证重点：固定数值和按使用者等级两种伤害口径直接写入 HP，属性免疫仍阻止固定伤害造成 HP 变化。
 */
class BattleFixedDamageSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `fixed damage skills use configured amount or user level without standard damage formula`() {
		val scenario = publicBattleRuleScenario(
			name = "fixed-damage-skills-use-fixed-amount-or-user-level",
			inputSummary = "使用者分别使用固定 20 点伤害技能和按自身等级造成伤害的技能命中普通目标。",
			expectedSummary = "固定伤害直接扣除对应 HP；不消费击中要害和伤害随机数，也不进入普通伤害公式倍率链。",
		)
		val fixedAmountSkill = fixedDamageSkill(
			skillId = 49,
			name = "音爆",
			fixedDamage = BattleFixedDamage.FixedAmount(20),
		)
		val levelDamageSkill = fixedDamageSkill(
			skillId = 69,
			name = "地球上投",
			damageClass = BattleDamageClass.PHYSICAL,
			fixedDamage = BattleFixedDamage.UserLevel(),
		)

		val fixedAmount = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("fixed-user", speed = 100, skill = fixedAmountSkill),
					second = participant("target", speed = 80, currentHp = 100),
				),
			),
			listOf(BattleAction.UseSkill("fixed-user", skillId = 49, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val levelDamage = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("level-user", speed = 100, skill = levelDamageSkill),
					second = participant("target", speed = 80, currentHp = 100),
				),
			),
			listOf(BattleAction.UseSkill("level-user", skillId = 69, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("fixed-damage-skills-use-fixed-amount-or-user-level")
		assertEquals(80, fixedAmount.participant("target")?.currentHp)
		assertEquals(20, fixedAmount.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(50, levelDamage.participant("target")?.currentHp)
		assertEquals(50, levelDamage.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	@Test
	fun `fixed damage still respects element immunity`() {
		val scenario = publicBattleRuleScenario(
			name = "fixed-damage-skill-respects-element-immunity",
			inputSummary = "固定 40 点伤害技能命中属性相性为 0 的目标。",
			expectedSummary = "技能产生 0 伤害事件，目标 HP 不变，固定伤害数值不会绕过属性免疫。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"fixed-user",
					speed = 100,
					elementId = 1,
					skill = fixedDamageSkill(
						skillId = 82,
						name = "龙之怒",
						elementId = 1,
						fixedDamage = BattleFixedDamage.FixedAmount(40),
					),
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
			listOf(BattleAction.UseSkill("fixed-user", skillId = 82, targetActorId = "immune-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("fixed-damage-skill-respects-element-immunity")
		assertEquals(100, resolved.participant("immune-target")?.currentHp)
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	private fun fixedDamageSkill(
		skillId: Long,
		name: String,
		elementId: Long = 1,
		damageClass: BattleDamageClass = BattleDamageClass.SPECIAL,
		fixedDamage: BattleFixedDamage,
	) = damagingSkill(
		skillId = skillId,
		name = name,
		elementId = elementId,
		damageClass = damageClass,
		power = null,
		fixedDamage = fixedDamage,
	)
}
