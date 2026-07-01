package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.statStage
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import kotlin.math.floor

/**
 * 现代普通伤害公式计算器。
 *
 * 该实现覆盖第一阶段 MVP 需要的基础公式：等级、威力、攻击/防御、随机浮动、击中要害、
 * 范围目标修正、属性一致加成和属性克制。它暂不处理护盾和其它复杂倍率；这些倍率会在后续 hook
 * 管线中以结构化 modifier 继续追加，并由对照测试分别验证。
 *
 * 取整规则按主系列常见公开公式建模：基础伤害部分在整数除法中逐步截断，最终倍率组合后向下取整。
 * 如果属性克制倍率为 0，最终伤害为 0；否则普通命中造成的最小伤害为 1。
 */
class BattleDamageCalculator(
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
) {
	/**
	 * 计算一次物理或特殊技能的普通伤害。
	 *
	 * @throws IllegalArgumentException 当技能为变化技能、缺少威力或防御能力无效时抛出。
	 */
	fun calculate(request: BattleDamageRequest): BattleDamageResult {
		require(request.skill.damageClass != BattleDamageClass.STATUS) { "status skill does not use standard damage formula" }
		val skillElementId = request.skill.effectiveElementId(request.environment.weather)
		val power = effectivePower(request)
		val attackingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> physicalAttackAfterBurn(request)
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.attacker.specialAttack,
				effectiveAttackingStage(request, BattleStat.SPECIAL_ATTACK),
			).let { attackingStatAfterAbility(request, BattleStat.SPECIAL_ATTACK, it) }
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		val defendingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> statStageModifiers.modifiedBattleStat(
				request.defender.defense,
				effectiveDefendingStage(request, BattleStat.DEFENSE),
			)
				.let { physicalDefenseAfterWeather(request, it) }
				.let { defendingStatAfterAbility(request, BattleStat.DEFENSE, it) }
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.defender.specialDefense,
				effectiveDefendingStage(request, BattleStat.SPECIAL_DEFENSE),
			)
				.let { specialDefenseAfterWeather(request, it) }
				.let { defendingStatAfterAbility(request, BattleStat.SPECIAL_DEFENSE, it) }
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		require(defendingStat > 0) { "defending stat must be positive" }

		val levelFactor = (2 * request.attacker.level) / 5 + 2
		val baseDamage = (((levelFactor * power * attackingStat) / defendingStat) / 50) + 2
		val sameElementBonus = sameElementBonus(request, skillElementId)
		val effectiveness = request.rules.elementChart.multiplier(skillElementId, request.defender.elementIds)
		val criticalHitMultiplier = if (request.criticalHit) 1.5 else 1.0
		val weatherMultiplier = weatherDamageMultiplier(request)
		val terrainMultiplier = terrainDamageMultiplier(request)
		val abilityMultiplier = abilityDamageMultiplier(request)
		val itemMultiplier = itemDamageMultiplier(request)
		val combined = baseDamage * request.targetMultiplier * (request.randomPercent / 100.0) * sameElementBonus *
			effectiveness * criticalHitMultiplier * weatherMultiplier * terrainMultiplier * abilityMultiplier * itemMultiplier *
			request.sideDamageReductionMultiplier
		val amount = if (effectiveness == 0.0) 0 else floor(combined).toInt().coerceAtLeast(1)
		return BattleDamageResult(
			amount = amount,
			baseDamage = baseDamage,
			targetMultiplier = request.targetMultiplier,
			sideDamageReductionMultiplier = request.sideDamageReductionMultiplier,
			sameElementBonus = sameElementBonus,
			effectiveness = effectiveness,
			criticalHitMultiplier = criticalHitMultiplier,
			weatherMultiplier = weatherMultiplier,
			terrainMultiplier = terrainMultiplier,
			abilityMultiplier = abilityMultiplier,
			itemMultiplier = itemMultiplier,
		)
	}

	/**
	 * 计算物理攻击侧数值。
	 *
	 * 第一批把灼伤减半作为普通物理伤害的固定修正接入，不处理特性或道具绕过灼伤减半的例外。
	 */
	private fun physicalAttackAfterBurn(request: BattleDamageRequest): Int {
		val stagedAttack = statStageModifiers.modifiedBattleStat(
			request.attacker.attack,
			effectiveAttackingStage(request, BattleStat.ATTACK),
		).let { attackingStatAfterAbility(request, BattleStat.ATTACK, it) }
		return if (
			request.attacker.majorStatus == BattleMajorStatus.BURN &&
			!request.attacker.ignoresBurnAttackReduction(request.environment.terrain, request.environment.weather)
		) {
			(stagedAttack / 2).coerceAtLeast(1)
		} else {
			stagedAttack
		}
	}

	/**
	 * 计算攻击方特性对攻击侧能力值的修正。
	 *
	 * 该修正发生在攻击/特攻能力阶级之后、基础伤害整数除法之前。物理技能还会在该修正之后继续应用灼伤物理伤害
	 * 减半，因此攻击翻倍类规则和普通灼伤规则能按乘法关系共同作用。环境要求不匹配的攻击侧特性保持中性。
	 */
	private fun attackingStatAfterAbility(request: BattleDamageRequest, stat: BattleStat, currentStat: Int): Int {
		val multiplier = request.attacker.abilityEffects.fold(1.0) { currentMultiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.AttackingStatMultiplier ->
					if (effect.matches(stat, request.attacker, request.environment.terrain, request.environment.weather)) {
						currentMultiplier * effect.multiplier
					} else {
						currentMultiplier
					}
				else -> currentMultiplier
			}
		}
		return floor(currentStat * multiplier).toInt().coerceAtLeast(1)
	}

	private fun BattleAbilityEffect.AttackingStatMultiplier.matches(
		stat: BattleStat,
		attacker: BattleParticipant,
		terrain: BattleTerrain,
		weather: BattleWeather,
	): Boolean =
		this.stat == stat &&
			(!requiresMajorStatus || attacker.majorStatus != null) &&
			(requiredTerrain == null || requiredTerrain == terrain) &&
			(requiredWeather == null || requiredWeather == weather)

	private fun BattleParticipant.ignoresBurnAttackReduction(terrain: BattleTerrain, weather: BattleWeather): Boolean =
		abilityEffects
			.filterIsInstance<BattleAbilityEffect.AttackingStatMultiplier>()
			.any { it.ignoresBurnAttackReduction && it.matches(BattleStat.ATTACK, this, terrain, weather) }

	/**
	 * 读取攻击侧在普通伤害公式中使用的有效攻击/特攻阶级。
	 *
	 * 防守方拥有“无视对手伤害公式能力阶级变化”效果时，攻击方的相关阶级固定按 0 处理；这不会删除攻击方
	 * 快照里的实际阶级，只影响当前这次伤害公式。若没有该效果，则继续应用现代击中要害规则：击中要害忽略
	 * 攻击方不利阶级，但保留攻击方有利阶级。
	 */
	private fun effectiveAttackingStage(request: BattleDamageRequest, stat: BattleStat): Int =
		if (!request.ignoreDefenderAbilityEffects && request.defender.ignoresOpponentDamageStatStages()) {
			0
		} else {
			attackingStage(request.attacker.statStage(stat), request.criticalHit)
		}

	/**
	 * 读取防御侧在普通伤害公式中使用的有效防御/特防阶级。
	 *
	 * 攻击方拥有“无视对手伤害公式能力阶级变化”效果时，防守方的相关阶级固定按 0 处理；这保留了防守方
	 * 快照中的实际阶级，便于后续行动和事件继续看到真实状态。若没有该效果，则继续应用现代击中要害规则：
	 * 击中要害忽略防守方有利阶级，但保留防守方不利阶级。
	 */
	private fun effectiveDefendingStage(request: BattleDamageRequest, stat: BattleStat): Int =
		if (request.attacker.ignoresOpponentDamageStatStages()) {
			0
		} else {
			defendingStage(request.defender.statStage(stat), request.criticalHit)
		}

	/**
	 * 计算进入普通伤害公式的有效威力。
	 *
	 * 天气球、日光束类技能会在特定天气下改变威力；传统属性强化道具也在威力阶段提供 1.2 倍修正，而不是最终
	 * 伤害阶段。这里把资料层给出的倍率应用在基础威力上并向下取整；取整后至少为 1，避免极端自定义倍率产生
	 * 无效威力。
	 */
	private fun effectivePower(request: BattleDamageRequest): Int {
		val basePower = requireNotNull(request.skill.power) { "damaging skill must define power" }
		val multiplier = (request.skill.powerMultipliersByWeather[request.environment.weather] ?: 1.0) *
			attackerItemPowerMultiplier(request)
		return floor(basePower * multiplier).toInt().coerceAtLeast(1)
	}

	/**
	 * 计算攻击方携带道具贡献到技能有效威力阶段的倍率。
	 *
	 * 该阶段覆盖传统属性强化道具以及按物理/特殊分类提升威力的道具。它与 [attackerItemDamageMultiplier] 分离，
	 * 是为了遵守现代公开公式中“威力修正”和“最终伤害修正”的不同取整位置。
	 */
	private fun attackerItemPowerMultiplier(request: BattleDamageRequest): Double =
		request.attacker.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.DamageClassPowerBoost -> if (request.skill.damageClass in effect.damageClasses) {
					multiplier * effect.multiplier
				} else {
					multiplier
				}
				is BattleItemEffect.ElementDamageBoost -> if (request.skill.effectiveElementId(request.environment.weather) == effect.elementId) {
					multiplier * effect.multiplier
				} else {
					multiplier
				}
				else -> multiplier
			}
		}

	/**
	 * 计算击中要害时参与攻击侧公式的能力阶级。
	 *
	 * 现代规则下，击中要害会忽略攻击方不利的攻击/特攻阶级，但不会忽略有利阶级，也不会忽略灼伤本身的物理减半。
	 */
	private fun attackingStage(stage: Int, criticalHit: Boolean): Int =
		if (criticalHit && stage < 0) 0 else stage

	/**
	 * 计算击中要害时参与防御侧公式的能力阶级。
	 *
	 * 现代规则下，击中要害会忽略防御方有利的防御/特防阶级，但不会忽略防御方不利阶级。
	 */
	private fun defendingStage(stage: Int, criticalHit: Boolean): Int =
		if (criticalHit && stage > 0) 0 else stage

	/**
	 * 计算天气对防御侧物防的修正。
	 *
	 * 现代雪景会让冰属性成员的物防按 1.5 倍参与普通伤害公式。该修正在能力阶级之后应用；
	 * 如果规则快照缺少冰属性 ID，则不应用该修正，避免引擎猜测资料库编号。
	 */
	private fun physicalDefenseAfterWeather(request: BattleDamageRequest, stagedDefense: Int): Int =
		if (request.environment.weather == BattleWeather.SNOW && request.defender.hasElement(request.rules.iceElementId)) {
			floor(stagedDefense * WEATHER_DEFENSE_BOOST_MULTIPLIER).toInt().coerceAtLeast(1)
		} else {
			stagedDefense
		}

	/**
	 * 计算天气对防御侧特防的修正。
	 *
	 * 现代沙暴会让岩属性成员的特防按 1.5 倍参与普通伤害公式。该修正在能力阶级之后应用；
	 * 如果规则快照缺少岩属性 ID，则不应用该修正。
	 */
	private fun specialDefenseAfterWeather(request: BattleDamageRequest, stagedSpecialDefense: Int): Int =
		if (request.environment.weather == BattleWeather.SANDSTORM && request.defender.hasElement(request.rules.rockElementId)) {
			floor(stagedSpecialDefense * WEATHER_DEFENSE_BOOST_MULTIPLIER).toInt().coerceAtLeast(1)
		} else {
			stagedSpecialDefense
		}

	/**
	 * 计算防守方特性对防御侧能力值的修正。
	 *
	 * 该修正发生在能力阶级和当前天气防御修正之后、基础伤害整数除法之前。这样物防翻倍或特防翻倍类规则会改变
	 * `baseDamage`，而不是表现成最终伤害倍率。若本次技能无视目标特性，或特性要求的场地不匹配，则保持当前
	 * 防御侧能力值不变。
	 */
	private fun defendingStatAfterAbility(request: BattleDamageRequest, stat: BattleStat, currentStat: Int): Int {
		if (request.ignoreDefenderAbilityEffects) {
			return currentStat
		}
		val multiplier = request.defender.abilityEffects.fold(1.0) { currentMultiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.DefendingStatMultiplier ->
					if (effect.matches(stat, request.environment.terrain)) {
						currentMultiplier * effect.multiplier
					} else {
						currentMultiplier
					}
				else -> currentMultiplier
			}
		}
		return floor(currentStat * multiplier).toInt().coerceAtLeast(1)
	}

	private fun BattleAbilityEffect.DefendingStatMultiplier.matches(stat: BattleStat, terrain: BattleTerrain): Boolean =
		this.stat == stat && (requiredTerrain == null || requiredTerrain == terrain)

	/**
	 * 计算普通伤害公式中的属性一致加成。
	 *
	 * 该倍率只在技能当前有效属性属于使用者属性集合时生效。默认现代倍率为 1.5；攻击方若拥有属性一致加成覆盖特性，
	 * 则使用结构化效果给出的倍率。该阶段位于基础伤害之后、最终倍率叠乘链之前，返回值会写入结果用于测试用例
	 * 直接断言，避免把 STAB 覆盖误混到泛用特性最终倍率。
	 */
	private fun sameElementBonus(request: BattleDamageRequest, skillElementId: Long): Double {
		if (skillElementId !in request.attacker.elementIds) {
			return 1.0
		}
		return request.attacker.abilityEffects
			.filterIsInstance<BattleAbilityEffect.SameElementBonusOverride>()
			.firstOrNull()
			?.multiplier
			?: DEFAULT_SAME_ELEMENT_BONUS
	}

	/**
	 * 计算天气对火/水属性普通伤害的倍率。
	 *
	 * 元素 ID 来自规则快照，避免引擎硬编码资料库编号。若快照缺少对应元素 ID，天气不会修改伤害。
	 * 第一批实现晴天和下雨对火/水伤害的互相增强/削弱；天气对防御侧能力的修正在独立函数中处理。
	 */
	private fun weatherDamageMultiplier(request: BattleDamageRequest): Double =
		when (request.environment.weather) {
			BattleWeather.SUN -> when (request.skill.effectiveElementId(request.environment.weather)) {
				request.rules.fireElementId -> 1.5
				request.rules.waterElementId -> 0.5
				else -> 1.0
			}
			BattleWeather.RAIN -> when (request.skill.effectiveElementId(request.environment.weather)) {
				request.rules.waterElementId -> 1.5
				request.rules.fireElementId -> 0.5
				else -> 1.0
			}
			BattleWeather.NONE,
			BattleWeather.SANDSTORM,
			BattleWeather.SNOW -> 1.0
		}

	/**
	 * 计算场地对普通伤害的倍率。
	 *
	 * 现代青草场地有两个伤害侧效果：接地成员使用草属性技能时伤害按 1.3 倍计算；地震、重踏、震级等带有
	 * 明确震动标签的技能命中接地目标时伤害减半。其它场地的状态免疫、先制阻挡和回合末回复不属于伤害公式。
	 */
	private fun terrainDamageMultiplier(request: BattleDamageRequest): Double =
		when (request.environment.terrain) {
			BattleTerrain.GRASSY -> {
				val grassBoost = if (
					request.attacker.grounded &&
					request.rules.grassElementId != null &&
					request.skill.effectiveElementId(request.environment.weather) == request.rules.grassElementId
				) {
					GRASSY_TERRAIN_GRASS_DAMAGE_MULTIPLIER
				} else {
					1.0
				}
				val groundMoveReduction = if (request.defender.grounded && request.skill.weakenedByGrassyTerrain) {
					GRASSY_TERRAIN_GROUND_MOVE_MULTIPLIER
				} else {
					1.0
				}
				grassBoost * groundMoveReduction
			}
			BattleTerrain.NONE,
			BattleTerrain.ELECTRIC,
			BattleTerrain.MISTY,
			BattleTerrain.PSYCHIC -> 1.0
		}

	/**
	 * 计算攻击方特性带来的伤害倍率。
	 *
	 * 当前支持固定属性增伤、低体力指定属性增伤、天气下指定属性增伤，以及拳击类、切割类、接触类、声音类技能
	 * 标签触发的稳定增伤；防守方声音类技能减伤、效果绝佳减伤、满 HP 减伤和指定伤害分类减伤也会合并到该倍率。
	 * 触发条件都来自运行时快照中的结构化字段，避免伤害公式读取技能名、特性名或本地化文本。
	 */
	private fun abilityDamageMultiplier(request: BattleDamageRequest): Double =
		attackerAbilityDamageMultiplier(request) * defenderAbilityDamageMultiplier(request)

	private fun attackerAbilityDamageMultiplier(request: BattleDamageRequest): Double =
		request.attacker.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.ElementSkillDamageBoost ->
					if (request.skill.effectiveElementId(request.environment.weather) in effect.elementIds) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				is BattleAbilityEffect.LowHpElementDamageBoost -> {
					val hpAtOrBelowThreshold =
						request.attacker.currentHp * effect.hpThresholdDenominator <=
							request.attacker.maxHp * effect.hpThresholdNumerator
					if (hpAtOrBelowThreshold && request.skill.effectiveElementId(request.environment.weather) == effect.elementId) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				}
				is BattleAbilityEffect.WeatherElementDamageBoost ->
					if (
						request.environment.weather == effect.weather &&
						request.skill.effectiveElementId(request.environment.weather) in effect.elementIds
					) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				is BattleAbilityEffect.PunchBasedSkillDamageBoost ->
					if (request.skill.punchBased) multiplier * effect.multiplier else multiplier
				is BattleAbilityEffect.SlicingBasedSkillDamageBoost ->
					if (request.skill.slicingBased) multiplier * effect.multiplier else multiplier
				is BattleAbilityEffect.ContactBasedSkillDamageBoost ->
					if (request.skill.makesContact) multiplier * effect.multiplier else multiplier
				is BattleAbilityEffect.SoundBasedSkillDamageBoost ->
					if (request.skill.soundBased) multiplier * effect.multiplier else multiplier
				else -> multiplier
			}
		}

	/**
	 * 计算防守方特性带来的普通伤害倍率。
	 *
	 * 目前支持声音类技能伤害减免、效果绝佳伤害减免、满 HP 伤害减免，以及按物理/特殊分类触发的伤害减免。
	 * 若本次伤害请求已经标记为忽略防守方特性，所有防守方特性倍率都保持中性。
	 */
	private fun defenderAbilityDamageMultiplier(request: BattleDamageRequest): Double =
		if (request.ignoreDefenderAbilityEffects) {
			1.0
		} else {
			val skillElementId = request.skill.effectiveElementId(request.environment.weather)
			val effectiveness = request.rules.elementChart.multiplier(skillElementId, request.defender.elementIds)
			request.defender.abilityEffects.fold(1.0) { multiplier, effect ->
				when (effect) {
					is BattleAbilityEffect.SoundBasedSkillDamageReduction ->
						if (request.skill.soundBased) multiplier * effect.multiplier else multiplier
					is BattleAbilityEffect.SuperEffectiveDamageReduction ->
						if (effectiveness > 1.0) multiplier * effect.multiplier else multiplier
					is BattleAbilityEffect.FullHpDamageReduction ->
						if (request.defender.currentHp >= request.defender.maxHp) multiplier * effect.multiplier else multiplier
					is BattleAbilityEffect.DamageClassDamageReduction ->
						if (request.skill.damageClass in effect.damageClasses) multiplier * effect.multiplier else multiplier
					else -> multiplier
				}
			}
		}

	/**
	 * 计算携带道具带来的最终伤害倍率。
	 *
	 * 这里只读取“伤害公式中的稳定倍率”。攻击方增伤道具和防守方抗性道具都会进入同一最终倍率链；带反伤的增伤
	 * 道具只在此处贡献倍率，反伤本身由状态机在伤害事件之后处理。防守方一次性减伤道具是否消费，同样由状态机
	 * 根据同一结构化效果在伤害写入前处理，计算器保持纯函数。
	 */
	private fun itemDamageMultiplier(request: BattleDamageRequest): Double =
		attackerItemDamageMultiplier(request) * defenderItemDamageMultiplier(request)

	/**
	 * 计算攻击方携带道具贡献的伤害倍率。
	 */
	private fun attackerItemDamageMultiplier(request: BattleDamageRequest): Double =
		request.attacker.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.DamageBoostWithRecoil -> multiplier * effect.multiplier
				is BattleItemEffect.SuperEffectiveDamageBoost -> if (
					request.rules.elementChart.multiplier(
						request.skill.effectiveElementId(request.environment.weather),
						request.defender.elementIds,
					) > 1.0
				) {
					multiplier * effect.multiplier
				} else {
					multiplier
				}
				else -> multiplier
			}
		}

	/**
	 * 计算防守方携带道具贡献的伤害倍率。
	 *
	 * 目前只支持本体受击时触发的指定属性减伤道具。替身挡住本体时，外层状态机会把
	 * `allowDefenderItemDamageReduction` 置为 false，因此本函数即使看到防守方携带抗性道具也会保持中性。
	 */
	private fun defenderItemDamageMultiplier(request: BattleDamageRequest): Double =
		if (!request.allowDefenderItemDamageReduction) {
			1.0
		} else {
			val skillElementId = request.skill.effectiveElementId(request.environment.weather)
			val effectiveness = request.rules.elementChart.multiplier(skillElementId, request.defender.elementIds)
			request.defender.itemEffects.fold(1.0) { multiplier, effect ->
				when (effect) {
					is BattleItemEffect.ElementDamageReduction -> if (effect.matches(skillElementId, effectiveness)) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
					else -> multiplier
				}
			}
		}

	private fun BattleParticipant.hasElement(elementId: Long?): Boolean =
		elementId != null && elementId in elementIds

	/**
	 * 判断成员是否在普通伤害公式中忽略对手相关能力阶级。
	 *
	 * 计算器只关心结构化效果，不关心数据库特性名称；这样同类规则资料可以共享同一个公式行为。
	 */
	private fun BattleParticipant.ignoresOpponentDamageStatStages(): Boolean =
		abilityEffects.any { it is BattleAbilityEffect.IgnoreOpponentDamageStatStages }

	private companion object {
		private const val DEFAULT_SAME_ELEMENT_BONUS = 1.5
		private const val GRASSY_TERRAIN_GRASS_DAMAGE_MULTIPLIER = 1.3
		private const val GRASSY_TERRAIN_GROUND_MOVE_MULTIPLIER = 0.5
		private const val WEATHER_DEFENSE_BOOST_MULTIPLIER = 1.5
	}
}
