package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleCoverageItemResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageSummaryResponse
import org.springframework.stereotype.Service

/**
 * 战斗规则实现覆盖报告服务。
 *
 * 该服务不读取数据库，因为覆盖状态来自代码、事件流和公开 fixture，而不是运营人员维护的资料。
 * 管理端使用它快速判断当前战斗引擎哪些规则已有公开对照、哪些只是部分接入、哪些仍在计划中。
 *
 * 清单中的 code 是稳定报告标识，不参与权限、路由或数据库主键。每次实现新规则并补充公开 fixture 后，
 * 应在这里同步把状态从 `PLANNED` 或 `PARTIAL` 推进到更准确的状态。
 */
@Service
class BattleRuleCoverageService {
	/**
	 * 读取当前战斗规则实现覆盖报告。
	 */
	fun getCoverage(): BattleRuleCoverageResponse {
		val items = coverageItems()
		val implementedCount = items.count { it.status == IMPLEMENTED }
		val partialCount = items.count { it.status == PARTIAL }
		val plannedCount = items.count { it.status == PLANNED }
		val fixtureCount = items.sumOf { it.fixtureNames.size }
		val implementationPercent = if (items.isEmpty()) {
			0
		} else {
			(implementedCount * 100) / items.size
		}
		return BattleRuleCoverageResponse(
			summary = BattleRuleCoverageSummaryResponse(
				totalCount = items.size,
				implementedCount = implementedCount,
				partialCount = partialCount,
				plannedCount = plannedCount,
				fixtureCount = fixtureCount,
				implementationPercent = implementationPercent,
			),
			items = items,
		)
	}

	private fun coverageItems(): List<BattleRuleCoverageItemResponse> =
		listOf(
			item(
				code = "damage.standard-physical-special",
				name = "常规物理/特殊伤害公式",
				category = "伤害",
				status = IMPLEMENTED,
				fixtures = listOf(
					"level-50-neutral-same-element-physical-damage",
					"level-50-neutral-same-element-critical-hit-damage",
				),
				references = listOf(
					"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
					"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/util.ts",
				),
				note = "覆盖等级、威力、攻防、随机浮动、属性一致、属性克制和现代击中要害倍率。",
			),
			item(
				code = "damage.double-spread",
				name = "双打范围伤害倍率",
				category = "伤害",
				status = IMPLEMENTED,
				fixtures = listOf("double-battle-spread-damage-uses-three-quarter-target-modifier"),
				references = listOf(
					"https://bulbapedia.bulbagarden.net/wiki/Damage",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/dex-moves.ts",
				),
				note = "多个实际目标时使用 0.75 目标倍率，只剩一个可战斗目标时保持 1.0。",
			),
			item(
				code = "field.side-damage-reduction",
				name = "一侧防守屏障伤害减免",
				category = "场上效果",
				status = IMPLEMENTED,
				fixtures = listOf("double-battle-side-screen-uses-two-thirds-damage-modifier"),
				references = listOf(
					"https://bulbapedia.bulbagarden.net/wiki/Reflect_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Light_Screen_(move)",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				),
				note = "已覆盖物理/特殊/全伤害屏障、单打 0.5、双打目标侧多人 2/3、击中要害忽略屏障，以及极光类屏障的天气前置条件。",
			),
			item(
				code = "field.side-speed-modifier",
				name = "一侧速度结算修正",
				category = "场上效果",
				status = IMPLEMENTED,
				fixtures = listOf("tailwind-doubles-user-side-speed-for-later-action-order"),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Tailwind_(move)",
				),
				note = "已覆盖顺风在使用者一侧建立 2 倍速度结算修正，效果从后续行动排序开始生效，并按回合末统一递减。",
			),
			item(
				code = "field.speed-order",
				name = "全场速度顺序效果",
				category = "场上效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"trick-room-reverses-speed-order-inside-same-priority-bracket",
					"trick-room-used-while-active-ends-the-field-speed-order-effect",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Trick_Room_(move)",
				),
				note = "已覆盖戏法空间建立全场低速先动规则、同优先度内反转速度比较、再次使用解除效果，以及持续回合自然结束。",
			),
			item(
				code = "field.side-entry-hazard",
				name = "一侧入场陷阱",
				category = "场上效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"spikes-establishes-target-side-and-stacks-to-three-layers",
					"stealth-rock-damage-uses-rock-effectiveness-after-switch",
					"spikes-third-layer-damages-grounded-switch-in-only",
					"toxic-spikes-two-layers-badly-poisons-and-poison-element-absorbs",
					"sticky-web-lowers-grounded-switch-in-speed-stage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Stealth_Rock_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Spikes_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Toxic_Spikes_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Sticky_Web_(move)",
				),
				note = "已覆盖目标侧建立、撒菱三层上限、隐形岩按岩属性相性扣血、撒菱只影响接地换入成员、毒菱中毒/剧毒与毒属性吸收，以及黏黏网降低接地换入成员速度阶级。",
			),
			item(
				code = "turn.target-slot-switch",
				name = "替换后的目标槽位重定向",
				category = "回合流程",
				status = IMPLEMENTED,
				fixtures = listOf("single-target-move-follows-replacement-slot"),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-queue.ts",
				),
				note = "主动替换先于普通技能结算，单体技能会命中同一槽位的新上场成员。",
			),
			item(
				code = "format.max-turn-limit",
				name = "格式回合上限裁定",
				category = "格式裁定",
				status = IMPLEMENTED,
				fixtures = listOf("max-turn-limit-ends-battle-as-draw-after-end-turn-effects"),
				references = listOf("https://bulbapedia.bulbagarden.net/wiki/Battle#Turn"),
				note = "格式声明最大回合数时，引擎在完整回合末检查上限；没有其它胜负结果时以无胜方平局结束。",
			),
			item(
				code = "turn.protection",
				name = "保护和连续保护成功率",
				category = "回合流程",
				status = IMPLEMENTED,
				fixtures = listOf(
					"protect-move-blocks-ordinary-target-move",
					"consecutive-protection-second-use-one-third-success",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				),
				note = "保护类技能建立本回合屏障，连续成功按 1/3、1/9 等概率递减。",
			),
			item(
				code = "status.residual-major",
				name = "灼伤、中毒和剧毒回合末伤害",
				category = "主要状态",
				status = IMPLEMENTED,
				fixtures = listOf("bad-poison-residual-damage-increases-each-active-turn"),
				references = listOf("https://bulbapedia.bulbagarden.net/wiki/Poison_(status_condition)"),
				note = "灼伤和普通中毒按固定比例扣血，剧毒递增计数并在离场时重置为 1。",
			),
			item(
				code = "status.sleep-electric-terrain",
				name = "睡眠持续和电气场地防睡眠",
				category = "主要状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"sleep-prevents-two-actions-from-scripted-duration",
					"electric-terrain-blocks-new-sleep",
				),
				references = listOf(
					"https://bulbapedia.bulbagarden.net/wiki/Sleep_(status_condition)",
					"https://bulbapedia.bulbagarden.net/wiki/Electric_Terrain_(move)",
				),
				note = "睡眠按 1..3 次阻止行动建模；电气场地只阻止当前上场且接地的成员新获得睡眠。",
			),
			item(
				code = "status.paralysis-speed-action",
				name = "麻痹速度和行动阻止",
				category = "主要状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"paralysis-prevents-action-without-pp-loss",
					"paralysis-allows-action-after-failed-block-roll",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Paralysis_(status_condition)",
				),
				note = "麻痹按有效速度减半参与行动排序，并在每次行动前以 25% 概率阻止技能且不消耗 PP。",
			),
			item(
				code = "status.volatile-flinch-confusion",
				name = "畏缩和混乱临时状态",
				category = "临时状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"flinch-prevents-slower-target-before-move",
					"late-flinch-does-not-carry-to-next-turn",
					"confusion-self-damage-and-later-clear",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Confusion_(status_condition)",
				),
				note = "畏缩只阻止本回合未行动成员；混乱使用 2..5 内部计数、33% 自伤和 40 威力物理自伤公式。",
			),
			item(
				code = "terrain.grassy-heal",
				name = "青草场地核心效果",
				category = "场地",
				status = IMPLEMENTED,
				fixtures = listOf(
					"grassy-terrain-heals-active-participants-at-end-turn",
					"grassy-terrain-heals-only-grounded-active-participants",
					"grassy-terrain-boosts-grounded-grass-damage",
					"grassy-terrain-weakens-tagged-ground-shaking-moves-against-grounded-targets",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Grassy_Terrain_(move)",
				),
				note = "已覆盖接地成员回合末回复、接地使用者草属性伤害增强，以及地面震动类技能命中接地目标减半。",
			),
			item(
				code = "terrain.psychic-priority-block",
				name = "精神场地先制阻挡",
				category = "场地",
				status = IMPLEMENTED,
				fixtures = listOf(
					"psychic-terrain-blocks-priority-move-against-grounded-opponent",
					"psychic-terrain-does-not-block-priority-move-against-ungrounded-opponent",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Psychic_Terrain_(move)",
				),
				note = "精神场地阻止对手针对接地成员的先制技能，非接地目标不受影响。",
			),
			item(
				code = "field.environment-duration",
				name = "天气和场地持续回合",
				category = "天气/场地",
				status = IMPLEMENTED,
				fixtures = listOf(
					"weather-duration-decrements-and-ends",
					"terrain-duration-ends-at-turn-end",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Weather",
					"https://bulbapedia.bulbagarden.net/wiki/Terrain",
				),
				note = "非永久天气/场地在回合末递减，耗尽时恢复为无并产生结束事件。",
			),
			item(
				code = "weather.sun-rain-damage",
				name = "天气核心效果",
				category = "天气",
				status = IMPLEMENTED,
				fixtures = listOf(
					"sun-boosts-fire-and-weakens-water-damage",
					"rain-boosts-water-and-weakens-fire-damage",
					"weather-power-multiplier-modifies-base-power",
					"sandstorm-boosts-rock-special-defense",
					"snow-boosts-ice-physical-defense",
					"sandstorm-damages-only-non-immune-active-participants",
					"weather-speed-ability-changes-skill-action-order",
					"ability-and-item-immunities-block-sandstorm-damage",
					"weather-accuracy-overrides-support-sure-hit-and-lowered-accuracy",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Weather",
				),
				note = "已覆盖晴雨火水伤害倍率、天气威力和命中例外、沙暴岩属性特防、雪景冰属性物防、沙暴固定伤害、天气速度特性和天气伤害免疫；天气速度特性的规则资料种子已接入运行时快照。",
			),
			item(
				code = "status.freeze",
				name = "冰冻和解冻流程",
				category = "主要状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"freeze-prevents-action-after-failed-thaw-roll",
					"freeze-thaws-before-action-and-continues",
					"fire-damage-thaws-frozen-target-that-survives",
					"self-thawing-skill-can-be-used-while-frozen",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Freeze_(status_condition)",
				),
				note = "已覆盖行动前自然解冻、未解冻阻止行动、被火属性伤害命中后解冻，以及带标签技能自解冻。",
			),
			item(
				code = "status.immunity-and-grounding",
				name = "状态免疫和是否接地",
				category = "免疫",
				status = IMPLEMENTED,
				fixtures = listOf(
					"element-immunities-block-major-statuses",
					"electric-terrain-does-not-block-sleep-for-ungrounded-target",
					"misty-terrain-blocks-major-status-for-grounded-target",
					"grass-target-blocks-powder-based-status-skill",
					"grassy-terrain-heals-only-grounded-active-participants",
					"ability-and-item-immunities-block-matching-major-statuses",
					"terrain-ability-and-item-immunities-block-confusion-before-duration-random",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				),
				note = "已覆盖主要状态属性免疫、粉末类技能草属性免疫、接地场地免疫、青草场地接地回复，以及特性/道具提供的主要状态和混乱免疫。",
			),
			item(
				code = "ability.switch-in-stat-stage",
				name = "出场特性能力阶级变化",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"switch-in-attack-drop-triggers-at-battle-start",
					"switch-in-attack-drop-targets-both-double-battle-opponents",
					"switch-in-attack-drop-triggers-after-voluntary-switch",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Intimidate_(Ability)",
				),
				note = "已覆盖战斗开始和主动换入时触发出场降攻特性，单打影响当前对手，双打影响对方两个当前上场成员。",
			),
			item(
				code = "ability.switch-in-weather",
				name = "出场特性设置天气",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"switch-in-weather-starts-rain-at-battle-start",
					"slower-switch-in-weather-overwrites-faster-weather-at-battle-start",
					"switch-in-weather-triggers-after-voluntary-switch",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Weather#Abilities_that_create_weather",
				),
				note = "已覆盖出场设置普通天气、战斗开始多天气按有效速度顺序覆盖，以及主动换入后的天气触发。",
			),
			item(
				code = "ability.switch-in-terrain",
				name = "出场特性设置场地",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"switch-in-terrain-starts-electric-terrain-at-battle-start",
					"slower-switch-in-terrain-overwrites-faster-terrain-at-battle-start",
					"switch-in-terrain-triggers-after-voluntary-switch",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Terrain#Abilities_that_create_terrain",
				),
				note = "已覆盖出场设置普通场地、战斗开始多场地按有效速度顺序覆盖，以及主动换入后的场地触发。",
			),
			item(
				code = "turn.multi-hit-and-locked-move",
				name = "多段技能和锁招混乱",
				category = "技能流程",
				status = IMPLEMENTED,
				fixtures = listOf(
					"multi-hit-skill-consumes-pp-once-and-applies-scripted-hit-count",
					"multi-hit-skill-stops-after-target-faints",
					"locked-move-overrides-submitted-skill-follows-target-slot-and-confuses",
					"locked-move-prevents-voluntary-switch-and-executes-continuation",
					"locked-move-disruption-before-final-turn-clears-lock-without-fatigue-confusion",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Outrage_(move)",
					"https://pokemondb.net/move/outrage",
				),
				note = "已覆盖 2..5 段命中分布、单次 PP 消耗、倒下中断、锁招续回合、目标槽位重定向、主动替换阻止和疲劳混乱。",
			),
		)

	private fun item(
		code: String,
		name: String,
		category: String,
		status: String,
		fixtures: List<String>,
		references: List<String>,
		note: String,
	): BattleRuleCoverageItemResponse =
		BattleRuleCoverageItemResponse(
			code = code,
			name = name,
			category = category,
			status = status,
			fixtureNames = fixtures,
			referenceUrls = references,
			note = note,
		)

	private companion object {
		private const val IMPLEMENTED = "IMPLEMENTED"
		private const val PARTIAL = "PARTIAL"
		private const val PLANNED = "PLANNED"
	}
}
