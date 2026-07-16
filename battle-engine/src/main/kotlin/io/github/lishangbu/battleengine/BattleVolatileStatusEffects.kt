package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 临时状态的附加与免疫结算器。
 *
 * 本类只处理畏缩、混乱、回复封锁、挑衅、定身法、无理取闹和束缚等不占用主要异常槽位的状态。它负责已有状态
 * 拒绝刷新、混乱和束缚持续时间随机数、定身法可禁用技能解析、临时状态写入事件，以及写入后立即触发的治愈
 * 道具。主要异常状态的属性和场地免疫由 [BattleMajorStatusEffects] 单独维护。
 *
 * 所有阻止判断都发生在混乱/束缚持续时间随机数消费前。这样薄雾场地、替身、特性免疫、道具免疫或已有状态阻止
 * 时，不会让 replay 随机脚本因为一次失败的临时状态附加而向前偏移。
 *
 * @property substituteBlocksOpponentEffect 判断目标替身是否会阻止对手技能效果。
 * @property skillIgnoresTargetAbilityEffects 判断本次技能是否无视目标侧防守特性。
 */
internal class BattleVolatileStatusEffects(
	private val substituteBlocksOpponentEffect: (BattleState, String, String, BattleSkillSlot) -> Boolean,
	private val skillIgnoresTargetAbilityEffects: (BattleState, String, String) -> Boolean,
) {
	private val statusCureEffects = BattleStatusCureEffects()

	/**
	 * 附加临时状态并处理状态私有计数。
	 *
	 * 畏缩只标记本回合行动前阻止；混乱成功时消费一个 `[0, 4)` 随机数并转成 2..5 的内部计数；回复封锁写入
	 * 固定 5 回合计数并在回合末递减；挑衅和定身法写入固定 3/4 回合计数；无理取闹写入离场清除的布尔状态；
	 * 束缚写入来源成员和 4..5 回合计数。
	 */
	fun applyVolatileStatus(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
		random: BattleRandom,
		randomReason: String,
		skill: BattleSkillSlot? = null,
	): BattleState {
		if (volatileStatusAlreadyPresent(recipient, status)) {
			return appendVolatileStatusBlockedEvent(
				state = state,
				actorId = actorId,
				targetActorId = recipient.actorId,
				status = status,
				reason = BattleStatusBlockReason.EXISTING_STATUS,
			)
		}
		val blockedReason = blockedVolatileStatusReason(state, actorId, recipient, status, skill)
		if (blockedReason != null) {
			return appendVolatileStatusBlockedEvent(
				state = state,
				actorId = actorId,
				targetActorId = recipient.actorId,
				status = status,
				reason = blockedReason,
			)
		}
		val disabledSkillId = if (status == BattleVolatileStatus.DISABLE) {
			disableTargetSkillId(recipient)
				?: return appendVolatileStatusBlockedEvent(
					state = state,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = BattleStatusBlockReason.NO_ELIGIBLE_SKILL,
				)
		} else {
			null
		}
		val confusionTurnsRemaining = if (status == BattleVolatileStatus.CONFUSION) {
			random.nextInt(4, randomReason) + 2
		} else {
			0
		}
		val healBlockTurnsRemaining = if (status == BattleVolatileStatus.HEAL_BLOCK) HEAL_BLOCK_TURNS else 0
		val tauntTurnsRemaining = if (status == BattleVolatileStatus.TAUNT) TAUNT_TURNS else 0
		val disabledSkillTurnsRemaining = if (status == BattleVolatileStatus.DISABLE) DISABLE_TURNS else 0
		val bindingTurnsRemaining = if (status == BattleVolatileStatus.BINDING) {
			state.participant(actorId)?.itemEffects
				?.filterIsInstance<BattleItemEffect.BindingDurationOverride>()
				?.firstOrNull()?.turns
				?: (random.nextInt(BINDING_TURN_SPAN, "binding duration for ${skill?.skillId ?: status.name}") + BINDING_MIN_TURNS)
		} else {
			0
		}
		val appliedState = state
			.replaceParticipant(
					recipient.applyVolatileStatus(
					status = status,
					confusionTurnsRemaining = confusionTurnsRemaining,
					healBlockTurnsRemaining = healBlockTurnsRemaining,
					tauntTurnsRemaining = tauntTurnsRemaining,
					disabledSkillId = disabledSkillId,
						disabledSkillTurnsRemaining = disabledSkillTurnsRemaining,
						infatuatedByActorId = if (status == BattleVolatileStatus.INFATUATION) actorId else null,
					boundByActorId = if (status == BattleVolatileStatus.BINDING) actorId else null,
					bindingTurnsRemaining = bindingTurnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.VolatileStatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
				),
			)
		val afterDisableEvent = if (disabledSkillId != null) {
			appliedState.appendEvent(
				BattleEvent.SkillDisabled(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					disabledSkillId = disabledSkillId,
					turnsRemaining = DISABLE_TURNS,
				),
			)
		} else {
			appliedState
		}
		val afterCure = statusCureEffects.applyVolatileStatusCureItem(afterDisableEvent, recipient.actorId, status)
		return if (status == BattleVolatileStatus.INFATUATION) {
			reflectInfatuation(afterCure, sourceActorId = actorId, holderActorId = recipient.actorId)
		} else {
			afterCure
		}
	}

	private fun reflectInfatuation(state: BattleState, sourceActorId: String, holderActorId: String): BattleState {
		if (sourceActorId == holderActorId) return state
		val holder = state.participant(holderActorId) ?: return state
		if (holder.itemEffects.none { it is BattleItemEffect.InfatuationReflectToSource }) return state
		val source = state.participant(sourceActorId) ?: return state
		if (!source.canBattle() || source.infatuatedByActorId != null) return state
		val reflected = source.applyVolatileStatus(
			status = BattleVolatileStatus.INFATUATION,
			infatuatedByActorId = holderActorId,
		)
		val applied = state.replaceParticipant(reflected).appendEvent(
			BattleEvent.VolatileStatusApplied(
				turnNumber = state.turnNumber,
				actorId = holderActorId,
				targetActorId = sourceActorId,
				status = BattleVolatileStatus.INFATUATION,
			),
		)
		return statusCureEffects.applyVolatileStatusCureItem(applied, sourceActorId, BattleVolatileStatus.INFATUATION)
	}

	/**
	 * 追加临时状态附加失败事件。
	 *
	 * 已有状态、场地、替身、特性、道具和“没有可禁用技能”都会停在同一种可观察事实：临时状态没有写入目标。
	 * 集中构造事件可以避免每个前置短路分支各自手写一份事件字段，后续新增阻止原因时也更不容易漏字段。
	 */
	private fun appendVolatileStatusBlockedEvent(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		status: BattleVolatileStatus,
		reason: BattleStatusBlockReason,
	): BattleState =
		state.appendEvent(
			BattleEvent.VolatileStatusApplicationBlocked(
				turnNumber = state.turnNumber,
				actorId = actorId,
				targetActorId = targetActorId,
				status = status,
				reason = reason,
			),
		)

	/**
	 * 解析定身法可以禁用的目标技能。
	 *
	 * 现代规则中定身法读取目标最近一次成功使用的技能；如果目标还没有成功使用过技能，或者该技能已经没有 PP，
	 * 定身法不会写入状态。这里不按名称判断具体技能，资料层只需把对应技能映射为 [BattleVolatileStatus.DISABLE]。
	 */
	private fun disableTargetSkillId(recipient: BattleParticipant): Long? {
		val skillId = recipient.lastSuccessfulSkillId ?: return null
		val slot = recipient.skillSlot(skillId) ?: return null
		return if (slot.remainingPp > 0) skillId else null
	}

	/**
	 * 判断目标是否已经拥有同类临时状态。
	 *
	 * 当前混乱、回复封锁、挑衅、定身法、无理取闹和束缚需要拒绝刷新。畏缩可以被多次尝试，但运行态只保存一个
	 * 布尔值，后续行动前或回合末都会清除，所以重复附加不会改变可观察持续时间。
	 */
	private fun volatileStatusAlreadyPresent(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		when (status) {
			BattleVolatileStatus.CONFUSION -> recipient.confusionTurnsRemaining > 0
			BattleVolatileStatus.HEAL_BLOCK -> recipient.healBlockTurnsRemaining > 0
			BattleVolatileStatus.TAUNT -> recipient.tauntTurnsRemaining > 0
			BattleVolatileStatus.DISABLE -> recipient.disabledSkillTurnsRemaining > 0
			BattleVolatileStatus.TORMENT -> recipient.tormented
			BattleVolatileStatus.INFATUATION -> recipient.infatuatedByActorId != null
			BattleVolatileStatus.BINDING -> recipient.bindingTurnsRemaining > 0
			BattleVolatileStatus.FLINCH -> false
		}

	/**
	 * 判断临时状态是否会在附加前被稳定免疫规则阻止。
	 *
	 * 薄雾场地只阻止接地成员获得混乱；替身只在技能来自对手且技能不是声音类时生效；特性和道具的结构化免疫
	 * 可以阻止资料层声明的任意临时状态。所有阻止判断都发生在混乱和束缚持续时间随机数消费前。
	 */
	private fun blockedVolatileStatusReason(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
		skill: BattleSkillSlot? = null,
	): BattleStatusBlockReason? =
		when {
			volatileStatusBlockedByTerrain(state, recipient, status) -> BattleStatusBlockReason.TERRAIN
			skill != null && substituteBlocksOpponentEffect(state, actorId, recipient.actorId, skill) ->
				BattleStatusBlockReason.SUBSTITUTE
			volatileStatusBlockedBySideProtection(state, actorId, recipient, status) -> BattleStatusBlockReason.SIDE_PROTECTION
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				volatileStatusBlockedByAbility(state, actorId, recipient, status) -> BattleStatusBlockReason.ABILITY
			volatileStatusBlockedByItem(recipient, status) -> BattleStatusBlockReason.ITEM
			else -> null
		}

	/**
	 * 判断当前场地是否阻止目标获得临时状态。
	 *
	 * 现代薄雾场地只阻止当前上场、接地成员获得混乱；它不会阻止畏缩、挑衅、定身法、回复封锁、无理取闹或束缚。
	 */
	private fun volatileStatusBlockedByTerrain(
		state: BattleState,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
	): Boolean =
		state.isActive(recipient.actorId) &&
			recipient.isEffectivelyGrounded() &&
			state.environment.terrain == BattleTerrain.MISTY &&
			status == BattleVolatileStatus.CONFUSION

	/**
	 * 判断目标所属侧的神秘守护类防护是否阻止本次临时状态。
	 *
	 * 现代神秘守护主要和混乱一起处理；畏缩、挑衅、回复封锁、定身法、无理取闹和束缚不是它负责的状态族。
	 * 和主要异常一样，自身给自身写入的状态不被拦截，避免防护状态干扰自我代价类规则。
	 */
	private fun volatileStatusBlockedBySideProtection(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
	): Boolean =
		actorId != recipient.actorId &&
			status == BattleVolatileStatus.CONFUSION &&
			state.sideHasProtection(recipient.actorId, BattleSideProtectionKind.STATUS_CONDITION)

	/**
	 * 判断目标特性是否稳定免疫指定临时状态。
	 *
	 * 该函数只读取目标身上的结构化免疫效果；是否因为攻击方特性而忽略目标特性，由调用方在进入该谓词前处理。
	 */
	private fun volatileStatusBlockedByAbility(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleVolatileStatus,
	): Boolean {
		if (recipient.abilityEffects.any { effect ->
				effect is BattleAbilityEffect.VolatileStatusImmunity && status in effect.statuses
			}) return true
		val actorSideId = state.sideOf(actorId)?.sideId ?: return false
		val recipientSide = state.sideOf(recipient.actorId) ?: return false
		if (actorSideId == recipientSide.sideId) return false
		return recipientSide.activeParticipants().any { holder ->
			holder.canBattle() && holder.abilityEffects
				.filterIsInstance<BattleAbilityEffect.SideVolatileStatusImmunity>()
				.any { status in it.statuses }
		}
	}

	/**
	 * 判断目标携带道具是否稳定免疫指定临时状态。
	 *
	 * 与主要异常状态道具免疫一样，这里不处理道具消费或临时失效；成员快照中的道具效果已经代表当前有效规则。
	 */
	private fun volatileStatusBlockedByItem(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		recipient.itemEffects.any { effect ->
			effect is BattleItemEffect.VolatileStatusImmunity && status in effect.statuses
		}

	private companion object {
		/**
		 * 定身法写入后的固定持续回合数。
		 */
		private const val DISABLE_TURNS = 4

		/**
		 * 束缚类状态最短持续回合数。
		 */
		private const val BINDING_MIN_TURNS = 4

		/**
		 * 束缚持续时间随机跨度；`nextInt(2) + 4` 产生 4..5 回合。
		 */
		private const val BINDING_TURN_SPAN = 2

		/**
		 * 回复封锁写入后的固定持续回合数。
		 */
		private const val HEAL_BLOCK_TURNS = 5

		/**
		 * 挑衅写入后的固定持续回合数。
		 */
		private const val TAUNT_TURNS = 3
	}
}
