package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_NATURES_TABLE = GameDataTableSpec(
	tableName = "game_nature",
	label = "性格资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "increased_stat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "decreased_stat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "likes_flavor_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "hates_flavor_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 性格资料持久化访问。
 */
@Repository
class GameNaturesRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_NATURES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_NATURES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_NATURES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_NATURES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_NATURES_TABLE, id)
	}
}
