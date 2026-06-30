package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 单次技能命中流程中的数值修正器。
 *
 * 本类只处理“是否命中”和“本次标准伤害是否被防守方一侧屏障减免”这两类纯计算。它不会推进 [BattleState]、
 * 不写 [io.github.lishangbu.battleengine.model.BattleEvent]，也不会决定技能阶段顺序。主状态机仍负责按现代
 * 主系列规则先处理保护、命中、吸收、伤害和附加效果；这里仅把命中率、命中/闪避阶级和双打屏障倍率这些数学
 * 细节集中起来，避免 [BattleEngine.resolveSkillAgainstTarget] 与伤害段结算函数同时背负过多公式。
 */
internal class BattleHitResolution(
	private val statStageModifiers: BattleStatStageModifiers,
) {
	/**
	 * 处理技能对单个目标的命中判定。
	 *
	 * `accuracy == null` 表示技能在当前天气下必中，直接返回命中并且不消费随机数。若技能会忽略目标特性，
	 * 则目标身上“忽略对手命中阶级变化”的效果也不能影响本次命中；攻击方自身“忽略对手闪避阶级变化”的效果
	 * 不属于目标特性，所以仍按攻击方当前状态判断。修正命中率达到 100 或更高时同样直接命中，避免没有必要的
	 * 随机消费，从而保持 replay 随机脚本稳定。
	 */
	fun accuracyCheck(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		ignoresTargetAbilityEffects: Boolean,
		random: BattleRandom,
	): BattleAccuracyCheck {
		val accuracy = effectiveAccuracy(state, skill) ?: return BattleAccuracyCheck(hit = true, roll = null)
		val actorAccuracyStage = if (!ignoresTargetAbilityEffects && target.ignoresOpponentAccuracyStatStages()) {
			0
		} else {
			actor.statStage(BattleStat.ACCURACY)
		}
		val targetEvasionStage = if (actor.ignoresOpponentAccuracyStatStages()) {
			0
		} else {
			target.statStage(BattleStat.EVASION)
		}
		val modifiedAccuracy = floor(
			accuracy *
				statStageModifiers.accuracyMultiplier(actorAccuracyStage) /
				statStageModifiers.accuracyMultiplier(targetEvasionStage),
		).toInt().coerceAtLeast(1)
		if (modifiedAccuracy >= 100) {
			return BattleAccuracyCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(100, "accuracy for ${skill.skillId}") + 1
		return BattleAccuracyCheck(hit = roll <= modifiedAccuracy, roll = roll)
	}

	/**
	 * 计算防守方一侧屏障对标准伤害的倍率。
	 *
	 * 现代主系列中，一侧屏障只影响物理/特殊标准伤害；变化类技能没有伤害公式，不应进入这里的减免。击中要害
	 * 会忽略防守方一侧屏障。同一侧即使同时存在多个覆盖当前伤害分类的屏障，本次伤害也只套用第一个匹配屏障
	 * 的倍率，不做乘法叠加。双打中目标侧仍有两名可战斗上场成员时倍率约为 2/3；单打或只剩一个上场目标时为
	 * 0.5。
	 */
	fun sideDamageReductionMultiplier(
		state: BattleState,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		criticalHit: Boolean,
	): Double {
		if (criticalHit || skill.damageClass == BattleDamageClass.STATUS) {
			return NO_DAMAGE_REDUCTION_MULTIPLIER
		}
		val targetSide = state.sideOf(target.actorId) ?: return NO_DAMAGE_REDUCTION_MULTIPLIER
		val reduction = targetSide.damageReductions
			.firstOrNull { it.appliesTo(skill.damageClass) }
			?: return NO_DAMAGE_REDUCTION_MULTIPLIER
		return reduction.damageReductionMultiplier(state, targetSide)
	}

	/**
	 * 读取当前天气下的技能命中率。
	 *
	 * 资料层可以显式声明某天气下覆盖为固定命中率，或覆盖为 null 表示必中；没有覆盖时使用技能基础命中率。这个
	 * 查表逻辑放在命中 resolver 内部，是因为调用方只关心最终命中判定，不需要知道命中来源是基础值还是天气覆盖。
	 */
	private fun effectiveAccuracy(state: BattleState, skill: BattleSkillSlot): Int? =
		if (skill.accuracyOverridesByWeather.containsKey(state.environment.weather)) {
			skill.accuracyOverridesByWeather[state.environment.weather]
		} else {
			skill.accuracy
		}

	/**
	 * 根据战斗模式和防守方当前可战斗上场人数，计算一侧屏障倍率。
	 *
	 * 该函数是 [sideDamageReductionMultiplier] 的最后一步，不再重复判断伤害分类、击中要害或屏障是否存在；
	 * 这些分支集中在公开入口里，便于从测试中直接覆盖完整的屏障入口行为。
	 */
	private fun BattleSideDamageReduction.damageReductionMultiplier(
		state: BattleState,
		targetSide: BattleSide,
	): Double {
		val targetSideActiveCount = targetSide.activeParticipants().count { it.canBattle() }
		return if (state.format.mode == BattleMode.DOUBLE && targetSideActiveCount > 1) {
			MULTI_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER
		} else {
			SINGLE_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER
		}
	}

	private companion object {
		/**
		 * 没有屏障、变化类技能、击中要害或找不到目标方时使用的中性倍率。
		 */
		private const val NO_DAMAGE_REDUCTION_MULTIPLIER = 1.0

		/**
		 * 双打中目标侧仍有多个可战斗上场成员时，一侧屏障对标准伤害的现代倍率。
		 */
		private const val MULTI_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 2.0 / 3.0

		/**
		 * 单打或目标侧只剩一个可战斗上场成员时，一侧屏障对标准伤害的现代倍率。
		 */
		private const val SINGLE_TARGET_SIDE_DAMAGE_REDUCTION_MULTIPLIER = 0.5
	}
}

/**
 * 命中判定结果。
 *
 * [roll] 只在真正消费命中随机数时存在；必中、天气必中或修正命中率达到 100 的场景都返回 null。事件层在未命中时
 * 会把 null 兜底成 0，但 resolver 保留 null，可以让测试精确区分“没有掷骰”和“掷出 0”这类不可能结果。
 */
internal data class BattleAccuracyCheck(
	val hit: Boolean,
	val roll: Int?,
)
