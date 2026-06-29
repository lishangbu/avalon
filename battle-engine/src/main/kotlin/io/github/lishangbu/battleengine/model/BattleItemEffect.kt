package io.github.lishangbu.battleengine.model

/**
 * 携带道具在战斗中的可执行效果。
 *
 * 第一批覆盖几类常见 hook：造成伤害时提升倍率并按最大 HP 比例反伤、回合末按最大 HP 比例回复、天气伤害免疫、
 * 环境和一侧屏障持续回合延长、低体力一次性回复、满 HP 致命伤害保留 1 HP、蓄力技能一次性跳过等待、
 * 稳定状态免疫、稳定指定属性/分类威力加成、受到指定属性伤害时减免、效果绝佳伤害加成，以及成功获得主要异常
 * 状态或临时状态后的即时解除。
 * 更复杂的道具生命周期会继续扩展为新的结构化效果，而不是在引擎中解析自由文本。
 */
sealed interface BattleItemEffect {
	/**
	 * 携带道具提供的一组主要异常状态免疫。
	 *
	 * 道具是否被消耗、能否被拍落等物品生命周期不在该效果中表达；这里仅描述进入状态附加流程前的稳定阻止条件。
	 */
	data class MajorStatusImmunity(
		val statuses: Set<BattleMajorStatus>,
	) : BattleItemEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 成员获得匹配的主要异常状态后，立刻解除该状态并可消费携带道具。
	 *
	 * 该效果用于表达现代主系列中“已被附加状态后立即治愈”的一次性携带道具。它不同于
	 * [MajorStatusImmunity]：免疫会阻止状态写入，也不会消费状态私有随机数；解除道具则必须等
	 * [BattleEvent.StatusApplied] 已经成为事实后触发，再追加 [BattleEvent.StatusCleared]，并按
	 * [consumesItem] 决定是否清空成员的携带道具与道具效果。
	 *
	 * `statuses` 允许资料层表达“解除任意主要异常状态”或“只解除睡眠”等变体。效果只处理主要异常状态，
	 * 不处理混乱、畏缩、替身、能力阶级变化或 HP 回复；这些规则应继续使用各自的结构化效果。
	 */
	data class MajorStatusCure(
		val statuses: Set<BattleMajorStatus>,
		val consumesItem: Boolean = true,
	) : BattleItemEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 携带道具提供的一组临时状态免疫。
	 *
	 * 该效果用于资料层把“阻止附加效果造成的畏缩/混乱”等稳定保护转成引擎可执行规则。道具是否被消耗、
	 * 能否被禁用或拍落不在这里表达，由后续道具生命周期规则单独处理。
	 */
	data class VolatileStatusImmunity(
		val statuses: Set<BattleVolatileStatus>,
	) : BattleItemEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 成员获得匹配的临时状态后，立刻解除该状态并可消费携带道具。
	 *
	 * 该效果用于表达现代规则中“不会阻止临时状态写入，但会在写入后立刻治愈”的一次性携带道具。它与
	 * [VolatileStatusImmunity] 的区别和主要异常状态一致：免疫发生在状态写入前，而治愈发生在
	 * [BattleEvent.VolatileStatusApplied] 之后，随后追加 [BattleEvent.VolatileStatusCleared]。
	 *
	 * 当前引擎第一批临时状态包含畏缩和混乱。资料层通常会把该效果挂到解除混乱的道具上；如果未来扩展其它临时
	 * 状态，只需要把对应枚举加入 `statuses`，无需在状态机里判断具体道具名称。
	 */
	data class VolatileStatusCure(
		val statuses: Set<BattleVolatileStatus>,
		val consumesItem: Boolean = true,
	) : BattleItemEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 携带道具提供的一组天气伤害免疫。
	 *
	 * 该效果只阻止天气在回合末直接造成的固定伤害，不表达粉末免疫、道具消耗或其它道具生命周期。
	 */
	data class WeatherDamageImmunity(
		val weathers: Set<BattleWeather>,
	) : BattleItemEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather damage immunity cannot target NONE" }
		}
	}

	/**
	 * 携带者主动建立指定天气时，把原本的普通持续回合替换为更长持续回合。
	 *
	 * 该效果用于表达现代规则中的天气延长道具。它只在“携带者通过技能或特性成功设置天气”这类明确有来源成员的
	 * 流程中读取，不会修改已经存在的环境，也不会影响其它成员、无来源规则或永久天气。`weathers` 是允许被延长
	 * 的天气集合，避免把某个天气专属道具误用于其它天气；`turnsRemaining` 是写入环境前的完整持续回合数，回合
	 * 末仍会由统一持续时间系统递减。
	 */
	data class WeatherDurationExtension(
		val weathers: Set<BattleWeather>,
		val turnsRemaining: Int,
	) : BattleItemEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather duration extension cannot target NONE" }
			require(turnsRemaining > 0) { "turnsRemaining must be positive" }
		}
	}

	/**
	 * 携带者主动建立场地时，把原本的普通持续回合替换为更长持续回合。
	 *
	 * 该效果覆盖现代规则中的场地延长道具。它不保存具体道具名称，也不解析技能或特性文本；资料层负责把道具
	 * policy 转成这里的结构化效果。引擎只在携带者成功设置场地时根据目标场地匹配 `terrains`，然后把即将写入的
	 * 持续回合替换为 `turnsRemaining`。场地的伤害、状态免疫、先制阻挡和回合末回复仍由其它环境规则处理。
	 */
	data class TerrainDurationExtension(
		val terrains: Set<BattleTerrain>,
		val turnsRemaining: Int,
	) : BattleItemEffect {
		init {
			require(terrains.isNotEmpty()) { "terrains must not be empty" }
			require(BattleTerrain.NONE !in terrains) { "terrain duration extension cannot target NONE" }
			require(turnsRemaining > 0) { "turnsRemaining must be positive" }
		}
	}

	/**
	 * 携带者成功建立指定一侧防守屏障时，把普通持续回合替换为更长持续回合。
	 *
	 * 该效果用于表达现代规则中的屏障延长道具。它只在携带者通过技能成功建立 [BattleSideDamageReduction] 时读取；
	 * 已经存在的同类屏障仍然不会被刷新，屏障被清除、被破坏或特殊失败语义也由后续专门规则处理。`kinds` 限定
	 * 可以被延长的屏障种类，使物理屏障、特殊屏障和同时覆盖两类标准伤害的屏障能共享同一种结构化道具效果。
	 */
	data class SideDamageReductionDurationExtension(
		val kinds: Set<BattleSideDamageReductionKind>,
		val turnsRemaining: Int,
	) : BattleItemEffect {
		init {
			require(kinds.isNotEmpty()) { "kinds must not be empty" }
			require(turnsRemaining > 0) { "turnsRemaining must be positive" }
		}
	}

	/**
	 * 造成伤害时提升最终伤害倍率，并在成功造成伤害后让使用者承受最大 HP 比例反伤。
	 *
	 * `recoilDenominator` 表示使用者最大 HP 的分母，例如 10 表示反伤为 `floor(maxHp / 10)`，最少 1 点。
	 * 反伤不取决于实际造成了多少伤害，因此不会被随机浮动、属性克制、屏障或其它伤害修正间接改变。
	 */
	data class DamageBoostWithRecoil(
		val multiplier: Double,
		val recoilDenominator: Int,
	) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(recoilDenominator > 0) { "recoilDenominator must be positive" }
		}
	}

	/**
	 * 携带道具对指定属性技能提供稳定威力倍率。
	 *
	 * 该效果用于表达现代规则中一批“携带后某个属性技能威力提高 20%”的非消耗型道具，例如一般、火、水、
	 * 草、电、冰、格斗、毒、地面、飞行、超能力、虫、岩石、幽灵、龙、恶、钢和妖精属性的常规增益道具。
	 * 它和 [DamageBoostWithRecoil] 的区别在于不会产生反伤；也不同于一次性宝石或特定生物形态道具，因为
	 * 本效果既不消费道具，也不改变成员属性、技能属性或其它战斗状态。
	 *
	 * `elementId` 来自规则资料层，不能由引擎猜测；`multiplier` 表示匹配属性时叠乘到技能有效威力上的倍率。
	 * 如果攻击技能属性不匹配，该效果保持中性倍率 1.0。多个同类效果会按资料声明顺序叠乘，便于自定义规则复用。
	 */
	data class ElementDamageBoost(
		val elementId: Long,
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 携带道具对指定伤害分类技能提供稳定威力倍率。
	 *
	 * 该效果用于表达现代规则中“物理技能威力提高 10%”或“特殊技能威力提高 10%”的非消耗型道具。它发生在
	 * 普通伤害公式的威力阶段，因此会先修改技能有效威力，再进入等级、攻防和取整流程；这和生命宝珠、达人带等
	 * 最终伤害阶段倍率不同。
	 *
	 * `damageClasses` 通常包含物理或特殊中的一个，也允许自定义规则同时覆盖两类直接伤害；变化类技能不会进入
	 * 普通伤害公式，因此即使资料层误配也不会产生伤害。
	 */
	data class DamageClassPowerBoost(
		val damageClasses: Set<BattleDamageClass>,
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(damageClasses.isNotEmpty()) { "damageClasses must not be empty" }
			require(BattleDamageClass.STATUS !in damageClasses) { "status skills do not have power in standard damage formula" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 携带道具在技能对目标效果绝佳时提供最终伤害倍率。
	 *
	 * 该效果用于表达达人带一类非消耗型道具。它不改变技能威力、属性、命中或属性克制结果，只在属性克制倍率
	 * 已经确认大于 1.0 后，把倍率叠乘到最终伤害修正链中。该效果不消费道具，也不对固定伤害、状态技能、
	 * 入场陷阱或天气伤害生效。
	 */
	data class SuperEffectiveDamageBoost(
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 防守方携带道具对指定属性技能提供一次性伤害减免。
	 *
	 * 该效果用于表达现代规则中的抗性树果：大多数抗性树果只在本体受到对应属性且效果绝佳的技能伤害时触发，
	 * 将最终伤害按 0.5 倍继续结算并消费道具。一般属性没有“效果绝佳”关系，因此一般属性抗性树果会把
	 * [requiresSuperEffective] 设为 false，只要求技能属性匹配且没有被替身等前置规则挡在本体之外。
	 *
	 * 本效果只描述普通直接技能伤害的公式倍率和道具消费开关，不处理自然恩惠、采摘、紧张感、魔术空间、
	 * 道具回收或其它完整道具生命周期。替身是否阻止触发由状态机在调用伤害计算器前显式传入，避免计算器直接
	 * 理解站位或替身运行态。
	 */
	data class ElementDamageReduction(
		val elementId: Long,
		val multiplier: Double,
		val requiresSuperEffective: Boolean = true,
		val consumesItem: Boolean = true,
	) : BattleItemEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}

		/**
		 * 判断本效果是否应该应用到当前技能属性和属性克制结果上。
		 */
		fun matches(skillElementId: Long, effectiveness: Double): Boolean =
			skillElementId == elementId && (!requiresSuperEffective || effectiveness > 1.0)
	}

	/**
	 * 当前上场成员在完整回合末按最大 HP 固定比例回复。
	 *
	 * `healDenominator` 表示回复分母，例如 16 表示回复 `floor(maxHp / 16)`，最少 1 点且不超过缺失 HP。
	 * 该效果描述稳定的携带道具回复，不消费道具，也不表达回复封锁、强制失效或复杂优先级。
	 */
	data class HeldEndTurnHeal(
		val healDenominator: Int,
	) : BattleItemEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * HP 降到指定比例及以下时触发的一次性回复。
	 *
	 * 该结构覆盖现代主系列里常见的低体力树果：触发线通常是最大 HP 的 1/2；回复量可以是固定值
	 * （例如固定回复 10 点），也可以是最大 HP 的固定分母比例（例如回复 1/4）。触发后由状态机消费携带道具，
	 * 因此同一个成员不会在后续伤害中重复触发。
	 */
	data class LowHpHeal(
		val triggerHpNumerator: Int = 1,
		val triggerHpDenominator: Int = 2,
		val fixedHealAmount: Int? = null,
		val healDenominator: Int? = null,
	) : BattleItemEffect {
		init {
			require(triggerHpNumerator > 0) { "triggerHpNumerator must be positive" }
			require(triggerHpDenominator > 0) { "triggerHpDenominator must be positive" }
			require(triggerHpNumerator <= triggerHpDenominator) {
				"trigger HP numerator must not exceed denominator"
			}
			require((fixedHealAmount != null) xor (healDenominator != null)) {
				"exactly one healing amount strategy must be configured"
			}
			require(fixedHealAmount == null || fixedHealAmount > 0) { "fixedHealAmount must be positive when present" }
			require(healDenominator == null || healDenominator > 0) { "healDenominator must be positive when present" }
		}

		/**
		 * 判断当前 HP 是否已经达到触发线。
		 */
		fun shouldTrigger(currentHp: Int, maxHp: Int): Boolean =
			currentHp > 0 && currentHp * triggerHpDenominator <= maxHp * triggerHpNumerator

		/**
		 * 计算本次触发的原始回复量，调用方再根据当前缺失 HP 夹取。
		 */
		fun healAmount(maxHp: Int): Int =
			fixedHealAmount ?: (maxHp / requireNotNull(healDenominator)).coerceAtLeast(1)
	}

	/**
	 * 满 HP 承受会导致倒下的直接伤害时保留 1 HP。
	 *
	 * 该效果用于一次性保命类携带道具。它只在技能直接伤害写入 HP 前触发；如果持有者已经不是满 HP，
	 * 或伤害来源是异常、天气、入场陷阱、混乱自伤、反作用伤害等非普通技能伤害，则不会触发。
	 * `consumesItem` 为 true 时，触发后会清空携带道具和其效果，保证同一成员不会重复触发。
	 */
	data class SurviveFatalDamageAtFullHp(
		val remainingHp: Int = 1,
		val consumesItem: Boolean = true,
	) : BattleItemEffect {
		init {
			require(remainingHp > 0) { "remainingHp must be positive" }
		}
	}

	/**
	 * 首次宣告蓄力技能时消耗携带道具并跳过等待回合。
	 *
	 * 该效果用于表达现代规则中的“一次性立刻释放蓄力技能”道具。触发阶段位于技能宣告、PP 消耗和 `SkillUsed`
	 * 事件之后，但早于保护、命中、免疫和伤害流程；因此 replay 会看到技能被使用、道具被消费，然后技能同回合
	 * 正常结算。效果本身不指定技能名单，是否需要蓄力仍由 [BattleSkillSlot.chargesBeforeUse] 决定。
	 */
	data class ChargeSkipOnce(
		val consumesItem: Boolean = true,
	) : BattleItemEffect

	/**
	 * 限制成员只能继续选择首次宣告的技能，并提供速度倍率。
	 *
	 * 该结构用于表达讲究类速度道具。它不是技能自身的“锁招”：技能锁招会强制继续执行并保存目标槽位，
	 * 而讲究类道具只限制后续可提交的技能，目标仍由玩家每回合重新选择。替换离场会清除成员上的锁定技能。
	 */
	data class ChoiceSkillLock(
		val speedMultiplier: Double,
	) : BattleItemEffect {
		init {
			require(speedMultiplier > 0.0) { "speedMultiplier must be positive" }
		}
	}
}
