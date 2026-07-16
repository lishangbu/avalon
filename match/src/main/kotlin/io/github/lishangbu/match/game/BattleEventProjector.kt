package io.github.lishangbu.match.game

import io.github.lishangbu.battleengine.model.BattleEvent
import tools.jackson.databind.ObjectMapper

/** 将引擎类型收敛为不携带中文文案、随机轨迹或内部类型名的客户端事件。 */
class BattleEventProjector(private val objectMapper: ObjectMapper) {
	fun project(events: List<BattleEvent>): List<MatchBattleEvent> = events.map(::project)

	fun project(event: BattleEvent): MatchBattleEvent {
		@Suppress("UNCHECKED_CAST")
		val values = objectMapper.readValue(objectMapper.writeValueAsString(event), Map::class.java) as Map<String, Any?>
		return MatchBattleEvent(
			code = event::class.simpleName.orEmpty().replace(CAMEL_BOUNDARY, "$1-$2").lowercase(),
			parameters = values.filterKeys { it !in HIDDEN_PARAMETERS },
		)
	}

	private companion object {
		val CAMEL_BOUNDARY = Regex("([a-z0-9])([A-Z])")
		val HIDDEN_PARAMETERS = setOf("randomPercent", "roll")
	}
}
