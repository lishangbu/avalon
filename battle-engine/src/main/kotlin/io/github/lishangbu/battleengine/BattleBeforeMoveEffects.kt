package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 行动前状态阻止阶段 resolver。
 *
 * 这个类只处理“成员已经轮到行动，但技能使用事件和 PP 消耗尚未发生”之前的状态检查。它会决定本次行动是否被
 * 休整、睡眠、冰冻、畏缩、混乱、回复封锁、挑衅、定身法、无理取闹或麻痹阻止，并在需要时追加
 * [BattleEvent.SkillPrevented]、状态解除事件或混乱自伤事件。
 *
 * 阶段顺序是本类最重要的规则，不应被事件总线或插件系统隐藏：
 * - 休整最先阻止行动，因为它来自上一回合技能的强制空过。
 * - 睡眠和冰冻发生在多数其它行动前状态之前；冰冻可能自然解冻或被自解冻技能解除。
 * - 畏缩只阻止一次行动，阻止后立即清除本回合标记。
 * - 混乱先递减内部计数，可能解除，也可能按概率自伤并阻止行动。
 * - 回复封锁、挑衅、定身法、无理取闹读取技能或上一成功技能，阻止时不消耗 PP。
 * - 麻痹最后按概率阻止行动，只有前面状态没有阻止时才消费麻痹随机数。
 *
 * 本类不处理锁招/蓄力被打断后的清理，因为那取决于行动来源 [SkillActionSource]，仍由 [BattleEngine] 在收到
 * [BattleBeforeMoveResult.blocked] 后收口。它也不直接实现低体力回复道具，而是通过 [lowHpItemHealing] 回调复用
 * 主状态机的统一道具触发逻辑，保证混乱自伤后的道具事件顺序和其它伤害来源一致。
 */
internal class BattleBeforeMoveEffects(
	private val statStageModifiers: BattleStatStageModifiers,
	private val lowHpItemHealing: (state: BattleState, actorId: String) -> BattleState,
) {
	/**
	 * 处理行动前可能阻止技能的状态。
	 *
	 * @param state 当前战斗状态。
	 * @param actor 即将行动的成员快照。调用方应保证它来自 [state]，本函数会在需要时重新写回更新后的成员。
	 * @param skill 本次准备使用的技能槽。回复封锁、挑衅、定身法和自解冻都依赖技能字段判断。
	 * @param random 本阶段所需随机源；只有冰冻自然解冻、混乱自伤、混乱伤害浮动和麻痹阻止会消费随机数。
	 * @return 新状态以及本次技能是否已经被阻止。
	 */
	fun resolve(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleBeforeMoveResult {
		if (actor.rechargeTurnsRemaining > 0) {
			return BattleBeforeMoveResult(
				state = consumeRechargeBlockedAction(state, actor),
				blocked = true,
			)
		}
		if (actor.majorStatus == BattleMajorStatus.SLEEP) {
			return BattleBeforeMoveResult(
				state = consumeSleepBlockedAction(state, actor),
				blocked = true,
			)
		}
		if (actor.majorStatus == BattleMajorStatus.FREEZE) {
			return resolveFreezeBeforeMove(state, actor, skill, random)
		}
		if (actor.flinched) {
			return BattleBeforeMoveResult(
				state = consumeFlinchBlockedAction(state, actor),
				blocked = true,
			)
		}
		if (actor.confusionTurnsRemaining > 0) {
			return resolveConfusionBeforeMove(state, actor, random)
		}
		if (actor.healBlockTurnsRemaining > 0 && healBlockPreventsSkill(skill)) {
			return BattleBeforeMoveResult(
				state = consumeHealBlockBlockedAction(state, actor, skill),
				blocked = true,
			)
		}
		if (actor.tauntTurnsRemaining > 0 && tauntPreventsSkill(skill)) {
			return BattleBeforeMoveResult(
				state = consumeTauntBlockedAction(state, actor, skill),
				blocked = true,
			)
		}
		if (actor.disabledSkillTurnsRemaining > 0 && actor.disabledSkillId == skill.skillId) {
			return BattleBeforeMoveResult(
				state = consumeDisableBlockedAction(state, actor, skill),
				blocked = true,
			)
		}
		if (actor.tormented && actor.lastSuccessfulSkillId == skill.skillId) {
			return BattleBeforeMoveResult(
				state = consumeTormentBlockedAction(state, actor, skill),
				blocked = true,
			)
		}
		if (actor.majorStatus == BattleMajorStatus.PARALYSIS && paralysisBlocksMove(actor, random)) {
			return BattleBeforeMoveResult(
				state = consumeParalysisBlockedAction(state, actor),
				blocked = true,
			)
		}
		return BattleBeforeMoveResult(state = state, blocked = false)
	}

	/**
	 * 处理冰冻的行动前自然解冻和行动阻止。
	 *
	 * 带有 `thawsUserBeforeMove` 的技能会先解除自身冰冻并继续行动，不消费自然解冻随机数。其它技能按现代规则消费
	 * 20% 自然解冻判定；成功则清除冰冻并继续行动，失败则追加技能阻止事件且不消耗 PP。
	 */
	private fun resolveFreezeBeforeMove(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleBeforeMoveResult {
		if (skill.thawsUserBeforeMove) {
			return BattleBeforeMoveResult(
				state = clearFrozenActorBeforeMove(state, actor),
				blocked = false,
			)
		}
		if (chanceSucceeds(FREEZE_THAW_CHANCE_PERCENT, random, "freeze thaw chance for ${actor.actorId}")) {
			return BattleBeforeMoveResult(
				state = clearFrozenActorBeforeMove(state, actor),
				blocked = false,
			)
		}
		return BattleBeforeMoveResult(
			state = consumeFreezeBlockedAction(state, actor),
			blocked = true,
		)
	}

	/**
	 * 清除行动者自身冰冻状态并追加可见解除事件。
	 *
	 * 调用方负责决定是否需要消费自然解冻随机数；本函数只表达“冰冻确实被清除”这一状态事实。
	 */
	private fun clearFrozenActorBeforeMove(state: BattleState, actor: BattleParticipant): BattleState =
		state
			.replaceParticipant(actor.clearMajorStatus())
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = BattleMajorStatus.FREEZE,
				),
			)

	/**
	 * 消耗睡眠对本次技能行动的阻止效果。
	 *
	 * 睡眠不消耗 PP、不产生技能使用事件，也不会继续进入命中或伤害流程。成员保存的是还会被阻止行动几次，因此
	 * 每次阻止后递减；递减到 0 时立即清除睡眠，并记录状态解除事件。
	 */
	private fun consumeSleepBlockedAction(state: BattleState, actor: BattleParticipant): BattleState {
		val turnsRemainingBefore = actor.sleepTurnsRemaining
		val updated = actor.consumeSleepBlockedTurn()
		val blocked = preventSkill(
			state = state.replaceParticipant(updated),
			actor = actor,
			reason = SkillPreventionReason.SLEEP,
			turnsRemainingBefore = turnsRemainingBefore,
		)
		return if (updated.majorStatus == null) {
			blocked.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					status = BattleMajorStatus.SLEEP,
				),
			)
		} else {
			blocked
		}
	}

	/**
	 * 消耗休整对本次技能行动的阻止效果。
	 *
	 * 休整来自上一次成功技能写入的强制空过状态。它不消耗本次提交技能的 PP，也不会触发讲究锁定、命中、伤害或
	 * 其它行动前随机判定。
	 */
	private fun consumeRechargeBlockedAction(state: BattleState, actor: BattleParticipant): BattleState {
		val turnsRemainingBefore = actor.rechargeTurnsRemaining
		return preventSkill(
			state = state.replaceParticipant(actor.consumeRechargeTurn()),
			actor = actor,
			reason = SkillPreventionReason.RECHARGE,
			turnsRemainingBefore = turnsRemainingBefore,
		)
	}

	/**
	 * 消耗一次“回复封锁阻止本次回复类技能”的行动结果。
	 *
	 * 回复封锁本身不会因为阻止一次行动而提前减少持续回合；它只在完整回合末统一递减。这里不消耗 PP、不追加
	 * [BattleEvent.SkillUsed]，也不触发讲究锁定、命中随机数、保护判定或伤害流程。这样主动回复技能和吸取回复类
	 * 攻击技能在回复封锁下都会稳定停在行动前阶段。
	 */
	private fun consumeHealBlockBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		preventSkill(
			state = state,
			actor = actor,
			reason = SkillPreventionReason.HEAL_BLOCK,
			skillId = skill.skillId,
			turnsRemainingBefore = actor.healBlockTurnsRemaining,
		)

	/**
	 * 判断回复封锁是否禁止成员宣告该技能。
	 *
	 * 现代规则中，直接回复使用者的变化技能、天气相关自我回复，以及伤害后吸取回复的攻击技能都不能在回复封锁
	 * 下使用。建立替身、反作用伤害和其它非回复类 HP 变化不在此列；它们继续交给技能命中后的各自阶段结算。
	 */
	private fun healBlockPreventsSkill(skill: BattleSkillSlot): Boolean =
		skill.hpEffects.any { effect ->
			effect is BattleSkillHpEffect.SelfHealMaxHpFraction ||
				effect is BattleSkillHpEffect.SelfHealMaxHpByWeather ||
				effect is BattleSkillHpEffect.DrainDamage
		}

	/**
	 * 消耗一次“挑衅阻止本次变化技能”的行动结果。
	 *
	 * 挑衅不会因为成功阻止一次行动而提前减少持续回合；它只在完整回合末统一递减。这里不消耗 PP、不追加技能使用
	 * 事件，也不触发命中、保护、附加效果或讲究类锁定流程，确保被挑衅成员的变化技能稳定停在行动前。
	 */
	private fun consumeTauntBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		preventSkill(
			state = state,
			actor = actor,
			reason = SkillPreventionReason.TAUNT,
			skillId = skill.skillId,
			turnsRemainingBefore = actor.tauntTurnsRemaining,
		)

	/**
	 * 判断挑衅是否禁止成员宣告该技能。
	 *
	 * 挑衅只阻止变化分类技能。物理和特殊分类技能即使没有造成伤害，仍不由挑衅在这里拦截；它们如果失败，应由
	 * 命中、保护、免疫、目标合法性或技能自身规则在后续阶段产生对应事件。
	 */
	private fun tauntPreventsSkill(skill: BattleSkillSlot): Boolean =
		skill.damageClass == BattleDamageClass.STATUS

	/**
	 * 消耗一次“定身法阻止本次被禁用技能”的行动结果。
	 *
	 * 定身法持续时间不因成功阻止一次行动而提前减少；它只在回合末统一递减。这里不消耗 PP、不追加技能使用事件，
	 * 也不触发命中、保护、附加效果或讲究类锁定流程。事件里保留被禁用技能 ID，方便对照测试确认阻止的是哪一招。
	 */
	private fun consumeDisableBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		preventSkill(
			state = state,
			actor = actor,
			reason = SkillPreventionReason.DISABLE,
			skillId = skill.skillId,
			turnsRemainingBefore = actor.disabledSkillTurnsRemaining,
		)

	/**
	 * 消耗一次“无理取闹阻止连续使用同一技能”的行动结果。
	 *
	 * 无理取闹没有回合倒计时，也不会因为阻止一次行动而解除。这里不消耗 PP、不追加技能使用事件，也不覆盖
	 * [BattleParticipant.lastSuccessfulSkillId]，让下一回合仍能依据“上一次真正成功使用的技能”判断是否继续阻止。
	 */
	private fun consumeTormentBlockedAction(
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState =
		preventSkill(
			state = state,
			actor = actor,
			reason = SkillPreventionReason.TORMENT,
			skillId = skill.skillId,
			previousSkillId = requireNotNull(actor.lastSuccessfulSkillId) {
				"torment requires previous successful skill"
			},
		)

	/**
	 * 记录冰冻未解冻时对本次技能行动的阻止效果。
	 *
	 * 冰冻状态本身保持不变；后续行动还会再次尝试自然解冻。该函数只追加阻止事件，不修改成员快照，也不消费 PP。
	 */
	private fun consumeFreezeBlockedAction(state: BattleState, actor: BattleParticipant): BattleState =
		preventSkill(state = state, actor = actor, reason = SkillPreventionReason.FREEZE)

	/**
	 * 消耗畏缩对本次技能行动的阻止效果。
	 *
	 * 畏缩是回合内临时状态，只会阻止一次行动。阻止发生后立即清掉成员上的标记；如果成员本回合没有尝试行动，
	 * 回合末清理会静默移除该标记，不产生解除事件。
	 */
	private fun consumeFlinchBlockedAction(state: BattleState, actor: BattleParticipant): BattleState =
		preventSkill(
			state = state.replaceParticipant(actor.consumeFlinch()),
			actor = actor,
			reason = SkillPreventionReason.VOLATILE_STATUS,
			status = BattleVolatileStatus.FLINCH,
		)

	/**
	 * 消耗麻痹对本次技能行动的阻止效果。
	 *
	 * 麻痹本身不会因为阻止一次行动而解除，也不消耗技能 PP。这里不修改成员快照，只把“本次行动被麻痹挡下”写入
	 * 事件流，方便 replay 和公开规则对照测试验证随机消费顺序。
	 */
	private fun consumeParalysisBlockedAction(state: BattleState, actor: BattleParticipant): BattleState =
		preventSkill(state = state, actor = actor, reason = SkillPreventionReason.PARALYSIS)

	/**
	 * 追加行动前技能阻止事件。
	 *
	 * 多种规则都会停在同一个事实：技能没有被使用、PP 没有消耗、后续命中/伤害/附加效果也不会发生。这个 helper
	 * 只构造共享事件；具体规则的状态递减、状态清除和随机消费仍留在各自函数里，避免把睡眠、休整、挑衅等不同
	 * 语义糊成一个大分支。
	 */
	private fun preventSkill(
		state: BattleState,
		actor: BattleParticipant,
		reason: SkillPreventionReason,
		skillId: Long? = null,
		previousSkillId: Long? = null,
		status: BattleVolatileStatus? = null,
		turnsRemainingBefore: Int? = null,
	): BattleState =
		state.appendEvent(
			BattleEvent.SkillPrevented(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				reason = reason,
				skillId = skillId,
				previousSkillId = previousSkillId,
				status = status,
				turnsRemainingBefore = turnsRemainingBefore,
			),
		)

	/**
	 * 判断麻痹是否阻止本次行动。
	 *
	 * 现代主系列规则为每次行动前 25% 概率无法行动。该判定发生在睡眠、冰冻、畏缩、混乱、回复封锁、挑衅、
	 * 定身法和无理取闹之后；因此如果前置状态已经阻止本次行动，就不会额外消费麻痹随机数。
	 */
	private fun paralysisBlocksMove(actor: BattleParticipant, random: BattleRandom): Boolean =
		chanceSucceeds(PARALYSIS_FULLY_PARALYZED_CHANCE_PERCENT, random, "paralysis chance for ${actor.actorId}")

	/**
	 * 处理混乱的行动前计数、解除、自伤和行动阻止。
	 *
	 * 混乱保存的是内部计数，而不是“还会自伤判定几次”。行动前先递减；递减到 0 表示成员恢复清醒并继续执行原
	 * 技能，不消费混乱概率随机数。递减后仍大于 0 时，消费 1 次 33/100 自伤判定；若自伤，再消费 1 次
	 * 85..100 伤害浮动并跳过原技能行动。
	 */
	private fun resolveConfusionBeforeMove(
		state: BattleState,
		actor: BattleParticipant,
		random: BattleRandom,
	): BattleBeforeMoveResult {
		val turnsRemainingBefore = actor.confusionTurnsRemaining
		val decremented = actor.decrementConfusionBeforeMove()
		val afterDecrement = state.replaceParticipant(decremented)
		if (decremented.confusionTurnsRemaining == 0) {
			return BattleBeforeMoveResult(
				state = afterDecrement.appendEvent(
					BattleEvent.VolatileStatusCleared(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						status = BattleVolatileStatus.CONFUSION,
					),
				),
				blocked = false,
			)
		}
		if (!chanceSucceeds(CONFUSION_SELF_DAMAGE_CHANCE_PERCENT, random, "confusion self-hit chance for ${actor.actorId}")) {
			return BattleBeforeMoveResult(state = afterDecrement, blocked = false)
		}
		val randomPercent = 85 + random.nextInt(16, "confusion damage random for ${actor.actorId}")
		val damage = confusionSelfDamage(decremented, randomPercent)
		val blockedEvent = BattleEvent.SkillPrevented(
			turnNumber = state.turnNumber,
			actorId = actor.actorId,
			reason = SkillPreventionReason.VOLATILE_STATUS,
			status = BattleVolatileStatus.CONFUSION,
		)
		val blockedState = afterDecrement.appendEvent(blockedEvent)
		if (decremented.hasIndirectDamageImmunity()) {
			return BattleBeforeMoveResult(state = blockedState, blocked = true)
		}
		val damaged = decremented.receiveDamage(damage)
		val afterDamage = afterDecrement
			.replaceParticipant(damaged)
			.appendEvents(
				listOf(
					blockedEvent,
					BattleEvent.ConfusionDamageApplied(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						amount = damage,
						randomPercent = randomPercent,
						turnsRemainingBefore = turnsRemainingBefore,
					),
				),
			)
		val afterLowHpItem = lowHpItemHealing(afterDamage, damaged.actorId)
		val latest = afterLowHpItem.participant(damaged.actorId) ?: damaged
		val afterFaint = afterLowHpItem.handleFaintAndResult(latest)
		return BattleBeforeMoveResult(state = afterFaint, blocked = true)
	}

	/**
	 * 计算混乱自伤。
	 *
	 * 混乱自伤按特殊的 40 威力物理伤害处理：使用攻击和防御能力阶级，带 85..100 随机浮动，但不套用属性一致、
	 * 属性克制、要害、道具和多数特性修正。这里独立实现公式，避免伪造普通技能后意外吃到普通伤害管线中的额外
	 * modifier。
	 */
	private fun confusionSelfDamage(actor: BattleParticipant, randomPercent: Int): Int {
		val attack = statStageModifiers.modifiedBattleStat(actor.attack, actor.statStage(BattleStat.ATTACK))
		val defense = statStageModifiers.modifiedBattleStat(actor.defense, actor.statStage(BattleStat.DEFENSE))
		require(defense > 0) { "confusion defending stat must be positive" }
		val levelFactor = (2 * actor.level) / 5 + 2
		val baseDamage = (((levelFactor * CONFUSION_BASE_POWER * attack) / defense) / 50) + 2
		return floor(baseDamage * (randomPercent / 100.0)).toInt().coerceAtLeast(1)
	}

	private companion object {
		private const val CONFUSION_BASE_POWER = 40
		private const val CONFUSION_SELF_DAMAGE_CHANCE_PERCENT = 33
		private const val FREEZE_THAW_CHANCE_PERCENT = 20
		private const val PARALYSIS_FULLY_PARALYZED_CHANCE_PERCENT = 25
	}
}

/**
 * 行动前状态阻止的返回值。
 *
 * [state] 是处理行动前状态后的最新战斗状态；[blocked] 表示本次技能行动是否已经被完全阻止。这个结果类型不携带
 * [BattleEngine] 的 `TurnContext`，因为行动前阶段只会推进战斗状态，不会修改本回合保护集合等临时编排字段。
 */
internal data class BattleBeforeMoveResult(
	val state: BattleState,
	val blocked: Boolean,
)
