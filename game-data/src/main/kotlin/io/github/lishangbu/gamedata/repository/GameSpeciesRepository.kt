package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_SPECIES_TABLE = GameDataTableSpec(
	tableName = "game_species",
	label = "种类资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "color_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "shape_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "habitat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "gender_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "capture_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "base_happiness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "hatch_counter", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "baby", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "legendary", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "mythical", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 种类资料持久化访问。
 */
@Repository
class GameSpeciesRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_SPECIES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_SPECIES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_SPECIES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_SPECIES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_SPECIES_TABLE, id)
	}
}
