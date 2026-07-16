package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleAbilityEffect

/**
 * 变化技能成功后的 HP 效果结算器。
 *
 * 变化技能不会进入普通伤害公式，也没有“本次造成的实际伤害量”。因此它们的自我回复、目标回复、天气/场地变量
 * 回复和建立替身不应该塞进伤害后 HP 效果类里，否则读代码时会误以为这些效果也参与吸取/反作用/低体力道具/
 * 倒下判定那条顺序。
 *
 * 本类只在变化技能已经通过目标、保护、命中等前置判定并成功后运行。满 HP、回复封锁或无法战斗时保持状态不变；
 * 替身比较特殊：已有替身或 HP 不足会让技能本身失败，因此会追加 [BattleEvent.SkillFailed]，方便 replay 和
 * 对照测试看到“技能已宣告并消耗 PP，但没有建立替身”的事实。
 */
internal class BattleStatusSkillHpEffects(
	private val majorStatusBlockReason: (
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		skill: BattleSkillSlot?,
	) -> BattleStatusBlockReason?,
) {
	private val statStageModifiers = BattleStatStageModifiers()

	/**
	 * 处理必须早于普通附加效果的 HP 规则。
	 *
	 * 大多数变化技能 HP 效果都可以在状态、能力阶级和场地等附加效果之后统一执行；但“按目标当前攻击实数回复”
	 * 的规则必须先读取目标这次被降低攻击之前的数值。把这类效果放进一个窄入口，而不是重排所有 HP 效果，
	 * 可以保留既有自我回复、目标治疗、替身和环境设置顺序，同时让这个特殊规则的先后关系在调用点一眼可见。
	 */
	fun applyBeforeAdditionalEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.hpEffects
			.filterIsInstance<BattleSkillHpEffect.SelfHealByTargetCurrentAttack>()
			.fold(state) { current, _ ->
				applySelfHealByTargetCurrentAttack(
					state = current,
					actorId = actorId,
					targetActorId = targetActorId,
					skill = skill,
				)
			}

	/**
	 * 处理变化技能成功后的 HP 回复和替身效果。
	 *
	 * 效果按技能槽中的 [BattleSkillSlot.hpEffects] 顺序执行。自我回复、天气变量回复、目标回复、场地变量目标
	 * 回复和攻击实数回复最终汇入对应的 HP helper；替身单独处理 HP 支付和替身 HP 写入。已经在
	 * [applyBeforeAdditionalEffects] 处理过的特殊前置 HP 效果会在这里跳过，避免同一技能重复回复。其它 HP
	 * 效果属于伤害技能后效，在这里直接跳过。
	 */
	fun apply(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.hpEffects
			.fold(applyRestFullHeal(state, actorId, skill)) { current, effect ->
				when (effect) {
					is BattleSkillHpEffect.SelfHealMaxHpFraction -> applyHealMaxHpFraction(
						state = current,
						sourceActorId = actorId,
						healedActorId = actorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
						failureReasonOnNoHealing = "no-hp-restored",
					)
					is BattleSkillHpEffect.SelfHealMaxHpByWeather -> {
						val actor = current.participant(actorId) ?: return@fold current
						val fraction = effect.weatherFractions[current.effectiveWeatherFor(actor)]
							?: effect.defaultFraction
						applyHealMaxHpFraction(
							state = current,
							sourceActorId = actorId,
							healedActorId = actorId,
							skill = skill,
							numerator = fraction.numerator,
							denominator = fraction.denominator,
							failureReasonOnNoHealing = "no-hp-restored",
						)
					}
					is BattleSkillHpEffect.TargetHealMaxHpFraction -> applyHealMaxHpFraction(
						state = current,
						sourceActorId = actorId,
						healedActorId = targetActorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
						failureReasonOnNoHealing = "no-hp-restored",
					)
					is BattleSkillHpEffect.TargetHealMaxHpByTerrain -> {
						val fraction = effect.terrainFractions[current.environment.terrain] ?: effect.defaultFraction
						applyHealMaxHpFraction(
							state = current,
							sourceActorId = actorId,
							healedActorId = targetActorId,
							skill = skill,
							numerator = fraction.numerator,
							denominator = fraction.denominator,
							failureReasonOnNoHealing = "no-hp-restored",
						)
					}
					is BattleSkillHpEffect.CreateSubstitute -> applyCreateSubstitute(
						state = current,
						actorId = actorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					BattleSkillHpEffect.MaximizeUserAttackWithHalfMaxHpCost() -> applyMaximizeUserAttackWithHalfMaxHpCost(
						state = current,
						actorId = actorId,
						skill = skill,
					)
					BattleSkillHpEffect.AverageUserAndTargetCurrentHp() -> applyAverageUserAndTargetCurrentHp(
						state = current,
						actorId = actorId,
						targetActorId = targetActorId,
						skill = skill,
					)
					BattleSkillHpEffect.SelfHealByTargetCurrentAttack() -> current
					is BattleSkillHpEffect.SelfHealAfterTargetMajorStatusCure -> applyTargetMajorStatusCureThenSelfHeal(
						state = current,
						actorId = actorId,
						targetActorId = targetActorId,
						skill = skill,
						numerator = effect.numerator,
						denominator = effect.denominator,
					)
					else -> current
				}
			}

	/**
	 * 处理睡觉的“固定睡眠 + 回满 HP + 清除原主要异常”组合效果。
	 *
	 * 睡觉不能复用普通主要异常附加：普通睡眠会拒绝已有主要异常，而睡觉必须在成功入睡后覆盖灼伤、中毒等旧状态。
	 * 它也不能复用普通自我回复：满 HP、回复封锁、睡眠免疫、场地免疫和已有睡眠都会让整招失败，不能只跳过回复。
	 * 因此这里以一个小的显式分支维护规则顺序：先检查 HP/回复封锁/已有睡眠，再复用主要异常阻止入口判断能否入睡，
	 * 成功后按“清除旧状态 -> 写入睡眠 -> 回满 HP”的事件顺序记录 replay。
	 */
	private fun applyRestFullHeal(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (!skill.restoresUserBySleeping) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		if (actor.currentHp == actor.maxHp) {
			return state.appendSkillFailed(actorId, actorId, skill, "rest-full-hp")
		}
		if (actor.majorStatus == BattleMajorStatus.SLEEP) {
			return state.appendSkillFailed(actorId, actorId, skill, "rest-already-asleep")
		}
		if (actor.healingBlocked()) {
			return state.appendSkillFailed(actorId, actorId, skill, "healing-blocked")
		}
		val blockedReason = majorStatusBlockReason(state, actorId, actor, BattleMajorStatus.SLEEP, skill)
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.StatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = actor.actorId,
					status = BattleMajorStatus.SLEEP,
					reason = blockedReason,
				),
			)
		}

		val previousStatus = actor.majorStatus
		val healAmount = actor.maxHp - actor.currentHp
		val rested = actor
			.clearMajorStatus()
			.applyMajorStatus(BattleMajorStatus.SLEEP, sleepTurnsRemaining = REST_SLEEP_BLOCKED_ACTIONS)
			.heal(healAmount)
		val afterState = state.replaceParticipant(rested)
		val afterPreviousStatusCleared = if (previousStatus == null) {
			afterState
		} else {
			afterState.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = previousStatus,
				),
			)
		}
		return afterPreviousStatusCleared
			.appendEvent(
				BattleEvent.StatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = actor.actorId,
					status = BattleMajorStatus.SLEEP,
				),
			)
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
				),
			)
	}

	private fun BattleState.appendSkillFailed(
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		reason: String,
	): BattleState =
		appendEvent(
			BattleEvent.SkillFailed(
				turnNumber = turnNumber,
				actorId = actorId,
				targetActorId = targetActorId,
				skillId = skill.skillId,
				reason = reason,
			),
		)

	private companion object {
		/**
		 * 睡觉在现代主系列中固定让使用者之后两次行动被睡眠阻止。
		 */
		private const val REST_SLEEP_BLOCKED_ACTIONS = 2

		/**
		 * 现代能力阶级的最大值。
		 */
		private const val MAX_STAT_STAGE = 6
	}

	/**
	 * 按最大 HP 比例回复指定成员。
	 *
	 * 自我回复、天气变量回复、目标回复和场地变量目标回复最终都汇入这里，确保满 HP 跳过、缺失 HP 夹取、回复封锁
	 * 和事件写入规则完全一致。普通回复技能在没有任何 HP 可写入时需要追加失败事件；复合技能如果已经完成其它主要
	 * 效果，例如先治愈目标异常，则可以把 [failureReasonOnNoHealing] 留空，表示“没有回复事件但技能仍然成功”。
	 */
	private fun applyHealMaxHpFraction(
		state: BattleState,
		sourceActorId: String,
		healedActorId: String,
		skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
		failureReasonOnNoHealing: String? = null,
	): BattleState {
		val healedActor = state.participant(healedActorId) ?: return state
		if (!healedActor.canReceiveHealing()) {
			return state.appendNoHealingFailure(sourceActorId, healedActorId, skill, failureReasonOnNoHealing)
		}
		val source = state.participant(sourceActorId)
		val pulseMultiplier = if (skill.pulseBased) {
			source?.abilityEffects?.filterIsInstance<BattleAbilityEffect.PulseBasedSkillDamageBoost>()
				?.fold(1.0) { multiplier, effect -> multiplier * effect.multiplier } ?: 1.0
		} else 1.0
		val healAmount = (fractionAmount(healedActor.maxHp, numerator, denominator) * pulseMultiplier).toInt()
			.coerceAtMost(healedActor.maxHp - healedActor.currentHp)
		if (healAmount <= 0) {
			return state.appendNoHealingFailure(sourceActorId, healedActorId, skill, failureReasonOnNoHealing)
		}
		return state
			.replaceParticipant(healedActor.heal(healAmount))
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = healedActor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 按目标当前攻击实数回复使用者。
	 *
	 * 目标攻击实数使用 [BattleStatStageModifiers.modifiedBattleStat] 计算，和普通能力阶级倍率保持同一套取整曲线。
	 * 该值在普通附加效果执行前读取，因此不会包含同一技能稍后造成的攻击下降。回复仍然遵守使用者自身的回复封锁、
	 * 倒下和满 HP 判断；如果使用者无法回复，目标后续降攻仍会按普通附加效果继续处理。
	 */
	private fun applySelfHealByTargetCurrentAttack(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val target = state.participant(targetActorId) ?: return state
		val targetCurrentAttack = statStageModifiers.modifiedBattleStat(
			base = target.attack,
			stage = target.statStage(BattleStat.ATTACK),
		)
		return applyFlatHealing(
			state = state,
			healedActorId = actorId,
			skill = skill,
			amount = targetCurrentAttack,
		)
	}

	/**
	 * 按固定数值回复指定成员。
	 *
	 * 这个 helper 只服务于“回复量来自另一个战斗实数”的技能，不参与最大 HP 比例取整。它和比例回复使用同一套
	 * 可回复判断、缺失 HP 夹取和事件写入口，避免攻击实数回复在满 HP、回复封锁或倒下时表现出另一套规则。
	 */
	private fun applyFlatHealing(
		state: BattleState,
		healedActorId: String,
		skill: BattleSkillSlot,
		amount: Int,
	): BattleState {
		val healedActor = state.participant(healedActorId) ?: return state
		if (!healedActor.canReceiveHealing()) {
			return state
		}
		val healAmount = amount.coerceAtMost(healedActor.maxHp - healedActor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(healedActor.heal(healAmount))
			.appendEvent(
				BattleEvent.SkillHealingApplied(
					turnNumber = state.turnNumber,
					actorId = healedActor.actorId,
					skillId = skill.skillId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 清除目标主要异常后按使用者最大 HP 比例回复使用者。
	 *
	 * 目标没有主要异常时，技能应在命中前 gate 失败，因此这里遇到空状态只保持状态不变，避免 HP 阶段自己承担失败
	 * 语义。清除异常必须先于回复事件写入 replay；如果使用者满 HP、倒下或被回复封锁，目标异常仍然已经被治愈，
	 * 只是不会产生回复事件。
	 */
	private fun applyTargetMajorStatusCureThenSelfHeal(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val target = state.participant(targetActorId) ?: return state
		val status = target.majorStatus ?: return state
		val afterCure = state
			.replaceParticipant(target.clearMajorStatus())
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = target.actorId,
					status = status,
				),
			)
		return applyHealMaxHpFraction(
			state = afterCure,
			sourceActorId = actorId,
			healedActorId = actorId,
			skill = skill,
			numerator = numerator,
			denominator = denominator,
		)
	}

	/**
	 * 支付使用者最大 HP 的固定比例来建立替身。
	 *
	 * 现代替身要求使用者当前 HP 必须严格大于费用，且不能已经拥有替身。失败时技能已经完成使用和 PP 消耗，因此
	 * 这里追加稳定的 [BattleEvent.SkillFailed] 原因，而不是静默保持状态；成功时本体扣除费用、替身获得同等 HP，
	 * 并用专用事件记录该运行态事实。
	 */
	private fun applyCreateSubstitute(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		numerator: Int,
		denominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		if (actor.hasSubstitute()) {
			return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = actor.actorId,
					skillId = skill.skillId,
					reason = "substitute-already-active",
				),
			)
		}
		val hpCost = fractionAmount(actor.maxHp, numerator, denominator)
			.coerceAtMost(actor.maxHp - 1)
		if (actor.currentHp <= hpCost) {
			return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = actor.actorId,
					skillId = skill.skillId,
					reason = "insufficient-hp-for-substitute",
				),
			)
		}
		val substituted = actor.startSubstitute(hpCost)
		return state
			.replaceParticipant(substituted)
			.appendEvent(
				BattleEvent.SubstituteStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					hpCost = hpCost,
					substituteHp = substituted.substituteHp,
				),
			)
	}

	/**
	 * 支付半数最大 HP 并把使用者攻击阶级直接设为 +6。
	 *
	 * 该规则不能复用普通能力阶级附加效果：普通效果按 delta 变化，而腹鼓类技能的公开规则是“直接最大化攻击”。
	 * 因此这里先在写入前检查两个完整失败条件：攻击已经达到 +6 时失败，当前 HP 扣除 `maxHp / 2` 后会倒下时失败。
	 * 成功时事件顺序固定为 HP 代价事件在前、能力阶级变化事件在后；这样 replay 可以解释“支付代价换取最大化攻击”
	 * 的因果关系，也避免其它伤害后流程误把该 HP 代价当成普通伤害。
	 */
	private fun applyMaximizeUserAttackWithHalfMaxHpCost(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val previousStage = actor.statStage(BattleStat.ATTACK)
		if (previousStage >= MAX_STAT_STAGE) {
			return state.appendSkillFailed(actorId, actorId, skill, "attack-stage-already-maximum")
		}
		val hpCost = actor.maxHp / 2
		if (actor.currentHp <= hpCost) {
			return state.appendSkillFailed(actorId, actorId, skill, "insufficient-hp-for-max-attack-cost")
		}
		val updated = actor.receiveDamage(hpCost).setStatStage(BattleStat.ATTACK, MAX_STAT_STAGE)
		val afterCost = state.replaceParticipant(updated)
		val afterCostEvent = if (hpCost == 0) {
			afterCost
		} else {
			afterCost.appendEvent(
				BattleEvent.SkillRecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					amount = hpCost,
					sourceDamageAmount = actor.maxHp,
				),
			)
		}
		return afterCostEvent.appendEvent(
			BattleEvent.StatStageChanged(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = actor.actorId,
				stat = BattleStat.ATTACK,
				delta = MAX_STAT_STAGE - previousStage,
				currentStage = MAX_STAT_STAGE,
			),
		)
	}

	/**
	 * 把使用者和目标 HP 设置为双方当前 HP 的平均值。
	 *
	 * 分担痛楚类规则不是普通伤害或回复：公开规则说明它忽略命中/闪避修正，且该 HP 变化不计为“造成伤害”。因此这里
	 * 直接写入双方当前 HP，并用 [BattleEvent.HpAveragedBySkill] 记录前后值；不会调用普通伤害、普通治疗、回复封锁
	 * 或伤害后特性流程。目标替身会在命中前 gate 中让技能失败，到达这里时表示目标本体可以被直接重分配 HP。
	 */
	private fun applyAverageUserAndTargetCurrentHp(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (!actor.canBattle() || !target.canBattle()) {
			return state
		}
		val averageHp = (actor.currentHp + target.currentHp) / 2
		val actorCurrentHp = averageHp.coerceAtMost(actor.maxHp)
		val targetCurrentHp = averageHp.coerceAtMost(target.maxHp)
		return state
			.replaceParticipant(actor.copy(currentHp = actorCurrentHp))
			.replaceParticipant(target.copy(currentHp = targetCurrentHp))
			.appendEvent(
				BattleEvent.HpAveragedBySkill(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					averageHp = averageHp,
					actorPreviousHp = actor.currentHp,
					actorCurrentHp = actorCurrentHp,
					targetPreviousHp = target.currentHp,
					targetCurrentHp = targetCurrentHp,
				),
			)
	}

	/**
	 * 在普通回复技能没有任何 HP 写入时追加失败事件。
	 *
	 * 回复类变化技能在满 HP、回复封锁或其它不可回复条件下会表现为技能失败；但复合技能可能已经完成治愈异常等
	 * 主要效果，此时“没有回复量”不应该覆盖先前成功事实。调用方用可空原因显式选择是否写失败事件，避免 helper
	 * 通过技能名称猜测语义。
	 */
	private fun BattleState.appendNoHealingFailure(
		sourceActorId: String,
		healedActorId: String,
		skill: BattleSkillSlot,
		reason: String?,
	): BattleState =
		if (reason == null) {
			this
		} else {
			appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = turnNumber,
					actorId = sourceActorId,
					targetActorId = healedActorId,
					skillId = skill.skillId,
					reason = reason,
				),
			)
		}

}
