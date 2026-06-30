package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证现代主系列天气、场地、全场速度顺序和一侧场上状态的边界行为。
 *
 * 场景类型：环境状态和场上状态生命周期 fixture。
 * 参考来源类型：公开成熟对战引擎资料、公开天气/场地/一侧状态说明，以及公开道具资料。该批次不新增自由文本脚本，
 * 而是把已经结构化进入引擎的天气、场地、屏障、顺风、速度顺序和回合末环境回复/伤害规则逐条钉住。
 * 验证重点：环境覆盖、延长道具只匹配声明目标、命中失败不写入环境、持续回合在回合末统一推进、重复一侧状态不刷新、
 * 天气前置条件失败不写入状态、环境阶段 HP 变化最小值/夹取值，以及回复封锁对环境回复的抑制。
 */
class BattleEnvironmentFieldBoundaryPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `weather setting skill replaces existing weather and starts new duration`() {
		val fixture = fixture(
			name = "weather-setting-skill-replaces-existing-weather-and-starts-new-duration",
			inputSummary = "场上已有晴天剩余 3 回合，使用者成功使用设置下雨的变化技能。",
			expectedSummary = "天气被下雨覆盖，天气开始事件记录完整 5 回合；同一回合末统一递减后剩余 4 回合。",
		)
		val skill = environmentSkill(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN, turnsRemaining = 5))
		val state = engine.start(
			initialState(
				first = participant("weather-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN, weatherTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("weather-user", skillId = 201, targetActorId = "weather-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("weather-setting-skill-replaces-existing-weather-and-starts-new-duration")
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
		assertEquals(4, resolved.environment.weatherTurnsRemaining)
		val started = resolved.events.filterIsInstance<BattleEvent.WeatherStarted>().single()
		assertEquals("weather-user", started.actorId)
		assertEquals(BattleWeather.RAIN, started.weather)
		assertEquals(5, started.turnsRemaining)
	}

	@Test
	fun `terrain setting skill replaces existing terrain and starts new duration`() {
		val fixture = fixture(
			name = "terrain-setting-skill-replaces-existing-terrain-and-starts-new-duration",
			inputSummary = "场上已有电气场地剩余 3 回合，使用者成功使用设置青草场地的变化技能。",
			expectedSummary = "场地被青草场地覆盖，场地开始事件记录完整 5 回合；同一回合末统一递减后剩余 4 回合。",
		)
		val skill = environmentSkill(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.GRASSY, turnsRemaining = 5))
		val state = engine.start(
			initialState(
				first = participant("terrain-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC, terrainTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("terrain-user", skillId = 201, targetActorId = "terrain-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("terrain-setting-skill-replaces-existing-terrain-and-starts-new-duration")
		assertEquals(BattleTerrain.GRASSY, resolved.environment.terrain)
		assertEquals(4, resolved.environment.terrainTurnsRemaining)
		val started = resolved.events.filterIsInstance<BattleEvent.TerrainStarted>().single()
		assertEquals("terrain-user", started.actorId)
		assertEquals(BattleTerrain.GRASSY, started.terrain)
		assertEquals(5, started.turnsRemaining)
	}

	@Test
	fun `non matching weather extension item does not extend weather skill`() {
		val fixture = fixture(
			name = "non-matching-weather-extension-item-does-not-extend-weather-skill",
			inputSummary = "使用者携带只延长沙暴的道具效果，却成功使用设置下雨的变化技能。",
			expectedSummary = "道具目标天气不匹配，天气按普通 5 回合建立；同一回合末递减后剩余 4 回合。",
		)
		val skill = environmentSkill(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN, turnsRemaining = 5))
		val state = engine.start(
			initialState(
				first = participant(
					"weather-user",
					speed = 100,
					skill = skill,
					itemId = 285,
					itemEffects = listOf(
						BattleItemEffect.WeatherDurationExtension(
							weathers = setOf(BattleWeather.SANDSTORM),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("weather-user", skillId = 201, targetActorId = "weather-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("non-matching-weather-extension-item-does-not-extend-weather-skill")
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
		assertEquals(4, resolved.environment.weatherTurnsRemaining)
		assertEquals(5, resolved.events.filterIsInstance<BattleEvent.WeatherStarted>().single().turnsRemaining)
	}

	@Test
	fun `non matching terrain extension item does not extend terrain skill`() {
		val fixture = fixture(
			name = "non-matching-terrain-extension-item-does-not-extend-terrain-skill",
			inputSummary = "使用者携带只延长电气场地的道具效果，却成功使用设置青草场地的变化技能。",
			expectedSummary = "道具目标场地不匹配，场地按普通 5 回合建立；同一回合末递减后剩余 4 回合。",
		)
		val skill = environmentSkill(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.GRASSY, turnsRemaining = 5))
		val state = engine.start(
			initialState(
				first = participant(
					"terrain-user",
					speed = 100,
					skill = skill,
					itemId = 896,
					itemEffects = listOf(
						BattleItemEffect.TerrainDurationExtension(
							terrains = setOf(BattleTerrain.ELECTRIC),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("terrain-user", skillId = 201, targetActorId = "terrain-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("non-matching-terrain-extension-item-does-not-extend-terrain-skill")
		assertEquals(BattleTerrain.GRASSY, resolved.environment.terrain)
		assertEquals(4, resolved.environment.terrainTurnsRemaining)
		assertEquals(5, resolved.events.filterIsInstance<BattleEvent.TerrainStarted>().single().turnsRemaining)
	}

	@Test
	fun `missed weather setting skill does not change weather`() {
		val fixture = fixture(
			name = "missed-weather-setting-skill-does-not-change-weather",
			inputSummary = "带命中判定的变化技能尝试设置下雨，但命中随机超过有效命中率。",
			expectedSummary = "技能未命中后不写入天气，也不产生天气开始事件。",
		)
		val skill = environmentSkill(
			effect = BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN, turnsRemaining = 5),
			accuracy = 50,
		)
		val random = ScriptedBattleRandom(listOf(50))
		val state = engine.start(
			initialState(
				first = participant("weather-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("weather-user", skillId = 201, targetActorId = "observer")),
			random,
		)

		fixture.assertNamed("missed-weather-setting-skill-does-not-change-weather")
		assertEquals(BattleWeather.NONE, resolved.environment.weather)
		assertNull(resolved.environment.weatherTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.WeatherStarted>())
		assertEquals(listOf("accuracy for 201"), random.consumedReasons())
	}

	@Test
	fun `field speed order duration decrements without ending`() {
		val fixture = fixture(
			name = "field-speed-order-duration-decrements-without-ending",
			inputSummary = "全场速度顺序效果剩余 2 回合，结算一个没有行动的完整回合。",
			expectedSummary = "回合末持续时间递减为 1，不产生全场速度顺序结束事件。",
		)
		val state = engine.start(
			initialState(
				environment = BattleEnvironment(
					fieldSpeedOrderEffect = BattleFieldSpeedOrderEffect(BattleFieldSpeedOrderKind.TRICK_ROOM, turnsRemaining = 2),
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("field-speed-order-duration-decrements-without-ending")
		assertEquals(1, resolved.environment.fieldSpeedOrderEffect?.turnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.FieldSpeedOrderEnded>())
	}

	@Test
	fun `field speed order duration ends before turn end`() {
		val fixture = fixture(
			name = "field-speed-order-duration-ends-before-turn-end",
			inputSummary = "全场速度顺序效果剩余 1 回合，结算一个没有行动的完整回合。",
			expectedSummary = "回合末效果被清除，结束事件出现在回合结束事件之前。",
		)
		val state = engine.start(
			initialState(
				environment = BattleEnvironment(
					fieldSpeedOrderEffect = BattleFieldSpeedOrderEffect(BattleFieldSpeedOrderKind.TRICK_ROOM, turnsRemaining = 1),
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("field-speed-order-duration-ends-before-turn-end")
		assertNull(resolved.environment.fieldSpeedOrderEffect)
		assertEquals(BattleFieldSpeedOrderKind.TRICK_ROOM, resolved.events.filterIsInstance<BattleEvent.FieldSpeedOrderEnded>().single().kind)
		assertEquals(BattleEvent.TurnEnded::class, resolved.events.last()::class)
	}

	@Test
	fun `side damage reduction duration decrements without ending`() {
		val fixture = fixture(
			name = "side-damage-reduction-duration-decrements-without-ending",
			inputSummary = "目标侧已有物理伤害减免屏障剩余 2 回合，结算一个没有行动的完整回合。",
			expectedSummary = "回合末屏障剩余回合递减为 1，屏障仍保留在该侧场上状态中。",
		)
		val state = engine.start(
			initialState(
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 2),
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("side-damage-reduction-duration-decrements-without-ending")
		assertEquals(1, resolved.sideOf("side-b-active")?.damageReductions?.single()?.turnsRemaining)
	}

	@Test
	fun `side damage reduction duration ends silently`() {
		val fixture = fixture(
			name = "side-damage-reduction-duration-ends-silently",
			inputSummary = "目标侧已有物理伤害减免屏障剩余 1 回合，结算一个没有行动的完整回合。",
			expectedSummary = "回合末屏障从该侧场上状态中移除；当前模型不为自然移除追加额外事件。",
		)
		val state = engine.start(
			initialState(
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 1),
				),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("side-damage-reduction-duration-ends-silently")
		assertEquals(emptyList(), resolved.sideOf("side-b-active")?.damageReductions)
	}

	@Test
	fun `side speed modifier duration decrements without ending`() {
		val fixture = fixture(
			name = "side-speed-modifier-duration-decrements-without-ending",
			inputSummary = "己方侧已有顺风速度修正剩余 2 回合，结算一个没有行动的完整回合。",
			expectedSummary = "回合末速度修正剩余回合递减为 1，修正仍保留在该侧场上状态中。",
		)
		val state = withSideSpeedModifier(
			engine.start(initialState()),
			sideId = "side-a",
			modifier = BattleSideSpeedModifier(BattleSideSpeedModifierKind.TAILWIND, turnsRemaining = 2),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("side-speed-modifier-duration-decrements-without-ending")
		assertEquals(1, resolved.sideOf("side-a-active")?.speedModifiers?.single()?.turnsRemaining)
	}

	@Test
	fun `side speed modifier duration ends silently`() {
		val fixture = fixture(
			name = "side-speed-modifier-duration-ends-silently",
			inputSummary = "己方侧已有顺风速度修正剩余 1 回合，结算一个没有行动的完整回合。",
			expectedSummary = "回合末速度修正从该侧场上状态中移除；当前模型不为自然移除追加额外事件。",
		)
		val state = withSideSpeedModifier(
			engine.start(initialState()),
			sideId = "side-a",
			modifier = BattleSideSpeedModifier(BattleSideSpeedModifierKind.TAILWIND, turnsRemaining = 1),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("side-speed-modifier-duration-ends-silently")
		assertEquals(emptyList(), resolved.sideOf("side-a-active")?.speedModifiers)
	}

	@Test
	fun `duplicate side damage reduction does not refresh duration`() {
		val fixture = fixture(
			name = "duplicate-side-damage-reduction-does-not-refresh-duration",
			inputSummary = "己方侧已有物理屏障剩余 3 回合，使用者再次成功使用同类物理屏障技能。",
			expectedSummary = "重复屏障不会刷新或追加新状态；回合末原有屏障只按自然流程递减为 2 回合。",
		)
		val screenSkill = sideDamageReductionSkill(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 5)
		val state = engine.start(
			initialState(
				first = participant("support", speed = 100, skill = screenSkill),
				second = participant("observer", speed = 50),
				firstSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 3),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("support", skillId = 301, targetActorId = "support")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("duplicate-side-damage-reduction-does-not-refresh-duration")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideDamageReductionStarted>())
		assertEquals(2, resolved.sideOf("support")?.damageReductions?.single()?.turnsRemaining)
	}

	@Test
	fun `duplicate side speed modifier does not refresh duration`() {
		val fixture = fixture(
			name = "duplicate-side-speed-modifier-does-not-refresh-duration",
			inputSummary = "己方侧已有顺风剩余 3 回合，使用者再次成功使用建立顺风的变化技能。",
			expectedSummary = "重复速度修正不会刷新或追加新状态；回合末原有顺风只按自然流程递减为 2 回合。",
		)
		val tailwindSkill = sideSpeedModifierSkill(BattleSideSpeedModifier(BattleSideSpeedModifierKind.TAILWIND, turnsRemaining = 4))
		val started = engine.start(
			initialState(
				first = participant("support", speed = 100, skill = tailwindSkill),
				second = participant("observer", speed = 50),
			),
		)
		val state = withSideSpeedModifier(
			started,
			sideId = "side-a",
			modifier = BattleSideSpeedModifier(BattleSideSpeedModifierKind.TAILWIND, turnsRemaining = 3),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("support", skillId = 302, targetActorId = "support")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("duplicate-side-speed-modifier-does-not-refresh-duration")
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideSpeedModifierStarted>())
		assertEquals(2, resolved.sideOf("support")?.speedModifiers?.single()?.turnsRemaining)
	}

	@Test
	fun `required weather side condition fails without weather`() {
		val fixture = fixture(
			name = "required-weather-side-condition-fails-without-weather",
			inputSummary = "技能声明只有雪天才能建立全伤害屏障，但当前天气为无天气。",
			expectedSummary = "天气前置条件不满足时不写入一侧场上状态，也不产生屏障开始事件。",
		)
		val snowScreenSkill = sideDamageReductionSkill(
			kind = BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE,
			turnsRemaining = 5,
			requiredWeather = BattleWeather.SNOW,
		)
		val state = engine.start(
			initialState(
				first = participant("support", speed = 100, skill = snowScreenSkill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("support", skillId = 301, targetActorId = "support")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("required-weather-side-condition-fails-without-weather")
		assertEquals(emptyList(), resolved.sideOf("support")?.damageReductions)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SideDamageReductionStarted>())
	}

	@Test
	fun `non matching screen extension item does not extend side damage reduction`() {
		val fixture = fixture(
			name = "non-matching-screen-extension-item-does-not-extend-side-damage-reduction",
			inputSummary = "使用者携带只延长特殊屏障的道具效果，却成功建立物理屏障。",
			expectedSummary = "道具目标屏障类型不匹配，物理屏障按普通 5 回合建立；同一回合末递减后剩余 4 回合。",
		)
		val screenSkill = sideDamageReductionSkill(BattleSideDamageReductionKind.PHYSICAL, turnsRemaining = 5)
		val state = engine.start(
			initialState(
				first = participant(
					"support",
					speed = 100,
					skill = screenSkill,
					itemId = 246,
					itemEffects = listOf(
						BattleItemEffect.SideDamageReductionDurationExtension(
							kinds = setOf(BattleSideDamageReductionKind.SPECIAL),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("support", skillId = 301, targetActorId = "support")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("non-matching-screen-extension-item-does-not-extend-side-damage-reduction")
		val started = resolved.events.filterIsInstance<BattleEvent.SideDamageReductionStarted>().single()
		assertEquals(BattleSideDamageReductionKind.PHYSICAL, started.kind)
		assertEquals(5, started.turnsRemaining)
		assertEquals(4, resolved.sideOf("support")?.damageReductions?.single()?.turnsRemaining)
	}

	@Test
	fun `sandstorm damage resolves before weather healing`() {
		val fixture = fixture(
			name = "sandstorm-damage-resolves-before-weather-healing",
			inputSummary = "沙暴下普通属性成员拥有匹配天气回复特性，当前 HP 为 80。",
			expectedSummary = "回合末先承受 1/16 沙暴伤害，再按 1/16 天气回复，事件顺序保持天气伤害在天气回复之前。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"sand-healer",
					speed = 100,
					currentHp = 80,
					elementId = 1,
					abilityEffects = listOf(
						BattleAbilityEffect.WeatherEndTurnHeal(setOf(BattleWeather.SANDSTORM), healDenominator = 16),
					),
				),
				second = participant("rock-observer", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("sandstorm-damage-resolves-before-weather-healing")
		assertEquals(80, resolved.participant("sand-healer")?.currentHp)
		val environmentEvents = resolved.events.filter {
			it is BattleEvent.WeatherDamageApplied || it is BattleEvent.WeatherHealingApplied
		}
		assertEquals(BattleEvent.WeatherDamageApplied::class, environmentEvents.first()::class)
		assertEquals(BattleEvent.WeatherHealingApplied::class, environmentEvents.last()::class)
		assertEquals(6, resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>().single().amount)
		assertEquals(6, resolved.events.filterIsInstance<BattleEvent.WeatherHealingApplied>().single().amount)
	}

	@Test
	fun `weather damage uses minimum one hp`() {
		val fixture = fixture(
			name = "weather-damage-uses-minimum-one-hp",
			inputSummary = "最大 HP 只有 10 的普通属性成员处于沙暴中。",
			expectedSummary = "沙暴按最大 HP 1/16 计算时向下取整不足 1，但固定伤害至少为 1 点。",
		)
		val fragile = participant("fragile", speed = 100, elementId = 1).copy(maxHp = 10, currentHp = 10)
		val state = engine.start(
			initialState(
				first = fragile,
				second = participant("rock-observer", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("weather-damage-uses-minimum-one-hp")
		assertEquals(9, resolved.participant("fragile")?.currentHp)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>().single().amount)
	}

	@Test
	fun `weather healing clamps to missing hp`() {
		val fixture = fixture(
			name = "weather-healing-clamps-to-missing-hp",
			inputSummary = "下雨下天气回复特性成员当前 HP 为 98，最大 HP 为 100。",
			expectedSummary = "天气回复理论值为 6，但实际回复被夹取到缺失的 2 点 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"rain-healer",
					speed = 100,
					currentHp = 98,
					abilityEffects = listOf(
						BattleAbilityEffect.WeatherEndTurnHeal(setOf(BattleWeather.RAIN), healDenominator = 16),
					),
				),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("weather-healing-clamps-to-missing-hp")
		assertEquals(100, resolved.participant("rain-healer")?.currentHp)
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.WeatherHealingApplied>().single().amount)
	}

	@Test
	fun `grassy terrain healing clamps to missing hp`() {
		val fixture = fixture(
			name = "grassy-terrain-healing-clamps-to-missing-hp",
			inputSummary = "青草场地中接地成员当前 HP 为 98，最大 HP 为 100。",
			expectedSummary = "青草场地回复理论值为 6，但实际回复被夹取到缺失的 2 点 HP。",
		)
		val state = engine.start(
			initialState(
				first = participant("grounded", speed = 100, currentHp = 98, grounded = true),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("grassy-terrain-healing-clamps-to-missing-hp")
		assertEquals(100, resolved.participant("grounded")?.currentHp)
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.TerrainHealingApplied>().single().amount)
	}

	@Test
	fun `heal block suppresses weather and terrain healing`() {
		val fixture = fixture(
			name = "heal-block-suppresses-weather-and-terrain-healing",
			inputSummary = "一个成员在下雨中拥有天气回复特性，另一个成员在青草场地中接地；两者都处于回复封锁。",
			expectedSummary = "回复封锁会分别抑制天气回复和场地回复，不产生对应回复事件，也不改变 HP。",
		)
		val weatherState = engine.start(
			initialState(
				first = participant(
					"rain-healer",
					speed = 100,
					currentHp = 80,
					abilityEffects = listOf(
						BattleAbilityEffect.WeatherEndTurnHeal(setOf(BattleWeather.RAIN), healDenominator = 16),
					),
				).copy(healBlockTurnsRemaining = 2),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)
		val terrainState = engine.start(
			initialState(
				first = participant("terrain-healer", speed = 100, currentHp = 80, grounded = true)
					.copy(healBlockTurnsRemaining = 2),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val afterWeather = engine.resolveTurn(weatherState, emptyList(), ScriptedBattleRandom(emptyList()))
		val afterTerrain = engine.resolveTurn(terrainState, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("heal-block-suppresses-weather-and-terrain-healing")
		assertEquals(80, afterWeather.participant("rain-healer")?.currentHp)
		assertEquals(emptyList(), afterWeather.events.filterIsInstance<BattleEvent.WeatherHealingApplied>())
		assertEquals(80, afterTerrain.participant("terrain-healer")?.currentHp)
		assertEquals(emptyList(), afterTerrain.events.filterIsInstance<BattleEvent.TerrainHealingApplied>())
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

	private fun environmentSkill(
		effect: BattleSkillEnvironmentEffect,
		accuracy: Int? = null,
	): BattleSkillSlot =
		damagingSkill(
			skillId = 201,
			name = "环境设置测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = accuracy,
			affectedByProtect = false,
			environmentEffects = listOf(effect),
		)

	private fun sideDamageReductionSkill(
		kind: BattleSideDamageReductionKind,
		turnsRemaining: Int,
		requiredWeather: BattleWeather? = null,
	): BattleSkillSlot =
		damagingSkill(
			skillId = 301,
			name = "一侧屏障测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			sideConditionApplications = listOf(
				BattleSideConditionApplication(
					targetSide = BattleSideConditionTarget.USER_SIDE,
					damageReduction = BattleSideDamageReduction(kind, turnsRemaining),
					requiredWeather = requiredWeather,
				),
			),
		)

	private fun sideSpeedModifierSkill(modifier: BattleSideSpeedModifier): BattleSkillSlot =
		damagingSkill(
			skillId = 302,
			name = "一侧速度测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			sideSpeedModifierApplications = listOf(
				BattleSideSpeedModifierApplication(
					targetSide = BattleSideConditionTarget.USER_SIDE,
					speedModifier = modifier,
				),
			),
		)

	private fun withSideSpeedModifier(
		state: BattleState,
		sideId: String,
		modifier: BattleSideSpeedModifier,
	): BattleState =
		state.copy(
			sides = state.sides.map { side ->
				if (side.sideId == sideId) {
					side.copy(speedModifiers = listOf(modifier))
				} else {
					side
				}
			},
		)
}
