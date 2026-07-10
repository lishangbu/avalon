package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
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
 * 验证声音类技能免疫特性的现代规则。
 *
 * 场景类型：命中前目标特性免疫 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，目标拥有声音类技能免疫时，其它成员使用的
 * 声音类伤害技能和变化技能都会在命中与效果写入前被阻止；目标自己使用的声音类技能不在该免疫范围内。
 * 若攻击方拥有无视目标特性效果，则对手目标的声音免疫会被本次技能跳过。
 */
class BattleSoundAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `sound immunity ability blocks sound damaging skill before damage`() {
		val scenario = publicBattleRuleScenario(
			name = "sound-immunity-ability-blocks-sound-damaging-skill",
			inputSummary = "目标拥有声音类技能免疫特性，对手使用声音类伤害技能。",
			expectedSummary = "目标特性在伤害前阻止技能，目标 HP 不变，不产生普通伤害事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(soundBased = true)),
				second = participant(
					"soundproof-target",
					speed = 80,
					abilityId = 43,
					abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "soundproof-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val blocked = resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().single()

		scenario.assertNamed("sound-immunity-ability-blocks-sound-damaging-skill")
		assertEquals(100, resolved.participant("soundproof-target")?.currentHp)
		assertEquals("soundproof-target", blocked.abilityHolderActorId)
		assertEquals(43, blocked.abilityId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `sound immunity ability blocks sound status skill before effect`() {
		val scenario = publicBattleRuleScenario(
			name = "sound-immunity-ability-blocks-sound-status-skill",
			inputSummary = "目标拥有声音类技能免疫特性，对手使用声音类变化技能并试图附加麻痹。",
			expectedSummary = "目标特性阻止该声音类变化技能，不写入主要异常状态，也不产生状态阻止事件。",
		)
		val soundStatusSkill = damagingSkill(
			soundBased = true,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.PARALYSIS,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = soundStatusSkill),
				second = participant(
					"soundproof-target",
					speed = 80,
					abilityId = 43,
					abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "soundproof-target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("sound-immunity-ability-blocks-sound-status-skill")
		assertEquals(null, resolved.participant("soundproof-target")?.majorStatus)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>().size)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>())
	}

	@Test
	fun `target ability ignore bypasses sound immunity ability`() {
		val scenario = publicBattleRuleScenario(
			name = "target-ability-ignore-bypasses-sound-immunity",
			inputSummary = "攻击方拥有无视目标特性效果，目标拥有声音类技能免疫特性，对手使用声音类伤害技能。",
			expectedSummary = "目标声音免疫被本次技能跳过，声音类伤害正常命中并造成直接伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(soundBased = true),
					abilityId = 104,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
				),
				second = participant(
					"soundproof-target",
					speed = 80,
					abilityId = 43,
					abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillImmunity()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "soundproof-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		scenario.assertNamed("target-ability-ignore-bypasses-sound-immunity")
		assertEquals(72, resolved.participant("soundproof-target")?.currentHp)
		assertEquals(28, damage.amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
	}
}
