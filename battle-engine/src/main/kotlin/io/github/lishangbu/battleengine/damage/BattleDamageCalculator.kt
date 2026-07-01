package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.statStage
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleMajorStatus
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
		val skillElementId = request.skill.effectiveElementId(request.environment.weather)
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
		val sameElementBonus = abilityModifiers.sameElementBonus(request, skillElementId)
		val effectiveness = request.rules.elementChart.multiplier(skillElementId, request.defender.elementIds)
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
	 * 天气球、日光束类技能会在特定天气下改变威力；传统属性强化道具也在威力阶段提供 1.2 倍修正，而不是最终
	 * 伤害阶段。这里把资料层给出的倍率应用在基础威力上并向下取整；取整后至少为 1，避免极端自定义倍率产生
	 * 无效威力。
	 */
	private fun effectivePower(request: BattleDamageRequest): Int {
		val basePower = requireNotNull(request.skill.power) { "damaging skill must define power" }
		val multiplier = (request.skill.powerMultipliersByWeather[request.environment.weather] ?: 1.0) *
			itemModifiers.powerMultiplier(request)
		return floor(basePower * multiplier).toInt().coerceAtLeast(1)
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
