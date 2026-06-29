package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleCoverageItemResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleRuleCoverageTargetSummaryResponse
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
			targetSummary = BattleRuleCoverageTargetSummaryResponse(
				targetRuleCount = FINAL_TARGET_RULE_COUNT,
				coveredRuleCount = FINAL_COVERED_RULE_COUNT,
				remainingRuleCount = FINAL_TARGET_RULE_COUNT - FINAL_COVERED_RULE_COUNT,
				implementationPercent = (FINAL_COVERED_RULE_COUNT * 100) / FINAL_TARGET_RULE_COUNT,
				coverageItemCount = items.size,
				basis = FINAL_TARGET_BASIS,
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
				code = "damage.fixed-damage",
				name = "固定伤害技能",
				category = "伤害",
				status = IMPLEMENTED,
				fixtures = listOf(
					"fixed-damage-skills-use-fixed-amount-or-user-level",
					"fixed-damage-skill-respects-element-immunity",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖固定数值伤害和按使用者等级造成固定伤害；这类技能不进入普通伤害公式、不消费要害或伤害浮动随机数，但仍受保护、命中、属性免疫、替身和伤害后流程影响。",
			),
			item(
				code = "damage.proportional-damage",
				name = "比例伤害技能",
				category = "伤害",
				status = IMPLEMENTED,
				fixtures = listOf(
					"proportional-damage-skill-halves-target-current-hp",
					"proportional-damage-skill-respects-element-immunity",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖按目标当前 HP 的 1/2 向下取整且至少 1 点的直接伤害；该规则不进入普通伤害公式，但仍受保护、命中、属性免疫、替身和伤害后流程影响。",
			),
			item(
				code = "damage.hp-derived-damage",
				name = "HP 派生直接伤害技能",
				category = "伤害",
				status = IMPLEMENTED,
				fixtures = listOf(
					"hp-difference-damage-skill-reduces-target-to-user-current-hp",
					"hp-difference-damage-skill-fails-when-target-not-above-user",
					"user-current-hp-sacrifice-damage-skill-faints-user",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖按目标与使用者当前 HP 差值造成直接伤害、差值不为正时失败，以及按使用者当前 HP 造成伤害并让使用者倒下；这些规则不进入普通伤害公式。",
			),
			item(
				code = "field.side-damage-reduction",
				name = "一侧防守屏障伤害减免",
				category = "场上效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"double-battle-side-screen-uses-two-thirds-damage-modifier",
					"screen-extending-item-makes-side-damage-reduction-last-eight-turns",
				),
				references = listOf(
					"https://bulbapedia.bulbagarden.net/wiki/Reflect_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Light_Screen_(move)",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Light_Clay",
				),
				note = "已覆盖物理/特殊/全伤害屏障、单打 0.5、双打目标侧多人 2/3、击中要害忽略屏障、极光类屏障的天气前置条件，以及屏障延长道具把普通持续回合改为 8。",
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
					"consecutive-protection-second-use-can-fail-and-leave-user-unprotected",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				),
				note = "保护类技能建立本回合屏障，连续保护按 1/3、1/9 等概率递减；失败时仍消耗 PP，但不会阻挡同回合攻击。",
			),
			item(
				code = "turn.accuracy-evasion-stage",
				name = "命中和闪避阶级",
				category = "命中",
				status = IMPLEMENTED,
				fixtures = listOf(
					"target-evasion-stage-lowers-effective-accuracy",
					"user-accuracy-stage-raises-effective-accuracy",
					"weather-accuracy-overrides-support-sure-hit-and-lowered-accuracy",
				),
				references = listOf(
					"https://bulbapedia.bulbagarden.net/wiki/Stat_modifier",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖使用者命中阶级和目标闪避阶级参与有效命中计算，以及天气命中覆盖对必中和降命中的影响。",
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
				code = "status.burn-physical-damage",
				name = "灼伤物理伤害减半",
				category = "主要状态",
				status = IMPLEMENTED,
				fixtures = listOf("burn-halves-physical-attacking-stat-before-damage"),
				references = listOf(
					"https://bulbapedia.bulbagarden.net/wiki/Burn_(status_condition)",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				),
				note = "已覆盖灼伤成员使用物理技能时，攻击侧数值先减半再进入普通伤害公式；击中要害仍不会绕过灼伤减半。",
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
					"existing-confusion-blocks-new-confusion-without-refresh",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Confusion_(status_condition)",
				),
				note = "畏缩只阻止本回合未行动成员；混乱使用 2..5 内部计数、33% 自伤和 40 威力物理自伤公式，已有混乱不会被再次附加刷新持续时间。",
			),
			item(
				code = "status.heal-block",
				name = "回复封锁临时状态",
				category = "临时状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-skill-applies-heal-block-to-target",
					"heal-blocked-participant-cannot-use-self-healing-status-skill",
					"heal-blocked-participant-cannot-use-draining-damage-skill",
					"heal-block-suppresses-end-turn-held-item-healing",
					"heal-block-clears-when-end-turn-duration-reaches-zero",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://wiki.52poke.com/wiki/回复封锁（招式）",
					"https://wiki.52poke.com/wiki/回复封锁（状态变化）",
				),
				note = "已覆盖回复封锁附加、主动回复技能失败、吸取回复技能失败、回合末道具回复被抑制，以及持续回合在回合末递减并自然解除。",
			),
			item(
				code = "status.taunt",
				name = "挑衅临时状态",
				category = "临时状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-skill-applies-taunt-to-target",
					"taunted-participant-cannot-use-status-skill",
					"taunted-participant-can-use-damaging-skill",
					"taunt-clears-when-end-turn-duration-reaches-zero",
					"existing-taunt-blocks-new-taunt-without-refreshing-duration",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://wiki.52poke.com/wiki/挑衅（招式）",
					"https://wiki.52poke.com/wiki/挑衅（状态变化）",
				),
				note = "已覆盖挑衅附加、变化技能在 PP 消耗前失败、攻击分类技能继续结算、回合末递减解除，以及已有挑衅不会刷新持续时间。",
			),
			item(
				code = "status.disable",
				name = "定身法临时状态",
				category = "临时状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-skill-disables-target-last-used-skill",
					"disabled-skill-cannot-be-used",
					"disabled-participant-can-use-other-skill",
					"disable-clears-when-end-turn-duration-reaches-zero",
					"disable-fails-when-target-has-no-last-used-skill",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://wiki.52poke.com/wiki/定身法（招式）",
				),
				note = "已覆盖定身法读取目标上一次使用技能、被禁用技能在 PP 消耗前失败、其它技能继续可用、回合末递减解除，以及没有可禁用技能时失败。",
			),
			item(
				code = "status.torment",
				name = "无理取闹临时状态",
				category = "临时状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-skill-applies-torment-to-target",
					"tormented-participant-cannot-use-same-skill-twice",
					"tormented-participant-can-use-different-skill",
					"torment-clears-when-participant-switches-out",
					"existing-torment-blocks-new-torment-without-refreshing-state",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://wiki.52poke.com/wiki/无理取闹（招式）",
					"https://wiki.52poke.com/wiki/无理取闹（状态变化）",
				),
				note = "已覆盖无理取闹附加、连续使用同一技能在 PP 消耗前失败、改用不同技能后更新最近技能、离场清除，以及已有无理取闹不会重复附加。",
			),
			item(
				code = "status.binding",
				name = "束缚临时状态",
				category = "临时状态",
				status = IMPLEMENTED,
				fixtures = listOf(
					"binding-skill-traps-target-and-deals-end-turn-damage",
					"bound-participant-cannot-switch-voluntarily",
					"bound-participant-takes-end-turn-binding-damage",
					"binding-clears-when-end-turn-duration-reaches-zero",
					"binding-clears-when-source-switches-out",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://wiki.52poke.com/wiki/绑紧（招式）",
					"https://wiki.52poke.com/wiki/束缚（状态变化）",
				),
				note = "已覆盖束缚类技能命中后附加目标临时状态、阻止目标主动替换、回合末最大 HP 1/8 间接伤害、持续回合归零解除，以及来源离场解除。",
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
					"psychic-terrain-does-not-block-priority-move-against-ally",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Psychic_Terrain_(move)",
				),
				note = "精神场地阻止对手针对接地成员的先制技能，非接地目标和同侧目标不受影响。",
			),
			item(
				code = "ability.status-priority-boost",
				name = "特性提升变化技能先制度",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-priority-ability-moves-status-skill-before-faster-opponent",
					"dark-target-blocks-opponent-status-skill-boosted-by-status-priority-ability",
					"status-priority-ability-does-not-make-ally-dark-target-immune",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Prankster_(Ability)",
				),
				note = "已覆盖变化技能获得额外优先度、对手恶属性目标免疫，以及同侧恶属性目标不触发免疫。",
			),
			item(
				code = "ability.priority-move-block",
				name = "特性阻止对手先制技能",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"priority-blocking-ability-blocks-opponent-priority-move-against-holder",
					"priority-blocking-ability-protects-active-ally-from-opponent-priority-move",
					"priority-blocking-ability-does-not-block-ally-priority-move",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Queenly_Majesty_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Dazzling_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Armor_Tail_(Ability)",
				),
				note = "已覆盖目标自身特性和同侧伙伴特性阻止对手先制技能；同侧成员主动使用先制技能不受阻挡。",
			),
			item(
				code = "ability.element-absorb-heal",
				name = "特性吸收指定属性技能并回复",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"element-absorb-ability-heals-and-blocks-matching-damage",
					"element-absorb-ability-at-full-hp-blocks-without-overhealing",
					"element-absorb-ability-blocks-matching-status-skill",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Volt_Absorb_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Water_Absorb_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Earth_Eater_(Ability)",
				),
				note = "已覆盖匹配属性技能命中目标后被特性吸收、按最大 HP 1/4 回复、满 HP 不溢出回复，以及变化技能附加效果被阻止。",
			),
			item(
				code = "ability.element-absorb-stat",
				name = "特性吸收指定属性技能并提升能力",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"element-absorb-stat-ability-blocks-electric-damage-and-raises-speed",
					"element-absorb-stat-ability-blocks-grass-damage-and-raises-attack",
					"element-absorb-stat-ability-blocks-fire-damage-and-raises-defense-by-two",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Motor_Drive_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Sap_Sipper_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Well-Baked_Body_(Ability)",
				),
				note = "已覆盖匹配属性技能命中目标后被特性吸收并阻止伤害，同时提升速度、攻击或防御能力阶级。",
			),
			item(
				code = "ability.element-damage-boost",
				name = "指定属性技能增伤特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"element-ability-boosts-matching-skill-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖固定属性技能按配置倍率提升伤害、非匹配属性不触发，以及天气改属性后按本次有效属性判断；龙、岩石、钢的 1.5 倍和电的约 1.3 倍规则资料已接入运行时快照。",
			),
			item(
				code = "ability.low-hp-element-boost",
				name = "低体力指定属性增伤特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"low-hp-element-ability-boosts-matching-damage-at-threshold",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Swarm_(Ability)",
				),
				note = "已覆盖 HP 小于等于最大 HP 1/3 时指定属性技能获得 1.5 倍伤害倍率，并验证高于阈值或技能属性不匹配时不触发；草、火、水、虫四类低体力增伤规则资料已接入运行时快照。",
			),
			item(
				code = "ability.weather-element-damage-boost",
				name = "指定天气下属性增伤特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"weather-element-ability-boosts-matching-element-in-sandstorm",
					"ability-and-item-immunities-block-sandstorm-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖沙暴下岩石、地面和钢属性技能获得 1.3 倍特性伤害倍率，并验证天气不匹配、有效属性不匹配和天气改属性技能的判断口径；同一特性的沙暴伤害免疫通过天气伤害免疫结构化效果接入运行时快照。",
			),
			item(
				code = "ability.super-effective-damage-reduction",
				name = "效果绝佳伤害减免特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"super-effective-defensive-ability-reduces-super-effective-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/dex-types.ts",
				),
				note = "已覆盖防守方受到效果绝佳直接技能伤害时按 0.75 倍降低最终伤害，并验证非效果绝佳和无视目标特性时不触发；同类规则资料已接入运行时快照。",
			),
			item(
				code = "ability.full-hp-damage-reduction",
				name = "满 HP 伤害减免特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"full-hp-defensive-ability-reduces-direct-skill-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				),
				note = "已覆盖防守方当前 HP 等于最大 HP 时直接技能伤害按 0.5 倍降低，并验证不满 HP 和无视目标特性时不触发；同类规则资料已接入运行时快照。",
			),
			item(
				code = "ability.damage-class-damage-reduction",
				name = "按伤害分类减免特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"special-damage-class-ability-reduces-special-skill-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				),
				note = "已覆盖防守方受到特殊分类直接技能伤害时按 0.5 倍降低最终伤害，并验证物理分类和无视目标特性时不触发；运行时快照使用通用伤害分类集合承载该类规则。",
			),
			item(
				code = "ability.defending-stat-multiplier",
				name = "防守能力值修正特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"defense-stat-ability-doubles-physical-defense-before-damage",
					"terrain-defense-stat-ability-boosts-defense-in-grassy-terrain",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				),
				note = "已覆盖物防翻倍和青草场地下物防 1.5 倍两类防御侧能力值修正；这些规则改变基础伤害公式中的防御值，而不是最终伤害倍率。",
			),
			item(
				code = "ability.attacking-stat-multiplier",
				name = "攻击能力值修正特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"attack-stat-ability-doubles-physical-attack-before-damage",
					"major-status-attack-ability-boosts-attack-and-skips-burn-drop",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				),
				note = "已覆盖物理攻击翻倍类特性，以及主要异常状态下物攻 1.5 倍并跳过灼伤物理减半；这些规则改变基础伤害，不进入最终伤害倍率。",
			),
			item(
				code = "ability.same-element-bonus-override",
				name = "属性一致加成覆盖特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"same-element-bonus-ability-uses-double-stab-multiplier",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				),
				note = "已覆盖攻击方拥有属性一致加成覆盖特性时，技能当前有效属性与自身属性一致会把 STAB 倍率从默认 1.5 改为 2.0；该规则不进入泛用最终伤害倍率。",
			),
			item(
				code = "ability.tagged-skill-damage-boost",
				name = "特性强化指定标签技能伤害",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"punch-based-ability-boosts-punch-tagged-skill-damage",
					"slicing-based-ability-boosts-slicing-tagged-skill-damage",
					"contact-based-ability-boosts-contact-skill-damage",
					"sound-based-ability-boosts-sound-skill-damage",
					"sound-based-defensive-ability-reduces-sound-skill-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				),
				note = "已覆盖拳类技能按 1.2 倍、切割类技能按 1.5 倍、接触类和声音类技能按 1.3 倍获得攻击方特性伤害倍率，并覆盖声音类技能命中防守方时 0.5 倍减伤；公式只读取结构化技能标签，不通过技能名称或文本猜测。",
			),
			item(
				code = "ability.indirect-damage-immunity",
				name = "特性免疫间接伤害",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"indirect-damage-immunity-blocks-residual-status-and-weather-damage",
					"indirect-damage-immunity-keeps-direct-damage-boost-but-blocks-item-recoil",
					"indirect-damage-immunity-blocks-move-recoil-and-entry-hazard-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Magic_Guard_(Ability)",
				),
				note = "已覆盖特性阻止异常状态回合末伤害、沙暴伤害、入场陷阱伤害、技能反作用伤害和携带道具反伤；直接技能伤害和伤害增幅道具倍率仍正常生效。",
			),
			item(
				code = "ability.skill-recoil-damage-immunity",
				name = "特性免疫技能反作用伤害",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"skill-recoil-immunity-blocks-move-recoil-damage",
					"skill-recoil-immunity-does-not-block-held-item-recoil",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Rock_Head_(Ability)",
				),
				note = "已覆盖特性阻止技能自身反作用伤害，但不会阻止携带道具在造成伤害后产生的固定反伤。",
			),
			item(
				code = "ability.critical-hit-immunity",
				name = "特性免疫击中要害",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"critical-hit-immunity-blocks-guaranteed-critical-hit-damage",
					"critical-hit-immunity-blocks-successful-random-critical-hit",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				),
				note = "已覆盖目标特性阻止必定要害和普通随机要害；随机要害仍消费随机数，但最终伤害事件按非要害记录。",
			),
			item(
				code = "ability.ignore-opponent-damage-stat-stages",
				name = "特性无视对手伤害阶级变化",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"defender-stat-stage-ignore-ability-ignores-attacker-attack-boost",
					"attacker-stat-stage-ignore-ability-ignores-defender-defense-boost",
					"stat-stage-ignore-ability-applies-to-special-damage-stages",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/damage-calc/blob/master/calc/src/mechanics/gen789.ts",
				),
				note = "已覆盖持有者作为防守方时忽略攻击方攻击/特攻阶级、作为攻击方时忽略防守方防御/特防阶级；命中/闪避阶级由命中流程覆盖项单独验证。",
			),
			item(
				code = "ability.ignore-opponent-accuracy-stat-stages",
				name = "特性无视对手命中阶级变化",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"defender-accuracy-stage-ignore-ability-ignores-attacker-accuracy-drop",
					"attacker-accuracy-stage-ignore-ability-ignores-target-evasion-boost",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				),
				note = "已覆盖持有者作为防守方时忽略攻击方命中阶级、作为攻击方时忽略目标闪避阶级；必中和天气命中覆盖仍按原流程处理。",
			),
			item(
				code = "ability.ignore-target-ability-effects",
				name = "攻击时无视目标防守特性",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"target-ability-ignore-bypasses-element-absorb",
					"target-ability-ignore-bypasses-full-hp-survival",
					"target-ability-ignore-bypasses-defender-stat-stage-ignore",
					"target-ability-ignore-bypasses-target-status-immunity",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/sim/battle-actions.ts",
				),
				note = "已覆盖攻击方使用技能时跳过目标侧属性吸收、满 HP 保命、伤害阶级无视和主要异常状态免疫等防守特性；目标道具、属性天然免疫、场地免疫和非技能来源不受影响。",
			),
			item(
				code = "ability.sound-based-skill-immunity",
				name = "特性免疫声音类技能",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"sound-immunity-ability-blocks-sound-damaging-skill",
					"sound-immunity-ability-blocks-sound-status-skill",
					"target-ability-ignore-bypasses-sound-immunity",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Soundproof_(Ability)",
				),
				note = "已覆盖目标特性在命中前阻止声音类伤害技能和声音类变化技能；攻击方无视目标特性时可以绕过该免疫。",
			),
			item(
				code = "terrain.setting-skill",
				name = "场地设置技能",
				category = "场地",
				status = IMPLEMENTED,
				fixtures = listOf("status-skill-starts-terrain-for-five-turns"),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Electric_Terrain_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Grassy_Terrain_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Misty_Terrain_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Psychic_Terrain_(move)",
				),
				note = "已覆盖变化技能成功后设置电气、青草、薄雾和精神场地，普通持续 5 回合；规则资料种子已接入运行时快照。",
			),
			item(
				code = "terrain.speed-ability",
				name = "场地速度特性",
				category = "场地",
				status = IMPLEMENTED,
				fixtures = listOf(
					"terrain-speed-ability-changes-skill-action-order",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Electric_Terrain_(move)",
				),
				note = "已覆盖电气场地下速度翻倍特性改变行动顺序，并已接入运行时规则资料。",
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
					"weather-element-override-participates-in-stab-weather-and-effectiveness",
					"weather-element-override-can-be-absorbed-by-matching-element-ability",
					"weather-element-override-to-fire-thaws-frozen-target",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Weather",
				),
				note = "已覆盖晴雨火水伤害倍率、天气威力/属性/命中例外、沙暴岩属性特防、雪景冰属性物防、沙暴固定伤害、天气速度特性和天气伤害免疫；天气速度特性和天气属性覆盖的规则资料种子已接入运行时快照。",
			),
			item(
				code = "weather.healing-ability",
				name = "天气回复特性",
				category = "天气",
				status = IMPLEMENTED,
				fixtures = listOf("weather-healing-ability-heals-active-participant-at-end-turn"),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Weather",
				),
				note = "已覆盖指定天气存在时，当前上场未满 HP 成员按最大 HP 1/16 回复；规则资料种子已接入运行时快照。",
			),
			item(
				code = "weather.setting-skill",
				name = "天气设置技能",
				category = "天气",
				status = IMPLEMENTED,
				fixtures = listOf("status-skill-starts-weather-for-five-turns"),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Rain_Dance_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Sunny_Day_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Sandstorm_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Snowscape_(move)",
				),
				note = "已覆盖变化技能成功后设置雨天、晴天、沙暴和雪天天气，普通持续 5 回合；规则资料种子已接入运行时快照。",
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
					"existing-major-status-blocks-new-major-status",
					"ability-and-item-immunities-block-matching-major-statuses",
					"terrain-ability-and-item-immunities-block-confusion-before-duration-random",
					"ability-and-item-immunities-block-flinch-before-action",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				),
				note = "已覆盖已有主要状态阻止覆盖、主要状态属性免疫、粉末类技能草属性免疫、接地场地免疫、青草场地接地回复，以及特性/道具提供的主要状态、混乱和畏缩免疫。",
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
				code = "ability.contact-status",
				name = "接触类特性返还异常状态",
				category = "特性",
				status = IMPLEMENTED,
				fixtures = listOf(
					"contact-status-ability-applies-paralysis-after-contact",
					"contact-status-ability-misses-when-chance-roll-fails",
					"contact-status-ability-respects-attacker-immunity",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Static_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Flame_Body_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Limber_(Ability)",
				),
				note = "已覆盖接触类技能成功造成伤害后触发目标特性，100% 概率不额外消费随机数，30% 概率失败时不附加状态，并复用攻击方主要异常免疫流程。",
			),
			item(
				code = "damage.full-hp-survival",
				name = "满 HP 致命伤害保留 1 HP",
				category = "伤害",
				status = IMPLEMENTED,
				fixtures = listOf(
					"full-hp-survival-ability-leaves-one-hp-before-faint",
					"consumable-full-hp-survival-item-leaves-one-hp-and-consumes-item",
					"full-hp-survival-does-not-trigger-after-prior-damage",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Sturdy_(Ability)",
					"https://bulbapedia.bulbagarden.net/wiki/Focus_Sash",
				),
				note = "已覆盖满 HP 被技能直接伤害一击打倒前由特性或一次性携带道具保留 1 HP；道具来源会在触发后被消费，非满 HP 不触发。",
			),
				item(
					code = "item.held-core-effects",
					name = "携带道具核心触发效果",
				category = "道具",
				status = IMPLEMENTED,
				fixtures = listOf(
					"low-hp-berry-heals-once-after-damage",
					"choice-speed-item-modifies-action-order",
					"damage-boost-item-uses-max-hp-recoil-after-damage",
					"damage-dealt-healing-item-restores-eighth-actual-damage",
					"damage-dealt-healing-item-counts-substitute-damage",
					"damage-dealt-healing-item-uses-total-multi-hit-damage",
					"end-turn-healing-item-restores-one-sixteenth-max-hp",
					"weather-extending-item-makes-weather-skill-last-eight-turns",
					"weather-extending-item-makes-weather-ability-last-eight-turns",
					"terrain-extending-item-makes-terrain-skill-last-eight-turns",
					"terrain-extending-item-makes-terrain-ability-last-eight-turns",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Oran_Berry",
					"https://bulbapedia.bulbagarden.net/wiki/Sitrus_Berry",
					"https://bulbapedia.bulbagarden.net/wiki/Life_Orb",
					"https://bulbapedia.bulbagarden.net/wiki/Shell_Bell",
					"https://bulbapedia.bulbagarden.net/wiki/Leftovers",
					"https://bulbapedia.bulbagarden.net/wiki/Damp_Rock",
					"https://bulbapedia.bulbagarden.net/wiki/Terrain_Extender",
				),
					note = "已覆盖低体力树果一次性回复并消费、讲究类速度道具改变行动顺序并锁定首次技能、生命宝珠类伤害增幅按最大 HP 反伤、贝壳之铃类按整次技能实际总伤害回复、剩饭类回合末最大 HP 1/16 回复，以及天气/场地延长道具把普通环境技能或出场环境特性的持续回合改为 8。",
				),
				item(
					code = "item.element-damage-boost",
					name = "携带道具指定属性伤害提升",
					category = "道具",
					status = IMPLEMENTED,
					fixtures = listOf(
						"element-damage-boost-item-multiplies-matching-damage",
						"element-damage-boost-item-ignores-non-matching-damage",
					),
					references = listOf(
						"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
						"https://bulbapedia.bulbagarden.net/wiki/Type-enhancing_item",
					),
					note = "已覆盖传统非消耗型属性强化道具：技能属性匹配时有效威力按 1.2 倍参与普通伤害公式，不匹配时保持中性，且不会消费道具或产生反伤。规则资料已接入现代 18 个属性的对应道具。",
				),
				item(
					code = "item.element-damage-reduction",
					name = "携带道具指定属性伤害减免",
					category = "道具",
					status = IMPLEMENTED,
					fixtures = listOf(
						"element-damage-reduction-item-halves-super-effective-damage",
						"element-damage-reduction-item-requires-super-effective-damage",
						"element-damage-reduction-item-does-not-activate-through-substitute",
						"normal-damage-reduction-item-halves-normal-damage",
					),
					references = listOf(
						"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
						"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
						"https://bulbapedia.bulbagarden.net/wiki/Damage-reducing_Berry",
						"https://bulbapedia.bulbagarden.net/wiki/Chilan_Berry",
					),
					note = "已覆盖抗性树果在本体受到对应属性且效果绝佳伤害时按 0.5 倍减伤并消费、命中替身时不触发，以及一般属性灯浆果不要求效果绝佳的例外。规则资料已接入现代 18 个属性的对应抗性树果。",
				),
				item(
					code = "item.conditional-damage-boost",
					name = "携带道具条件伤害提升",
					category = "道具",
					status = IMPLEMENTED,
					fixtures = listOf(
						"physical-power-boost-item-raises-matching-damage-class",
						"physical-power-boost-item-ignores-special-damage-class",
						"super-effective-damage-boost-item-raises-final-damage",
						"super-effective-damage-boost-item-ignores-neutral-damage",
					),
					references = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts"),
					note = "已覆盖物理/特殊分类威力提升道具在威力阶段生效，以及效果绝佳伤害提升道具在最终伤害阶段生效；三者均不消费道具、不反伤、不锁招。",
				),
				item(
					code = "item.major-status-cure",
					name = "携带道具解除主要异常状态",
					category = "道具",
					status = IMPLEMENTED,
					fixtures = listOf(
						"major-status-cure-item-clears-status-after-application",
						"major-status-cure-item-clears-contact-ability-status",
						"specific-status-cure-item-keeps-unmatched-status",
					),
					references = listOf(
						"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
						"https://bulbapedia.bulbagarden.net/wiki/Lum_Berry",
						"https://bulbapedia.bulbagarden.net/wiki/Chesto_Berry",
						"https://bulbapedia.bulbagarden.net/wiki/Rawst_Berry",
					),
					note = "已覆盖主要异常状态成功写入后由携带道具立即解除并消费；技能附加状态和接触特性返还状态共用同一 after-status 钩子，非匹配状态不会消费道具。规则资料已接入全状态解除，以及麻痹、睡眠、中毒、灼伤、冰冻的指定状态解除道具。",
				),
				item(
					code = "item.volatile-status-cure",
					name = "携带道具解除临时状态",
					category = "道具",
					status = IMPLEMENTED,
					fixtures = listOf(
						"volatile-status-cure-item-clears-confusion-after-application",
					),
					references = listOf(
						"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
						"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
						"https://bulbapedia.bulbagarden.net/wiki/Persim_Berry",
						"https://bulbapedia.bulbagarden.net/wiki/Confusion_(status_condition)",
					),
					note = "已覆盖混乱成功写入并消费持续时间随机数后，由携带道具立即解除混乱并消费自身。该规则使用独立临时状态治愈模型，不与主要异常状态混用。",
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
			item(
				code = "turn.recharge-after-use",
				name = "成功后休整技能",
				category = "技能流程",
				status = IMPLEMENTED,
				fixtures = listOf(
					"recharge-skill-prevents-next-turn-action-without-pp-loss",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Hyper_Beam_(move)",
				),
				note = "已覆盖成功造成实际伤害后写入一次休整、下一次技能行动前阻止执行且不扣 PP，以及主动替换校验阻止；未造成实际伤害时不会进入休整。",
			),
			item(
				code = "turn.charge-before-use",
				name = "蓄力后发动技能",
				category = "技能流程",
				status = IMPLEMENTED,
				fixtures = listOf(
					"charge-skill-releases-next-turn-without-extra-pp",
					"charge-skill-skips-charge-in-sun",
					"charge-skill-consumes-item-to-skip-charge",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Solar_Beam_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Power_Herb",
				),
				note = "已覆盖首次使用消耗 PP 并进入蓄力、下一次技能行动自动释放且不重复扣 PP、主动替换阻止、指定天气跳过蓄力、一次性道具消费后同回合释放，以及释放阶段复用普通命中和伤害流程。",
			),
			item(
				code = "turn.substitute",
				name = "替身建立、代伤和状态阻止",
				category = "技能流程",
				status = IMPLEMENTED,
				fixtures = listOf(
					"substitute-pays-quarter-max-hp-and-absorbs-damage-until-broken",
					"substitute-keeps-remaining-hp-after-partial-damage",
					"substitute-blocks-opponent-major-status",
					"substitute-blocks-opponent-volatile-status",
					"sound-damage-skill-bypasses-substitute",
					"sound-status-skill-bypasses-substitute",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Substitute_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Sound-based_move",
				),
				note = "已覆盖支付最大 HP 1/4 建立替身、对手普通伤害先扣替身 HP、替身破裂或保留剩余 HP、替身阻止对手主要异常状态和混乱等临时状态、普通降能力变化技被替身阻止，以及声音类伤害和声音类变化技穿过替身。",
			),
			item(
				code = "skill.major-status-effects",
				name = "技能主要异常状态附加",
				category = "技能效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-skill-applies-burn-and-end-turn-residual-damage",
					"grass-target-blocks-powder-based-status-skill",
					"sleep-prevents-two-actions-from-scripted-duration",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Will-O-Wisp_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Powder_and_spore_moves",
					"https://bulbapedia.bulbagarden.net/wiki/Sleep_(status_condition)",
				),
				note = "已覆盖变化技能附加灼伤、中毒、麻痹和睡眠，粉末类技能标签与草属性免疫，以及睡眠持续行动阻止；基础状态类技能规则资料已接入运行时快照。",
			),
			item(
				code = "skill.stat-stage-effects",
				name = "技能能力阶级变化",
				category = "技能效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"stat-stage-effect-changes-later-action-damage-in-the-same-turn",
					"status-skill-applies-multiple-user-stat-stage-changes",
					"all-opponents-status-skill-applies-stat-stage-change-to-each-active-opponent",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Stat_modifier",
					"https://bulbapedia.bulbagarden.net/wiki/Swords_Dance_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Shell_Smash_(move)",
				),
				note = "已覆盖命中后降低目标能力阶级、变化技能提升使用者单项或多项能力阶级、双打全体对手逐个降阶，并已接入基础自我强化和目标降阶技能规则资料。",
			),
			item(
				code = "skill.stat-stage-operations",
				name = "技能能力阶级特殊操作",
				category = "技能效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"damaging-skill-clears-target-stat-stages-after-hit",
					"status-skill-clears-every-active-participant-stat-stages",
					"status-skill-copies-target-stat-stages-to-user",
					"status-skill-swaps-attack-stat-stages-between-user-and-target",
					"status-skill-swaps-defense-stat-stages-between-user-and-target",
					"status-skill-swaps-all-stat-stages-between-user-and-target",
					"status-skill-inverts-target-stat-stages",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Clear_Smog_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Haze_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Psych_Up_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Power_Swap_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Guard_Swap_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Heart_Swap_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Topsy-Turvy_(move)",
				),
				note = "已覆盖命中后清除目标能力阶级、全场清除当前上场成员能力阶级、复制目标阶级给使用者、交换攻击组、防御组和全部能力阶级，以及取反目标能力阶级。",
			),
			item(
				code = "skill.hp-effects",
				name = "技能 HP 回复效果",
				category = "技能效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"draining-damage-skill-heals-user-by-half-damage-dealt",
					"draining-damage-skill-honors-configured-drain-fraction",
					"self-healing-status-skill-restores-half-max-hp",
					"weather-sensitive-self-healing-skill-uses-current-weather-fraction",
					"recoil-damage-skill-uses-target-hp-actually-lost",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Absorb_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Draining_Kiss_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Recover_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Synthesis_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Double-Edge_(move)",
				),
				note = "已覆盖造成伤害后按实际伤害 1/2 或 3/4 吸取回复、变化技能固定 1/2 自我回复、晨光/光合作用/月光类天气变量回复，以及按目标实际损失 HP 计算的技能反作用伤害。",
			),
			item(
				code = "skill.forced-switch",
				name = "技能强制替换目标",
				category = "技能效果",
				status = IMPLEMENTED,
				fixtures = listOf(
					"status-skill-forces-target-side-random-bench-switch",
					"damaging-skill-applies-damage-before-forced-target-switch",
				),
				references = listOf(
					"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
					"https://bulbapedia.bulbagarden.net/wiki/Roar_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Whirlwind_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Circle_Throw_(move)",
					"https://bulbapedia.bulbagarden.net/wiki/Dragon_Tail_(move)",
				),
				note = "已覆盖变化技能命中后随机强制目标侧后备换入，以及伤害技能先造成普通伤害再强制目标侧后备换入；换入继续触发入场陷阱和出场特性流程。",
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
		private const val FINAL_TARGET_RULE_COUNT = 312
		private const val FINAL_COVERED_RULE_COUNT = 120
		private const val FINAL_TARGET_BASIS =
			"按可复用规则行为族统计，详见 docs/superpowers/plans/2026-06-29-battle-rule-final-coverage-ledger.md。"
	}
}
