package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_STAT_NATURE_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_stat_nature_effect",
	label = "数值项性格影响",
	columns = listOf(
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "effect_type", type = GameDataColumnType.STRING, required = true, maxLength = 20),
	),
	searchColumns = listOf("stat_id", "nature_id"),
)

/**
 * 数值项性格影响持久化访问。
 */
@Repository
class GameStatNatureEffectsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_STAT_NATURE_EFFECTS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_STAT_NATURE_EFFECTS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_STAT_NATURE_EFFECTS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_STAT_NATURE_EFFECTS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_STAT_NATURE_EFFECTS_TABLE, id)
	}
}
