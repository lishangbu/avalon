package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证按双方当前 HP 推导伤害的技能。
 *
 * 场景类型：状态机级 fixture。
 * 参考来源类型：公开对战引擎技能资料中的 `damageCallback` 和命中后失败条件；本测试只记录输入、行动和期望事件。
 * 固定随机序列意图：HP 派生伤害不消费击中要害或伤害浮动随机数，因此成功命中场景使用空随机脚本即可复现。
 * 验证重点：差额伤害把目标 HP 降到使用者当前 HP；差额不为正时技能失败；自身体力伤害会让使用者倒下。
 */
class BattleHpDerivedDamageSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `HP difference damage reduces target to user current hp`() {
		val fixture = publicBattleRuleFixture(
			name = "hp-difference-damage-skill-reduces-target-to-user-current-hp",
			inputSummary = "使用者当前 HP 为 37，目标当前 HP 为 92，使用目标 HP 与自身 HP 差额伤害技能命中。",
			expectedSummary = "目标损失 55 HP，最终当前 HP 与使用者当前 HP 同为 37；技能不进入普通伤害公式。",
		)
		val skill = hpDerivedDamageSkill(
			skillId = 283,
			name = "蛮干",
			damageClass = BattleDamageClass.PHYSICAL,
			hpDerivedDamage = BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp,
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, currentHp = 37, skill = skill),
					second = participant("target", speed = 80, currentHp = 92),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 283, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("hp-difference-damage-skill-reduces-target-to-user-current-hp")
		assertEquals(37, resolved.participant("target")?.currentHp)
		assertEquals(55, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertNull(resolved.result)
	}

	@Test
	fun `HP difference damage fails when target hp is not greater than user hp`() {
		val fixture = publicBattleRuleFixture(
			name = "hp-difference-damage-skill-fails-when-target-not-above-user",
			inputSummary = "使用者当前 HP 为 70，目标当前 HP 为 60，使用目标 HP 与自身 HP 差额伤害技能命中。",
			expectedSummary = "技能因目标当前 HP 不高于使用者当前 HP 而失败，不产生伤害事件，也不会退回普通公式。",
		)
		val skill = hpDerivedDamageSkill(
			skillId = 283,
			name = "蛮干",
			damageClass = BattleDamageClass.PHYSICAL,
			hpDerivedDamage = BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp,
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, currentHp = 70, skill = skill),
					second = participant("target", speed = 80, currentHp = 60),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 283, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("hp-difference-damage-skill-fails-when-target-not-above-user")
		assertEquals(60, resolved.participant("target")?.currentHp)
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().size)
		assertEquals(
			"target-hp-not-greater-than-user-hp",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `user current hp sacrifice damage faints user after hit`() {
		val fixture = publicBattleRuleFixture(
			name = "user-current-hp-sacrifice-damage-skill-faints-user",
			inputSummary = "使用者当前 HP 为 73，使用自身当前 HP 等量伤害技能命中 HP 为 73 的目标。",
			expectedSummary = "目标和使用者都损失 73 HP，双方同时倒下并产生无胜方结果。",
		)
		val skill = hpDerivedDamageSkill(
			skillId = 515,
			name = "搏命",
			elementId = 2,
			damageClass = BattleDamageClass.SPECIAL,
			hpDerivedDamage = BattleHpDerivedDamage.UserCurrentHpAndUserFaints,
		)

		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("user", speed = 100, currentHp = 73, elementId = 2, skill = skill),
					second = participant("target", speed = 80, currentHp = 73),
				),
			),
			listOf(BattleAction.UseSkill("user", skillId = 515, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("user-current-hp-sacrifice-damage-skill-faints-user")
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(0, resolved.participant("user")?.currentHp)
		assertEquals(73, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(73, resolved.events.filterIsInstance<BattleEvent.SkillSelfSacrificeDamageApplied>().single().amount)
		assertEquals(
			listOf("target", "user"),
			resolved.events.filterIsInstance<BattleEvent.ParticipantFainted>().map { it.actorId },
		)
		assertNull(resolved.result?.winningSideId)
		assertEquals("all-sides-fainted", resolved.result?.reason)
	}

	private fun hpDerivedDamageSkill(
		skillId: Long,
		name: String,
		elementId: Long = 1,
		damageClass: BattleDamageClass,
		hpDerivedDamage: BattleHpDerivedDamage,
	) = damagingSkill(
		skillId = skillId,
		name = name,
		elementId = elementId,
		damageClass = damageClass,
		power = null,
		hpDerivedDamage = hpDerivedDamage,
	)
}
