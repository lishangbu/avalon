package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import kotlin.math.floor

/**
 * 回合行动排序所需的优先度和有效速度计算。
 *
 * 该组件位于战斗生命周期的“行动排序”阶段，只回答三类问题：技能行动的有效优先度、成员在当前环境下的有效
 * 速度，以及速度比较方向。它不消费随机数、不追加事件、不决定目标，也不执行技能效果；这些仍由
 * [BattleEngine] 的状态机负责。把排序计算从主引擎中拆出后，替换排序、技能排序和入场特性排序可以共享同一套
 * 公式，同时避免主状态机继续膨胀。
 *
 * 输入模型是已经冻结的 [BattleState]、行动者 [BattleParticipant] 和技能槽 [BattleSkillSlot]；输出是纯值
 * [SkillPriorityContext]、有效速度整数或比较器。速度公式的不变量是：先应用速度能力阶级，再应用麻痹减半，
 * 然后按天气、场地、道具和一侧速度修正的倍率相乘，最终向下取整且至少为 1。速度顺序反转只影响同优先度内的
 * 速度比较，不影响优先度本身或同速随机键消费。
 */
internal class BattleActionOrdering(
	private val statStageModifiers: BattleStatStageModifiers,
) {
	/**
	 * 计算行动排序使用的有效速度。
	 *
	 * 天气、场地、道具和一侧场上效果都以结构化规则效果表达；这里不识别资料 ID 或本地化文本。调用方负责在
	 * 同速时消费确定性随机数，本函数只返回排序键。
	 */
	internal fun effectiveSpeed(state: BattleState, participant: BattleParticipant): Int {
		val staged = statStageModifiers.modifiedBattleStat(
			participant.speed,
			participant.statStage(BattleStat.SPEED),
		)
		val afterStatus = if (participant.majorStatus == BattleMajorStatus.PARALYSIS) {
			(staged / 2).coerceAtLeast(1)
		} else {
			staged
		}
		return floor(
			afterStatus *
				weatherSpeedMultiplier(state, participant) *
				terrainSpeedMultiplier(state, participant) *
				itemSpeedMultiplier(participant) *
				sideSpeedModifierMultiplier(state, participant),
		)
			.toInt()
			.coerceAtLeast(1)
	}

	/**
	 * 返回当前环境下行动队列使用的速度比较器。
	 *
	 * 普通环境中高有效速度先行动；速度顺序反转存在时只反转速度比较方向，优先度、锁招续回合和同速随机仍沿用
	 * 原有排序层次。
	 */
	internal fun speedComparator(state: BattleState): Comparator<Int> =
		if (state.environment.fieldSpeedOrderEffect?.kind?.reversesSpeedOrder == true) {
			compareBy<Int> { it }
		} else {
			compareByDescending<Int> { it }
		}

	/**
	 * 计算技能行动的有效优先度和随优先度提升产生的目标免疫标记。
	 *
	 * 变化类先制度特性会同时影响行动排序、精神场地/先制阻挡特性的判断，以及现代规则中恶属性目标对这类
	 * 对手变化技能的免疫。把这些事实集中成上下文，可以保证同一次行动在所有判断点使用同一份结论。
	 */
	internal fun skillPriorityContext(actor: BattleParticipant, skill: BattleSkillSlot): SkillPriorityContext {
		if (skill.damageClass != BattleDamageClass.STATUS) {
			return SkillPriorityContext(effectivePriority = skill.priority)
		}
		val effects = actor.abilityEffects.filterIsInstance<BattleAbilityEffect.StatusSkillPriorityBoost>()
		val priorityDelta = effects.maxOfOrNull { it.priorityDelta } ?: 0
		return SkillPriorityContext(
			effectivePriority = skill.priority + priorityDelta,
			statusPriorityBoostedByAbility = priorityDelta > 0,
			darkElementTargetsImmune = priorityDelta > 0 && effects.any { it.darkElementTargetsImmune },
		)
	}

	private fun weatherSpeedMultiplier(state: BattleState, participant: BattleParticipant): Double =
		participant.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.WeatherSpeedMultiplier ->
					if (state.environment.weather == effect.weather) multiplier * effect.multiplier else multiplier
				else -> multiplier
			}
		}

	private fun terrainSpeedMultiplier(state: BattleState, participant: BattleParticipant): Double =
		participant.abilityEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleAbilityEffect.TerrainSpeedMultiplier ->
					if (state.environment.terrain == effect.terrain) multiplier * effect.multiplier else multiplier
				else -> multiplier
			}
		}

	private fun itemSpeedMultiplier(participant: BattleParticipant): Double =
		participant.itemEffects.fold(1.0) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.ChoiceSkillLock -> multiplier * effect.speedMultiplier
				else -> multiplier
			}
		}

	private fun sideSpeedModifierMultiplier(state: BattleState, participant: BattleParticipant): Double =
		state.sideOf(participant.actorId)
			?.speedModifiers
			?.fold(1.0) { multiplier, modifier -> multiplier * modifier.multiplier }
			?: 1.0
}

/**
 * 单次技能行动的优先度上下文。
 *
 * `effectivePriority` 作为行动排序第一键；后两个字段记录变化技能优先度提升带来的后续规则影响，让精神场地、
 * 先制阻挡和目标属性免疫读取同一份结果，避免在不同阶段重复推导。
 */
internal data class SkillPriorityContext(
	val effectivePriority: Int,
	val statusPriorityBoostedByAbility: Boolean = false,
	val darkElementTargetsImmune: Boolean = false,
)
