package io.github.lishangbu.battleengine.model

/**
 * 携带道具在战斗中的可执行效果。
 *
 * 当前覆盖几类常见 hook：造成伤害时提升倍率并按最大 HP 比例反伤、回合末按最大 HP 比例回复或扣血、天气伤害免疫、
 * 环境和一侧屏障持续回合延长、低体力一次性回复、满 HP 致命伤害保留 1 HP、蓄力技能一次性跳过等待、
 * 稳定状态免疫、稳定指定属性/分类威力加成、受到指定属性伤害时减免、效果绝佳伤害加成、造成伤害后回复，
 * 成功获得主要异常状态或临时状态后的即时解除，以及体重相关规则读取时的当前体重修正。
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
	 * 资料层通常会把该效果挂到解除混乱的道具上；如果某个道具能解除其它临时状态，只需要把对应枚举加入
	 * `statuses`，无需在状态机里判断具体道具名称。
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
	 * 携带者成功造成伤害后，按实际伤害量的一定比例回复 HP。
	 *
	 * 该效果用于表达贝壳之铃一类非消耗型道具。回复基数是本次技能最终造成的实际 HP 损失，而不是伤害公式的
	 * 原始结果；因此打到替身时使用替身实际损失 HP，打到本体时使用本体实际损失 HP，目标剩余 HP 不足导致的
	 * 溢出伤害不会被计入。效果不处理主动使用道具、回复封锁或强制换人等更复杂流程。
	 */
	data class DamageDealtHeal(
		val healDenominator: Int,
	) : BattleItemEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 携带道具对指定属性技能提供稳定威力倍率。
	 *
	 * 该效果用于表达现代规则中一批“携带后某个属性技能威力提高 20%”的非消耗型道具，例如一般、火、水、
	 * 草、电、冰、格斗、毒、地面、飞行、超能力、虫、岩石、幽灵、龙、恶、钢和妖精属性的常规增益道具。
	 * 它和 [DamageBoostWithRecoil] 的区别在于不会产生反伤；也不同于一次性宝石或特定精灵形态道具，因为
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
	 * 携带道具对拳击类技能提供稳定威力倍率。
	 *
	 * 该效果用于表达现代规则中拳击手套的伤害部分：携带者使用带 [BattleSkillSlot.punchBased] 标签的技能时，
	 * 技能有效威力提升 10%。它和 [PunchBasedContactSuppression] 刻意拆开，因为同一个道具有两个独立后果：
	 * 一个影响伤害公式的威力阶段，另一个影响“本次是否接触”的事件事实。把二者拆成两个结构化效果，可以让
	 * 伤害计算器、保护 gate、接触副作用和未来的道具禁用/拍落流程分别读取自己真正需要的事实。
	 *
	 * 该倍率只参与普通直接伤害公式，不会影响固定伤害、比例伤害、天气/陷阱等间接伤害，也不会消费道具。它按
	 * 威力阶段处理，因此会先和属性强化、分类强化、天气/场地威力修正一起得到有效威力，再进入基础伤害取整。
	 */
	data class PunchBasedSkillPowerBoost(
		val multiplier: Double = 1.1,
	) : BattleItemEffect {
		init {
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

	/** 让携带者在个人技能与伤害结算中忽略晴天和雨天。 */
	data class SunRainEffectImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/** 仅当持有者拥有指定属性时，在完整回合末按最大 HP 比例回复。 */
	data class HeldEndTurnHealForElement(
		val elementId: Long,
		val healDenominator: Int,
	) : BattleItemEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 当前上场成员在完整回合末按自身最大 HP 固定比例受到间接伤害。
	 *
	 * 该效果用于表达附着针这类非消耗型道具的回合末自伤部分。它和 [DamageBoostWithRecoil]、[ContactDamageToAttacker]
	 * 的触发边界刻意分开：这里不要求本回合造成伤害，也不要求发生接触，只要成员在回合末仍然持有该道具且可以战斗，
	 * 就按 `damageDenominator` 计算 `floor(maxHp / denominator)`，最少 1 点。伤害属于间接伤害，因此会被间接伤害
	 * 免疫阻止；倒下、低体力道具和胜负收口仍由回合末伤害统一流程处理。
	 */
	data class HeldEndTurnDamage(
		val damageDenominator: Int,
	) : BattleItemEffect {
		init {
			require(damageDenominator > 0) { "damageDenominator must be positive" }
		}
	}

	/** 仅当持有者不拥有指定属性时，在完整回合末按最大 HP 比例受到间接伤害。 */
	data class HeldEndTurnDamageWithoutElement(
		val elementId: Long,
		val damageDenominator: Int,
	) : BattleItemEffect {
		init {
			require(damageDenominator > 0) { "damageDenominator must be positive" }
		}
	}

	/** 对指定属性伤害技能提供一次性威力倍率，成功造成本体伤害后消费。 */
	data class ConsumableElementDamageBoost(val elementId: Long, val multiplier: Double) : BattleItemEffect

	/**
	 * 携带期间阻止粉末和孢子类技能影响持有者。
	 *
	 * 该效果只读取技能的结构化 `powderBased` 标记，不按技能名称猜测；技能仍会被宣告并消耗 PP，但不会继续消费
	 * 命中或状态持续时间随机数。道具移除后效果随成员道具效果集合一起消失。
	 */
	data class PowderSkillImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/**
	 * 携带期间忽略所在侧已经存在的全部入场陷阱。
	 *
	 * 换入事件仍然正常产生，但隐形岩、撒菱、毒菱和黏黏网均不会造成伤害、状态、能力阶级变化或吸收毒菱。
	 * 道具本身不消费；失去道具后下一次换入会重新按场上陷阱结算。
	 */
	data class EntryHazardImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/**
	 * 允许持有者忽略由对手技能或特性形成的主动替换限制。
	 *
	 * 当前引擎已结构化承载束缚来源，因此该效果先用于绕过束缚的主动替换限制；休息、蓄力和技能自身锁招不属于
	 * 对手施加的逃脱限制，仍会阻止主动替换。
	 */
	data class SwitchRestrictionImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/**
	 * 完整回合末尝试让当前持有者获得指定主要异常状态。
	 *
	 * 该效果用于火焰宝珠和剧毒宝珠。状态只在持有者仍可战斗、仍持有道具且没有已有主要异常时尝试附加，
	 * 并继续经过属性、场地、特性与道具免疫判断；本回合新附加的状态不会倒回更早的持续伤害阶段立即扣血。
	 */
	data class HeldEndTurnMajorStatus(
		val status: BattleMajorStatus,
	) : BattleItemEffect

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
		val confusesIfNatureDecreases: BattleStat? = null,
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
	 * HP 降到指定比例及以下时，提升一项能力阶级并消耗携带道具。
	 *
	 * 默认触发线是最大 HP 的 1/4，适用于攻击、防御、速度、特攻和特防树果。能力已经达到上限时不会消费道具。
	 */
	data class LowHpStatStageBoost(
		val stat: BattleStat,
		val stageDelta: Int,
		val triggerHpNumerator: Int = 1,
		val triggerHpDenominator: Int = 4,
	) : BattleItemEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
			require(triggerHpNumerator > 0) { "triggerHpNumerator must be positive" }
			require(triggerHpDenominator > 0) { "triggerHpDenominator must be positive" }
			require(triggerHpNumerator <= triggerHpDenominator) {
				"trigger HP numerator must not exceed denominator"
			}
		}

		fun shouldTrigger(currentHp: Int, maxHp: Int): Boolean =
			currentHp > 0 && currentHp.toLong() * triggerHpDenominator <= maxHp.toLong() * triggerHpNumerator
	}

	/** HP 降到指定比例及以下时，从未到上限的候选能力中随机提升一项并消费携带道具。 */
	data class LowHpRandomStatStageBoost(
		val stats: Set<BattleStat>,
		val stageDelta: Int,
		val triggerHpNumerator: Int = 1,
		val triggerHpDenominator: Int = 4,
	) : BattleItemEffect {
		init {
			require(stats.isNotEmpty()) { "stats must not be empty" }
			require(stageDelta > 0) { "stageDelta must be positive" }
			require(triggerHpNumerator in 1..triggerHpDenominator) { "trigger HP fraction must be valid" }
		}

		fun shouldTrigger(currentHp: Int, maxHp: Int): Boolean =
			currentHp > 0 && currentHp.toLong() * triggerHpDenominator <= maxHp.toLong() * triggerHpNumerator
	}

	/** HP 降到指定比例及以下时，保存下一次技能行动的命中率倍率并消费携带道具。 */
	data class LowHpNextSkillAccuracyBoost(
		val multiplier: Double,
		val triggerHpNumerator: Int = 1,
		val triggerHpDenominator: Int = 4,
	) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(triggerHpNumerator in 1..triggerHpDenominator) { "trigger HP fraction must be valid" }
		}

		fun shouldTrigger(currentHp: Int, maxHp: Int): Boolean =
			currentHp > 0 && currentHp.toLong() * triggerHpDenominator <= maxHp.toLong() * triggerHpNumerator
	}

	/** HP 降到指定比例及以下时，建立在场期间的要害等级加成并消耗携带道具。 */
	data class LowHpCriticalHitStageBoost(
		val stageBonus: Int,
		val triggerHpNumerator: Int = 1,
		val triggerHpDenominator: Int = 4,
	) : BattleItemEffect {
		init {
			require(stageBonus > 0) { "stageBonus must be positive" }
			require(triggerHpNumerator > 0) { "triggerHpNumerator must be positive" }
			require(triggerHpDenominator > 0) { "triggerHpDenominator must be positive" }
			require(triggerHpNumerator <= triggerHpDenominator) {
				"trigger HP numerator must not exceed denominator"
			}
		}

		fun shouldTrigger(currentHp: Int, maxHp: Int): Boolean =
			currentHp > 0 && currentHp.toLong() * triggerHpDenominator <= maxHp.toLong() * triggerHpNumerator
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

	/** 承受致命技能伤害时按概率保留指定 HP，触发后不消费道具。 */
	data class RandomFatalDamageSurvival(
		val chancePercent: Int,
		val remainingHp: Int = 1,
	) : BattleItemEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between 1 and 100" }
			require(remainingHp > 0) { "remainingHp must be positive" }
		}
	}

	/**
	 * 携带期间为所有技能提供固定的击中要害等级加成。
	 *
	 * 加成在每次要害判定时与技能自身等级、成员在场期间的聚气等级相加；道具被移除时效果会随
	 * [BattleParticipant.itemEffects] 一起消失，不写入成员的持久要害等级字段。
	 */
	data class CriticalHitStageBoost(
		val stageDelta: Int,
	) : BattleItemEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 携带期间乘算使用者普通技能的最终命中率。 */
	data class AccuracyMultiplier(
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 目标已经完成本回合技能行动时，乘算使用者普通技能的最终命中率。 */
	data class AccuracyMultiplierAfterTargetActed(val multiplier: Double) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 携带期间乘算对手以持有者为目标时普通技能的最终命中率。 */
	data class OpponentAccuracyMultiplier(
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 携带期间乘算伤害公式读取的指定防守能力。 */
	data class DefendingStatMultiplier(
		val stats: Set<BattleStat>,
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(stats.isNotEmpty()) { "stats must not be empty" }
			require(stats.all { it == BattleStat.DEFENSE || it == BattleStat.SPECIAL_DEFENSE }) {
				"defending stats must contain only defense or special defense"
			}
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 携带期间禁止玩家选择变化分类技能。 */
	data class StatusSkillRestriction(private val marker: Unit = Unit) : BattleItemEffect

	/** 携带期间强制把成员视为接地，并使地面属性的飞行属性免疫失效。 */
	data class GroundingOverride(private val marker: Unit = Unit) : BattleItemEffect

	/** 携带期间让成员不接地；受到本体伤害后消费并失去该效果。 */
	data class AirborneUntilDamaged(private val marker: Unit = Unit) : BattleItemEffect

	/** 携带期间让成员由自身属性提供的伤害免疫失效。 */
	data class TypeImmunitySuppression(private val marker: Unit = Unit) : BattleItemEffect

	/** 携带期间阻止对手技能或特性降低持有者能力阶级。 */
	data class OpponentStatStageReductionImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/** 能力阶级实际下降后，把持有者全部负能力阶级恢复到 0 并消费道具。 */
	data class NegativeStatStageReset(private val marker: Unit = Unit) : BattleItemEffect

	/** 因对手出场特性实际降低能力后，提升指定能力并消费道具。 */
	data class AbilityStatReductionReactiveBoost(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleItemEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 对手在本次技能结算中提升能力后，复制其正向能力变化并消费道具。 */
	data class OpponentPositiveStatStageCopy(private val marker: Unit = Unit) : BattleItemEffect

	/** 承受技能实际伤害后让携带者强制换下。 */
	data class DamagedForceSelfSwitch(private val marker: Unit = Unit) : BattleItemEffect

	/** 承受技能实际伤害后让攻击者强制换下。 */
	data class DamagedForceAttackerSwitch(private val marker: Unit = Unit) : BattleItemEffect

	/** 能力被实际降低后让携带者强制换下。 */
	data class NegativeStatStageForceSelfSwitch(private val marker: Unit = Unit) : BattleItemEffect

	/** 携带者陷入着迷后，让状态来源也对携带者着迷。 */
	data class InfatuationReflectToSource(private val marker: Unit = Unit) : BattleItemEffect

	/** 指定特性持有者入场时消费道具并强化原始数值最高的能力。 */
	data class HighestStatBoosterActivation(
		val abilityIds: Set<Long>,
	) : BattleItemEffect {
		init {
			require(abilityIds.isNotEmpty() && abilityIds.all { it > 0 }) { "abilityIds must contain positive ids" }
		}
	}

	/** 阻止伤害技能对持有者造成的追加状态、能力变化和强制替换等效果。 */
	data class DamagingSkillSecondaryEffectImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/** 连续成功使用同一技能时，按使用次数逐步提高最终伤害倍率。 */
	data class ConsecutiveSkillDamageBoost(
		val boostPerRepeat: Double = 0.2,
		val maximumBoost: Double = 1.0,
	) : BattleItemEffect {
		init {
			require(boostPerRepeat > 0.0) { "boostPerRepeat must be positive" }
			require(maximumBoost > 0.0) { "maximumBoost must be positive" }
		}
	}

	/** 仅在持有者仍可进化时乘算指定防守能力。 */
	data class EvolvableDefendingStatMultiplier(
		val stats: Set<BattleStat>,
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(stats.isNotEmpty()) { "stats must not be empty" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 阻止对手技能忽略持有者的特性效果。 */
	data class AbilityIgnoreProtection(private val marker: Unit = Unit) : BattleItemEffect

	/** 指定基础形态携带时，在初始状态装配阶段改用目标形态的完整运行时资料。 */
	data class CreatureFormOverride(
		val sourceCreatureId: Long,
		val targetCreatureId: Long,
	) : BattleItemEffect {
		init {
			require(sourceCreatureId > 0 && targetCreatureId > 0) { "creature ids must be positive" }
			require(sourceCreatureId != targetCreatureId) { "source and target creature ids must differ" }
		}
	}

	/** 携带期间按固定倍率修正行动排序使用的速度。 */
	data class SpeedMultiplier(val multiplier: Double) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 指定场地生效且持有者接地时，提升能力阶级并消费携带道具。 */
	data class TerrainActivatedStatStageBoost(
		val terrain: BattleTerrain,
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleItemEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "terrain must not be NONE" }
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 指定全场速度顺序效果成功建立时，改变场上持有者的能力阶级并消费携带道具。 */
	data class FieldSpeedOrderActivatedStatStageChange(
		val kind: BattleFieldSpeedOrderKind,
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleItemEffect {
		init {
			require(stageDelta != 0) { "stageDelta must not be zero" }
		}
	}

	/** 受到指定属性或效果绝佳的技能伤害后，提升一组能力阶级并消费携带道具。 */
	data class ReceivedDamageStatStageBoost(
		val elementId: Long? = null,
		val requiresSuperEffective: Boolean = false,
		val stageChanges: Map<BattleStat, Int>,
	) : BattleItemEffect {
		init {
			require(elementId == null || elementId > 0) { "elementId must be positive when present" }
			require(stageChanges.isNotEmpty()) { "stageChanges must not be empty" }
			require(stageChanges.values.all { it > 0 }) { "stage changes must be positive" }
		}
	}

	/** 技能成功后若满足标签条件，提升使用者能力阶级并消费携带道具。 */
	data class SuccessfulSkillStatStageBoost(
		val requiresSoundBased: Boolean = false,
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleItemEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 技能因命中判定落空后，提升使用者能力阶级并消费携带道具。 */
	data class AccuracyMissStatStageBoost(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleItemEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 对没有自带畏缩效果的伤害技能追加一次畏缩概率判定。 */
	data class AdditionalFlinchChance(val chancePercent: Int) : BattleItemEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between 1 and 100" }
		}
	}

	/** 按有理数倍率提高吸取类技能产生的回复量。 */
	data class DrainHealingMultiplier(val numerator: Int, val denominator: Int) : BattleItemEffect {
		init {
			require(numerator > 0 && denominator > 0) { "drain healing multiplier must be positive" }
		}
	}

	/** 将使用者施加的束缚持续时间固定为指定回合数。 */
	data class BindingDurationOverride(val turns: Int) : BattleItemEffect {
		init {
			require(turns > 0) { "turns must be positive" }
		}
	}

	/** 使用者维持束缚时，以指定最大 HP 分母计算回合末伤害。 */
	data class BindingDamageDenominator(val denominator: Int) : BattleItemEffect {
		init {
			require(denominator > 0) { "denominator must be positive" }
		}
	}

	/** 每回合按指定概率进入同优先度行动的先行档。 */
	data class RandomActionOrderBoost(val chancePercent: Int) : BattleItemEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between 1 and 100" }
		}
	}

	/** 携带期间进入同优先度行动的后行档。 */
	data class ForcedLastActionOrder(private val marker: Unit = Unit) : BattleItemEffect

	/** HP 不高于四分之一时进入先行档并消费携带道具。 */
	data class LowHpActionOrderBoost(
		val triggerHpNumerator: Int = 1,
		val triggerHpDenominator: Int = 4,
	) : BattleItemEffect {
		fun shouldTrigger(currentHp: Int, maxHp: Int): Boolean =
			currentHp > 0 && currentHp.toLong() * triggerHpDenominator <= maxHp.toLong() * triggerHpNumerator
	}

	/** 仅对指定资料形态生效的战斗能力倍率。 */
	data class CreatureStatMultiplier(
		val creatureIds: Set<Long>,
		val stats: Set<BattleStat>,
		val multiplier: Double,
	) : BattleItemEffect {
		init {
			require(creatureIds.isNotEmpty()) { "creatureIds must not be empty" }
			require(stats.isNotEmpty()) { "stats must not be empty" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 仅对指定资料形态和技能属性生效的威力倍率。 */
	data class CreatureElementDamageBoost(
		val creatureIds: Set<Long>,
		val elementIds: Set<Long>,
		val multiplier: Double,
	) : BattleItemEffect

	/** 仅对指定资料形态的全部普通伤害技能生效的威力倍率。 */
	data class CreatureDamageBoost(val creatureIds: Set<Long>, val multiplier: Double) : BattleItemEffect

	/**
	 * 把匹配的随机多段技能实际段数限制到更窄区间。
	 *
	 * 默认表达现代 2..5 段技能被道具收窄到 4..5 段；固定段数或其它原始区间不会受影响。段数仍通过战斗随机源
	 * 选择并进入随机轨迹，避免把“至少四段”错误实现为固定四段。
	 */
	data class MultiHitCountRangeOverride(
		val minHits: Int,
		val maxHits: Int,
		val requiredMinHits: Int = 2,
		val requiredMaxHits: Int = 5,
	) : BattleItemEffect {
		init {
			require(minHits > 0) { "minHits must be positive" }
			require(maxHits >= minHits) { "maxHits must not be less than minHits" }
			require(requiredMinHits > 0) { "requiredMinHits must be positive" }
			require(requiredMaxHits >= requiredMinHits) { "requiredMaxHits must not be less than requiredMinHits" }
		}

		fun matches(skill: BattleSkillSlot): Boolean =
			skill.minHits == requiredMinHits && skill.maxHits == requiredMaxHits
	}

	/**
	 * 携带者使用拳击类技能时，本次技能不再视为接触。
	 *
	 * 该效果用于表达现代规则中拳击手套对“拳击类技能”的动态接触改写。它不修改技能资料里的
	 * [BattleSkillSlot.makesContact] 或 [BattleSkillSlot.punchBased]，因为同一个技能在没有该道具时仍然是接触技能；
	 * 状态机必须在每次技能发动时根据使用者当前携带道具重新计算 [makesEffectiveContact]。
	 *
	 * 动态非接触会同时影响所有读取接触事实的规则：目标侧接触反制特性不会发动、接触类伤害增强/减伤不应触发，
	 * 接触类保护绕过能力也不会生效。拳击类伤害增强仍由其它效果独立表达；本效果只回答“这次有没有接触”。
	 */
	data class PunchBasedContactSuppression(private val marker: Unit = Unit) : BattleItemEffect

	/**
	 * 免疫因主动接触目标而承受的反制副作用。
	 *
	 * 该效果用于表达部位护具类道具。它与 [PunchBasedContactSuppression] 的边界刻意不同：部位护具不会把技能变成
	 * 非接触，使用者仍然可以触发依赖接触事实的正向规则，例如接触类保护绕过能力和接触类伤害倍率；它只阻止目标
	 * 因“被接触”而反过来影响攻击方的副作用，例如接触后让攻击方陷入主要异常状态的防守方特性。
	 *
	 * 该效果不消费道具，也不阻止技能本身的附加效果、目标携带道具减伤、属性免疫或保护 gate。未来若接入凸凸头盔
	 * 等接触反伤道具，应复用同一效果来判断攻击方是否免疫这类接触副作用。
	 */
	data class ContactSideEffectImmunity(private val marker: Unit = Unit) : BattleItemEffect

	/**
	 * 持有者被接触类技能成功命中后，让攻击方按其自身最大 HP 比例受到伤害。
	 *
	 * 该效果用于表达凸凸头盔这类纯接触反伤道具。它和 [ContactSideEffectImmunity] 使用同一套“接触副作用”边界：
	 * 如果攻击方的本次技能没有形成有效接触，或攻击方携带了免疫接触副作用的道具，就不会触发；但它不会改变技能
	 * 本身的接触事实，也不会影响接触类保护绕过、接触类威力倍率等正向规则。
	 *
	 * `damageDenominator` 表示攻击方最大 HP 的分母。道具反伤属于间接伤害，会被攻击方的间接伤害免疫阻止；
	 * 它不消费持有者道具，也不读取目标受到的实际伤害量。复合型道具不要塞进这个效果里：例如附着针会用
	 * [ContactTransferToAttacker] 和 [HeldEndTurnDamage] 分别表达转移与回合末自伤，避免一个效果对象承担多段生命周期。
	 */
	data class ContactDamageToAttacker(
		val damageDenominator: Int,
	) : BattleItemEffect {
		init {
			require(damageDenominator > 0) { "damageDenominator must be positive" }
		}
	}

	/**
	 * 持有者被无携带道具的攻击方接触命中后，将当前携带道具转移给攻击方。
	 *
	 * 该效果用于表达附着针的接触转移部分。它不保存具体 `itemId` 或效果列表，因为被转移的就是持有者当前快照上的
	 * 携带道具；这样数据库只需要声明“这个道具有接触转移规则”，真正转移时仍以运行态为准，避免道具被其它规则
	 * 替换、消费或禁用后还从静态配置里凭空复制旧效果。转移只改变双方携带道具状态，不额外造成伤害，也不消费随机数。
	 */
	data class ContactTransferToAttacker(private val marker: Unit = Unit) : BattleItemEffect

	/**
	 * 修正携带者在体重相关规则中被读取到的当前体重。
	 *
	 * 该效果用于表达非消耗型携带道具让成员体重按固定比例变化的规则。它不改变资料表中的基础体重，也不把道具
	 * 名称暴露给公式层；公式层只读取结构化分数倍率。使用分数而不是浮点数，是为了让 20.1kg 减半后仍作为
	 * 10.05kg 参与阈值比较，而不是被提前截断成 10.0kg。
	 *
	 * 道具是否被拍落、禁用、交换或消费由成员快照上的 [BattleParticipant.itemEffects] 决定；只要该效果仍在
	 * 快照里，就代表当前回合它可以影响体重动态威力。
	 */
	data class WeightMultiplier(
		val numerator: Int,
		val denominator: Int,
	) : BattleItemEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
		}
	}
}
