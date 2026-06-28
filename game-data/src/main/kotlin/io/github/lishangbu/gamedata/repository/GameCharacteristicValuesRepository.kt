package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_CHARACTERISTIC_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_characteristic_value",
	label = "个体特征取值",
	columns = listOf(
		GameDataColumnSpec(name = "characteristic_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "possible_value", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("characteristic_id", "possible_value"),
)

/**
 * 个体特征取值持久化访问。
 */
@Repository
class GameCharacteristicValuesRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_CHARACTERISTIC_VALUES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_CHARACTERISTIC_VALUES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_CHARACTERISTIC_VALUES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_CHARACTERISTIC_VALUES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_CHARACTERISTIC_VALUES_TABLE, id)
	}
}
