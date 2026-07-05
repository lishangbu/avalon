package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ITEM_FLING_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_item_fling_effect",
	label = "道具投掷效果",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "effect"),
)

/**
 * 道具投掷效果持久化访问。
 */
@Repository
class GameItemFlingEffectsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ITEM_FLING_EFFECTS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ITEM_FLING_EFFECTS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ITEM_FLING_EFFECTS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ITEM_FLING_EFFECTS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ITEM_FLING_EFFECTS_TABLE, id)
	}
}
