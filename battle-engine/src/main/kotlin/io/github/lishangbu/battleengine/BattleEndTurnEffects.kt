package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 回合末环境与持续伤害结算器。
 *
 * 主状态机仍然决定“什么时候进入回合末结算”和“回合末结算之后还要推进哪些持续时间”；本类只封装回合末
 * 内部那些严格按固定顺序执行的规则细节。当前顺序是：
 * 1. 主要异常状态造成的持续伤害，例如灼伤、中毒、剧毒。
 * 2. 束缚类临时状态造成的持续伤害和束缚自然解除。
 * 3. 天气造成的持续伤害。
 * 4. 天气相关回复。
 * 5. 场地相关回复。
 * 6. 携带道具的回合末回复。
 *
 * 这里不实现通用事件总线，也不把每个效果做成可注册插件。回合末规则的难点不是“能不能动态发现处理器”，而是
 * 现代规则中阶段顺序非常敏感：例如低体力回复道具必须发生在扣血之后、倒下判断之前；青草场地回复不应该跑到
 * 沙暴伤害前面；束缚来源离场时应立即解除束缚，而不是继续扣血。把这些顺序写成直接函数调用，测试和调试时都
 * 能清楚看到状态如何一步步推进。
 *
 * @property lowHpItemHealing 低体力一次性回复道具处理回调。它仍由主引擎提供，是因为同一套道具触发顺序还被
 * 普通伤害、混乱自伤和入场陷阱复用；如果在本类里复制一份，后续修正道具消费或回复封锁时会产生两套规则。
 */
internal class BattleEndTurnEffects(
	private val lowHpItemHealing: (BattleState, String) -> BattleState,
) {
	/**
	 * 执行一次完整的回合末持续伤害与回复结算。
	 *
	 * 该函数只在战斗尚未结束时由 [BattleEngine.resolveTurn] 调用。任意扣血阶段产生胜负结果后，后续天气、
	 * 场地和道具回复都不会再执行，避免已经结束的战斗继续追加可见事件。若所有扣血阶段都没有结束战斗，则继续
	 * 处理回复阶段；回复阶段不会主动判定胜负，因为它只增加 HP，不会让成员倒下。
	 */
	fun apply(state: BattleState): BattleState {
		val afterResidual = applyResidualStatusDamage(state)
		if (afterResidual.result != null) {
			return afterResidual
		}
		val afterBinding = applyBindingDamage(afterResidual)
		if (afterBinding.result != null) {
			return afterBinding
		}
		val afterWeather = applyWeatherDamage(afterBinding)
		if (afterWeather.result != null) {
			return afterWeather
		}
		val afterWeatherHealing = applyWeatherHealing(afterWeather)
		val afterTerrainHealing = applyTerrainHealing(afterWeatherHealing)
		return applyHeldItemHealing(afterTerrainHealing)
	}

	/**
	 * 判断成员当前束缚来源是否仍在场并可战斗。
	 *
	 * 这个判断同时被两个阶段使用：
	 * - 换人阶段用它判断被束缚成员能否主动离场。
	 * - 回合末束缚伤害阶段用它判断束缚是否还应继续造成伤害。
	 *
	 * 保持单一实现可以避免“换人认为束缚仍有效，但回合末认为已经失效”这类状态矛盾。函数只读取快照，不追加
	 * 事件；真正的阻止换人事件或束缚解除事件仍由调用阶段负责。
	 */
	fun isBindingSourceActive(state: BattleState, participant: BattleParticipant): Boolean {
		val sourceActorId = participant.boundByActorId ?: return false
		val source = state.participant(sourceActorId) ?: return false
		return source.canBattle() && state.isActive(sourceActorId)
	}

	/**
	 * 清除指定来源成员维持的所有束缚。
	 *
	 * 当束缚来源主动换下或被强制换下时，来源不再“在场维持”束缚，所有由它施加的束缚都应立刻结束。这里遍历
	 * 完整队伍而不是只看当前上场成员，是为了让双打、强制替换或未来多目标束缚场景中，离场来源维持的多个目标
	 * 都能一致解除。每个被解除的目标都会追加 [BattleEvent.VolatileStatusCleared]，方便 replay 精确定位解除
	 * 发生在换人事件之后、入场陷阱之前。
	 */
	fun clearBindingsFromSource(state: BattleState, sourceActorId: String): BattleState =
		state.sides
			.flatMap { it.participants }
			.filter { it.boundByActorId == sourceActorId && it.bindingTurnsRemaining > 0 }
			.fold(state) { current, participant -> clearBindingState(current, participant) }

	/**
	 * 应用格式级回合上限裁定。
	 *
	 * 回合上限只在完整回合末检查，因此异常伤害、天气/场地副作用、回合末回复以及持续时间推进都会先完成。
	 * 当前格式快照没有声明点数裁定规则时，到达上限按平局结束，`winningSideId=null` 明确表示没有胜方。该函数
	 * 单独暴露给主状态机，是为了让主状态机可以在天气、场地、一侧状态持续时间都推进后再调用它。
	 */
	fun applyTurnLimit(state: BattleState): BattleState {
		val maxTurns = state.format.maxTurns ?: return state
		if (state.turnNumber < maxTurns) {
			return state
		}
		val result = BattleResult(winningSideId = null, reason = MAX_TURNS_REACHED_REASON)
		return state
			.copy(result = result)
			.appendEvent(
				BattleEvent.BattleEnded(
					turnNumber = state.turnNumber,
					winningSideId = result.winningSideId,
					reason = result.reason,
				),
			)
	}

	/**
	 * 写入一次回合末扣血结果，并完成低体力回复道具、倒下和胜负收口。
	 *
	 * 回合末的异常、束缚、天气伤害都共享同一个收口顺序：
	 * 1. 先把扣血后的成员写回状态。
	 * 2. 追加本次扣血对应的可见事件。
	 * 3. 执行该伤害之后立刻触发的低体力回复道具。
	 * 4. 基于道具处理后的最新成员状态判断倒下和胜负。
	 *
	 * [afterEvent] 用于束缚这种“扣血事件之后，可能还要追加自然解除事件”的小阶段。它只允许在倒下判断之前追加
	 * 阶段内事件，不能跳转到其它阶段，避免把回合末大顺序藏进回调里。
	 */
	private fun BattleState.applyEndTurnDamageResult(
		damaged: BattleParticipant,
		event: BattleEvent,
		afterEvent: (BattleState) -> BattleState = { it },
	): BattleState {
		val afterDamage = afterEvent(replaceParticipant(damaged).appendEvent(event))
		val afterLowHpItem = lowHpItemHealing(afterDamage, damaged.actorId)
		val latestAfterItem = afterLowHpItem.participant(damaged.actorId) ?: damaged
		return afterLowHpItem.handleFaintAndResult(latestAfterItem)
	}

	/**
	 * 结算回合末主要异常状态伤害。
	 *
	 * 当前覆盖灼伤、中毒和剧毒扣血。剧毒伤害读取成员运行态中的递增计数，并且只有成员在扣血后仍可战斗时才推进
	 * 计数；如果该次伤害直接让成员倒下，递增计数没有继续存在的意义，也不应该污染 replay 中的最终快照。
	 * 间接伤害免疫会在计算伤害前短路，保证免疫目标不会追加“0 点伤害”事件。
	 */
	private fun applyResidualStatusDamage(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || latest.hasIndirectDamageImmunity()) {
					current
				} else {
					val residualDamage = residualDamage(latest) ?: return@fold current
					val damaged = latest.receiveDamage(residualDamage)
					val afterStatusCounter = if (damaged.canBattle()) {
						damaged.advanceBadPoisonCounter()
					} else {
						damaged
					}
					current.applyEndTurnDamageResult(
						damaged = afterStatusCounter,
						event = BattleEvent.ResidualDamageApplied(
							turnNumber = current.turnNumber,
							actorId = latest.actorId,
							status = requireNotNull(latest.majorStatus),
							amount = residualDamage,
						),
					)
				}
			}

	/**
	 * 处理束缚类临时状态的回合末伤害和持续时间。
	 *
	 * 束缚和普通“回合末递减状态”不同：它有来源成员、会造成伤害、会因为来源离场而提前解除。这里先验证来源
	 * 是否仍在场且可战斗；来源失效时只解除束缚，不造成伤害。成功造成伤害后再递减剩余回合，并在递减前剩余
	 * 1 回合时追加解除事件。扣血、解除事件、低体力回复道具、倒下判断的相对顺序保持稳定，测试可以逐项断言。
	 */
	private fun applyBindingDamage(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (latest.bindingTurnsRemaining <= 0) {
					return@fold current
				}
				if (!isBindingSourceActive(current, latest)) {
					return@fold clearBindingState(current, latest)
				}
				if (!latest.canBattle() || latest.hasIndirectDamageImmunity()) {
					return@fold current
				}
				val sourceActorId = requireNotNull(latest.boundByActorId) { "binding source must be present" }
				val turnsRemainingBefore = latest.bindingTurnsRemaining
				val damage = (latest.maxHp / BINDING_DAMAGE_DENOMINATOR).coerceAtLeast(1)
				val damaged = latest.receiveDamage(damage).decrementBindingEndTurn()
				current.applyEndTurnDamageResult(
					damaged = damaged,
					event = BattleEvent.BindingDamageApplied(
						turnNumber = current.turnNumber,
						actorId = latest.actorId,
						sourceActorId = sourceActorId,
						amount = damage,
						turnsRemainingBefore = turnsRemainingBefore,
					),
					afterEvent = { afterDamage ->
						if (turnsRemainingBefore == 1) {
							afterDamage.appendEvent(
								BattleEvent.VolatileStatusCleared(
									turnNumber = current.turnNumber,
									actorId = latest.actorId,
									status = BattleVolatileStatus.BINDING,
								),
							)
						} else {
							afterDamage
						}
					},
				)
			}

	/**
	 * 清除单个成员的束缚并追加解除事件。
	 *
	 * 调用方已经确认目标确实存在束缚；本函数只做不可变状态替换和事件追加。它不判断来源成员、不处理伤害，也不
	 * 消费随机数，因此可以同时供“来源离场清理”和“回合末自然结束”两个场景复用。
	 */
	private fun clearBindingState(state: BattleState, participant: BattleParticipant): BattleState =
		state
			.replaceParticipant(participant.clearBinding())
			.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = BattleVolatileStatus.BINDING,
				),
			)

	/**
	 * 处理回合末天气伤害。
	 *
	 * 当前只覆盖现代沙暴固定伤害：当前上场、仍可战斗，且没有天气伤害免疫的成员会受到最大 HP 的 1/16 伤害。
	 * 免疫判断集中在 [BattleParticipant.immuneToWeatherDamage]，它会合并间接伤害免疫、特性/道具天气免疫和沙暴
	 * 属性免疫。这里故意只保留伤害阶段本身，让“天气会不会伤害目标”和“伤害之后如何收口”分别保持清晰。
	 */
	private fun applyWeatherDamage(state: BattleState): BattleState {
		if (state.environment.weather != BattleWeather.SANDSTORM) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || latest.immuneToWeatherDamage(current, BattleWeather.SANDSTORM)) {
					current
				} else {
					val damage = (latest.maxHp / WEATHER_DAMAGE_DENOMINATOR).coerceAtLeast(1)
					val damaged = latest.receiveDamage(damage)
					current.applyEndTurnDamageResult(
						damaged = damaged,
						event = BattleEvent.WeatherDamageApplied(
							turnNumber = current.turnNumber,
							actorId = latest.actorId,
							weather = BattleWeather.SANDSTORM,
							amount = damage,
						),
					)
				}
			}
	}

	/**
	 * 处理天气阶段的特性回复。
	 *
	 * 现代规则中，部分特性会在指定天气存在时于回合末按最大 HP 固定比例回复。该阶段放在天气伤害之后、场地回复
	 * 之前，保持事件流语义清晰：天气先造成或免除伤害，再处理同属天气阶段的回复，最后进入场地和携带道具。
	 * 每个回复效果都会重新读取最新成员快照，避免同一成员多个回复效果连续结算时使用过期 HP。
	 */
	private fun applyWeatherHealing(state: BattleState): BattleState {
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
					val healAmount = (currentParticipant.maxHp / effect.healDenominator).coerceAtLeast(1)
						.coerceAtMost(currentParticipant.maxHp - currentParticipant.currentHp)
					healingState
						.replaceParticipant(currentParticipant.heal(healAmount))
						.appendEvent(
							BattleEvent.WeatherHealingApplied(
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
	 * 第一批只实现青草场地的固定比例回复，并只作用于当前上场、仍可战斗、未满血、未被回复封锁且接地的成员。
	 * 飞行、漂浮、携带道具免疫地面场地等来源应在进入引擎前折算为成员的 `grounded=false`；本阶段只读取已经
	 * 冻结的运行态，不再尝试根据资料名称推断接地状态。
	 */
	private fun applyTerrainHealing(state: BattleState): BattleState {
		if (state.environment.terrain != BattleTerrain.GRASSY) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) terrainHealing@ { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@terrainHealing current
				if (!latest.grounded || !latest.canReceiveHealing()) {
					return@terrainHealing current
				}
				val healAmount = (latest.maxHp / current.rules.grassyTerrainHealDenominator).coerceAtLeast(1)
					.coerceAtMost(latest.maxHp - latest.currentHp)
				current
					.replaceParticipant(latest.heal(healAmount))
					.appendEvent(
						BattleEvent.TerrainHealingApplied(
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
	 * 触发，已经由 [applyEndTurnDamageResult] 通过 [lowHpItemHealing] 处理。每个道具效果结算前都会重新读取成员
	 * 最新快照，确保多个回复效果连续触发时不会越过最大 HP，也不会在回复封锁被其它效果改变后继续使用旧判断。
	 */
	private fun applyHeldItemHealing(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) endTurnHealing@ { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@endTurnHealing current
				if (!latest.canReceiveHealing()) {
					return@endTurnHealing current
				}
				latest.itemEffects
					.filterIsInstance<BattleItemEffect.HeldEndTurnHeal>()
					.fold(current) itemHealing@ { healingState, effect ->
						val currentParticipant = healingState.participant(latest.actorId) ?: return@itemHealing healingState
						if (!currentParticipant.canReceiveHealing()) {
							return@itemHealing healingState
						}
						val healAmount = (currentParticipant.maxHp / effect.healDenominator).coerceAtLeast(1)
							.coerceAtMost(currentParticipant.maxHp - currentParticipant.currentHp)
						healingState
							.replaceParticipant(currentParticipant.heal(healAmount))
							.appendEvent(
								BattleEvent.HealingApplied(
									turnNumber = healingState.turnNumber,
									actorId = currentParticipant.actorId,
									amount = healAmount,
								),
							)
					}
			}

	/**
	 * 计算主要异常状态在回合末造成的固定伤害。
	 *
	 * 灼伤、中毒和剧毒都按现代固定分母取整，最少 1 点。剧毒使用成员运行态里的 `badPoisonCounter`，并在调用方
	 * 成功造成伤害且目标仍可战斗时递增；这里不修改计数，保持函数是纯计算，方便测试直接覆盖伤害数值。
	 */
	private fun residualDamage(participant: BattleParticipant): Int? =
		when (participant.majorStatus) {
			BattleMajorStatus.BURN -> (participant.maxHp / 16).coerceAtLeast(1)
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON -> (participant.maxHp * participant.badPoisonCounter.coerceAtLeast(1) / 16)
				.coerceAtLeast(1)
			else -> null
		}

	private companion object {
		/**
		 * 束缚类持续伤害使用最大 HP 的 1/8。
		 */
		private const val BINDING_DAMAGE_DENOMINATOR = 8

		/**
		 * 格式回合上限触发时写入战斗结果的稳定原因码。
		 */
		private const val MAX_TURNS_REACHED_REASON = "max-turns-reached"

		/**
		 * 沙暴等天气持续伤害使用最大 HP 的 1/16。
		 */
		private const val WEATHER_DAMAGE_DENOMINATOR = 16
	}
}
