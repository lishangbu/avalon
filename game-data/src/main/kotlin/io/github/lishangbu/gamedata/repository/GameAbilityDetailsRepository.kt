package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ABILITY_DETAILS_TABLE = GameDataTableSpec(
	tableName = "game_ability_detail",
	label = "特性详情",
	columns = listOf(
		GameDataColumnSpec(name = "ability_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "short_effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("ability_id", "effect", "flavor_text"),
)

/**
 * 特性详情持久化访问。
 */
@Repository
class GameAbilityDetailsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ABILITY_DETAILS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ABILITY_DETAILS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ABILITY_DETAILS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ABILITY_DETAILS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ABILITY_DETAILS_TABLE, id)
	}
}
