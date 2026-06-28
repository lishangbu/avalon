package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_location_area_encounter_condition_value",
	label = "区域遭遇条件绑定",
	columns = listOf(
		GameDataColumnSpec(name = "encounter_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "condition_value_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("encounter_id", "condition_value_id"),
)

/**
 * 区域遭遇条件绑定持久化访问。
 */
@Repository
class GameLocationAreaEncounterConditionValuesRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE, id)
	}
}
