package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 回合末回复阶段结算器。
 *
 * 回合末大流程中，扣血阶段会触发低体力道具、倒下检查和胜负收口；回复阶段只增加 HP，不会让成员倒下，也不会
 * 主动结束战斗。这个类只承接“天气回复 -> 场地回复 -> 携带道具回复”三段加血逻辑，让 [BattleEndTurnEffects]
 * 保持对扣血顺序和战斗结束短路的控制。
 *
 * 三种回复来源共享两个不变量：
 * - 结算前必须重新读取最新成员快照，避免同一成员连续多个回复效果时使用过期 HP。
 * - 回复量按最大 HP 固定分母计算，至少 1 点，但不能超过当前缺失 HP；满血、倒下或回复封锁的成员不会产生回复
 *   事件。
 *
 * 低体力一次性回复道具不在这里处理。那类道具必须紧贴“受到伤害后”触发，因此仍由伤害阶段通过统一的
 * `lowHpItemHealing` 回调处理，避免普通伤害、混乱自伤、入场陷阱和回合末扣血出现四套不同事件顺序。
 */
internal class BattleEndTurnHealingEffects {
	/**
	 * 处理天气阶段的特性回复。
	 *
	 * 现代规则中，部分特性会在指定天气存在时于回合末按最大 HP 固定比例回复。该阶段放在天气伤害之后、场地回复
	 * 之前，保持事件流语义清晰：天气先造成或免除伤害，再处理同属天气阶段的回复，最后进入场地和携带道具。
	 * 每个回复效果都会重新读取最新成员快照，避免同一成员多个回复效果连续结算时使用过期 HP。
	 */
	fun applyWeatherHealing(state: BattleState): BattleState {
		if (state.weatherEffectsSuppressed()) return state
		val weather = state.environment.weather
		if (weather == BattleWeather.NONE) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) weatherHealing@ { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@weatherHealing current
				if (!latest.canReceiveHealing()) {
					return@weatherHealing current
				}
				val healEffects = latest.abilityEffects
					.filterIsInstance<BattleAbilityEffect.WeatherEndTurnHeal>()
					.filter { weather in it.weathers }
				healEffects.fold(current) effectHealing@ { healingState, effect ->
					val currentParticipant = healingState.participant(latest.actorId) ?: return@effectHealing healingState
					if (!currentParticipant.canReceiveHealing()) {
						return@effectHealing healingState
					}
					val healAmount = currentParticipant.endTurnHealAmount(effect.healDenominator)
					healingState.applyEndTurnHealingResult(
						participant = currentParticipant,
						healAmount = healAmount,
						event = BattleEvent.WeatherHealingApplied(
							turnNumber = healingState.turnNumber,
							actorId = currentParticipant.actorId,
							weather = weather,
							amount = healAmount,
						),
					)
				}
			}
	}

	/**
	 * 处理回合末场地回复。
	 *
	 * 当前只有青草场地会在回合末提供固定比例回复，并且只作用于当前上场、仍可战斗、未满血、未被回复封锁且接地
	 * 的成员。飞行、漂浮、携带道具免疫地面场地等来源应在进入引擎前折算为成员的 `grounded=false`；本阶段只
	 * 读取已经冻结的运行态，不再尝试根据资料名称推断接地状态。
	 */
	fun applyTerrainHealing(state: BattleState): BattleState {
		if (state.environment.terrain != BattleTerrain.GRASSY) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) terrainHealing@ { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@terrainHealing current
				if (!latest.isEffectivelyGrounded() || !latest.canReceiveHealing()) {
					return@terrainHealing current
				}
				val healAmount = latest.endTurnHealAmount(current.rules.grassyTerrainHealDenominator)
				current.applyEndTurnHealingResult(
					participant = latest,
					healAmount = healAmount,
					event = BattleEvent.TerrainHealingApplied(
						turnNumber = current.turnNumber,
						actorId = latest.actorId,
						terrain = BattleTerrain.GRASSY,
						amount = healAmount,
					),
				)
			}
	}

	/**
	 * 处理回合末携带道具回复。
	 *
	 * 这里覆盖固定最大 HP 比例回复类道具。函数不会处理低体力一次性回复道具，因为那类道具必须紧贴“受到伤害后”
	 * 触发，已经由扣血阶段通过统一低体力道具回调处理。每个道具效果结算前都会重新读取成员最新快照，确保多个
	 * 回复效果连续触发时不会越过最大 HP，也不会在回复封锁被其它效果改变后继续使用旧判断。
	 */
	fun applyHeldItemHealing(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) endTurnHealing@ { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@endTurnHealing current
				if (!latest.canReceiveHealing()) {
					return@endTurnHealing current
				}
				val afterUnconditionalHealing = latest.itemEffects
					.filterIsInstance<BattleItemEffect.HeldEndTurnHeal>()
					.fold(current) itemHealing@ { healingState, effect ->
						val currentParticipant = healingState.participant(latest.actorId) ?: return@itemHealing healingState
						if (!currentParticipant.canReceiveHealing()) {
							return@itemHealing healingState
						}
						val healAmount = currentParticipant.endTurnHealAmount(effect.healDenominator)
						healingState.applyEndTurnHealingResult(
							participant = currentParticipant,
							healAmount = healAmount,
							event = BattleEvent.HealingApplied(
								turnNumber = healingState.turnNumber,
								actorId = currentParticipant.actorId,
								amount = healAmount,
							),
						)
					}
				val afterUnconditionalParticipant =
					afterUnconditionalHealing.participant(latest.actorId) ?: return@endTurnHealing afterUnconditionalHealing
				afterUnconditionalParticipant.itemEffects
					.filterIsInstance<BattleItemEffect.HeldEndTurnHealForElement>()
					.filter { afterUnconditionalParticipant.hasElement(it.elementId) }
					.fold(afterUnconditionalHealing) itemHealing@ { healingState, effect ->
						val currentParticipant = healingState.participant(latest.actorId) ?: return@itemHealing healingState
						if (!currentParticipant.canReceiveHealing() || !currentParticipant.hasElement(effect.elementId)) {
							return@itemHealing healingState
						}
						val healAmount = currentParticipant.endTurnHealAmount(effect.healDenominator)
						healingState.applyEndTurnHealingResult(
							participant = currentParticipant,
							healAmount = healAmount,
							event = BattleEvent.HealingApplied(
								turnNumber = healingState.turnNumber,
								actorId = currentParticipant.actorId,
								amount = healAmount,
							),
						)
					}
			}

	/**
	 * 写入一次回合末回复结果。
	 *
	 * 天气、场地和携带道具回复虽然来自不同规则来源，但它们共享同一个底层状态迁移：按已经计算好的回复量修改
	 * 成员 HP，然后追加对应来源的可见事件。这里不处理回复封锁、成员是否满血或是否仍可战斗，因为这些前置条件
	 * 必须留在具体阶段中，才能让每个阶段的规则说明和事件类型保持清楚。
	 */
	private fun BattleState.applyEndTurnHealingResult(
		participant: BattleParticipant,
		healAmount: Int,
		event: BattleEvent,
	): BattleState =
		replaceParticipant(participant.heal(healAmount)).appendEvent(event)

	/**
	 * 计算回合末固定最大 HP 比例回复量。
	 *
	 * 所有回合末固定比例回复都遵循同一条边界：至少回复 1 点，但不能超过成员当前缺失 HP。调用方必须先确认成员
	 * 可以回复；如果在满 HP 时调用，本函数会得到 0 上限，从而暴露错误的调用顺序，而不是偷偷制造一次 0 回复事件。
	 */
	private fun BattleParticipant.endTurnHealAmount(denominator: Int): Int =
		(maxHp / denominator).coerceAtLeast(1).coerceAtMost(maxHp - currentHp)
}
