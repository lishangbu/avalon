package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE = GameDataTableSpec(
	tableName = "game_item_attribute_binding",
	label = "道具属性绑定",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "attribute_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("item_id", "attribute_id"),
)

/**
 * 道具属性绑定持久化访问。
 */
@Repository
class GameItemAttributeBindingsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ITEM_ATTRIBUTE_BINDINGS_TABLE, id)
	}
}
