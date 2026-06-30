package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 主要异常状态与临时状态的附加结算器。
 *
 * 这个类只处理“状态能否写入目标运行态、写入后是否立刻被道具治愈、以及相关事件如何追加”。它不负责命中率、
 * 技能目标选择、概率触发、伤害、倒下或回合末持续时间推进；这些阶段仍由主状态机或其它 resolver 明确调用。
 *
 * 抽出本类的目的很窄：让 [BattleEngine] 不再同时承担当回合技能编排、伤害、状态免疫和道具治愈四类职责。
 * 状态规则本身仍然是直接函数调用，不做事件总线，也不做“按状态枚举动态分发处理器”。原因是状态附加最重要的
 * 不是可插拔，而是阻止顺序和随机数消费顺序稳定：
 * - 已有状态、属性免疫、场地免疫、替身、特性免疫、道具免疫都必须发生在睡眠/混乱持续时间随机数之前。
 * - 成功写入状态后，道具治愈事件必须紧跟状态写入事件。
 * - 定身法只有找到仍有 PP 的最近成功技能时才写入状态，否则产生明确阻止原因。
 *
 * 替身阻挡和“攻击方是否无视目标特性”仍通过回调从主引擎传入。它们不是状态专属规则：普通伤害、固定伤害、
 * 接触特性和状态附加都会读取同样的判断。把共享判断留在主引擎，可以避免本类和伤害流程各自维护一份近似逻辑。
 *
 * @property substituteBlocksOpponentEffect 判断目标替身是否会阻止对手技能效果。
 * @property skillIgnoresTargetAbilityEffects 判断本次技能是否无视目标侧防守特性。
 */
internal class BattleStatusEffects(
	private val substituteBlocksOpponentEffect: (BattleState, String, String, BattleSkillSlot) -> Boolean,
	private val skillIgnoresTargetAbilityEffects: (BattleState, String, String) -> Boolean,
) {
	/**
	 * 附加主要异常状态并处理现代属性免疫、接地场地免疫、替身、特性/道具免疫和状态私有计数。
	 *
	 * 该函数假定调用方已经完成技能命中、概率触发和目标是否仍可战斗的前置判断。函数内部只维护状态写入阶段的
	 * 不变量：已有主要异常状态会拒绝刷新；睡眠成功时消费一个 `[0, 3)` 随机数并转成 1..3 次行动前检查；其它
	 * 主要异常状态不消费持续时间随机数。任何阻止原因都会在随机数消费前短路，保证 replay 随机脚本稳定。
	 */
	fun applyMajorStatus(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		random: BattleRandom,
		randomReason: String,
		skill: BattleSkillSlot? = null,
	): BattleState {
		val blockedReason = if (recipient.majorStatus != null) {
			BattleStatusBlockReason.EXISTING_STATUS
		} else {
			blockedMajorStatusReason(state, actorId, recipient, status, skill)
		}
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.StatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = blockedReason,
				),
			)
		}
		val sleepTurnsRemaining = if (status == BattleMajorStatus.SLEEP) {
			random.nextInt(3, randomReason) + 1
		} else {
			0
		}
		val appliedState = state
			.replaceParticipant(recipient.applyMajorStatus(status, sleepTurnsRemaining))
			.appendEvent(
				BattleEvent.StatusApplied(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
				),
			)
		return applyMajorStatusCureItem(appliedState, recipient.actorId)
	}

	/**
	 * 判断主要异常状态是否会在附加前被稳定免疫规则阻止。
	 *
	 * 该方法公开给入场陷阱 resolver 使用，因为毒菱类入场效果和技能附加状态共享同一套属性、场地、特性、
	 * 道具免疫判断。顺序保持为属性、场地、替身、特性、道具：如果目标属性已经天然免疫，就不再把原因归给
	 * 场地或道具；如果技能传入且被替身阻挡，也不会继续读取特性/道具。这种顺序让测试断言能定位真正先命中的
	 * 规则来源，而不是被后续同样成立的免疫遮蔽。
	 */
	fun blockedMajorStatusReason(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		skill: BattleSkillSlot? = null,
	): BattleStatusBlockReason? =
		when {
			statusBlockedByElement(state.rules, recipient, status) -> BattleStatusBlockReason.ELEMENT
			statusBlockedByTerrain(state, recipient, status) -> BattleStatusBlockReason.TERRAIN
			skill != null && substituteBlocksOpponentEffect(state, actorId, recipient.actorId, skill) ->
				BattleStatusBlockReason.SUBSTITUTE
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				statusBlockedByAbility(recipient, status) -> BattleStatusBlockReason.ABILITY
			statusBlockedByItem(recipient, status) -> BattleStatusBlockReason.ITEM
			else -> null
		}

	/**
	 * 附加临时状态并处理状态私有计数。
	 *
	 * 畏缩只标记本回合行动前阻止；混乱成功时消费一个 `[0, 4)` 随机数并转成 2..5 的内部计数；回复封锁写入
	 * 固定 5 回合计数并在回合末递减；挑衅和定身法写入固定 3/4 回合计数；无理取闹写入离场清除的布尔状态；
	 * 束缚写入来源成员和 4..5 回合计数。
	 *
	 * 已有可持续临时状态不会刷新旧持续回合，会产生 [BattleStatusBlockReason.EXISTING_STATUS]。场地、替身、
	 * 特性或道具免疫会在混乱/束缚持续时间随机数消费前短路，避免“没有成功写入状态”的分支污染随机脚本。
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
			return state.appendEvent(
				BattleEvent.VolatileStatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = BattleStatusBlockReason.EXISTING_STATUS,
				),
			)
		}
		val blockedReason = blockedVolatileStatusReason(state, actorId, recipient, status, skill)
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.VolatileStatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = recipient.actorId,
					status = status,
					reason = blockedReason,
				),
			)
		}
		val disabledSkillId = if (status == BattleVolatileStatus.DISABLE) {
			disableTargetSkillId(recipient)
				?: return state.appendEvent(
					BattleEvent.VolatileStatusApplicationBlocked(
						turnNumber = state.turnNumber,
						actorId = actorId,
						targetActorId = recipient.actorId,
						status = status,
						reason = BattleStatusBlockReason.NO_ELIGIBLE_SKILL,
					),
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
			random.nextInt(BINDING_TURN_SPAN, "binding duration for ${skill?.skillId ?: status.name}") + BINDING_MIN_TURNS
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
		return applyVolatileStatusCureItem(afterDisableEvent, recipient.actorId, status)
	}

	/**
	 * 处理成功获得主要异常状态后的即时治愈携带道具。
	 *
	 * 该阶段只在 [applyMajorStatus] 已经写入状态并追加 [BattleEvent.StatusApplied] 后运行，因此它不会和属性、
	 * 场地、替身、特性或道具免疫混淆。触发成功时先清除主要异常状态及其附属计数，再按效果声明消费携带道具，
	 * 最后追加 [BattleEvent.StatusCleared]。事件流因此稳定呈现“状态写入 -> 道具治愈”的顺序。
	 */
	private fun applyMajorStatusCureItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		val status = participant.majorStatus ?: return state
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.MajorStatusCure>()
			.firstOrNull { status in it.statuses }
			?: return state
		val cured = if (effect.consumesItem) {
			participant.clearMajorStatus().consumeHeldItem()
		} else {
			participant.clearMajorStatus()
		}
		return state
			.replaceParticipant(cured)
			.appendEvent(
				BattleEvent.StatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
	}

	/**
	 * 判断目标属性是否天然免疫指定主要异常状态。
	 *
	 * 当前覆盖现代主系列最稳定的类型免疫：火属性免疫灼伤，电属性免疫麻痹，毒/钢属性免疫中毒和剧毒，冰属性
	 * 免疫冰冻。睡眠没有通用属性免疫，因此返回 false。属性 ID 来自 [BattleRuleSnapshot]，避免在引擎里硬编码
	 * 资料库编号。
	 */
	private fun statusBlockedByElement(
		rules: BattleRuleSnapshot,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	): Boolean =
		when (status) {
			BattleMajorStatus.BURN -> recipient.hasElement(rules.fireElementId)
			BattleMajorStatus.PARALYSIS -> recipient.hasElement(rules.electricElementId)
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON -> recipient.hasElement(rules.poisonElementId) || recipient.hasElement(rules.steelElementId)
			BattleMajorStatus.FREEZE -> recipient.hasElement(rules.iceElementId)
			BattleMajorStatus.SLEEP -> false
		}

	/**
	 * 判断当前场地是否阻止目标获得主要异常状态。
	 *
	 * 现代场地免疫只影响当前上场且接地的成员。电气场地阻止睡眠；薄雾场地阻止所有主要异常状态。成员是否接地
	 * 已经显式进入运行态，因此飞行、漂浮、携带道具等来源应在进入引擎前折算到 [BattleParticipant.grounded]。
	 */
	private fun statusBlockedByTerrain(
		state: BattleState,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	): Boolean {
		if (!state.isActive(recipient.actorId) || !recipient.grounded) {
			return false
		}
		return when (state.environment.terrain) {
			BattleTerrain.ELECTRIC -> status == BattleMajorStatus.SLEEP
			BattleTerrain.MISTY -> true
			else -> false
		}
	}

	/**
	 * 判断目标特性是否稳定免疫指定主要异常状态。
	 *
	 * 调用方已经判断本次技能是否无视目标特性；这里只读取目标当前有效的结构化特性效果，避免把攻击方上下文混入
	 * 纯谓词中。
	 */
	private fun statusBlockedByAbility(recipient: BattleParticipant, status: BattleMajorStatus): Boolean =
		recipient.abilityEffects.any { effect ->
			effect is BattleAbilityEffect.MajorStatusImmunity && status in effect.statuses
		}

	/**
	 * 判断目标携带道具是否稳定免疫指定主要异常状态。
	 *
	 * 道具是否会被消耗、失效或被其它规则临时压制不在这里处理；本函数只读取成员快照中已经生效的
	 * [BattleItemEffect.MajorStatusImmunity]。
	 */
	private fun statusBlockedByItem(recipient: BattleParticipant, status: BattleMajorStatus): Boolean =
		recipient.itemEffects.any { effect ->
			effect is BattleItemEffect.MajorStatusImmunity && status in effect.statuses
		}

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
			BattleVolatileStatus.BINDING -> recipient.bindingTurnsRemaining > 0
			BattleVolatileStatus.FLINCH -> false
		}

	/**
	 * 处理成功获得临时状态后的即时治愈携带道具。
	 *
	 * 该阶段只在 [applyVolatileStatus] 已经写入临时状态并追加 [BattleEvent.VolatileStatusApplied] 后运行，因此不会
	 * 遮蔽薄雾场地、替身、特性免疫、道具免疫或已有混乱的前置阻止语义。触发成功时，函数先清除目标临时状态，
	 * 再按 [BattleItemEffect.VolatileStatusCure.consumesItem] 决定是否消费携带道具，最后追加
	 * [BattleEvent.VolatileStatusCleared]。
	 */
	private fun applyVolatileStatusCureItem(
		state: BattleState,
		actorId: String,
		status: BattleVolatileStatus,
	): BattleState {
		val participant = state.participant(actorId) ?: return state
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.VolatileStatusCure>()
			.firstOrNull { status in it.statuses }
			?: return state
		val cured = if (effect.consumesItem) {
			participant.clearVolatileStatus(status).consumeHeldItem()
		} else {
			participant.clearVolatileStatus(status)
		}
		return state
			.replaceParticipant(cured)
			.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = status,
				),
			)
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
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				volatileStatusBlockedByAbility(recipient, status) -> BattleStatusBlockReason.ABILITY
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
			recipient.grounded &&
			state.environment.terrain == BattleTerrain.MISTY &&
			status == BattleVolatileStatus.CONFUSION

	/**
	 * 判断目标特性是否稳定免疫指定临时状态。
	 *
	 * 该函数只读取目标身上的结构化免疫效果；是否因为攻击方特性而忽略目标特性，由调用方在进入该谓词前处理。
	 */
	private fun volatileStatusBlockedByAbility(recipient: BattleParticipant, status: BattleVolatileStatus): Boolean =
		recipient.abilityEffects.any { effect ->
			effect is BattleAbilityEffect.VolatileStatusImmunity && status in effect.statuses
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
