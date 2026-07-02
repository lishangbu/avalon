package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import kotlin.math.floor

/**
 * 普通伤害公式中的特性修正集合。
 *
 * 伤害公式里有三类特性影响容易混在一起：能力值阶段修正、属性一致加成覆盖、最终伤害倍率。它们虽然都来自
 * 成员的结构化特性效果，但取整位置完全不同：能力值修正会进入基础伤害整数公式，属性一致加成位于最终倍率链，
 * 泛用增伤/减伤也位于最终倍率链。把这些判断集中到本类，可以让 [BattleDamageCalculator] 继续像公式本身一样
 * 自上而下阅读，同时避免计算器直接堆叠所有特性分支。
 *
 * 本类仍然保持纯函数风格：不消费道具、不追加事件、不修改成员状态。会产生事件或持久状态变化的特性效果应该
 * 留在战斗状态机里处理，避免“公式计算”同时承担“战斗流程”的副作用。
 */
internal class BattleDamageAbilityModifiers {
	/**
	 * 计算攻击方特性对攻击侧能力值的修正。
	 *
	 * 该修正发生在攻击/特攻能力阶级之后、基础伤害整数除法之前。物理技能还会在该修正之后继续应用灼伤物理伤害
	 * 减半，因此攻击翻倍类规则和普通灼伤规则能按乘法关系共同作用。环境要求不匹配的攻击侧特性保持中性。
	 */
	fun attackingStatAfterAbility(request: BattleDamageRequest, stat: BattleStat, currentStat: Int): Int {
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

	/**
	 * 计算防守方特性对防御侧能力值的修正。
	 *
	 * 该修正发生在能力阶级和当前天气防御修正之后、基础伤害整数除法之前。这样物防翻倍或特防翻倍类规则会改变
	 * `baseDamage`，而不是表现成最终伤害倍率。若本次技能无视目标特性，或特性要求的场地不匹配，则保持当前
	 * 防御侧能力值不变。
	 */
	fun defendingStatAfterAbility(request: BattleDamageRequest, stat: BattleStat, currentStat: Int): Int {
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

	/**
	 * 计算普通伤害公式中的属性一致加成。
	 *
	 * 该倍率只在技能当前有效属性属于使用者属性集合时生效。默认现代倍率为 1.5；攻击方若拥有属性一致加成覆盖特性，
	 * 则使用结构化效果给出的倍率。该阶段位于基础伤害之后、最终倍率叠乘链之前，返回值会写入结果用于测试用例
	 * 直接断言，避免把 STAB 覆盖误混到泛用特性最终倍率。
	 */
	fun sameElementBonus(request: BattleDamageRequest, skillElementId: Long): Double {
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
	 * 计算攻击方和防守方特性共同带来的最终伤害倍率。
	 *
	 * 当前支持固定属性增伤、低体力指定属性增伤、天气下指定属性增伤，以及拳击类、切割类、接触类、声音类技能
	 * 标签触发的稳定增伤；防守方声音类技能减伤、效果绝佳减伤、满 HP 减伤和指定伤害分类减伤也会合并到该倍率。
	 * 触发条件都来自运行时快照中的结构化字段，避免伤害公式读取技能名、特性名或本地化文本。
	 */
	fun damageMultiplier(request: BattleDamageRequest): Double =
		attackerDamageMultiplier(request) * defenderDamageMultiplier(request)

	/**
	 * 判断成员是否通过攻击侧能力值修正特性绕过灼伤物理攻击减半。
	 *
	 * 绕过灼伤减半和普通攻击值倍率共用同一组结构化效果；这里复用相同的天气/场地/异常条件判断，确保“倍率生效”
	 * 与“灼伤例外生效”不会因为两个独立分支而漂移。
	 */
	fun ignoresBurnAttackReduction(participant: BattleParticipant, terrain: BattleTerrain, weather: BattleWeather): Boolean =
		participant.abilityEffects
			.filterIsInstance<BattleAbilityEffect.AttackingStatMultiplier>()
			.any { it.ignoresBurnAttackReduction && it.matches(BattleStat.ATTACK, participant, terrain, weather) }

	/**
	 * 判断成员是否在普通伤害公式中忽略对手相关能力阶级。
	 *
	 * 计算器只关心结构化效果，不关心数据库特性名称；这样同类规则资料可以共享同一个公式行为。
	 */
	fun ignoresOpponentDamageStatStages(participant: BattleParticipant): Boolean =
		participant.abilityEffects.any { it is BattleAbilityEffect.IgnoreOpponentDamageStatStages }

	private fun attackerDamageMultiplier(request: BattleDamageRequest): Double =
		request.attacker.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.ElementSkillDamageBoost ->
					if (request.skill.effectiveElementId(request.environment.weather, request.environment.terrain) in effect.elementIds) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				is BattleAbilityEffect.LowHpElementDamageBoost -> {
					val hpAtOrBelowThreshold =
						request.attacker.currentHp * effect.hpThresholdDenominator <=
							request.attacker.maxHp * effect.hpThresholdNumerator
					if (
						hpAtOrBelowThreshold &&
						request.skill.effectiveElementId(request.environment.weather, request.environment.terrain) == effect.elementId
					) {
						multiplier * effect.multiplier
					} else {
						multiplier
					}
				}
				is BattleAbilityEffect.WeatherElementDamageBoost ->
					if (
						request.environment.weather == effect.weather &&
						request.skill.effectiveElementId(request.environment.weather, request.environment.terrain) in effect.elementIds
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
	private fun defenderDamageMultiplier(request: BattleDamageRequest): Double =
		if (request.ignoreDefenderAbilityEffects) {
			1.0
		} else {
			val skillElementId = request.skill.effectiveElementId(request.environment.weather, request.environment.terrain)
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

	private fun BattleAbilityEffect.DefendingStatMultiplier.matches(stat: BattleStat, terrain: BattleTerrain): Boolean =
		this.stat == stat && (requiredTerrain == null || requiredTerrain == terrain)

	private companion object {
		private const val DEFAULT_SAME_ELEMENT_BONUS = 1.5
	}
}
