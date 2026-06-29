package io.github.lishangbu.battleengine.model

/**
 * 特性在战斗中的可执行效果。
 *
 * 该 sealed 类型是规则资料 policy code 进入纯引擎后的结构化形态。第一批实现几个高价值 hook：
 * 低体力时强化指定属性伤害、满 HP 承受致命伤害时保留 1 HP、吸收指定属性技能并回复或提阶、阻止对手先制技能影响己方、
 * 天气下速度修正、天气伤害免疫、天气下回合末回复、受到接触类技能后有概率给攻击方附加主要异常状态，稳定状态免疫、环境下速度修正，
 * 间接伤害免疫、技能反作用伤害免疫、击中要害免疫、无视对手伤害公式能力阶级变化、无视对手命中/闪避阶级
 * 变化、攻击时无视目标特性效果、免疫声音类技能，以及成员出场时的能力阶级变化、天气设置和场地设置。
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
	 * 免疫非技能直接伤害。
	 *
	 * 该效果用于表达现代规则中“只会被直接攻击伤害扣减 HP”的稳定特性。引擎把普通物理/特殊技能命中后写入目标
	 * 的 [io.github.lishangbu.battleengine.model.BattleEvent.DamageApplied] 视为直接伤害；异常状态回合末伤害、
	 * 天气伤害、入场陷阱、技能反作用伤害、携带道具反伤和混乱自伤等都属于间接伤害，命中该效果时不改变 HP、
	 * 不触发低体力回复道具，也不追加对应伤害事件。
	 *
	 * 该效果只描述伤害免疫，不阻止主要异常状态或临时状态本身写入，也不移除已存在的天气、陷阱或携带道具倍率。
	 * 例如造成伤害后附带反伤的道具仍然提供伤害倍率，但它产生的反伤会被这里阻止。
	 */
	object IndirectDamageImmunity : BattleAbilityEffect

	/**
	 * 免疫技能自身造成的反作用伤害。
	 *
	 * 该效果只阻止 [BattleSkillHpEffect.RecoilByDamageDealt] 这类“技能命中并造成实际伤害后，使用者按伤害
	 * 比例自损”的反作用伤害，不阻止携带道具反伤、混乱自伤、入场陷阱、异常状态、天气或其它间接伤害。
	 * 因此它比 [IndirectDamageImmunity] 范围更窄：拥有者仍会正常受到伤害增幅道具造成的最大 HP 固定反伤。
	 *
	 * 公开现代规则中挣扎等特殊技能的自损不被这类特性阻止；当前引擎尚未建模挣扎的专用伤害来源，所以该例外
	 * 会在加入对应技能来源时通过更细的 HP effect 或来源标记接入，而不是在这里判断技能名称。
	 */
	object SkillRecoilDamageImmunity : BattleAbilityEffect

	/**
	 * 免疫被技能击中要害。
	 *
	 * 该效果用于表达现代规则中“对手技能无法对拥有者造成击中要害”的稳定特性。它不改变技能自身的
	 * `criticalHitStage`，也不改变要害随机数的消费规则；状态机先按技能等级结算是否原本会击中要害，再由
	 * 目标方的该效果把本次伤害请求中的 `criticalHit` 置为 false。这样普通随机要害仍保留可复盘的随机轨迹，
	 * 必定要害技能也会被明确降回非要害伤害。
	 *
	 * 它只影响直接技能伤害是否按要害倍率计算，不阻止伤害本身、附加状态、能力阶级变化、天气、场地或其它
	 * 伤害来源。若后续接入能绕过目标特性的规则，应在特性抑制层统一处理，而不是在这里判断具体技能名称。
	 */
	object CriticalHitImmunity : BattleAbilityEffect

	/**
	 * 无视对手在伤害公式中使用的能力阶级变化。
	 *
	 * 该效果用于表达现代规则中“结算双方直接伤害时，不读取对手强化或削弱过的相关攻防阶级”的稳定特性：
	 * - 持有效果的一方作为攻击方时，忽略目标的防御或特防阶级，按目标原始防御侧数值进入公式。
	 * - 持有效果的一方作为防守方时，忽略使用者的攻击或特攻阶级，按使用者原始攻击侧数值进入公式。
	 *
	 * 它不清除双方状态，也不修改战斗快照里的阶级；只影响本次普通物理/特殊伤害计算。击中要害、灼伤物理减半、
	 * 天气防御修正、场地修正、道具倍率和特性倍率仍按各自规则继续结算。命中与闪避阶级由
	 * [IgnoreOpponentAccuracyStatStages] 在状态机命中流程中处理，避免伤害公式承担命中语义。
	 */
	object IgnoreOpponentDamageStatStages : BattleAbilityEffect

	/**
	 * 无视对手在命中判定中使用的命中或闪避阶级变化。
	 *
	 * 该效果用于表达现代规则中“结算技能命中率时，不读取对手改变过的命中相关阶级”的稳定特性：
	 * - 持有效果的一方作为攻击方时，忽略目标的闪避阶级，按目标 0 闪避阶级计算有效命中。
	 * - 持有效果的一方作为防守方时，忽略使用者的命中阶级，按使用者 0 命中阶级计算有效命中。
	 *
	 * 它只影响状态机中的命中随机判定，不改变双方快照里的真实阶级，也不影响必中技能、天气命中覆盖、保护、
	 * 属性免疫或普通伤害公式。伤害公式中的攻击/防御/特攻/特防阶级由 [IgnoreOpponentDamageStatStages]
	 * 独立处理，便于 fixture 精确定位失败发生在哪个结算阶段。
	 */
	object IgnoreOpponentAccuracyStatStages : BattleAbilityEffect

	/**
	 * 使用技能时无视目标侧防守特性效果。
	 *
	 * 该效果用于表达现代规则中“本成员使用技能时，目标的部分特性不会影响本次技能”的稳定特性。它不删除目标
	 * 快照上的特性，也不让特性在回合末、入场、天气、道具或目标自己行动时失效；只在本次技能结算链路里跳过
	 * 目标侧防守型特性检查，例如属性吸收、满 HP 致命伤害保留、击中要害免疫、主要/临时状态免疫、先制技能侧
	 * 防护和目标作为防守方时无视攻击者命中或伤害阶级变化。
	 *
	 * 判断范围刻意限定为“攻击方对对手目标使用技能”。同侧辅助、自身目标、使用者自己的攻击侧特性和目标携带
	 * 道具都不受该效果影响，避免把特性抑制误扩散到道具、场地、属性天然免疫或非技能伤害来源。
	 */
	object IgnoreTargetAbilityEffects : BattleAbilityEffect

	/**
	 * 免疫其它成员使用的声音类技能。
	 *
	 * 该效果用于表达现代规则中“拥有者不会受到声音类技能影响”的稳定特性。它只读取技能槽上的
	 * `soundBased` 结构化标签，不判断技能名称；伤害技能和变化技能都会在命中与附加效果前被阻止。
	 *
	 * 拥有者自己使用的声音类技能不会被该效果阻止；对手若拥有 [IgnoreTargetAbilityEffects]，则可以在本次技能中
	 * 绕过目标的该免疫。替身是否被声音类技能穿透仍由替身规则单独处理，这里只表达目标特性的免疫能力。
	 */
	object SoundBasedSkillImmunity : BattleAbilityEffect

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

	/**
	 * 低体力时强化指定属性技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“当前 HP 小于等于最大 HP 的 1/3 时，指定属性技能伤害提高 50%”这类稳定特性。
	 * 它只影响使用者主动造成的普通物理/特殊技能伤害；是否匹配属性、当前 HP 是否达到阈值以及倍率叠乘都由
	 * 伤害计算阶段读取，状态机本身不关心具体特性名称。
	 */
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

	/**
	 * 受到接触类技能成功命中后，按概率把主要异常状态附加给攻击方。
	 *
	 * 该效果用于表达现代规则中一类防守方受接触后反制攻击方的稳定特性。它只在普通技能已经命中并至少完成
	 * 本次目标结算后触发；若技能被属性免疫、保护、替身完全阻止、目标已经倒下前未发生有效接触，或技能槽没有
	 * `makesContact` 标签，则不会进入该效果。
	 *
	 * `chancePercent` 为 100 时不消费额外随机数；低于 100 时使用独立的接触状态随机掷点。状态真正附加给攻击方前，
	 * 仍会复用主要异常状态的属性、场地、特性、道具和已有状态阻止流程，因此它不会绕过攻击方自身免疫。
	 */
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
