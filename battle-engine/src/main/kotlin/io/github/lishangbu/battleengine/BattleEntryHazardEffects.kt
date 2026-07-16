package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 换入成员承受入场陷阱的阶段 resolver。
 *
 * 本类只负责“成员已经完成换入之后，所在侧已有入场陷阱如何影响这个成员”这一段规则。调用方必须先追加
 * [BattleEvent.ParticipantSwitched]，再调用 [applyOnSwitchIn]；这样事件流会稳定表现为“成员进入场地 -> 入场
 * 陷阱触发”。如果把换人事件和陷阱事件揉在主状态机里，后续新增强制替换、主动替换、濒死后换入等入口时，
 * 很容易出现某个入口漏结算陷阱或事件顺序不同的问题。
 *
 * 这里没有把主要异常免疫和低体力道具逻辑复制进来，而是通过构造参数接收两个回调：
 * - [majorStatusBlockReason] 使用状态结算器已经统一实现的属性、场地、特性、道具免疫判断，保证毒菱和技能中毒
 *   共享完全相同的阻止原因。
 * - [lowHpItemHealing] 使用伤害后结算器已经统一实现的低体力回复道具逻辑，保证入场伤害、技能伤害和回合末伤害后
 *   触发道具时拥有一致的事件顺序。
 *
 * 这个类刻意不是事件总线、不是规则插件，也不解析资料文本。现代规则里不同入场陷阱的语义差异很大：隐形岩
 * 读取属性克制倍率，撒菱读取层数和接地状态，毒菱可能被毒属性成员吸收，黏黏网改变速度阶级。显式 `when`
 * 比字符串策略或反射脚本更短、更容易用公开对照测试锁住行为。
 */
internal class BattleEntryHazardEffects(
	private val majorStatusBlockReason: (
		state: BattleState,
		actorId: String,
		recipient: BattleParticipant,
		status: BattleMajorStatus,
	) -> BattleStatusBlockReason?,
	private val lowHpItemHealing: (state: BattleState, actorId: String, random: BattleRandom?) -> BattleState,
) {
	/**
	 * 结算成员换入后所在侧已有的所有入场陷阱。
	 *
	 * 入场陷阱按 [BattleState] 中目标侧保存的顺序逐个结算。每处理一个陷阱之前都会重新读取成员当前快照，
	 * 因为前一个陷阱可能已经造成伤害、触发低体力道具、让成员倒下，甚至结束战斗。若战斗已经结束或成员已经
	 * 无法战斗，后续陷阱不再继续触发；这样可以避免倒下成员继续被附加异常状态或能力阶级变化。
	 *
	 * @param state 已经包含换人事件的战斗状态。
	 * @param sideId 换入成员所属侧 ID，用于读取该侧当前入场陷阱列表。
	 * @param actorId 刚刚换入的成员 ID。
	 * @return 结算完入场陷阱、低体力道具、倒下和胜负后的新状态。
	 */
	fun applyOnSwitchIn(
		state: BattleState,
		sideId: String,
		actorId: String,
		random: BattleRandom,
	): BattleState {
		val switchedIn = state.participant(actorId) ?: return state
		if (switchedIn.itemId != null && switchedIn.itemEffects.any { it is BattleItemEffect.EntryHazardImmunity }) {
			return state
		}
		val hazards = state.sides.firstOrNull { it.sideId == sideId }?.entryHazards.orEmpty()
		return hazards.fold(state) { current, hazard ->
			if (current.result != null) {
				current
			} else {
				val participant = current.participant(actorId) ?: return@fold current
				if (!participant.canBattle()) {
					current
				} else {
					applySingleHazard(current, sideId, participant, hazard, random)
				}
			}
		}
	}

	/**
	 * 分派单个入场陷阱的具体效果。
	 *
	 * 该函数是这个 resolver 中唯一的分派点。每个分支都保留一个独立私有函数，是为了让各类陷阱的公开规则、
	 * 事件、短路条件和伤害计算能就近注释，而不是塞进一个难以审计的大 `when` 分支。
	 */
	private fun applySingleHazard(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
		random: BattleRandom,
	): BattleState =
		when (hazard.kind) {
			BattleSideEntryHazardKind.STEALTH_ROCK -> applyStealthRockDamage(state, sideId, participant, hazard, random)
			BattleSideEntryHazardKind.SPIKES -> applySpikesDamage(state, sideId, participant, hazard, random)
			BattleSideEntryHazardKind.TOXIC_SPIKES -> applyToxicSpikesStatus(state, sideId, participant, hazard)
			BattleSideEntryHazardKind.STICKY_WEB -> applyStickyWebStatStage(state, sideId, participant, hazard)
		}

	/**
	 * 结算隐形岩类入场伤害。
	 *
	 * 现代规则按“岩属性攻击换入成员属性”的克制倍率计算最大 HP 的 1/8 倍伤害，并向下取整；只要倍率为正，
	 * 最少造成 1 点伤害。岩属性 ID 来自冻结的规则快照。如果快照没有提供岩属性 ID，就无法可靠计算克制倍率，
	 * 因此这里直接保持状态不变，避免在纯引擎中硬编码资料编号。
	 */
	private fun applyStealthRockDamage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
		random: BattleRandom,
	): BattleState {
		val rockElementId = state.rules.elementId("rock") ?: return state
		val effectiveness = state.rules.elementChart.multiplier(rockElementId, participant.elementIds)
		val damage = fractionDamage(participant.maxHp, effectiveness / STEALTH_ROCK_DAMAGE_DENOMINATOR)
		return applyDamage(
			state = state,
			sideId = sideId,
			participant = participant,
			hazard = hazard,
			amount = damage,
			effectiveness = effectiveness,
			random = random,
		)
	}

	/**
	 * 结算撒菱类入场伤害。
	 *
	 * 撒菱只影响接地成员。层数在模型层已经被限制到合法范围，因此这里使用清晰的 `when` 直接映射公开规则：
	 * 一层最大 HP 的 1/8，两层 1/6，三层 1/4。非接地成员不会产生事件，也不会触发低体力道具。
	 */
	private fun applySpikesDamage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
		random: BattleRandom,
	): BattleState {
		if (!participant.isEffectivelyGrounded()) {
			return state
		}
		val denominator = when (hazard.layers) {
			1 -> SPIKES_ONE_LAYER_DAMAGE_DENOMINATOR
			2 -> SPIKES_TWO_LAYER_DAMAGE_DENOMINATOR
			else -> SPIKES_THREE_LAYER_DAMAGE_DENOMINATOR
		}
		return applyDamage(
			state = state,
			sideId = sideId,
			participant = participant,
			hazard = hazard,
			amount = fractionAmount(participant.maxHp, numerator = 1, denominator = denominator),
			effectiveness = 1.0,
			random = random,
		)
	}

	/**
	 * 结算毒菱类入场效果。
	 *
	 * 毒菱只影响接地成员。接地且拥有毒属性的成员会吸收目标侧毒菱，并产生移除事件；其它接地成员在一层时
	 * 获得普通中毒，两层及以上获得剧毒。若成员已经有主要异常状态，则毒菱不刷新、不覆盖，也不追加事件。
	 *
	 * 具体免疫判断不在本类重复实现，而是委托给 [majorStatusBlockReason]。这样毒菱中毒会和普通技能附加中毒
	 * 共用同一套属性免疫、薄雾场地免疫、特性免疫、道具免疫规则，后续修正免疫顺序时只需要改一个地方。
	 */
	private fun applyToxicSpikesStatus(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState {
		if (!participant.isEffectivelyGrounded()) {
			return state
		}
		if (participant.hasElement(state.rules.elementId("poison"))) {
			return state.removeSideEntryHazard(sideId, BattleSideEntryHazardKind.TOXIC_SPIKES)
				?.appendEvent(
					BattleEvent.SideEntryHazardRemoved(
						turnNumber = state.turnNumber,
						actorId = participant.actorId,
						sideId = sideId,
						kind = BattleSideEntryHazardKind.TOXIC_SPIKES,
					),
				)
				?: state
		}
		if (participant.majorStatus != null) {
			return state
		}
		val status = if (hazard.layers >= 2) BattleMajorStatus.BAD_POISON else BattleMajorStatus.POISON
		val blockedReason = majorStatusBlockReason(state, participant.actorId, participant, status)
		if (blockedReason != null) {
			return state.appendEvent(
				BattleEvent.EntryHazardStatusApplicationBlocked(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					status = status,
					reason = blockedReason,
				),
			)
		}
		return state
			.replaceParticipant(participant.applyMajorStatus(status))
			.appendEvent(
				BattleEvent.EntryHazardStatusApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					status = status,
				),
			)
	}

	/**
	 * 结算黏黏网类入场效果。
	 *
	 * 黏黏网只影响接地成员，并在换入时降低速度能力阶级 1 级。能力阶级已经达到 -6 时，成员快照不会变化，
	 * 因此也不产生事件；这让 replay 中的 `EntryHazardStatStageChanged` 可以被理解为“速度阶级确实改变了”。
	 */
	private fun applyStickyWebStatStage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
	): BattleState {
		if (!participant.isEffectivelyGrounded()) {
			return state
		}
		if (participant.itemId != null && participant.itemEffects.any { it is BattleItemEffect.OpponentStatStageReductionImmunity }) {
			return state
		}
		val beforeStage = participant.statStage(BattleStat.SPEED)
		val updated = participant.changeStatStage(BattleStat.SPEED, -1)
		val afterStage = updated.statStage(BattleStat.SPEED)
		if (beforeStage == afterStage) {
			return state
		}
		return state
			.replaceParticipant(updated)
			.appendEvent(
				BattleEvent.EntryHazardStatStageChanged(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					stat = BattleStat.SPEED,
					delta = afterStage - beforeStage,
					currentStage = afterStage,
				),
			)
			.applyNegativeStatStageResetItem(participant.actorId)
	}

	/**
	 * 写入入场陷阱伤害并接续低体力道具、倒下和胜负判定。
	 *
	 * 入场伤害不是普通技能直接伤害，所以会被间接伤害免疫阻止；被阻止时不追加伤害事件，也不触发后续低体力
	 * 道具。真正造成 HP 变化后，流程会先追加 [BattleEvent.EntryHazardDamageApplied]，再调用 [lowHpItemHealing]
	 * 处理低体力回复类道具，最后按最新成员快照处理倒下和胜负。这个顺序与主状态机中其它间接伤害阶段保持一致。
	 */
	private fun applyDamage(
		state: BattleState,
		sideId: String,
		participant: BattleParticipant,
		hazard: BattleSideEntryHazard,
		amount: Int,
		effectiveness: Double,
		random: BattleRandom,
	): BattleState {
		if (amount <= 0 || participant.hasIndirectDamageImmunity()) {
			return state
		}
		val damaged = participant.receiveDamage(amount)
		val afterDamage = state
			.replaceParticipant(damaged)
			.appendEvent(
				BattleEvent.EntryHazardDamageApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					sideId = sideId,
					kind = hazard.kind,
					amount = amount,
					layers = hazard.layers,
					effectiveness = effectiveness,
				),
			)
		val afterLowHpItem = lowHpItemHealing(afterDamage, damaged.actorId, random)
		val latestAfterItem = afterLowHpItem.participant(damaged.actorId) ?: damaged
		return afterLowHpItem.handleFaintAndResult(latestAfterItem)
	}

	/**
	 * 计算最大 HP 比例型入场伤害。
	 *
	 * 公开规则中的比例伤害通常向下取整；当倍率为正但向下取整得到 0 时，仍至少造成 1 点伤害。倍率为 0 或
	 * 负数表示没有实际伤害，调用方会跳过伤害事件。
	 */
	private fun fractionDamage(maxHp: Int, fraction: Double): Int =
		if (fraction <= 0.0) {
			0
		} else {
			floor(maxHp * fraction).toInt().coerceAtLeast(1)
		}

	private companion object {
		private const val SPIKES_ONE_LAYER_DAMAGE_DENOMINATOR = 8
		private const val SPIKES_TWO_LAYER_DAMAGE_DENOMINATOR = 6
		private const val SPIKES_THREE_LAYER_DAMAGE_DENOMINATOR = 4
		private const val STEALTH_ROCK_DAMAGE_DENOMINATOR = 8.0
	}
}
