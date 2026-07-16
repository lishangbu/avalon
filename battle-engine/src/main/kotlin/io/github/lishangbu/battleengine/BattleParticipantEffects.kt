package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleWeather

/*
 * 成员运行态的稳定谓词。
 *
 * 这个文件只放“读取当前快照并返回 true/false”的规则判断，不修改 [BattleParticipant]，也不追加
 * [io.github.lishangbu.battleengine.model.BattleEvent]。战斗引擎里很多阶段都会反复询问同一种事实：
 * 成员是否拥有某个属性、是否免疫间接伤害、是否能回复、天气伤害是否会被特性或道具阻止。若这些判断散落
 * 在伤害、入场陷阱、行动前、回合末多个阶段，一旦后续新增效果，很容易只改到其中一处，导致同一条规则在
 * 不同阶段出现不一致。
 *
 * 把它们集中成 internal extension 的原因是：
 * - 它们仍然属于 battle-engine 内部实现细节，不应该成为对外 API。
 * - 调用点读起来仍像领域语言，例如 `participant.canReceiveHealing()`。
 * - 每个函数只依赖传入的快照，因此不会隐藏事件顺序，也不会引入事件总线或规则插件系统。
 *
 * 这里刻意不处理“何时触发事件”“何时消费随机数”“何时结算倒下”。这些都必须留在具体阶段 resolver 中，
 * 因为现代战斗规则最容易出错的地方正是阶段顺序，而不是单个布尔条件本身。
 */
/**
 * 判断成员是否拥有指定属性。
 *
 * `elementId` 允许为 null，是为了让规则快照在缺少某个资料 ID 时可以安全短路。例如隐形岩需要读取岩属性
 * ID 才能计算入场伤害倍率；如果资料层没有提供该 ID，引擎应该保持状态不变，而不是把 null 当成某个特殊
 * 属性或硬编码资料编号。返回 false 能让调用方明确走“无法匹配属性”的分支。
 */
internal fun BattleParticipant.hasElement(elementId: Long?): Boolean =
	elementId != null && elementId in elementIds

/**
 * 判断成员是否免疫非直接攻击造成的 HP 损失。
 *
 * 该谓词对应结构化特性 [BattleAbilityEffect.IndirectDamageImmunity]。它只回答“这次伤害来源是否应该被
 * 间接伤害免疫挡下”，不负责判断伤害来源本身是否属于间接伤害；来源归类必须由调用阶段决定。例如：
 * 入场陷阱、异常状态回合末伤害、天气伤害、束缚伤害、混乱自伤、道具反伤都可以在各自阶段调用这里；
 * 普通技能命中后的 [io.github.lishangbu.battleengine.model.BattleEvent.DamageApplied] 不应调用这里短路。
 */
internal fun BattleParticipant.hasIndirectDamageImmunity(): Boolean =
	abilityEffects.any { it is BattleAbilityEffect.IndirectDamageImmunity }

/**
 * 判断成员是否免疫技能自身的反作用伤害。
 *
 * 这个判断比 [hasIndirectDamageImmunity] 更窄，只用于技能命中后由技能本身造成的 recoil。携带道具反伤、
 * 混乱自伤、天气伤害等仍由其它谓词或阶段判断。这样可以表达“免疫技能反作用，但不免疫所有间接伤害”的现代
 * 规则差异，避免把两个看起来相似的免疫合并成一个过宽条件。
 */
internal fun BattleParticipant.hasSkillRecoilDamageImmunity(): Boolean =
	abilityEffects.any { it is BattleAbilityEffect.SkillRecoilDamageImmunity }

/**
 * 判断成员是否免疫被技能击中要害。
 *
 * 命中要害的随机判定通常已经在伤害阶段消费完成；该谓词只决定随机判定成功后是否真正记为 critical hit。
 * 因此调用点仍应负责随机消费和“无视目标特性”这类前置条件，不能把这些顺序藏进本函数。
 */
internal fun BattleParticipant.hasCriticalHitImmunity(): Boolean =
	abilityEffects.any { it is BattleAbilityEffect.CriticalHitImmunity }

/**
 * 判断成员是否让对手无法读取自己的命中/闪避阶级修正。
 *
 * 该效果参与命中率计算。函数名站在“效果拥有者”的角度描述：拥有该效果的成员会让对手忽略相关阶级变化。
 * 调用方需要根据当前是攻击方还是防守方分别判断，避免在这里混入命中公式上下文。
 */
internal fun BattleParticipant.ignoresOpponentAccuracyStatStages(): Boolean =
	abilityEffects.any { it is BattleAbilityEffect.IgnoreOpponentAccuracyStatStages }

internal fun BattleParticipant.expandedQuarterHpItemThresholdReached(
	triggerHpNumerator: Int,
	triggerHpDenominator: Int,
): Boolean =
	abilityEffects.any { it is BattleAbilityEffect.LowHpItemTriggerThresholdHalf } &&
		triggerHpNumerator * 4 == triggerHpDenominator &&
		currentHp > 0 && currentHp.toLong() * 2 <= maxHp.toLong()

internal fun BattleParticipant.heldBerryEffectMultiplier(): Double =
	if (itemEffects.none { it is io.github.lishangbu.battleengine.model.BattleItemEffect.BerryMarker }) {
		1.0
	} else {
		abilityEffects.filterIsInstance<BattleAbilityEffect.BerryEffectMultiplier>()
			.fold(1.0) { current, effect -> current * effect.multiplier }
	}

/** 返回成员是否拥有实际或由特性模拟的主要异常状态。 */
internal fun BattleParticipant.hasEffectiveMajorStatus(): Boolean =
	majorStatus != null || abilityEffects.any { it is BattleAbilityEffect.AlwaysTreatedAsleep }

/** 判断当前对手场上特性是否阻止指定成员消费树果。 */
internal fun BattleState.berryConsumptionBlocked(actorId: String): Boolean {
	val participant = participant(actorId) ?: return false
	if (participant.itemEffects.none { it is io.github.lishangbu.battleengine.model.BattleItemEffect.BerryMarker }) {
		return false
	}
	val sideId = sideOf(actorId)?.sideId ?: return false
	return sides.filterNot { it.sideId == sideId }
		.flatMap { it.activeParticipants() }
		.any { opponent ->
			opponent.canBattle() && opponent.abilityEffects.any {
				it is BattleAbilityEffect.OpponentBerryConsumptionPrevention
			}
		}
}

/**
 * 判断成员是否处于无法回复 HP 的状态。
 *
 * 目前由回复封锁临时状态表达。这里没有判断当前 HP、濒死与否或具体回复来源，只表达“规则禁止回复”这一项。
 * 这样吸取回复、天气回复、场地回复、道具回复和主动回复技能都可以组合使用同一个谓词。
 */
internal fun BattleParticipant.healingBlocked(): Boolean =
	healBlockTurnsRemaining > 0

/**
 * 判断成员当前是否可以获得正向 HP 回复。
 *
 * 可回复需要同时满足：成员仍可战斗、当前 HP 未满、没有被回复封锁。调用阶段仍要负责计算具体回复量、追加事件、
 * 以及决定该回复属于技能、天气、场地还是道具；本函数只提供所有回复入口共享的前置条件。
 */
internal fun BattleParticipant.canReceiveHealing(): Boolean =
	canBattle() && currentHp < maxHp && !healingBlocked()

/**
 * 成员当前用于体重相关规则的有效体重。
 *
 * 资料表中的体重使用整数刻度保存，常见为十分之一千克；体重减半后可能出现 0.5 个刻度。这里用分数保留精度，
 * 让低踢、打草结的阈值比较和重磅冲撞、高温重压的比例比较都能用交叉相乘完成，不需要把体重转成 Double，
 * 也不需要在公式层决定四舍五入策略。
 */
internal data class BattleEffectiveWeight(
	val numerator: Long,
	val denominator: Long,
) {
	init {
		require(numerator > 0) { "numerator must be positive" }
		require(denominator > 0) { "denominator must be positive" }
	}

	/**
	 * 判断当前有效体重是否小于等于给定资料刻度阈值。
	 */
	fun atMost(weight: Int): Boolean =
		numerator <= weight.toLong() * denominator

	/**
	 * 判断当前有效体重是否至少达到另一成员有效体重的指定整数倍。
	 */
	fun atLeastMultipleOf(other: BattleEffectiveWeight, ratio: Int): Boolean =
		numerator * other.denominator >= other.numerator * denominator * ratio.toLong()
}

/**
 * 计算成员在当前快照中的有效体重。
 *
 * 现代体重顺序是：基础体重先扣除技能造成的临时减轻量，并夹到最低资料刻度 1；之后应用特性倍率；最后应用携带
 * 道具倍率。多个倍率叠乘时仍保留分数精度，最终再保证有效体重不低于 0.1kg 对应的资料刻度 1。
 */
internal fun BattleParticipant.effectiveWeight(): BattleEffectiveWeight {
	var numerator = (weight - weightReduction).coerceAtLeast(1).toLong()
	var denominator = 1L
	abilityEffects.forEach { effect ->
		if (effect is BattleAbilityEffect.WeightMultiplier) {
			numerator *= effect.numerator.toLong()
			denominator *= effect.denominator.toLong()
		}
	}
	itemEffects.forEach { effect ->
		if (effect is BattleItemEffect.WeightMultiplier) {
			numerator *= effect.numerator.toLong()
			denominator *= effect.denominator.toLong()
		}
	}
	if (numerator < denominator) {
		return BattleEffectiveWeight(numerator = 1, denominator = 1)
	}
	return BattleEffectiveWeight(numerator, denominator)
}

/**
 * 判断指定天气是否会对成员造成回合末天气伤害。
 *
 * 该函数只处理“天气伤害是否被阻止”，不计算伤害数值，也不追加天气伤害事件。它合并了三类稳定阻止来源：
 * - 通用的间接伤害免疫。
 * - 特性或道具声明的指定天气伤害免疫。
 * - 天气自己的属性免疫，例如沙暴不伤害岩、地面、钢属性成员。
 *
 * 属性免疫依赖 [BattleState.rules] 中的属性 ID，而不是硬编码资料编号。这样资料层可以替换 ID 来源，战斗
 * 引擎仍只读取已经冻结的规则快照。
 */
internal fun BattleParticipant.immuneToWeatherDamage(state: BattleState, weather: BattleWeather): Boolean =
	hasIndirectDamageImmunity() ||
		weatherDamageBlockedByAbility(weather) ||
		weatherDamageBlockedByItem(weather) ||
		when (weather) {
			BattleWeather.SANDSTORM -> hasElement(state.rules.elementId("rock")) ||
				hasElement(state.rules.elementId("ground")) ||
				hasElement(state.rules.elementId("steel"))
			else -> false
		}

/**
 * 判断特性是否声明了当前天气伤害免疫。
 *
 * 这是 [immuneToWeatherDamage] 的私有组成部分。保持私有是为了避免外部阶段绕过完整判断，只检查特性而漏掉
 * 道具、属性或通用间接伤害免疫。
 */
private fun BattleParticipant.weatherDamageBlockedByAbility(weather: BattleWeather): Boolean =
	abilityEffects.any { effect ->
		effect is BattleAbilityEffect.WeatherDamageImmunity && weather in effect.weathers
	}

/**
 * 判断携带道具是否声明了当前天气伤害免疫。
 *
 * 这是 [immuneToWeatherDamage] 的私有组成部分。道具是否会被消耗、失效或移除不在这里处理；调用本函数时
 * 成员快照中的 [BattleItemEffect] 已经代表当前有效的道具效果。
 */
private fun BattleParticipant.weatherDamageBlockedByItem(weather: BattleWeather): Boolean =
	itemEffects.any { effect ->
		effect is BattleItemEffect.WeatherDamageImmunity && weather in effect.weathers
	}
