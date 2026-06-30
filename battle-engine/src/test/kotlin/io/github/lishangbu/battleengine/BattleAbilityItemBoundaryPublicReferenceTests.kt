package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证特性和携带道具效果在非匹配、绕过和边界值上的公开对照行为。
 *
 * 场景类型：目标前置特性、防守特性绕过、环境联动特性、伤害后道具、低体力回复道具和抗性减伤道具 fixture。
 * 参考来源类型：公开成熟对战引擎的特性/道具资料、公开行动结算实现、公开天气/场地/属性修正说明。该批次不新增
 * 资料库概念，只把已经结构化的可执行效果固定到更细的失败条件和事件顺序上。
 * 验证重点：吸收特性必须匹配属性且必须命中，能力阶级吸收达到上限时仍阻止技能但不伪造提阶事件，无视目标特性
 * 不能扩散到同侧目标或携带道具，声音/先制免疫只在对应技能标签和敌对目标关系下触发，天气/场地速度与回复特性
 * 只在环境匹配时生效，道具回复与减伤在缺失 HP、倒下、非消耗和满 HP 边界上保持稳定。
 */
class BattleAbilityItemBoundaryPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `element absorb heal ignores non matching element`() {
		val fixture = fixture(
			name = "element-absorb-heal-ignores-non-matching-element",
			inputSummary = "目标拥有吸收电属性技能并回复的结构化特性，对手使用草属性物理攻击。",
			expectedSummary = "技能属性不匹配，目标特性不触发；技能进入普通伤害流程并造成直接伤害。",
		)
		val skill = damagingSkill(name = "非匹配吸收测试", elementId = 12)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2, skill = skill),
				second = participant(
					"absorber",
					speed = 50,
					currentHp = 50,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 13)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("element-absorb-heal-ignores-non-matching-element")
		assertEquals(31, resolved.participant("absorber")?.currentHp)
		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>())
	}

	@Test
	fun `element absorb heal waits for hit and skips on miss`() {
		val fixture = fixture(
			name = "element-absorb-heal-waits-for-hit-and-skips-on-miss",
			inputSummary = "目标拥有吸收电属性技能并回复的结构化特性，对手使用带命中判定的电属性技能但命中失败。",
			expectedSummary = "技能未命中时不会进入吸收特性阶段，目标 HP 不变，也不产生吸收事件。",
		)
		val skill = damagingSkill(name = "未命中吸收测试", elementId = 13, accuracy = 50)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = skill),
				second = participant(
					"absorber",
					speed = 50,
					currentHp = 50,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 13)),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(listOf(99)),
		)

		fixture.assertNamed("element-absorb-heal-waits-for-hit-and-skips-on-miss")
		assertEquals(50, resolved.participant("absorber")?.currentHp)
		assertEquals(100, resolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>())
	}

	@Test
	fun `element absorb stat at upper bound absorbs without stage event`() {
		val fixture = fixture(
			name = "element-absorb-stat-at-upper-bound-absorbs-without-stage-event",
			inputSummary = "目标速度阶级已经为 +6，并拥有吸收电属性技能后提升速度的结构化特性。",
			expectedSummary = "技能仍被目标特性吸收并阻止伤害，但速度阶级已达上限，因此不产生能力阶级变化事件。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = damagingSkill(elementId = 13)),
				second = participant(
					"absorber",
					speed = 50,
					abilityEffects = listOf(
						BattleAbilityEffect.ElementSkillAbsorbStatStage(
							elementId = 13,
							stat = BattleStat.SPEED,
							stageDelta = 1,
						),
					),
				).copy(statStages = mapOf(BattleStat.SPEED to 6)),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "absorber")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("element-absorb-stat-at-upper-bound-absorbs-without-stage-event")
		assertEquals(6, resolved.participant("absorber")?.statStage(BattleStat.SPEED))
		assertEquals(13, resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single().elementId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `target ability ignore does not bypass held damage reduction item`() {
		val fixture = fixture(
			name = "target-ability-ignore-does-not-bypass-held-damage-reduction-item",
			inputSummary = "攻击方拥有无视目标特性效果，目标携带火属性抗性减伤道具并受到效果绝佳火属性技能。",
			expectedSummary = "无视目标特性不会影响目标携带道具，抗性减伤仍按 0.5 倍触发并消费道具。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = damagingSkill(elementId = 10),
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects),
				),
				second = participant(
					"holder",
					speed = 50,
					elementId = 12,
					itemId = 161,
					itemEffects = listOf(BattleItemEffect.ElementDamageReduction(elementId = 10, multiplier = 0.5)),
				),
				rules = fireSuperEffectiveAgainstGrassRules(),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val reduced = resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>().single()

		fixture.assertNamed("target-ability-ignore-does-not-bypass-held-damage-reduction-item")
		assertEquals(81, resolved.participant("holder")?.currentHp)
		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(true, reduced.consumed)
		assertEquals(null, resolved.participant("holder")?.itemId)
	}

	@Test
	fun `target ability ignore keeps same side target ability active`() {
		val fixture = fixture(
			name = "target-ability-ignore-keeps-same-side-target-ability-active",
			inputSummary = "双打中攻击方拥有无视目标特性效果，却把匹配属性技能指向同侧伙伴；伙伴拥有属性吸收回复特性。",
			expectedSummary = "无视目标特性只作用于敌对目标，同侧伙伴的吸收特性仍触发并阻止技能继续结算。",
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant(
					"attacker",
					speed = 100,
					skill = damagingSkill(elementId = 13),
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects),
				),
				firstB = participant(
					"ally",
					speed = 80,
					currentHp = 50,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 13)),
				),
				secondA = participant("opponent-a", speed = 60),
				secondB = participant("opponent-b", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "ally")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("target-ability-ignore-keeps-same-side-target-ability-active")
		assertEquals(75, resolved.participant("ally")?.currentHp)
		assertEquals(25, resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single().healAmount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `sound immunity does not block user own sound skill`() {
		val fixture = fixture(
			name = "sound-immunity-does-not-block-user-own-sound-skill",
			inputSummary = "成员拥有声音类技能免疫特性，并把一个声音类变化技能指向自己。",
			expectedSummary = "声音免疫只阻止其它成员的声音类技能，不阻止拥有者自己使用声音类技能。",
		)
		val skill = damagingSkill(
			skillId = 260,
			name = "自身声音测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			soundBased = true,
		)
		val state = engine.start(
			initialState(
				first = participant(
					"speaker",
					speed = 100,
					skill = skill,
					abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillImmunity),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("speaker", skillId = 260, targetActorId = "speaker")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("sound-immunity-does-not-block-user-own-sound-skill")
		assertEquals("speaker", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().actorId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
	}

	@Test
	fun `sound immunity ignores non sound skill`() {
		val fixture = fixture(
			name = "sound-immunity-ignores-non-sound-skill",
			inputSummary = "目标拥有声音类技能免疫特性，对手使用没有声音标签的普通物理技能。",
			expectedSummary = "技能标签不匹配，声音免疫不触发；技能进入普通伤害流程。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2),
				second = participant(
					"listener",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.SoundBasedSkillImmunity),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "listener")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("sound-immunity-ignores-non-sound-skill")
		assertEquals(81, resolved.participant("listener")?.currentHp)
		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
	}

	@Test
	fun `priority side immunity ignores same side priority support`() {
		val fixture = fixture(
			name = "priority-side-immunity-ignores-same-side-priority-support",
			inputSummary = "双打中一侧成员拥有先制技能侧防护特性，同侧伙伴使用先制变化技能指向它。",
			expectedSummary = "先制侧防护只阻止敌对成员的先制技能，同侧辅助不会被该特性阻挡。",
		)
		val skill = damagingSkill(
			skillId = 261,
			name = "同侧先制测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			priority = 1,
		)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("supporter", speed = 100, skill = skill),
				firstB = participant(
					"protected",
					speed = 80,
					abilityEffects = listOf(BattleAbilityEffect.PriorityMoveImmunityForSide()),
				),
				secondA = participant("opponent-a", speed = 60),
				secondB = participant("opponent-b", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("supporter", skillId = 261, targetActorId = "protected")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("priority-side-immunity-ignores-same-side-priority-support")
		assertEquals("supporter", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().single().actorId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
	}

	@Test
	fun `priority side immunity ignores non priority skill`() {
		val fixture = fixture(
			name = "priority-side-immunity-ignores-non-priority-skill",
			inputSummary = "目标侧当前上场成员拥有先制技能侧防护特性，对手使用普通优先度物理技能。",
			expectedSummary = "技能没有先制优先度，侧防护特性不触发；目标正常受到伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2),
				second = participant(
					"protected",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.PriorityMoveImmunityForSide()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protected")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("priority-side-immunity-ignores-non-priority-skill")
		assertEquals(81, resolved.participant("protected")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
	}

	@Test
	fun `priority side immunity can be bypassed by target ability ignore`() {
		val fixture = fixture(
			name = "priority-side-immunity-can-be-bypassed-by-target-ability-ignore",
			inputSummary = "攻击方拥有无视目标特性效果，使用先制物理技能攻击拥有先制侧防护特性的目标。",
			expectedSummary = "目标侧防护特性被本次技能绕过，先制技能继续进入普通伤害流程。",
		)
		val skill = damagingSkill(name = "绕过先制防护测试", priority = 1)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					elementId = 2,
					skill = skill,
					abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects),
				),
				second = participant(
					"protected",
					speed = 50,
					abilityEffects = listOf(BattleAbilityEffect.PriorityMoveImmunityForSide()),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "protected")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("priority-side-immunity-can-be-bypassed-by-target-ability-ignore")
		assertEquals(81, resolved.participant("protected")?.currentHp)
		assertEquals(19, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByAbility>())
	}

	@Test
	fun `weather damage immunity requires matching weather`() {
		val fixture = fixture(
			name = "weather-damage-immunity-requires-matching-weather",
			inputSummary = "成员只有雪景伤害免疫特性，但当前天气是沙暴。",
			expectedSummary = "免疫天气不匹配时不会阻止沙暴回合末伤害。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"weathered",
					speed = 100,
					abilityEffects = listOf(BattleAbilityEffect.WeatherDamageImmunity(setOf(BattleWeather.SNOW))),
				),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("weather-damage-immunity-requires-matching-weather")
		assertEquals(94, resolved.participant("weathered")?.currentHp)
		assertEquals(6, resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>().first { it.actorId == "weathered" }.amount)
	}

	@Test
	fun `weather healing ability requires matching weather`() {
		val fixture = fixture(
			name = "weather-healing-ability-requires-matching-weather",
			inputSummary = "成员拥有下雨时回合末回复的结构化特性，但当前天气是晴天。",
			expectedSummary = "天气不匹配时不会触发天气回复事件，成员 HP 保持不变。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"healer",
					speed = 100,
					currentHp = 50,
					abilityEffects = listOf(BattleAbilityEffect.WeatherEndTurnHeal(setOf(BattleWeather.RAIN), healDenominator = 16)),
				),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("weather-healing-ability-requires-matching-weather")
		assertEquals(50, resolved.participant("healer")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.WeatherHealingApplied>())
	}

	@Test
	fun `weather speed multiplier requires matching weather`() {
		val fixture = fixture(
			name = "weather-speed-multiplier-requires-matching-weather",
			inputSummary = "较慢成员拥有下雨时速度翻倍的结构化特性，但当前天气是晴天；较快对手同优先度行动。",
			expectedSummary = "天气不匹配时速度倍率不生效，较快对手先行动。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"weather-runner",
					speed = 60,
					abilityEffects = listOf(BattleAbilityEffect.WeatherSpeedMultiplier(BattleWeather.RAIN, multiplier = 2.0)),
				),
				second = participant("fast-opponent", speed = 100),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("weather-runner", skillId = 1, targetActorId = "fast-opponent"),
				BattleAction.UseSkill("fast-opponent", skillId = 1, targetActorId = "weather-runner"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		fixture.assertNamed("weather-speed-multiplier-requires-matching-weather")
		assertEquals("fast-opponent", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}

	@Test
	fun `terrain speed multiplier requires matching terrain`() {
		val fixture = fixture(
			name = "terrain-speed-multiplier-requires-matching-terrain",
			inputSummary = "较慢成员拥有电气场地下速度翻倍的结构化特性，但当前场地是青草场地；较快对手同优先度行动。",
			expectedSummary = "场地不匹配时速度倍率不生效，较快对手先行动。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"terrain-runner",
					speed = 60,
					abilityEffects = listOf(BattleAbilityEffect.TerrainSpeedMultiplier(BattleTerrain.ELECTRIC, multiplier = 2.0)),
				),
				second = participant("fast-opponent", speed = 100),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("terrain-runner", skillId = 1, targetActorId = "fast-opponent"),
				BattleAction.UseSkill("fast-opponent", skillId = 1, targetActorId = "terrain-runner"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		fixture.assertNamed("terrain-speed-multiplier-requires-matching-terrain")
		assertEquals("fast-opponent", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}

	@Test
	fun `damage dealt healing item clamps to missing hp`() {
		val fixture = fixture(
			name = "damage-dealt-healing-item-clamps-to-missing-hp",
			inputSummary = "使用者只缺失 1 HP，携带按造成伤害八分之一回复的道具并造成 28 点实际伤害。",
			expectedSummary = "理论回复为 3 点，但实际回复夹取到缺失的 1 点 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					currentHp = 99,
					itemEffects = listOf(BattleItemEffect.DamageDealtHeal(healDenominator = 8)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("damage-dealt-healing-item-clamps-to-missing-hp")
		assertEquals(100, resolved.participant("attacker")?.currentHp)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single().amount)
	}

	@Test
	fun `damage dealt healing item skips full hp user`() {
		val fixture = fixture(
			name = "damage-dealt-healing-item-skips-full-hp-user",
			inputSummary = "满 HP 使用者携带按造成伤害回复的道具并成功造成直接伤害。",
			expectedSummary = "使用者没有缺失 HP，因此道具不产生回复事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"attacker",
					speed = 100,
					itemEffects = listOf(BattleItemEffect.DamageDealtHeal(healDenominator = 8)),
				),
				second = participant("defender", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("damage-dealt-healing-item-skips-full-hp-user")
		assertEquals(100, resolved.participant("attacker")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HealingApplied>())
	}

	@Test
	fun `low hp healing item clamps to missing hp and consumes`() {
		val fixture = fixture(
			name = "low-hp-healing-item-clamps-to-missing-hp-and-consumes",
			inputSummary = "目标携带高触发线低体力回复道具，受伤后达到触发线且理论回复超过缺失 HP。",
			expectedSummary = "道具回复夹取到缺失 HP，使目标回到满 HP，并在触发后被消费。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant(
					"holder",
					speed = 50,
					currentHp = 95,
					itemId = 2001,
					itemEffects = listOf(
						BattleItemEffect.LowHpHeal(
							triggerHpNumerator = 9,
							triggerHpDenominator = 10,
							healDenominator = 2,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("low-hp-healing-item-clamps-to-missing-hp-and-consumes")
		assertEquals(100, resolved.participant("holder")?.currentHp)
		assertEquals(33, resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single().amount)
		assertEquals(null, resolved.participant("holder")?.itemId)
		assertEquals(emptyList(), resolved.participant("holder")?.itemEffects)
	}

	@Test
	fun `low hp healing item skips fainted holder`() {
		val fixture = fixture(
			name = "low-hp-healing-item-skips-fainted-holder",
			inputSummary = "目标携带低体力回复道具，但本次直接伤害把目标 HP 扣到 0。",
			expectedSummary = "已经倒下的成员不会触发低体力回复，道具不消费，也不产生回复事件。",
		)
		val itemEffect = BattleItemEffect.LowHpHeal(fixedHealAmount = 10)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100),
				second = participant(
					"holder",
					speed = 50,
					currentHp = 10,
					itemId = 2002,
					itemEffects = listOf(itemEffect),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		fixture.assertNamed("low-hp-healing-item-skips-fainted-holder")
		assertEquals(0, resolved.participant("holder")?.currentHp)
		assertEquals(2002, resolved.participant("holder")?.itemId)
		assertEquals(listOf(itemEffect), resolved.participant("holder")?.itemEffects)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HealingApplied>())
	}

	@Test
	fun `held end turn healing item clamps to missing hp`() {
		val fixture = fixture(
			name = "held-end-turn-healing-item-clamps-to-missing-hp",
			inputSummary = "成员只缺失 2 HP，并携带回合末回复最大 HP 1/16 的道具。",
			expectedSummary = "理论回复为 6 点，但实际回合末回复夹取为缺失的 2 点 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"holder",
					speed = 100,
					currentHp = 98,
					itemEffects = listOf(BattleItemEffect.HeldEndTurnHeal(healDenominator = 16)),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("held-end-turn-healing-item-clamps-to-missing-hp")
		assertEquals(100, resolved.participant("holder")?.currentHp)
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.HealingApplied>().single().amount)
	}

	@Test
	fun `non consumable element damage reduction keeps held item`() {
		val fixture = fixture(
			name = "non-consumable-element-damage-reduction-keeps-held-item",
			inputSummary = "目标携带不消费的火属性减伤效果，受到效果绝佳火属性物理技能。",
			expectedSummary = "减伤按 0.5 倍生效，但道具不会被消费，目标仍保留携带道具和效果。",
		)
		val reduction = BattleItemEffect.ElementDamageReduction(
			elementId = 10,
			multiplier = 0.5,
			consumesItem = false,
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 2, skill = damagingSkill(elementId = 10)),
				second = participant(
					"holder",
					speed = 50,
					elementId = 12,
					itemId = 2003,
					itemEffects = listOf(reduction),
				),
				rules = fireSuperEffectiveAgainstGrassRules(),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val reduced = resolved.events.filterIsInstance<BattleEvent.DamageReducedByItem>().single()

		fixture.assertNamed("non-consumable-element-damage-reduction-keeps-held-item")
		assertEquals(81, resolved.participant("holder")?.currentHp)
		assertEquals(2003, resolved.participant("holder")?.itemId)
		assertEquals(listOf(reduction), resolved.participant("holder")?.itemEffects)
		assertEquals(false, reduced.consumed)
	}

	private fun fixture(
		name: String,
		inputSummary: String,
		expectedSummary: String,
	): PublicBattleRuleFixture =
		publicBattleRuleFixture(
			name = name,
			inputSummary = inputSummary,
			expectedSummary = expectedSummary,
		)

	private fun fireSuperEffectiveAgainstGrassRules() =
		neutralRules().copy(
			elementChart = ElementEffectivenessChart(
				mapOf(10L to mapOf(12L to 2.0)),
			),
		)
}
