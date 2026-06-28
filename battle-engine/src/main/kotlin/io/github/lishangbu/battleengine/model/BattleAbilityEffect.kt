package io.github.lishangbu.battleengine.model

/**
 * 特性在战斗中的可执行效果。
 *
 * 该 sealed 类型是规则资料 policy code 进入纯引擎后的结构化形态。第一批实现几个高价值 hook：
 * 低体力时强化指定属性伤害、满 HP 承受致命伤害时保留 1 HP、吸收指定属性技能并回复或提阶、阻止对手先制技能影响己方、
 * 天气下速度修正、天气伤害免疫、天气下回合末回复、受到接触类技能后有概率给攻击方附加主要异常状态，稳定状态免疫、环境下速度修正，
 * 以及成员出场时的能力阶级变化、天气设置和场地设置。
 *
 * 后续每新增一种复杂特性，都应该先明确触发阶段、输入状态、不变量和对照 fixture，再扩展这里或拆分专门处理器。
 */
sealed interface BattleAbilityEffect {
	/**
	 * 免疫一组主要异常状态。
	 *
	 * 用于表达免疫中毒、免疫灼伤、免疫睡眠等稳定特性。具体特性名不进入引擎，避免把本地化文本或资料库名称
	 * 混进规则状态机；资料层负责把特性翻译成这类结构化效果。
	 */
	data class MajorStatusImmunity(
		val statuses: Set<BattleMajorStatus>,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 免疫一组临时状态。
	 *
	 * 用于表达混乱免疫、畏缩免疫等稳定特性。不同临时状态的生命周期仍由状态机决定：畏缩只持续到本回合
	 * 行动前或回合末，混乱使用独立持续计数；该效果只负责在附加前阻止状态写入。
	 */
	data class VolatileStatusImmunity(
		val statuses: Set<BattleVolatileStatus>,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 指定天气下的速度倍率。
	 *
	 * 用于表达雨天下速度提升、晴天下速度提升、沙暴/雪景下速度提升等稳定特性。具体特性名称不进入公式；
	 * 引擎只读取天气和倍率，保证同一快照可复盘。
	 */
	data class WeatherSpeedMultiplier(
		val weather: BattleWeather,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(weather != BattleWeather.NONE) { "weather speed multiplier requires an active weather" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 指定场地下的速度倍率。
	 *
	 * 用于表达电气场地下速度提升等稳定特性。该效果只参与行动排序、替换排序和其它读取有效速度的流程；
	 * 场地本身的持续时间、伤害修正或状态免疫仍由环境状态和其它规则处理。
	 */
	data class TerrainSpeedMultiplier(
		val terrain: BattleTerrain,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "terrain speed multiplier requires an active terrain" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 免疫一组天气造成的回合末伤害。
	 */
	data class WeatherDamageImmunity(
		val weathers: Set<BattleWeather>,
	) : BattleAbilityEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather damage immunity cannot target NONE" }
		}
	}

	/**
	 * 指定天气下的回合末回复。
	 *
	 * 该效果用于表达雨天、雪天等环境中按最大 HP 固定比例回复的特性。它只描述回复条件和比例，不处理天气本身
	 * 的持续回合、天气伤害或道具回复；状态机在回合末天气阶段读取它并产生专用天气回复事件。
	 */
	data class WeatherEndTurnHeal(
		val weathers: Set<BattleWeather>,
		val healDenominator: Int,
	) : BattleAbilityEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather healing cannot target NONE" }
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 满 HP 承受会导致倒下的直接伤害时保留 1 HP。
	 *
	 * 该效果只处理普通伤害结算中的“从满 HP 被一击打倒”场景，不阻止异常状态、天气、入场陷阱、混乱自伤、
	 * 反作用伤害或其它非技能直接伤害。破格、特性失效等绕过条件会在对应规则模型存在后接入，不在这里判断具体
	 * 特性名称。
	 */
	data class SurviveFatalDamageAtFullHp(
		val remainingHp: Int = 1,
	) : BattleAbilityEffect {
		init {
			require(remainingHp > 0) { "remainingHp must be positive" }
		}
	}

	/**
	 * 阻止对手先制技能影响拥有者所在一侧。
	 *
	 * 现代规则中，这类特性保护拥有者和同侧伙伴，不阻止同侧成员主动使用先制技能，也不阻止没有目标到己方成员的
	 * 场地、撒场或自我目标技能。`protectsAllies` 保留为结构化字段，便于后续接入只保护自身的变体时不改变事件
	 * 和状态机入口。
	 */
	data class PriorityMoveImmunityForSide(
		val protectsAllies: Boolean = true,
	) : BattleAbilityEffect

	/**
	 * 提升变化类技能的行动优先度。
	 *
	 * 该效果只作用于 [BattleDamageClass.STATUS] 技能，不改变物理或特殊技能的基础优先度。现代规则中，由这类
	 * 特性提升优先度的对手变化技能无法影响恶属性目标；`darkElementTargetsImmune` 明确保存这一副作用，避免
	 * 状态机靠具体特性名判断。该免疫只针对对手目标，同侧辅助技能仍正常结算。
	 */
	data class StatusSkillPriorityBoost(
		val priorityDelta: Int = 1,
		val darkElementTargetsImmune: Boolean = true,
	) : BattleAbilityEffect {
		init {
			require(priorityDelta > 0) { "priorityDelta must be positive" }
		}
	}

	/**
	 * 吸收指定属性技能并按最大 HP 回复。
	 *
	 * 该效果用于表达现代规则中“被指定属性技能命中时无效并回复 HP”的稳定特性。它在命中判定成功后、
	 * 普通伤害或附加效果写入前触发；即使目标已经满 HP，也仍会阻止该技能继续结算，只是回复量会被夹取为 0。
	 * `healDenominator` 表示回复最大 HP 的分母，例如 4 表示最多回复最大 HP 的 1/4。
	 */
	data class ElementSkillAbsorbHeal(
		val elementId: Long,
		val healDenominator: Int = 4,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 吸收指定属性技能并提升自身能力阶级。
	 *
	 * 该效果用于表达现代规则中“被指定属性技能命中时无效，并提升自身某项能力”的稳定特性。触发位置与
	 * [ElementSkillAbsorbHeal] 一致：命中判定成功后、普通伤害或附加效果写入前。能力阶级仍受 -6..6 边界夹取；
	 * 如果已经到达上限，技能仍被吸收，只是不产生 [BattleEvent.StatStageChanged] 事件。
	 */
	data class ElementSkillAbsorbStatStage(
		val elementId: Long,
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(stageDelta in -6..6 && stageDelta != 0) { "stageDelta must be between -6 and 6 and not zero" }
		}
	}

	data class LowHpElementDamageBoost(
		val elementId: Long,
		val hpThresholdNumerator: Int = 1,
		val hpThresholdDenominator: Int = 3,
		val multiplier: Double = 1.5,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(hpThresholdNumerator > 0) { "hpThresholdNumerator must be positive" }
			require(hpThresholdDenominator > 0) { "hpThresholdDenominator must be positive" }
			require(hpThresholdNumerator <= hpThresholdDenominator) { "hp threshold numerator must not exceed denominator" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	data class ContactStatusOnAttacker(
		val status: BattleMajorStatus,
		val chancePercent: Int,
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		}
	}

	/**
	 * 成员出场时修改当前对手上场成员的能力阶级。
	 *
	 * 该结构用于表达现代规则中“出场时令对手能力下降”的稳定特性。它不保存具体特性名称，也不保存本地化文本；
	 * 资料层把特性 policy 转换为要修改的能力项和阶级变化量。第一批只支持当前对手上场成员作为目标，
	 * 适合单打和双打中的常见出场降攻规则。替身、反制能力提升、特性失效等复杂交互会在对应模型具备后继续扩展。
	 */
	data class SwitchInStatStageChange(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta in -6..6 && stageDelta != 0) { "stageDelta must be between -6 and 6 and not zero" }
		}
	}

	/**
	 * 成员出场时设置全场天气。
	 *
	 * 该结构用于表达现代规则中“进入场地后改变天气”的稳定特性。引擎只保存目标天气和持续回合，不保存具体
	 * 特性名称、技能名或本地化文本。`turnsRemaining` 为 null 时表示永久天气或测试 fixture 不要求递减；
	 * 现代主系列普通天气特性通常传入 5，让回合末持续时间系统统一递减和结束。
	 */
	data class SwitchInWeatherChange(
		val weather: BattleWeather,
		val turnsRemaining: Int? = 5,
	) : BattleAbilityEffect {
		init {
			require(weather != BattleWeather.NONE) { "switch-in weather change requires an active weather" }
			require(turnsRemaining == null || turnsRemaining > 0) {
				"turnsRemaining must be positive when present"
			}
		}
	}

	/**
	 * 成员出场时设置全场场地。
	 *
	 * 该结构覆盖现代规则中“进入场地后改变场地”的稳定特性。场地具体如何影响伤害、状态免疫、回合末回复或
	 * 先制封锁，仍由环境规则和状态机在对应阶段解释；出场特性只负责把环境事实写入战斗状态。
	 */
	data class SwitchInTerrainChange(
		val terrain: BattleTerrain,
		val turnsRemaining: Int? = 5,
	) : BattleAbilityEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "switch-in terrain change requires an active terrain" }
			require(turnsRemaining == null || turnsRemaining > 0) {
				"turnsRemaining must be positive when present"
			}
		}
	}
}
