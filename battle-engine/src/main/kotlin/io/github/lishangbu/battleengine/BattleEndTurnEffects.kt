package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleState
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
 * @property damageResultEffects 回合末扣血后的低体力道具、倒下和胜负收口。
 * @property bindingEffects 束缚类临时状态的回合末伤害与来源离场清理规则。
 */
internal class BattleEndTurnEffects(
	private val damageResultEffects: BattleEndTurnDamageResultEffects,
	private val bindingEffects: BattleBindingEffects,
) {
	private val healingEffects = BattleEndTurnHealingEffects()

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
		val afterBinding = bindingEffects.applyEndTurnDamage(afterResidual)
		if (afterBinding.result != null) {
			return afterBinding
		}
		val afterWeather = applyWeatherDamage(afterBinding)
		if (afterWeather.result != null) {
			return afterWeather
		}
		val afterWeatherHealing = healingEffects.applyWeatherHealing(afterWeather)
		val afterTerrainHealing = healingEffects.applyTerrainHealing(afterWeatherHealing)
		return healingEffects.applyHeldItemHealing(afterTerrainHealing)
	}

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
					damageResultEffects.apply(
						state = current,
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
					damageResultEffects.apply(
						state = current,
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
		 * 格式回合上限触发时写入战斗结果的稳定原因码。
		 */
		private const val MAX_TURNS_REACHED_REASON = "max-turns-reached"

		/**
		 * 沙暴等天气持续伤害使用最大 HP 的 1/16。
		 */
		private const val WEATHER_DAMAGE_DENOMINATOR = 16
	}
}
