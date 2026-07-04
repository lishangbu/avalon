package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_LOCATION_AREA_ENCOUNTERS_TABLE = GameDataTableSpec(
	tableName = "game_location_area_encounter",
	label = "区域精灵遭遇",
	columns = listOf(
		GameDataColumnSpec(name = "area_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "min_level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "max_level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "chance", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "max_chance", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("area_id", "creature_id"),
)

/**
 * 区域精灵遭遇持久化访问。
 */
@Repository
class GameLocationAreaEncountersRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_LOCATION_AREA_ENCOUNTERS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_LOCATION_AREA_ENCOUNTERS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_LOCATION_AREA_ENCOUNTERS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_LOCATION_AREA_ENCOUNTERS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_LOCATION_AREA_ENCOUNTERS_TABLE, id)
	}
}
