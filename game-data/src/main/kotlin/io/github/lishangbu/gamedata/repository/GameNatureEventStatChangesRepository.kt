package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_NATURE_EVENT_STAT_CHANGES_TABLE = GameDataTableSpec(
	tableName = "game_nature_event_stat_change",
	label = "性格活动能力变化",
	columns = listOf(
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "event_stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "max_change", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("nature_id", "event_stat_id"),
)

/**
 * 性格活动能力变化持久化访问。
 */
@Repository
class GameNatureEventStatChangesRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_NATURE_EVENT_STAT_CHANGES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_NATURE_EVENT_STAT_CHANGES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_NATURE_EVENT_STAT_CHANGES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_NATURE_EVENT_STAT_CHANGES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_NATURE_EVENT_STAT_CHANGES_TABLE, id)
	}
}
