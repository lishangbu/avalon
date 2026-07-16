package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
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
	 * 一击必杀类技能使用独立公式：基础命中率加上使用者与目标的等级差，并且不读取命中/闪避阶级；目标等级
	 * 高于使用者的失败条件已经由命中前 gate 写成 [io.github.lishangbu.battleengine.model.BattleEvent.SkillFailed]。
	 * 普通技能中，`accuracy == null` 表示技能在当前天气下必中，直接返回命中并且不消费随机数。若技能会忽略目标
	 * 特性，则目标身上“忽略对手命中阶级变化”的效果也不能影响本次命中；攻击方自身“忽略对手闪避阶级变化”的效果
	 * 不属于目标特性，所以仍按攻击方当前状态判断。修正命中率达到 100 或更高时同样直接命中，避免没有必要的
	 * 随机消费，从而保持 replay 随机脚本稳定。
	 */
	fun accuracyCheck(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		targetAlreadyActed: Boolean,
		ignoresTargetAbilityEffects: Boolean,
		random: BattleRandom,
	): BattleAccuracyCheck {
		if (actor.hasAccuracyLockOn(target.actorId)) {
			return BattleAccuracyCheck(hit = true, roll = null)
		}
		val oneHitKnockOutAccuracy = oneHitKnockOutAccuracy(state, actor, target, skill)
		if (oneHitKnockOutAccuracy != null) {
			return rollAccuracy(skill, oneHitKnockOutAccuracy, random)
		}
		val accuracy = effectiveAccuracy(state, actor, skill) ?: return BattleAccuracyCheck(hit = true, roll = null)
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
		if (
			actor.abilityEffects.any { it is BattleAbilityEffect.AlwaysHit } ||
			target.abilityEffects.any { it is BattleAbilityEffect.AlwaysHit }
		) {
			return BattleAccuracyCheck(hit = true, roll = null)
		}
		val actorItemMultiplier = actor.itemEffects.fold(actor.nextSkillAccuracyMultiplier) { multiplier, effect ->
			when (effect) {
				is BattleItemEffect.AccuracyMultiplier -> multiplier * effect.multiplier
				is BattleItemEffect.AccuracyMultiplierAfterTargetActed ->
					if (targetAlreadyActed) multiplier * effect.multiplier else multiplier
				else -> multiplier
			}
		}
		val actorAbilityMultiplier = actor.abilityEffects.fold(1.0) { multiplier, effect ->
			if (
				effect is BattleAbilityEffect.AccuracyMultiplier &&
				(effect.damageClasses.isEmpty() || skill.damageClass in effect.damageClasses)
			) {
				multiplier * effect.multiplier
			} else {
				multiplier
			}
		}
		val targetItemMultiplier = target.itemEffects.fold(1.0) { multiplier, effect ->
			if (effect is BattleItemEffect.OpponentAccuracyMultiplier) multiplier * effect.multiplier else multiplier
		}
		val targetAbilityMultiplier = if (ignoresTargetAbilityEffects) 1.0 else {
			target.abilityEffects.fold(1.0) { multiplier, effect ->
				if (
					effect is BattleAbilityEffect.OpponentAccuracyMultiplier &&
					(effect.requiredWeather == null || effect.requiredWeather == state.environment.weather) &&
					(!effect.requiresConfusion || target.confusionTurnsRemaining > 0)
				) {
					multiplier * effect.multiplier
				} else {
					multiplier
				}
			}
		}
		val rawModifiedAccuracy = floor(
			accuracy *
				statStageModifiers.accuracyMultiplier(actorAccuracyStage) /
				statStageModifiers.accuracyMultiplier(targetEvasionStage) *
				actorItemMultiplier *
				actorAbilityMultiplier *
				targetItemMultiplier *
				targetAbilityMultiplier,
		).toInt().coerceAtLeast(1)
		val statusAccuracyCap = if (ignoresTargetAbilityEffects || skill.damageClass != BattleDamageClass.STATUS) null else {
			target.abilityEffects.filterIsInstance<BattleAbilityEffect.StatusSkillAccuracyCap>()
				.minOfOrNull { it.maximumAccuracy }
		}
		val modifiedAccuracy = statusAccuracyCap?.let(rawModifiedAccuracy::coerceAtMost) ?: rawModifiedAccuracy
		return rollAccuracy(skill, modifiedAccuracy, random)
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
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		criticalHit: Boolean,
	): Double {
		if (
			criticalHit ||
			skill.damageClass == BattleDamageClass.STATUS ||
			actor.abilityEffects.any { it is BattleAbilityEffect.OpponentBarrierBypass }
		) {
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
	private fun effectiveAccuracy(state: BattleState, actor: BattleParticipant, skill: BattleSkillSlot): Int? {
		val weather = state.effectiveWeatherFor(actor)
		return if (skill.accuracyOverridesByWeather.containsKey(weather)) {
			skill.accuracyOverridesByWeather[weather]
		} else {
			skill.accuracy
		}
	}

	/**
	 * 计算一击必杀类技能的专用命中率。
	 *
	 * 现代规则不使用 `BattleSkillSlot.accuracy`，也不应用命中/闪避阶级、重力、命中道具或类似修正；这些技能只读取
	 * 资料模型中的基础命中率，并加上等级差。目标等级高于使用者时应在命中前 gate 失败，因此这里仍用
	 * `coerceAtLeast(1)` 保护直接调用入口，避免异常资料把命中率降成无意义的 0 或负数。
	 */
	private fun oneHitKnockOutAccuracy(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): Int? {
		val oneHitKnockOut = skill.oneHitKnockOut ?: return null
		val skillElementId = skill.effectiveElementId(state.effectiveWeatherFor(actor), state.environment.terrain, actor)
		val baseAccuracy = oneHitKnockOut.baseAccuracyFor(
			skillElementId = skillElementId,
			actorElementIds = actor.elementIds,
		)
		return (baseAccuracy + actor.level - target.level).coerceAtLeast(1)
	}

	/**
	 * 按给定最终命中率执行随机判定。
	 *
	 * 普通技能和一击必杀技能都需要“100 或更高不消费随机数”的稳定 replay 口径，因此共用这段收口。随机原因仍沿用
	 * `accuracy for <skillId>`，避免 replay 断言因为内部公式分支暴露出新的命名差异。
	 */
	private fun rollAccuracy(skill: BattleSkillSlot, accuracy: Int, random: BattleRandom): BattleAccuracyCheck {
		if (accuracy >= 100) {
			return BattleAccuracyCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(100, "accuracy for ${skill.skillId}") + 1
		return BattleAccuracyCheck(hit = roll <= accuracy, roll = roll)
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
