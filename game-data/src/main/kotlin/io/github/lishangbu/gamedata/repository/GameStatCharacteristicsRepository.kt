package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_STAT_CHARACTERISTICS_TABLE = GameDataTableSpec(
	tableName = "game_stat_characteristic",
	label = "数值项特征",
	columns = listOf(
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "characteristic_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("stat_id", "characteristic_id"),
)

/**
 * 数值项特征持久化访问。
 */
@Repository
class GameStatCharacteristicsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_STAT_CHARACTERISTICS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_STAT_CHARACTERISTICS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_STAT_CHARACTERISTICS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_STAT_CHARACTERISTICS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_STAT_CHARACTERISTICS_TABLE, id)
	}
}
