package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证接触技能绕过保护的攻击方特性，以及与接触改写道具的交互。
 *
 * 场景类型：命中前保护 gate、动态接触事实和接触后副作用的公开规则场景。
 * 参考来源类型：公开资料和成熟对战引擎都把这类能力建模为“接触技能绕过防住类招式”，而不是把技能本身改成
 * 永久不受保护影响。公开资料还明确给出两个道具例外：拳击手套让拳击类技能本次不再接触，因此不会触发该绕过；
 * 部位护具只免疫接触副作用，不移除接触事实，因此仍然允许该能力绕过保护。
 *
 * 验证重点：
 * - 绕过保护只发生在使用者对对手目标使用本次仍然接触的技能时。
 * - 绕过不会破除目标个人保护，也不会移除快速防守等本回合一侧临时防护；同回合后续普通技能仍会被原防护拦下。
 * - 动态接触事实必须由同一个入口决定，避免拳击手套只影响接触反制却漏掉保护 gate 或伤害公式。
 * - 部位护具类副作用免疫不能错误地把技能改成非接触，否则会破坏接触类保护绕过的现代规则边界。
 */
class BattleContactProtectionBypassAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `contact protection bypass ability ignores target personal protection without breaking it`() {
		val scenario = publicBattleRuleScenario(
			name = "contact-protection-bypass-ignores-personal-protection-without-breaking-it",
			inputSummary = "目标先建立个人保护屏障；对手拥有接触技能绕过保护能力，并使用接触类伤害技能命中目标。",
			expectedSummary = "接触技能不被保护阻挡并造成伤害，但不会追加保护破除事件，也不会重置目标连续保护计数。",
		)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 100, skill = protectionSkill()),
				second = participant(
					"bypass-user",
					speed = 50,
					skill = contactStrike(),
					abilityEffects = listOf(BattleAbilityEffect.ContactSkillProtectionBypass),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "protector"),
				BattleAction.UseSkill("bypass-user", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("contact-protection-bypass-ignores-personal-protection-without-breaking-it")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.ProtectionBroken>())
		assertEquals("bypass-user", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().actorId)
		assertEquals(1, resolved.participant("protector")?.protectionChain)
		assertTrue(requireNotNull(resolved.participant("protector")).currentHp < 100)
	}

	@Test
	fun `contact protection bypass ability ignores side guard without removing it`() {
		val scenario = publicBattleRuleScenario(
			name = "contact-protection-bypass-ignores-side-guard-without-removing-it",
			inputSummary = "双打中目标侧先建立快速防守；对手一名成员用带绕过能力的接触先制技能命中目标侧伙伴，另一名成员随后使用普通先制技能。",
			expectedSummary = "带绕过能力的接触技能不被快速防守阻挡；快速防守仍然存在，随后普通先制技能仍被同一层防护拦下。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("guard-user", speed = 100, skill = prioritySideGuardSkill()),
				firstB = participant("guard-ally", speed = 90),
				secondA = participant(
					"bypass-user",
					speed = 80,
					skill = contactStrike(skillId = 10, name = "接触先制测试", priority = 1),
					abilityEffects = listOf(BattleAbilityEffect.ContactSkillProtectionBypass),
				),
				secondB = participant(
					"ordinary-priority-user",
					speed = 70,
					skill = contactStrike(skillId = 11, name = "普通先制测试", priority = 1),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("guard-user", skillId = 501, targetActorId = "guard-user"),
				BattleAction.UseSkill("bypass-user", skillId = 10, targetActorId = "guard-ally"),
				BattleAction.UseSkill("ordinary-priority-user", skillId = 11, targetActorId = "guard-user"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("contact-protection-bypass-ignores-side-guard-without-removing-it")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.ProtectionBroken>())
		assertEquals("bypass-user", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().actorId)
		assertEquals("guard-user", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
		assertTrue(requireNotNull(resolved.participant("guard-ally")).currentHp < 100)
		assertEquals(100, resolved.participant("guard-user")?.currentHp)
	}

	@Test
	fun `punch based contact suppression prevents protection bypass ability`() {
		val scenario = publicBattleRuleScenario(
			name = "punch-based-contact-suppression-prevents-protection-bypass",
			inputSummary = "目标建立个人保护；对手拥有接触技能绕过保护能力，但携带让拳击类技能本次不再接触的道具，并使用拳击类接触技能。",
			expectedSummary = "拳击类技能被动态改为非接触，因此绕过能力不生效，技能仍被保护阻挡且不消费命中或伤害随机数。",
		)
		val state = engine.start(
			initialState(
				first = participant("protector", speed = 100, skill = protectionSkill()),
				second = participant(
					"bypass-user",
					speed = 50,
					skill = contactStrike(punchBased = true),
					abilityEffects = listOf(BattleAbilityEffect.ContactSkillProtectionBypass),
					itemEffects = listOf(BattleItemEffect.PunchBasedContactSuppression),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "protector"),
				BattleAction.UseSkill("bypass-user", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("punch-based-contact-suppression-prevents-protection-bypass")
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
		assertEquals(100, resolved.participant("protector")?.currentHp)
	}

	@Test
	fun `contact side effect immunity does not suppress protection bypass`() {
		val scenario = publicBattleRuleScenario(
			name = "contact-side-effect-immunity-does-not-suppress-protection-bypass",
			inputSummary = "目标建立个人保护并拥有接触后麻痹攻击方的防守特性；攻击方拥有接触保护绕过能力，并携带只免疫接触副作用的道具。",
			expectedSummary = "接触事实仍然存在，攻击方绕过保护并造成伤害；但接触副作用被道具免疫，不会让攻击方陷入麻痹。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"protector",
					speed = 100,
					skill = protectionSkill(),
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.PARALYSIS,
							chancePercent = 100,
						),
					),
				),
				second = participant(
					"bypass-user",
					speed = 50,
					skill = contactStrike(),
					abilityEffects = listOf(BattleAbilityEffect.ContactSkillProtectionBypass),
					itemEffects = listOf(BattleItemEffect.ContactSideEffectImmunity),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "protector"),
				BattleAction.UseSkill("bypass-user", skillId = 1, targetActorId = "protector"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("contact-side-effect-immunity-does-not-suppress-protection-bypass")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals("bypass-user", resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().actorId)
		assertEquals(null, resolved.participant("bypass-user")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
	}

	private fun contactStrike(
		skillId: Long = 1,
		name: String = "接触攻击测试",
		priority: Int = 0,
		punchBased: Boolean = false,
	): BattleSkillSlot =
		damagingSkill(
			skillId = skillId,
			name = name,
			damageClass = BattleDamageClass.PHYSICAL,
			power = 40,
			makesContact = true,
			punchBased = punchBased,
			priority = priority,
		)

	private fun prioritySideGuardSkill(): BattleSkillSlot =
		damagingSkill(
			skillId = 501,
			name = "快速防守",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			protectsUserSideFromPrioritySkills = true,
			priority = 3,
		).copy(remainingPp = 15, maxPp = 15)
}
