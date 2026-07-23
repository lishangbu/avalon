package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 主要异常状态的附加与免疫结算器。
 *
 * 本类只处理灼伤、麻痹、睡眠、冰冻、中毒和剧毒这些会占用成员主要异常槽位的状态。它负责判断状态是否能写入
 * 目标运行态、成功写入后的睡眠持续回合随机数，以及状态写入后是否立即被携带道具治愈。临时状态有完全不同的
 * 持续字段和阻止规则，已经由 [BattleVolatileStatusEffects] 单独维护。
 *
 * 阻止顺序是这里最重要的不变量：已有主要异常、属性免疫、场地免疫、替身、特性免疫、道具免疫都必须发生在
 * 睡眠持续时间随机数之前。这样失败分支不会污染 replay 随机脚本，事件里的阻止原因也能稳定指向最先命中的
 * 规则来源，而不是被后续同样成立的免疫遮蔽。
 *
 * 替身阻挡和“攻击方是否无视目标特性”仍通过回调传入。它们不是主要异常状态专属规则，普通伤害、固定伤害、
 * 接触特性和临时状态附加都会读取同一类判断；通过回调复用主防守 resolver，可以避免本类复制一份近似逻辑。
 *
 * @property substituteBlocksOpponentEffect 判断目标替身是否会阻止对手技能效果。
 * @property skillIgnoresTargetAbilityEffects 判断本次技能是否无视目标侧防守特性。
 */
internal class BattleMajorStatusEffects(
	private val substituteBlocksOpponentEffect: (BattleState, String, String, BattleSkillSlot) -> Boolean,
	private val skillIgnoresTargetAbilityEffects: (BattleState, String, String) -> Boolean,
	private val applyVolatileStatus: (
		BattleState,
		String,
		BattleParticipant,
		BattleVolatileStatus,
		BattleRandom,
		String,
	) -> BattleState,
) {
	private val statusCureEffects = BattleStatusCureEffects()

	/**
	 * 附加主要异常状态并处理现代属性免疫、接地场地免疫、替身、特性/道具免疫和状态私有计数。
	 *
	 * 调用方应已经完成技能命中、概率触发和目标是否仍可战斗的前置判断。函数内部只维护状态写入阶段的不变量：
	 * 已有主要异常状态会拒绝刷新；睡眠成功时消费一个 `[0, 3)` 随机数并转成 1..3 次行动前检查；其它主要异常
	 * 状态不消费持续时间随机数。任何阻止原因都会在随机数消费前短路，保证 replay 随机脚本稳定。
	 */
	fun applyMajorStatus(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		random: BattleRandom,
		randomReason: String,
		skill: BattleSkillSlot? = null,
		allowReflection: Boolean = true,
	): BattleState {
		val blockedReason = if (recipient.majorStatus != null) {
			BattleStatusBlockReason.EXISTING_STATUS
		} else {
			blockedMajorStatusReason(state, actorId, recipient, status, skill)
		}
		if (blockedReason != null) {
			return appendMajorStatusBlockedEvent(
				state = state,
				actorId = actorId,
				targetActorId = recipient.actorId,
				status = status,
				reason = blockedReason,
			)
		}
		val sleepTurnsRemaining = if (status == BattleMajorStatus.SLEEP) {
			val rolledTurns = random.nextInt(3, randomReason) + 1
			val divisor = recipient.abilityEffects.filterIsInstance<BattleAbilityEffect.SleepDurationDivisor>()
				.fold(1) { current, effect -> current * effect.divisor }
			(rolledTurns / divisor).coerceAtLeast(1)
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
		val afterPoisonConfusion = if (
			status in setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON) &&
			state.sideOf(actorId)?.sideId != state.sideOf(recipient.actorId)?.sideId &&
			state.participant(actorId)?.abilityEffects?.any {
				it is BattleAbilityEffect.PoisonApplicationConfusion
			} == true
		) {
			applyVolatileStatus(
				appliedState,
				actorId,
				requireNotNull(appliedState.participant(recipient.actorId)),
				BattleVolatileStatus.CONFUSION,
				random,
				"poison application confusion for $actorId",
			)
		} else {
			appliedState
		}
		val reflectedState = if (
			allowReflection &&
			actorId != recipient.actorId &&
			status in REFLECTABLE_STATUSES &&
			state.sideOf(actorId)?.sideId != state.sideOf(recipient.actorId)?.sideId &&
			recipient.abilityEffects.any { it is BattleAbilityEffect.OpponentMajorStatusReflection } &&
			(skill == null || !skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId))
		) {
			val source = afterPoisonConfusion.participant(actorId)
			if (source != null && source.canBattle()) {
				applyMajorStatus(
					state = afterPoisonConfusion,
					actorId = recipient.actorId,
					recipient = source,
					status = status,
					random = random,
					randomReason = "major-status-reflection:${recipient.actorId}",
					allowReflection = false,
				)
			} else {
				afterPoisonConfusion
			}
		} else {
			afterPoisonConfusion
		}
		return statusCureEffects.applyMajorStatusCureItem(reflectedState, recipient.actorId)
	}

	private companion object {
		private val REFLECTABLE_STATUSES = setOf(
			BattleMajorStatus.BURN,
			BattleMajorStatus.PARALYSIS,
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON,
		)
	}

	/**
	 * 判断主要异常状态是否会在附加前被稳定免疫规则阻止。
	 *
	 * 该方法公开给入场陷阱 resolver 使用，因为毒菱类入场效果和技能附加状态共享同一套属性、场地、特性、
	 * 道具免疫判断。顺序保持为属性、场地、替身、特性、道具：如果目标属性已经天然免疫，就不再把原因归给
	 * 场地或道具；如果技能传入且被替身阻挡，也不会继续读取特性/道具。
	 */
	fun blockedMajorStatusReason(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
		skill: BattleSkillSlot? = null,
	): BattleStatusBlockReason? =
		when {
			statusBlockedByElement(state.rules, recipient, status) && !poisonElementImmunityBypassed(state, actorId, status) ->
				BattleStatusBlockReason.ELEMENT
			statusBlockedByTerrain(state, recipient, status) -> BattleStatusBlockReason.TERRAIN
			skill != null && substituteBlocksOpponentEffect(state, actorId, recipient.actorId, skill) ->
				BattleStatusBlockReason.SUBSTITUTE
			statusBlockedBySideProtection(state, actorId, recipient) -> BattleStatusBlockReason.SIDE_PROTECTION
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				statusBlockedByAbility(state, recipient, status) -> BattleStatusBlockReason.ABILITY
			!skillIgnoresTargetAbilityEffects(state, actorId, recipient.actorId) &&
				statusBlockedBySideAbility(state, recipient, status) -> BattleStatusBlockReason.ABILITY
			statusBlockedByItem(recipient, status) -> BattleStatusBlockReason.ITEM
			else -> null
		}

	/**
	 * 追加主要异常状态附加失败事件。
	 *
	 * 主要异常和临时状态的阻止事件字段非常接近，但事件类型不同。把事件构造集中到一个小函数里，可以让主流程只
	 * 保留“先判定阻止原因，再决定是否写入状态”的规则结构。
	 */
	private fun appendMajorStatusBlockedEvent(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		status: BattleMajorStatus,
		reason: BattleStatusBlockReason,
	): BattleState =
		state.appendEvent(
			BattleEvent.StatusApplicationBlocked(
				turnNumber = state.turnNumber,
				actorId = actorId,
				targetActorId = targetActorId,
				status = status,
				reason = reason,
			),
		)

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
			BattleMajorStatus.BURN -> recipient.hasElement(rules.elementId("fire"))
			BattleMajorStatus.PARALYSIS -> recipient.hasElement(rules.elementId("electric"))
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON -> recipient.hasElement(rules.elementId("poison")) || recipient.hasElement(rules.elementId("steel"))
			BattleMajorStatus.FREEZE -> recipient.hasElement(rules.elementId("ice"))
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
		if (!state.isActive(recipient.actorId) || !recipient.isEffectivelyGrounded()) {
			return false
		}
		return when (state.environment.terrain) {
			BattleTerrain.ELECTRIC -> status == BattleMajorStatus.SLEEP
			BattleTerrain.MISTY -> true
			else -> false
		}
	}

	/**
	 * 判断目标所属侧的神秘守护类防护是否阻止本次主要异常状态。
	 *
	 * 一侧防护只阻止“其它成员”附加的状态，不阻止成员自身技能把状态写给自己；这样后续睡觉、自我异常代价等规则
	 * 不会被己方神秘守护误拦。该判断放在替身之后、特性/道具之前，是为了让已经被替身明确挡下的对手技能仍报告
	 * 替身原因，而不是被同样存在的一侧防护遮蔽。
	 */
	private fun statusBlockedBySideProtection(
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
	): Boolean =
		actorId != recipient.actorId &&
			state.sideHasProtection(recipient.actorId, BattleSideProtectionKind.STATUS_CONDITION)

	/**
	 * 判断目标特性是否稳定免疫指定主要异常状态。
	 *
	 * 调用方已经判断本次技能是否无视目标特性；这里只读取目标当前有效的结构化特性效果，避免把攻击方上下文混入
	 * 纯谓词中。
	 */
	private fun statusBlockedByAbility(state: BattleState, recipient: BattleParticipant, status: BattleMajorStatus): Boolean =
		recipient.abilityEffects.any { effect ->
			effect is BattleAbilityEffect.AlwaysTreatedAsleep || (
				effect is BattleAbilityEffect.MajorStatusImmunity && status in effect.statuses &&
					(effect.requiredWeather == null || effect.requiredWeather == state.effectiveWeatherFor(recipient))
			) || (
				effect is BattleAbilityEffect.EndTurnHpFormChange &&
					effect.majorStatusImmuneFormCodes.any { code ->
						recipient.battleFormProfiles[code]?.creatureId == recipient.creatureId
					}
			)
		}

	private fun poisonElementImmunityBypassed(
		state: BattleState,
		actorId: String,
		status: BattleMajorStatus,
	): Boolean =
		status in setOf(BattleMajorStatus.POISON, BattleMajorStatus.BAD_POISON) &&
			state.participant(actorId)?.abilityEffects?.any { it is BattleAbilityEffect.PoisonElementStatusBypass } == true

	private fun statusBlockedBySideAbility(
		state: BattleState,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	): Boolean =
		state.sideOf(recipient.actorId)?.activeParticipants()?.any { holder ->
			holder.canBattle() && (
				holder.abilityEffects.filterIsInstance<BattleAbilityEffect.SideMajorStatusImmunity>()
					.any { status in it.statuses } ||
					holder.abilityEffects.filterIsInstance<BattleAbilityEffect.SideElementMajorStatusImmunity>()
					.any { status in it.statuses && recipient.hasElement(it.elementId) }
			)
		} == true

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
}
