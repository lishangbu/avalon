package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_GROWTH_RATES_TABLE = GameDataTableSpec(
	tableName = "game_growth_rate",
	label = "成长速率",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "formula", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 成长速率持久化访问。
 */
@Repository
class GameGrowthRatesRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_GROWTH_RATES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_GROWTH_RATES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_GROWTH_RATES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_GROWTH_RATES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_GROWTH_RATES_TABLE, id)
	}
}
