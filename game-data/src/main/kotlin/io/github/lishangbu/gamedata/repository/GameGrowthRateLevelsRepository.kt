package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_GROWTH_RATE_LEVELS_TABLE = GameDataTableSpec(
	tableName = "game_growth_rate_level",
	label = "成长等级经验",
	columns = listOf(
		GameDataColumnSpec(name = "growth_rate_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "experience", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("growth_rate_id", "level"),
)

/**
 * 成长等级经验持久化访问。
 */
@Repository
class GameGrowthRateLevelsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_GROWTH_RATE_LEVELS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_GROWTH_RATE_LEVELS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_GROWTH_RATE_LEVELS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_GROWTH_RATE_LEVELS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_GROWTH_RATE_LEVELS_TABLE, id)
	}
}
