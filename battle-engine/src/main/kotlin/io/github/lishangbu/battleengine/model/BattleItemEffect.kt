package io.github.lishangbu.battleengine.model

/**
 * 携带道具在战斗中的可执行效果。
 *
 * 第一批覆盖几类常见 hook：造成伤害时提升倍率并按最大 HP 比例反伤、回合末按最大 HP 比例回复、天气伤害免疫、
 * 环境持续回合延长、低体力一次性回复、蓄力技能一次性跳过等待，以及稳定状态免疫。
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
