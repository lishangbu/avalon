package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_BERRY_FLAVOR_POTENCIES_TABLE = GameDataTableSpec(
	tableName = "game_berry_flavor_potency",
	label = "树果口味强度",
	columns = listOf(
		GameDataColumnSpec(name = "berry_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "flavor_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "potency", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("berry_id", "flavor_id"),
)

/**
 * 树果口味强度持久化访问。
 */
@Repository
class GameBerryFlavorPotenciesRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_BERRY_FLAVOR_POTENCIES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_BERRY_FLAVOR_POTENCIES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_BERRY_FLAVOR_POTENCIES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_BERRY_FLAVOR_POTENCIES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_BERRY_FLAVOR_POTENCIES_TABLE, id)
	}
}
