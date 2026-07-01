package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 全场环境持续时间推进器。
 *
 * 环境“建立”和环境“回合末推进”发生在不同阶段：技能或出场特性负责写入天气/场地开始事件，回合末负责递减
 * 天气、场地和全场速度顺序效果的剩余回合。把推进逻辑单独放在这里，可以让 [BattleEnvironmentEffects] 专注
 * 建立环境时的来源、延长道具和去重语义。
 *
 * 本类不处理天气伤害、天气回复、场地回复或行动排序本身；它只维护环境快照上的生命周期字段。剩余回合为空表示
 * 永久环境或调用方不希望引擎管理持续时间；剩余回合为 1 时在本次回合末结束环境并追加对应结束事件；大于 1 时
 * 只递减计数，不制造噪声事件。
 */
internal class BattleEnvironmentDurationEffects {
	/**
	 * 按固定顺序推进天气、场地和全场速度顺序效果。
	 *
	 * 顺序保持为天气、场地、全场速度顺序，与主回合末阶段中“先结算伤害/回复，再推进持续时间”的大顺序配合。
	 * 这里不会因为天气结束而跳过场地或戏法空间持续时间推进；它们是彼此独立的环境字段。
	 */
	fun advance(state: BattleState): BattleState {
		val afterWeather = advanceWeatherDuration(state)
		val afterTerrain = advanceTerrainDuration(afterWeather)
		return advanceFieldSpeedOrderDuration(afterTerrain)
	}

	/**
	 * 推进天气持续回合并在耗尽时恢复无天气。
	 *
	 * 如果当前天气已经是无天气但仍残留持续回合，直接清理持续字段，不追加结束事件；这表示输入快照已经处于无
	 * 天气状态，只是携带了不该继续存在的计数。真实天气结束只在非 NONE 天气从 1 递减到 0 时产生事件。
	 */
	private fun advanceWeatherDuration(state: BattleState): BattleState {
		val turnsRemaining = state.environment.weatherTurnsRemaining ?: return state
		if (state.environment.weather == BattleWeather.NONE) {
			return state.copy(environment = state.environment.copy(weatherTurnsRemaining = null))
		}
		return if (turnsRemaining <= 1) {
			state
				.copy(environment = state.environment.copy(weather = BattleWeather.NONE, weatherTurnsRemaining = null))
				.appendEvent(
					BattleEvent.WeatherEnded(
						turnNumber = state.turnNumber,
						weather = state.environment.weather,
					),
				)
		} else {
			state.copy(environment = state.environment.copy(weatherTurnsRemaining = turnsRemaining - 1))
		}
	}

	/**
	 * 推进场地持续回合并在耗尽时恢复无场地。
	 *
	 * 与天气一致，如果场地已经是 NONE 但持续字段仍然存在，就只清掉残留计数。真实场地结束事件只表示一个有效
	 * 场地在回合末自然耗尽。
	 */
	private fun advanceTerrainDuration(state: BattleState): BattleState {
		val turnsRemaining = state.environment.terrainTurnsRemaining ?: return state
		if (state.environment.terrain == BattleTerrain.NONE) {
			return state.copy(environment = state.environment.copy(terrainTurnsRemaining = null))
		}
		return if (turnsRemaining <= 1) {
			state
				.copy(environment = state.environment.copy(terrain = BattleTerrain.NONE, terrainTurnsRemaining = null))
				.appendEvent(
					BattleEvent.TerrainEnded(
						turnNumber = state.turnNumber,
						terrain = state.environment.terrain,
					),
				)
		} else {
			state.copy(environment = state.environment.copy(terrainTurnsRemaining = turnsRemaining - 1))
		}
	}

	/**
	 * 推进全场速度顺序效果持续回合。
	 *
	 * 戏法空间等全场速度顺序效果在回合末按天气/场地同样的生命周期递减，耗尽时恢复普通速度排序并记录事件。
	 * 这里不重新计算行动队列；新的排序只会在下一次行动规划时由 [BattleActionOrdering] 读取环境快照。
	 */
	private fun advanceFieldSpeedOrderDuration(state: BattleState): BattleState {
		val effect = state.environment.fieldSpeedOrderEffect ?: return state
		val nextEffect = effect.advanceTurn()
		return if (nextEffect == null) {
			state
				.copy(environment = state.environment.copy(fieldSpeedOrderEffect = null))
				.appendEvent(
					BattleEvent.FieldSpeedOrderEnded(
						turnNumber = state.turnNumber,
						kind = effect.kind,
					),
				)
		} else {
			state.copy(environment = state.environment.copy(fieldSpeedOrderEffect = nextEffect))
		}
	}
}
