package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather

/**
 * 负责把“建立天气/场地”这类全场环境效果写入战斗状态。
 *
 * 环境效果同时来自技能和出场特性，两条入口共享相同的去重语义、事件类型和持续回合延长规则。将这部分从
 * [BattleEngine] 主流程拆出后，状态机只需要决定何时触发环境写入，而不用重复维护天气、场地和延长道具的细节。
 * 该类仍保持纯状态转换：输入不可变 [BattleState]，输出新状态，不读取数据库、不依赖随机源，也不解析名称文本。
 */
internal class BattleEnvironmentEffects {
	/**
	 * 推进天气、场地和全场速度顺序效果的持续回合。
	 *
	 * 剩余回合为空表示该环境来自永久规则或 fixture 不关心持续时间，不会被这里修改。剩余回合为 1 时，本回合末
	 * 结束环境并产生结束事件；大于 1 时只递减计数，不产生额外事件，避免 replay 事件流过于嘈杂。
	 */
	fun advanceDurations(state: BattleState): BattleState {
		val afterWeather = advanceWeatherDuration(state)
		val afterTerrain = advanceTerrainDuration(afterWeather)
		return advanceFieldSpeedOrderDuration(afterTerrain)
	}

	/**
	 * 处理技能成功后的全场环境效果。
	 *
	 * 环境写入放在技能命中成功之后执行，只读取 [BattleSkillEnvironmentEffect] 这类结构化效果。当前支持普通天气
	 * 与场地技能；若目标环境和剩余回合没有变化，则不追加事件，避免 replay 端把重复设置误读成真实变化。
	 */
	fun applySkillEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
	): BattleState =
		skill.environmentEffects.fold(state) { current, effect ->
			when (effect) {
				is BattleSkillEnvironmentEffect.SetWeather -> applySkillWeatherChange(
					state = current,
					actorId = actorId,
					effect = effect,
				)
				is BattleSkillEnvironmentEffect.SetTerrain -> applySkillTerrainChange(
					state = current,
					actorId = actorId,
					effect = effect,
				)
			}
		}

	/**
	 * 执行出场特性的天气设置。
	 *
	 * 现代普通天气特性会覆盖当前普通天气并写入固定持续回合。携带者若有匹配天气的延长道具效果，则与天气技能
	 * 使用同一套持续回合延长逻辑。若当前环境已经是同一天气且剩余回合一致，则保持状态并跳过事件。
	 */
	fun applySwitchInWeatherChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInWeatherChange,
	): BattleState =
		applyWeatherChange(
			state = state,
			actorId = actorId,
			weather = effect.weather,
			turnsRemaining = extendedWeatherTurnsRemaining(
				state = state,
				actorId = actorId,
				weather = effect.weather,
				baseTurnsRemaining = effect.turnsRemaining,
			),
		)

	/**
	 * 执行出场特性的场地设置。
	 *
	 * 普通场地会覆盖当前场地并写入固定持续回合。若当前环境已经是同一场地且剩余回合一致，则不产生事件；携带者
	 * 若拥有匹配场地的延长道具效果，则与场地技能使用同一套持续回合延长逻辑。
	 */
	fun applySwitchInTerrainChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInTerrainChange,
	): BattleState =
		applyTerrainChange(
			state = state,
			actorId = actorId,
			terrain = effect.terrain,
			turnsRemaining = extendedTerrainTurnsRemaining(
				state = state,
				actorId = actorId,
				terrain = effect.terrain,
				baseTurnsRemaining = effect.turnsRemaining,
			),
		)

	/**
	 * 将技能天气效果写入战斗环境。
	 *
	 * 该函数与出场天气特性保持相同事件类型和去重语义；区别只在触发来源来自技能槽而不是成员特性。
	 */
	private fun applySkillWeatherChange(
		state: BattleState,
		actorId: String,
		effect: BattleSkillEnvironmentEffect.SetWeather,
	): BattleState =
		applyWeatherChange(
			state = state,
			actorId = actorId,
			weather = effect.weather,
			turnsRemaining = extendedWeatherTurnsRemaining(
				state = state,
				actorId = actorId,
				weather = effect.weather,
				baseTurnsRemaining = effect.turnsRemaining,
			),
		)

	/**
	 * 将技能场地效果写入战斗环境。
	 *
	 * 该函数与出场场地特性保持相同事件类型和去重语义；区别只在触发来源来自技能槽而不是成员特性。
	 */
	private fun applySkillTerrainChange(
		state: BattleState,
		actorId: String,
		effect: BattleSkillEnvironmentEffect.SetTerrain,
	): BattleState =
		applyTerrainChange(
			state = state,
			actorId = actorId,
			terrain = effect.terrain,
			turnsRemaining = extendedTerrainTurnsRemaining(
				state = state,
				actorId = actorId,
				terrain = effect.terrain,
				baseTurnsRemaining = effect.turnsRemaining,
			),
		)

	/**
	 * 写入天气并追加统一的天气开始事件。
	 *
	 * 重复设置同一天气且剩余回合不变时直接返回原状态；这让 `WeatherStarted` 在事件流中只代表真实环境变化，
	 * 而不是一次没有产生状态差异的触发尝试。
	 */
	private fun applyWeatherChange(
		state: BattleState,
		actorId: String,
		weather: BattleWeather,
		turnsRemaining: Int?,
	): BattleState {
		if (state.environment.weather == weather && state.environment.weatherTurnsRemaining == turnsRemaining) {
			return state
		}
		return state
			.copy(
				environment = state.environment.copy(
					weather = weather,
					weatherTurnsRemaining = turnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.WeatherStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					weather = weather,
					turnsRemaining = turnsRemaining,
				),
			)
	}

	/**
	 * 写入场地并追加统一的场地开始事件。
	 *
	 * 重复设置同一场地且剩余回合不变时直接返回原状态；强制封锁或特殊机制后续会以独立效果扩展，不在这里通过
	 * 技能名、特性名或道具名做隐式判断。
	 */
	private fun applyTerrainChange(
		state: BattleState,
		actorId: String,
		terrain: BattleTerrain,
		turnsRemaining: Int?,
	): BattleState {
		if (state.environment.terrain == terrain && state.environment.terrainTurnsRemaining == turnsRemaining) {
			return state
		}
		return state
			.copy(
				environment = state.environment.copy(
					terrain = terrain,
					terrainTurnsRemaining = turnsRemaining,
				),
			)
			.appendEvent(
				BattleEvent.TerrainStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					terrain = terrain,
					turnsRemaining = turnsRemaining,
				),
			)
	}

	/**
	 * 计算指定来源成员建立天气时最终写入的持续回合。
	 *
	 * 技能环境效果和出场天气特性都从这里读取持续回合延长道具。基础持续回合为空表示永久天气或调用方不希望
	 * 引擎管理持续时间，此时道具不会把它改成有限回合，避免改变强天气或 fixture 语义。
	 */
	private fun extendedWeatherTurnsRemaining(
		state: BattleState,
		actorId: String,
		weather: BattleWeather,
		baseTurnsRemaining: Int?,
	): Int? {
		if (baseTurnsRemaining == null) {
			return null
		}
		val actor = state.participant(actorId) ?: return baseTurnsRemaining
		return actor.itemEffects
			.filterIsInstance<BattleItemEffect.WeatherDurationExtension>()
			.filter { weather in it.weathers }
			.maxOfOrNull { it.turnsRemaining }
			?: baseTurnsRemaining
	}

	/**
	 * 计算指定来源成员建立场地时最终写入的持续回合。
	 *
	 * 场地延长道具只影响来源成员自己建立的场地，并按匹配场地选择最长的结构化持续回合；若基础持续回合为空，
	 * 则保留永久/不管理持续回合的语义。
	 */
	private fun extendedTerrainTurnsRemaining(
		state: BattleState,
		actorId: String,
		terrain: BattleTerrain,
		baseTurnsRemaining: Int?,
	): Int? {
		if (baseTurnsRemaining == null) {
			return null
		}
		val actor = state.participant(actorId) ?: return baseTurnsRemaining
		return actor.itemEffects
			.filterIsInstance<BattleItemEffect.TerrainDurationExtension>()
			.filter { terrain in it.terrains }
			.maxOfOrNull { it.turnsRemaining }
			?: baseTurnsRemaining
	}

	/**
	 * 推进天气持续回合并在耗尽时恢复无天气。
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
