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
				name = "青草场地回合末回复",
				category = "场地",
				status = PARTIAL,
				fixtures = listOf("grassy-terrain-heals-active-participants-at-end-turn"),
				references = listOf("https://bulbapedia.bulbagarden.net/wiki/Grassy_Terrain_(move)"),
				note = "已覆盖接地成员固定比例回复；其它场地效果仍需补齐。",
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
				name = "晴天/下雨伤害倍率",
				category = "天气",
				status = PARTIAL,
				fixtures = listOf(
					"sun-boosts-fire-and-weakens-water-damage",
					"rain-boosts-water-and-weakens-fire-damage",
				),
				references = listOf("https://bulbapedia.bulbagarden.net/wiki/Weather"),
				note = "已用公开 fixture 覆盖晴天/下雨对火/水伤害的倍率；天气回合末伤害和免疫等副作用仍需补齐。",
			),
			item(
				code = "status.freeze",
				name = "冰冻和解冻流程",
				category = "主要状态",
				status = PARTIAL,
				fixtures = listOf(
					"freeze-prevents-action-after-failed-thaw-roll",
					"freeze-thaws-before-action-and-continues",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Freeze_(status_condition)",
				),
				note = "已覆盖行动前自然解冻和未解冻阻止行动；火属性技能、特定技能自解冻和状态免疫仍需补齐。",
			),
			item(
				code = "status.immunity-and-grounding",
				name = "状态免疫和是否接地",
				category = "免疫",
				status = PARTIAL,
				fixtures = listOf(
					"element-immunities-block-major-statuses",
					"electric-terrain-does-not-block-sleep-for-ungrounded-target",
					"misty-terrain-blocks-major-status-for-grounded-target",
					"grassy-terrain-heals-only-grounded-active-participants",
				),
				references = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts"),
				note = "已覆盖主要状态的基础属性免疫、接地场地状态免疫和青草场地接地回复；特性、道具、临时状态免疫和粉末类免疫仍需补齐。",
			),
			item(
				code = "turn.multi-hit-and-locked-move",
				name = "多段技能和锁招混乱",
				category = "技能流程",
				status = PLANNED,
				fixtures = emptyList(),
				references = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts"),
				note = "需要技能执行上下文支持多段命中、锁定技能、疲劳混乱和中断规则。",
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
