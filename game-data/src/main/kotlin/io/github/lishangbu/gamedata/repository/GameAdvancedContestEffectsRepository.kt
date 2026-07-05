package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ADVANCED_CONTEST_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_advanced_contest_effect",
	label = "高级评价效果",
	columns = listOf(
		GameDataColumnSpec(name = "appeal", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("flavor_text"),
)

/**
 * 高级评价效果持久化访问。
 */
@Repository
class GameAdvancedContestEffectsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ADVANCED_CONTEST_EFFECTS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ADVANCED_CONTEST_EFFECTS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ADVANCED_CONTEST_EFFECTS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ADVANCED_CONTEST_EFFECTS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ADVANCED_CONTEST_EFFECTS_TABLE, id)
	}
}
