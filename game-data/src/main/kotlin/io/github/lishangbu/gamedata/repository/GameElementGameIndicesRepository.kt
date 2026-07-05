package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ELEMENT_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_element_game_index",
	label = "属性索引",
	columns = listOf(
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("element_id"),
)

/**
 * 属性索引持久化访问。
 */
@Repository
class GameElementGameIndicesRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ELEMENT_GAME_INDICES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ELEMENT_GAME_INDICES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ELEMENT_GAME_INDICES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ELEMENT_GAME_INDICES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ELEMENT_GAME_INDICES_TABLE, id)
	}
}
