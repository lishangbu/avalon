package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleReceivedDamage
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证按本回合已受伤害反打的技能。
 *
 * 场景类型：伤害记忆、目标重定向、直接伤害 场景。
 * 参考来源类型：公开成熟对战引擎技能资料中的 `damageCallback` / `onTryHit` 语义，以及公开规则说明对反打类技能
 * 的共同描述；测试只记录输入行动、事件顺序和期望伤害，不复制外部实现代码。
 * 固定随机序列意图：反打类技能一旦找到合格伤害记忆，就按事件中的实际伤害倍数直接写 HP，不消费击中要害或伤害
 * 浮动随机数；因此成功与失败用空随机脚本即可暴露任何意外的普通公式回退。
 * 验证重点：
 * - 没有合格伤害类别时技能失败，而不是进入普通公式或使用提交目标。
 * - 有合格伤害时，目标在命中前重定向为最后造成该伤害的对手。
 * - 伤害数值按已承受实际伤害乘以配置倍数并向下取整，属性相性只保留完全免疫判断。
 */
class BattleReceivedDamageSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `received physical damage skill returns double damage to last physical attacker`() {
		val scenario = publicBattleRuleScenario(
			name = "received-physical-damage-skill-returns-double-damage-to-last-physical-attacker",
			inputSummary = "较快对手先用物理直接伤害打掉使用者 23 HP，使用者随后发动只读取物理伤害的反打技能。",
			expectedSummary = "反打技能命中最后的物理伤害来源并造成 46 HP 伤害；整个流程不消费普通伤害公式随机数。",
		)
		val hitSkill = fixedDamageHitSkill(skillId = 10, damageClass = BattleDamageClass.PHYSICAL, amount = 23)
		val responseSkill = receivedDamageSkill(
			skillId = 68,
			name = "双倍奉还",
			damageClass = BattleDamageClass.PHYSICAL,
			receivedDamage = BattleReceivedDamage(setOf(BattleDamageClass.PHYSICAL), numerator = 2, denominator = 1),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("response-user", speed = 50, skill = responseSkill),
					second = participant("physical-attacker", speed = 100, skill = hitSkill),
				),
			),
			listOf(
				BattleAction.UseSkill("physical-attacker", skillId = 10, targetActorId = "response-user"),
				BattleAction.UseSkill("response-user", skillId = 68, targetActorId = "physical-attacker"),
			),
			random,
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		scenario.assertNamed("received-physical-damage-skill-returns-double-damage-to-last-physical-attacker")
		assertEquals(77, resolved.participant("response-user")?.currentHp)
		assertEquals(54, resolved.participant("physical-attacker")?.currentHp)
		assertEquals(listOf(23, 46), damageEvents.map { it.amount })
		assertEquals(listOf("response-user", "physical-attacker"), damageEvents.map { it.targetActorId })
		assertTrue(random.isFullyConsumed())
	}

	@Test
	fun `received damage skill fails when last received damage class is not accepted`() {
		val scenario = publicBattleRuleScenario(
			name = "received-damage-skill-fails-when-last-received-damage-class-is-not-accepted",
			inputSummary = "较快对手先用物理直接伤害打掉使用者 HP，使用者随后发动只读取特殊伤害的反打技能。",
			expectedSummary = "技能找不到合格特殊伤害记忆而失败，不产生第二段伤害，也不会回退到普通公式。",
		)
		val hitSkill = fixedDamageHitSkill(skillId = 10, damageClass = BattleDamageClass.PHYSICAL, amount = 20)
		val responseSkill = receivedDamageSkill(
			skillId = 243,
			name = "镜面反射",
			damageClass = BattleDamageClass.SPECIAL,
			receivedDamage = BattleReceivedDamage(setOf(BattleDamageClass.SPECIAL), numerator = 2, denominator = 1),
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("response-user", speed = 50, skill = responseSkill),
					second = participant("physical-attacker", speed = 100, skill = hitSkill),
				),
			),
			listOf(
				BattleAction.UseSkill("physical-attacker", skillId = 10, targetActorId = "response-user"),
				BattleAction.UseSkill("response-user", skillId = 243, targetActorId = "physical-attacker"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("received-damage-skill-fails-when-last-received-damage-class-is-not-accepted")
		assertEquals(80, resolved.participant("response-user")?.currentHp)
		assertEquals(100, resolved.participant("physical-attacker")?.currentHp)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().size)
		assertEquals(
			"received-damage-memory-unavailable",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `received damage target redirects to last attacker before hit gates`() {
		val scenario = publicBattleRuleScenario(
			name = "received-damage-target-redirects-to-last-attacker-before-hit-gates",
			inputSummary = "双打中左侧对手先打伤使用者，使用者提交目标却指向右侧对手。",
			expectedSummary = "反打技能忽略提交目标，在命中前重定向到最后造成合格伤害的左侧对手。",
		)
		val hitSkill = fixedDamageHitSkill(skillId = 10, damageClass = BattleDamageClass.SPECIAL, amount = 25)
		val responseSkill = receivedDamageSkill(
			skillId = 368,
			name = "金属爆炸",
			damageClass = BattleDamageClass.PHYSICAL,
			receivedDamage = BattleReceivedDamage(
				acceptedDamageClasses = setOf(BattleDamageClass.PHYSICAL, BattleDamageClass.SPECIAL),
				numerator = 3,
				denominator = 2,
			),
		)

		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("response-user", speed = 50, skill = responseSkill),
					firstB = participant("ally", speed = 40),
					secondA = participant("actual-attacker", speed = 100, skill = hitSkill),
					secondB = participant("submitted-target", speed = 30),
				),
			),
			listOf(
				BattleAction.UseSkill("actual-attacker", skillId = 10, targetActorId = "response-user"),
				BattleAction.UseSkill("response-user", skillId = 368, targetActorId = "submitted-target"),
			),
			ScriptedBattleRandom(emptyList()),
		)
		val damageEvents = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()

		scenario.assertNamed("received-damage-target-redirects-to-last-attacker-before-hit-gates")
		assertEquals(listOf("response-user", "actual-attacker"), damageEvents.map { it.targetActorId })
		assertEquals(63, resolved.participant("actual-attacker")?.currentHp)
		assertEquals(100, resolved.participant("submitted-target")?.currentHp)
	}

	private fun fixedDamageHitSkill(
		skillId: Long,
		damageClass: BattleDamageClass,
		amount: Int,
	) = damagingSkill(
		skillId = skillId,
		name = "固定伤害测试",
		damageClass = damageClass,
		power = null,
		fixedDamage = BattleFixedDamage.FixedAmount(amount),
	)

	private fun receivedDamageSkill(
		skillId: Long,
		name: String,
		damageClass: BattleDamageClass,
		receivedDamage: BattleReceivedDamage,
	) = damagingSkill(
		skillId = skillId,
		name = name,
		damageClass = damageClass,
		power = null,
		receivedDamage = receivedDamage,
		priority = -5,
	)
}
