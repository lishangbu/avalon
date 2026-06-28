package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 接触类特性公开对照 fixture。
 *
 * 现代主系列中，一部分防守方特性会在攻击方使用接触技能命中后，以固定概率把主要异常状态返还给攻击方。
 * 这组测试只验证状态机层的公共生命周期：必须在普通伤害成功后触发，100% 概率不消费额外随机数，非 100%
 * 概率使用独立随机掷点，并且仍要走攻击方自身的主要异常免疫流程。具体特性名称由资料层映射为
 * [BattleAbilityEffect.ContactStatusOnAttacker]，引擎不直接解析本地化名称。
 */
class BattleContactAbilityPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `contact status ability applies paralysis after successful contact like public fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "contact-status-ability-applies-paralysis-after-contact",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Static_(Ability)",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
			),
			inputSummary = "攻击方使用接触类物理技能命中目标；目标携带接触后 100% 麻痹攻击方的结构化特性效果。",
			expectedSummary = "目标先受到普通伤害；随后攻击方获得麻痹状态。100% 概率不会额外消费接触特性的随机数。",
		)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true),
				),
				second = participant(
					"defender",
					speed = 80,
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.PARALYSIS,
							chancePercent = 100,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)
		val statusEvent = resolved.events.filterIsInstance<BattleEvent.StatusApplied>().single()

		fixture.assertNamed("contact-status-ability-applies-paralysis-after-contact")
		assertEquals(72, resolved.participant("defender")?.currentHp)
		assertEquals(BattleMajorStatus.PARALYSIS, resolved.participant("attacker")?.majorStatus)
		assertEquals("defender", statusEvent.actorId)
		assertEquals("attacker", statusEvent.targetActorId)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}

	@Test
	fun `contact status ability skips status when chance roll fails like public fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "contact-status-ability-misses-when-chance-roll-fails",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Flame_Body_(Ability)",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
			),
			inputSummary = "攻击方使用接触类技能命中目标；目标携带接触后 30% 灼伤攻击方的结构化特性效果，脚本掷点为 81。",
			expectedSummary = "目标正常受到伤害；接触特性的概率掷点失败，攻击方不会获得灼伤状态。",
		)
		val random = ScriptedBattleRandom(listOf(1, 15, 80))
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true),
				),
				second = participant(
					"defender",
					speed = 80,
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.BURN,
							chancePercent = 30,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)

		fixture.assertNamed("contact-status-ability-misses-when-chance-roll-fails")
		assertEquals(72, resolved.participant("defender")?.currentHp)
		assertEquals(null, resolved.participant("attacker")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		assertEquals(
			listOf("critical hit for 1", "damage random for 1", "contact status for defender"),
			random.consumedReasons(),
		)
	}

	@Test
	fun `contact status ability respects attacker immunity like public fixture`() {
		val fixture = publicBattleRuleFixture(
			name = "contact-status-ability-respects-attacker-immunity",
			sourceUrls = listOf(
				"https://bulbapedia.bulbagarden.net/wiki/Static_(Ability)",
				"https://bulbapedia.bulbagarden.net/wiki/Limber_(Ability)",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
			),
			inputSummary = "攻击方使用接触类技能命中目标；目标接触后 100% 麻痹攻击方，但攻击方自身有麻痹免疫特性。",
			expectedSummary = "目标受到普通伤害；麻痹附加流程被攻击方特性免疫阻止，事件流记录 ABILITY 阻止原因。",
		)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(makesContact = true),
					abilityEffects = listOf(
						BattleAbilityEffect.MajorStatusImmunity(setOf(BattleMajorStatus.PARALYSIS)),
					),
				),
				second = participant(
					"defender",
					speed = 80,
					abilityEffects = listOf(
						BattleAbilityEffect.ContactStatusOnAttacker(
							status = BattleMajorStatus.PARALYSIS,
							chancePercent = 100,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			random,
		)
		val blocked = resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()

		fixture.assertNamed("contact-status-ability-respects-attacker-immunity")
		assertEquals(72, resolved.participant("defender")?.currentHp)
		assertEquals(null, resolved.participant("attacker")?.majorStatus)
		assertEquals("defender", blocked.actorId)
		assertEquals("attacker", blocked.targetActorId)
		assertEquals(BattleStatusBlockReason.ABILITY, blocked.reason)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), random.consumedReasons())
	}
}
