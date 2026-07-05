package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ITEM_CATEGORY_POCKETS_TABLE = GameDataTableSpec(
	tableName = "game_item_category_pocket",
	label = "道具分类口袋",
	columns = listOf(
		GameDataColumnSpec(name = "category_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "pocket_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("category_id", "pocket_id"),
)

/**
 * 道具分类口袋持久化访问。
 */
@Repository
class GameItemCategoryPocketsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ITEM_CATEGORY_POCKETS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ITEM_CATEGORY_POCKETS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ITEM_CATEGORY_POCKETS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ITEM_CATEGORY_POCKETS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ITEM_CATEGORY_POCKETS_TABLE, id)
	}
}
