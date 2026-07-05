package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillWeightEffect
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能成功后的结构化附加效果流水线。
 *
 * 本类只在技能已经通过目标、保护、命中、免疫、吸收和伤害结算之后运行。它按照资料中的固定顺序应用主要异常、
 * 临时状态、普通能力阶级变化、复杂能力阶级操作、一侧/场地效果、入场陷阱、使用者侧清理和强制换人。它不负责 PP、命中、
 * 普通伤害、直接伤害、接触特性、低体力道具或锁招推进；这些阶段仍由 [BattleEngine] 决定。这样附加效果顺序
 * 有一处可读实现，主状态机也不会继续膨胀成效果脚本。
 */
internal class BattleSkillAdditionalEffects(
	private val majorStatusEffects: BattleMajorStatusEffects,
	private val volatileStatusEffects: BattleVolatileStatusEffects,
	private val statStageEffects: BattleStatStageEffects,
	private val fieldEffects: BattleFieldEffects,
	private val targetDefenseEffects: BattleTargetDefenseEffects,
	private val leechSeedEffects: BattleLeechSeedEffects,
	private val userSideCleanupEffects: BattleUserSideCleanupEffects,
	private val forcedSwitchEffects: BattleForcedSwitchEffects,
) {
	/**
	 * 应用技能命中后的全部结构化附加效果。
	 *
	 * 每个效果族内部按技能槽列表顺序结算，概率小于 100 的效果会消费随机数。若目标已经倒下、已有主要异常状态、
	 * 阶级变化被上下限夹住、对手替身挡住效果，或同类一侧/场地状态已经存在，则保持状态不变并跳过对应事件。
	 * 使用者侧清理放在寄生种子写入之后、强制换人之前，确保高速旋转类技能先完成命中后状态事实，再清理自身束缚、
	 * 寄生和己方入场陷阱；强制换人放在最后，确保伤害与普通附加效果先完成，再触发换下目标和后续入场阶段。
	 */
	fun apply(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val afterStatuses = applyMajorStatusApplications(state, actorId, targetActorId, skill, random)
		val afterVolatileStatuses = applyVolatileStatusApplications(afterStatuses, actorId, targetActorId, skill, random)
		val afterStatStageEffects = applyStatStageEffects(afterVolatileStatuses, actorId, targetActorId, skill, random)
		val afterWeightEffects = applyWeightEffects(
			state = afterStatStageEffects,
			beforeStatStageEffects = afterVolatileStatuses,
			actorId = actorId,
			targetActorId = targetActorId,
			skill = skill,
		)
		val afterCriticalHitBoost = applyCriticalHitStageBoost(afterWeightEffects, actorId, skill)
		val afterStatStageOperations = statStageEffects.applyOperations(afterCriticalHitBoost, actorId, targetActorId, skill, random)
		val afterSideDamageReductions = applySideConditions(afterStatStageOperations, actorId, targetActorId, skill, random)
		val afterSideSpeedModifiers = applySideSpeedModifiers(afterSideDamageReductions, actorId, targetActorId, skill, random)
		val afterSideProtections = applySideProtections(afterSideSpeedModifiers, actorId, targetActorId, skill, random)
		val afterSideEntryHazards = applySideEntryHazards(afterSideProtections, actorId, targetActorId, skill, random)
		val afterFieldSpeedOrder = applyFieldSpeedOrder(afterSideEntryHazards, actorId, skill, random)
		val afterAccuracyLock = applyAccuracyLock(afterFieldSpeedOrder, actorId, targetActorId, skill)
		val afterLeechSeed = leechSeedEffects.apply(afterAccuracyLock, actorId, targetActorId, skill)
		val afterUserSideCleanup = userSideCleanupEffects.apply(afterLeechSeed, actorId, skill)
		val afterUserMajorStatusCure = applyUserMajorStatusCure(afterUserSideCleanup, actorId, skill)
		val afterUserSideActiveMajorStatusCures = applyUserSideActiveMajorStatusCures(afterUserMajorStatusCure, actorId, skill)
		val afterUserSideMajorStatusCures = applyUserSideMajorStatusCures(afterUserSideActiveMajorStatusCures, actorId, skill)
		val afterTargetLastSkillPpReduction = applyTargetLastSkillPpReduction(
			afterUserSideMajorStatusCures,
			actorId,
			targetActorId,
			skill,
		)
		return forcedSwitchEffects.apply(afterTargetLastSkillPpReduction, actorId, targetActorId, skill, random)
	}

	/**
	 * 应用技能声明的主要异常状态附加。
	 *
	 * 真正的状态免疫、睡眠回合随机数、状态治愈道具等规则由 [BattleMajorStatusEffects] 负责；这里仅按技能资料解析
	 * 接收者、概率和效果顺序。
	 */
	private fun applyMajorStatusApplications(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.statusApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "status chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, application.target) ?: return@fold current
				if (!recipient.canBattle()) {
					current
				} else {
					majorStatusEffects.applyMajorStatus(
						state = current,
						actorId = actorId,
						recipient = recipient,
						status = application.status,
						random = random,
						randomReason = "sleep duration for ${skill.skillId}",
						skill = skill,
					)
				}
			}
		}

	/**
	 * 应用技能声明的临时状态附加。
	 *
	 * 混乱持续时间等需要随机数的状态细节仍由 [BattleVolatileStatusEffects] 统一处理；这里保持和主要异常相同的接收者解析
	 * 与概率消费口径。
	 */
	private fun applyVolatileStatusApplications(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.volatileStatusApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "volatile status chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, application.target) ?: return@fold current
				if (!recipient.canBattle()) {
					current
				} else {
					volatileStatusEffects.applyVolatileStatus(
						state = current,
						actorId = actorId,
						recipient = recipient,
						status = application.status,
						random = random,
						randomReason = "confusion duration for ${skill.skillId}",
						skill = skill,
					)
				}
			}
		}

	/**
	 * 扣减目标最近一次成功使用技能的剩余 PP。
	 *
	 * 怨恨的现代规则读取目标最近一次真正使用过的技能，并固定尝试扣 4 PP；如果目标从未成功使用技能、技能槽已经
	 * 不存在，或该技能当前 PP 已经为 0，则本次技能失败。替身不会阻止怨恨，因此这里刻意不调用
	 * [BattleTargetDefenseEffects.substituteBlocksOpponentEffect]；保护、命中和特性/属性等命中前 gate 已在外层完成。
	 */
	private fun applyTargetLastSkillPpReduction(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (skill.targetLastSkillPpReduction == 0) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		val targetLastSkillId = target.lastSuccessfulSkillId
			?: return state.appendTargetLastSkillPpReductionFailed(actorId, targetActorId, skill)
		val targetLastSkillSlot = target.skillSlot(targetLastSkillId)
			?: return state.appendTargetLastSkillPpReductionFailed(actorId, targetActorId, skill)
		if (targetLastSkillSlot.remainingPp <= 0) {
			return state.appendTargetLastSkillPpReductionFailed(actorId, targetActorId, skill)
		}
		val updatedSlot = targetLastSkillSlot.reducePp(skill.targetLastSkillPpReduction)
		val actualReduction = targetLastSkillSlot.remainingPp - updatedSlot.remainingPp
		return state
			.replaceParticipant(target.replaceSkillSlot(updatedSlot))
			.appendEvent(
				BattleEvent.SkillPpReduced(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					reducedSkillId = targetLastSkillId,
					amount = actualReduction,
					previousRemainingPp = targetLastSkillSlot.remainingPp,
					currentRemainingPp = updatedSlot.remainingPp,
				),
			)
	}

	/**
	 * 记录“目标没有可扣减 PP 的最近技能”失败。
	 *
	 * 失败原因合并“没有最近技能、技能槽不存在、PP 已为 0”三种内部情况，是因为公开战斗日志只需要表达怨恨没有
	 * 找到可扣减的目标技能；拆得过细会把内部运行态泄漏给前端和 replay。
	 */
	private fun BattleState.appendTargetLastSkillPpReductionFailed(
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		appendEvent(
			BattleEvent.SkillFailed(
				turnNumber = turnNumber,
				actorId = actorId,
				targetActorId = targetActorId,
				skillId = skill.skillId,
				reason = "target-has-no-last-skill-with-pp",
			),
		)

	/**
	 * 应用普通能力阶级增减。
	 *
	 * 普通阶级变化只做 delta 叠加，并让 [BattleParticipant.changeStatStage] 负责 -6..6 边界夹取。若边界夹取后没有
	 * 实际变化，则不写事件。对手替身会阻止指向目标的非声音类技能效果。
	 */
	private fun applyStatStageEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.statStageEffects.fold(state) { current, effect ->
			if (!chanceSucceeds(effect.chancePercent, random, "stat stage chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, effect.target) ?: return@fold current
				if (targetDefenseEffects.substituteBlocksOpponentEffect(current, actorId, recipient.actorId, skill)) {
					return@fold current
				}
				if (statStageDropBlockedBySideProtection(current, actorId, recipient, effect.stageDelta)) {
					return@fold current.appendEvent(
						BattleEvent.StatStageChangeBlocked(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = recipient.actorId,
							stat = effect.stat,
							attemptedDelta = effect.stageDelta,
							reason = BattleStatusBlockReason.SIDE_PROTECTION,
						),
					)
				}
				val beforeStage = recipient.statStage(effect.stat)
				val updated = recipient.changeStatStage(effect.stat, effect.stageDelta)
				val afterStage = updated.statStage(effect.stat)
				if (beforeStage == afterStage) {
					current
				} else {
					current
						.replaceParticipant(updated)
						.appendEvent(
							BattleEvent.StatStageChanged(
								turnNumber = current.turnNumber,
								actorId = actorId,
								targetActorId = recipient.actorId,
								stat = effect.stat,
								delta = afterStage - beforeStage,
								currentStage = afterStage,
							),
						)
				}
			}
		}

	/**
	 * 判断白雾类一侧防护是否阻止本次普通能力阶级下降。
	 *
	 * 只拦截负数 delta，因为白雾不阻止能力提升，也不阻止使用者自己降低自己的能力。这里使用“来源不是目标本人”
	 * 判断是否属于外部施加的能力下降，让双打中同伴误降能力也会被防护挡住，同时保留自我代价类技能的规则空间。
	 */
	private fun statStageDropBlockedBySideProtection(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		stageDelta: Int,
	): Boolean =
		stageDelta < 0 &&
			actorId != recipient.actorId &&
			state.sideHasProtection(recipient.actorId, BattleSideProtectionKind.STAT_STAGE_REDUCTION)

	/**
	 * 应用技能声明的临时体重减轻效果。
	 *
	 * 这类效果依赖同一次技能的能力阶级是否真的发生变化，因此调用方会传入能力阶级效果执行前后的状态。若指定
	 * [BattleSkillWeightEffect.requiredChangedStat] 没有变化，或成员已经处于最低有效体重，函数保持状态不变且不写事件。
	 */
	private fun applyWeightEffects(
		state: BattleState,
		beforeStatStageEffects: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.weightEffects.fold(state) { current, effect ->
			val recipient = current.effectRecipient(actorId, targetActorId, effect.target) ?: return@fold current
			val beforeRecipient = beforeStatStageEffects.participant(recipient.actorId) ?: return@fold current
			if (!effect.requiredStatChanged(beforeRecipient, recipient)) {
				return@fold current
			}
			val maxReduction = (recipient.weight - effect.minimumWeight).coerceAtLeast(0)
			val nextReduction = (recipient.weightReduction + effect.reduction).coerceAtMost(maxReduction)
			if (nextReduction == recipient.weightReduction) {
				current
			} else {
				current
					.replaceParticipant(recipient.copy(weightReduction = nextReduction))
					.appendEvent(
						BattleEvent.WeightReductionChanged(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = recipient.actorId,
							skillId = skill.skillId,
							previousReduction = recipient.weightReduction,
							currentReduction = nextReduction,
						),
					)
			}
		}

	private fun BattleSkillWeightEffect.requiredStatChanged(before: BattleParticipant, after: BattleParticipant): Boolean =
		requiredChangedStat == null || before.statStage(requiredChangedStat) != after.statStage(requiredChangedStat)

	/**
	 * 应用聚气类要害等级加成。
	 *
	 * 聚气是一个在场期间的成员状态：成功后让后续技能的要害等级在技能自身基础上额外 +2，离场清除。若成员已经有
	 * 不低于本技能声明的加成，现代规则表现为技能失败而不是刷新或叠加；这里记录稳定失败原因，避免 replay 只看到
	 * PP 被消耗却不知道为什么没有新的状态变化。
	 */
	private fun applyCriticalHitStageBoost(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (skill.criticalHitStageBoost == 0) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (actor.criticalHitStageBonus >= skill.criticalHitStageBoost) {
			return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = actorId,
					skillId = skill.skillId,
					reason = "critical-hit-stage-boost-already-active",
				),
			)
		}
		return state
			.replaceParticipant(actor.copy(criticalHitStageBonus = skill.criticalHitStageBoost))
			.appendEvent(
				BattleEvent.CriticalHitStageBoostStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					skillId = skill.skillId,
					stageBonus = skill.criticalHitStageBoost,
				),
			)
	}

	/**
	 * 应用命中锁定效果。
	 *
	 * Lock-On / Mind Reader 类技能自身是一次普通命中后的变化效果：它不造成伤害，也不改变目标；真正被修改的是
	 * 使用者的“下回合前对这个目标必中”运行态。现代主系列规则允许多个使用者同时锁定同一个目标，因此这里只写入
	 * 当前使用者的运行态，不清理其它来源。替身阻挡和“同一使用者重复锁定当前目标”的失败条件已经在单目标结算器中
	 * 于旧锁定被消费前处理；到达本函数时表示锁定效果确实可以建立。
	 */
	private fun applyAccuracyLock(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (!skill.locksAccuracyOnTarget) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (!actor.canBattle() || !target.canBattle()) {
			return state
		}
		return state
			.replaceParticipant(actor.lockAccuracyOnTarget(target.actorId))
			.appendEvent(
				BattleEvent.AccuracyLockStarted(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
				),
			)
	}

	/**
	 * 应用防守方一侧的屏障/条件效果。
	 */
	private fun applySideConditions(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.sideConditionApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side condition chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applySideCondition(current, actorId, targetActorId, skill, application)
			}
		}

	/**
	 * 应用一侧速度修正场地效果。
	 */
	private fun applySideSpeedModifiers(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.sideSpeedModifierApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side speed condition chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applySideSpeedModifier(current, actorId, targetActorId, skill, application)
			}
		}

	/**
	 * 应用一侧防护效果。
	 */
	private fun applySideProtections(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.sideProtectionApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side protection chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applySideProtection(current, actorId, targetActorId, skill, application)
			}
		}

	/**
	 * 清除使用者所属侧全部成员的主要异常状态。
	 *
	 * 治愈铃声这类技能的目标是“使用者队伍”，不是当前选中的单个目标；因此它必须读取 [BattleState.sideOf] 中的
	 * 完整成员列表，包含后备成员。现代主要异常槽位不包含混乱、束缚、寄生等临时状态，所以这里只调用
	 * [BattleParticipant.clearMajorStatus]，并为每个真实发生清除的成员追加独立 [BattleEvent.StatusCleared]。
	 * 没有成员带主要异常时技能仍然成功，只是不产生清除事件。
	 */
	private fun applyUserSideMajorStatusCures(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (!skill.curesUserSideMajorStatuses) {
			return state
		}
		val side = state.sideOf(actorId) ?: return state
		return side.participants.fold(state) { current, sideParticipant ->
			val participant = current.participant(sideParticipant.actorId) ?: return@fold current
			val status = participant.majorStatus ?: return@fold current
			current
				.replaceParticipant(participant.clearMajorStatus())
				.appendEvent(
					BattleEvent.StatusCleared(
						turnNumber = current.turnNumber,
						actorId = participant.actorId,
						status = status,
					),
				)
		}
	}

	/**
	 * 清除使用者自身的主要异常状态。
	 *
	 * 勇气填充这类技能只治疗使用者自己，不能复用治愈铃声的整队净化，也不能复用丛林治疗/新月祈祷的同侧上场净化。
	 * 这里直接读取 [actorId] 对应成员，只有实际存在主要异常时才改写状态并追加 [BattleEvent.StatusCleared]；
	 * 没有异常时技能仍可继续完成后续能力阶级变化，符合“净化 + 强化”复合技能的现代规则语义。
	 */
	private fun applyUserMajorStatusCure(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (!skill.curesUserMajorStatus) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		val status = actor.majorStatus ?: return state
		return state
			.replaceParticipant(actor.clearMajorStatus())
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = status,
				),
			)
	}

	/**
	 * 清除使用者同侧当前上场成员的主要异常状态。
	 *
	 * 生命水滴只负责回复 HP；丛林治疗和新月祈祷则同时回复 HP 并治愈“当前在场”的使用者与同伴。这个范围和
	 * 治愈铃声不同，不能遍历整队成员，否则后备成员会被错误治愈。这里读取同侧 `activeActorIds`，并在每个目标
	 * 命中结算中幂等执行：第一次会清除所有上场成员，后续目标再次经过本函数时已经没有主要异常，不会重复事件。
	 */
	private fun applyUserSideActiveMajorStatusCures(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (!skill.curesUserSideActiveMajorStatuses) {
			return state
		}
		val side = state.sideOf(actorId) ?: return state
		return side.activeActorIds.fold(state) { current, activeActorId ->
			val participant = current.participant(activeActorId) ?: return@fold current
			val status = participant.majorStatus ?: return@fold current
			current
				.replaceParticipant(participant.clearMajorStatus())
				.appendEvent(
					BattleEvent.StatusCleared(
						turnNumber = current.turnNumber,
						actorId = participant.actorId,
						status = status,
					),
				)
		}
	}

	/**
	 * 应用一侧入场陷阱效果。
	 */
	private fun applySideEntryHazards(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.sideEntryHazardApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "side entry hazard chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applySideEntryHazard(current, actorId, targetActorId, skill, application)
			}
		}

	/**
	 * 应用全场速度顺序效果。
	 */
	private fun applyFieldSpeedOrder(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState =
		skill.fieldSpeedOrderApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "field speed order chance for ${skill.skillId}")) {
				current
			} else {
				fieldEffects.applyFieldSpeedOrder(current, actorId, skill, application)
			}
		}

	/**
	 * 根据效果目标枚举找到实际承受效果的成员。
	 */
	private fun BattleState.effectRecipient(actorId: String, targetActorId: String, target: BattleEffectTarget): BattleParticipant? =
		when (target) {
			BattleEffectTarget.USER -> participant(actorId)
			BattleEffectTarget.TARGET -> participant(targetActorId)
		}
}
