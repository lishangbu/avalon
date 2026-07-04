package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_CREATURE_HELD_ITEMS_TABLE = GameDataTableSpec(
	tableName = "game_creature_held_item",
	label = "精灵持有道具",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "rarity", type = GameDataColumnType.INT),
	),
	searchColumns = listOf("creature_id", "item_id"),
)

/**
 * 精灵持有道具持久化访问。
 */
@Repository
class GameCreatureHeldItemsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_CREATURE_HELD_ITEMS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_CREATURE_HELD_ITEMS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_CREATURE_HELD_ITEMS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_CREATURE_HELD_ITEMS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_CREATURE_HELD_ITEMS_TABLE, id)
	}
}
