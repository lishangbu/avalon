package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_BERRIES_TABLE = GameDataTableSpec(
	tableName = "game_berry",
	label = "树果资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "firmness_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "natural_gift_element_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "growth_time", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "max_harvest", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "natural_gift_power", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "size", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "smoothness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "soil_dryness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 树果资料持久化访问。
 */
@Repository
class GameBerriesRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_BERRIES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_BERRIES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_BERRIES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_BERRIES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_BERRIES_TABLE, id)
	}
}
