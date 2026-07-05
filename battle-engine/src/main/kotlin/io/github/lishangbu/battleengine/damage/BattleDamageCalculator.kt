package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.effectiveWeight
import io.github.lishangbu.battleengine.statStage
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillDynamicPower
import io.github.lishangbu.battleengine.model.BattleSkillPowerMultiplier
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import kotlin.math.floor

/**
 * 现代普通伤害公式计算器。
 *
 * 该实现只负责“已经通过命中、保护、替身和免疫 gate 之后”的标准数值计算：等级、有效威力、攻击/防御、
 * 随机浮动、击中要害、范围目标修正、属性一致加成、属性克制、天气/场地、特性、道具和一侧减伤倍率都会在
 * 这里按固定顺序组合。会追加事件、消费道具、修改 HP 或改变战斗状态的规则仍留在外层结算器中处理，避免一个
 * 纯公式类同时承担战斗流程副作用。
 *
 * 取整规则按主系列常见公开公式建模：基础伤害部分在整数除法中逐步截断，最终倍率组合后向下取整。
 * 如果属性克制倍率为 0，最终伤害为 0；否则普通命中造成的最小伤害为 1。
 */
class BattleDamageCalculator(
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
) {
	private val abilityModifiers = BattleDamageAbilityModifiers()
	private val environmentModifiers = BattleDamageEnvironmentModifiers()
	private val itemModifiers = BattleDamageItemModifiers()

	/**
	 * 计算一次物理或特殊技能的普通伤害。
	 *
	 * @throws IllegalArgumentException 当技能为变化技能、缺少威力或防御能力无效时抛出。
	 */
	fun calculate(request: BattleDamageRequest): BattleDamageResult {
		require(request.skill.damageClass != BattleDamageClass.STATUS) { "status skill does not use standard damage formula" }
		val power = effectivePower(request)
		val attackingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> physicalAttackAfterBurn(request)
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.attacker.specialAttack,
				effectiveAttackingStage(request, BattleStat.SPECIAL_ATTACK),
			).let { abilityModifiers.attackingStatAfterAbility(request, BattleStat.SPECIAL_ATTACK, it) }
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		val defendingStat = when (request.skill.damageClass) {
			BattleDamageClass.PHYSICAL -> statStageModifiers.modifiedBattleStat(
				request.defender.defense,
				effectiveDefendingStage(request, BattleStat.DEFENSE),
			)
				.let { environmentModifiers.physicalDefenseAfterWeather(request, it) }
				.let { abilityModifiers.defendingStatAfterAbility(request, BattleStat.DEFENSE, it) }
			BattleDamageClass.SPECIAL -> statStageModifiers.modifiedBattleStat(
				request.defender.specialDefense,
				effectiveDefendingStage(request, BattleStat.SPECIAL_DEFENSE),
			)
				.let { environmentModifiers.specialDefenseAfterWeather(request, it) }
				.let { abilityModifiers.defendingStatAfterAbility(request, BattleStat.SPECIAL_DEFENSE, it) }
			BattleDamageClass.STATUS -> error("status skill does not use standard damage formula")
		}
		require(defendingStat > 0) { "defending stat must be positive" }

		val levelFactor = (2 * request.attacker.level) / 5 + 2
		val baseDamage = (((levelFactor * power * attackingStat) / defendingStat) / 50) + 2
		val sameElementBonus = if (request.skill.typelessDamage) {
			1.0
		} else {
			abilityModifiers.sameElementBonus(request, request.skillElementId)
		}
		val effectiveness = request.typeEffectiveness
		val criticalHitMultiplier = if (request.criticalHit) 1.5 else 1.0
		val weatherMultiplier = environmentModifiers.weatherDamageMultiplier(request)
		val terrainMultiplier = environmentModifiers.terrainDamageMultiplier(request)
		val abilityMultiplier = abilityModifiers.damageMultiplier(request)
		val itemMultiplier = itemModifiers.damageMultiplier(request)
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
	 * 灼伤减半发生在攻击方能力阶级和攻击侧特性倍率之后。若攻击方的结构化特性效果明确声明可绕过灼伤减半，
	 * 这里会跳过该修正；这样“攻击值倍率是否生效”和“灼伤例外是否生效”共享同一组天气、场地和状态条件，
	 * 不需要在公式中识别具体特性名称。
	 */
	private fun physicalAttackAfterBurn(request: BattleDamageRequest): Int {
		val stagedAttack = statStageModifiers.modifiedBattleStat(
			request.attacker.attack,
			effectiveAttackingStage(request, BattleStat.ATTACK),
		).let { abilityModifiers.attackingStatAfterAbility(request, BattleStat.ATTACK, it) }
		return if (
			request.attacker.majorStatus == BattleMajorStatus.BURN &&
			!request.skill.ignoresUserBurnAttackReduction &&
			!abilityModifiers.ignoresBurnAttackReduction(request.attacker, request.environment.terrain, request.environment.weather)
		) {
			(stagedAttack / 2).coerceAtLeast(1)
		} else {
			stagedAttack
		}
	}

	/**
	 * 读取攻击侧在普通伤害公式中使用的有效攻击/特攻阶级。
	 *
	 * 防守方拥有“无视对手伤害公式能力阶级变化”效果时，攻击方的相关阶级固定按 0 处理；这不会删除攻击方
	 * 快照里的实际阶级，只影响当前这次伤害公式。若没有该效果，则继续应用现代击中要害规则：击中要害忽略
	 * 攻击方不利阶级，但保留攻击方有利阶级。
	 */
	private fun effectiveAttackingStage(request: BattleDamageRequest, stat: BattleStat): Int =
		if (!request.ignoreDefenderAbilityEffects && abilityModifiers.ignoresOpponentDamageStatStages(request.defender)) {
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
		if (abilityModifiers.ignoresOpponentDamageStatStages(request.attacker)) {
			0
		} else {
			defendingStage(request.defender.statStage(stat), request.criticalHit)
		}

	/**
	 * 计算进入普通伤害公式的有效威力。
	 *
	 * 动态威力技能会先从当前战斗快照推导本次基础威力；普通技能则读取技能表里的固定威力。随后天气球、日光束类
	 * 技能的天气倍率、条件威力倍率和传统属性强化道具倍率继续在“威力”阶段相乘，而不是放到最终伤害倍率阶段。
	 * 这里对倍率结果向下取整；取整后至少为 1，避免极端自定义倍率产生无效威力。
	 */
	private fun effectivePower(request: BattleDamageRequest): Int {
		val basePower = dynamicPower(request) ?: requireNotNull(request.skill.power) { "damaging skill must define power" }
		val groundedTerrainMultiplier = if (request.attacker.grounded) {
			request.skill.groundedPowerMultipliersByTerrain[request.environment.terrain] ?: 1.0
		} else {
			1.0
		}
		val multiplier = (request.skill.powerMultipliersByWeather[request.environment.weather] ?: 1.0) *
			groundedTerrainMultiplier *
			conditionalPowerMultiplier(request) *
			itemModifiers.powerMultiplier(request)
		return floor(basePower * multiplier).toInt().coerceAtLeast(1)
	}

	/**
	 * 根据当前战斗快照计算动态基础威力。
	 *
	 * 这个分支只返回“基础威力”，不直接计算伤害，也不消费随机数或追加事件。它读取的是快照中的真实能力阶级，
	 * 不受击中要害对攻防阶级的忽略规则影响；原因是辅助力量、嚣张、惩罚这类技能的公开规则描述的是技能威力
	 * 如何随能力提升变化，而不是普通伤害公式里的有效攻防值如何变化。
	 */
	private fun dynamicPower(request: BattleDamageRequest): Int? =
		when (val rule = request.skill.dynamicPower) {
			null -> null
			is BattleSkillDynamicPower.PositiveStatStageSum -> {
				val source = when (rule.source) {
					BattleEffectTarget.USER -> request.attacker
					BattleEffectTarget.TARGET -> request.defender
				}
				val positiveStageSum = BattleStat.entries.sumOf { source.statStage(it).coerceAtLeast(0) }
				val rawPower = rule.basePower + rule.powerPerPositiveStage * positiveStageSum
				rule.maxPower?.let { rawPower.coerceAtMost(it) } ?: rawPower
			}
			is BattleSkillDynamicPower.UserSpeedRatioThresholds -> {
				val attackerSpeed = requireNotNull(request.attackerEffectiveSpeed) {
					"attackerEffectiveSpeed is required for speed ratio power"
				}
				val defenderSpeed = requireNotNull(request.defenderEffectiveSpeed) {
					"defenderEffectiveSpeed is required for speed ratio power"
				}
				rule.thresholds
					.firstOrNull { threshold -> attackerSpeed.toLong() >= defenderSpeed.toLong() * threshold.minimumRatio }
					?.power
					?: rule.fallbackPower
			}
			is BattleSkillDynamicPower.TargetToUserSpeedRatio -> {
				val attackerSpeed = requireNotNull(request.attackerEffectiveSpeed) {
					"attackerEffectiveSpeed is required for target-to-user speed ratio power"
				}
				val defenderSpeed = requireNotNull(request.defenderEffectiveSpeed) {
					"defenderEffectiveSpeed is required for target-to-user speed ratio power"
				}
				(((rule.multiplier.toLong() * defenderSpeed) / attackerSpeed) + rule.additivePower)
					.coerceAtMost(rule.maxPower.toLong())
					.toInt()
			}
			is BattleSkillDynamicPower.TargetWeightThresholds -> {
				val defenderWeight = request.defender.effectiveWeight()
				rule.thresholds
					.firstOrNull { threshold -> defenderWeight.atMost(threshold.maxWeightInclusive) }
					?.power
					?: rule.fallbackPower
			}
			is BattleSkillDynamicPower.UserTargetWeightRatioThresholds -> {
				val attackerWeight = request.attacker.effectiveWeight()
				val defenderWeight = request.defender.effectiveWeight()
				rule.thresholds
					.firstOrNull { threshold ->
						attackerWeight.atLeastMultipleOf(defenderWeight, threshold.minimumUserToTargetRatio)
					}
					?.power
					?: rule.fallbackPower
			}
		}

	/**
	 * 计算技能自身声明的条件威力倍率。
	 *
	 * 这些倍率发生在普通伤害公式的“威力”阶段，而不是最终伤害倍率阶段。多个条件若未来同时存在会相乘；当前资料
	 * 只使用单一条件，但 fold 形式能让组合规则保持可预测，不需要为每个组合新增一个特殊 policy。
	 */
	private fun conditionalPowerMultiplier(request: BattleDamageRequest): Double =
		request.skill.conditionalPowerMultipliers.fold(1.0) { multiplier, rule ->
			multiplier * if (rule.appliesTo(request)) rule.multiplier else 1.0
		}

	private fun BattleSkillPowerMultiplier.appliesTo(request: BattleDamageRequest): Boolean =
		when (this) {
			is BattleSkillPowerMultiplier.UserMajorStatus -> request.attacker.majorStatus in statuses
			is BattleSkillPowerMultiplier.TargetMajorStatus -> request.defender.majorStatus in statuses
			is BattleSkillPowerMultiplier.TargetCurrentHpAtMostFraction ->
				request.defender.currentHp.toLong() * denominator <= request.defender.maxHp.toLong() * numerator
			is BattleSkillPowerMultiplier.UserHasNoHeldItem -> request.attacker.itemId == null
			is BattleSkillPowerMultiplier.ActiveTerrain -> request.environment.terrain == terrain
			is BattleSkillPowerMultiplier.TargetGroundedTerrain ->
				request.environment.terrain == terrain && request.defender.grounded
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

}
