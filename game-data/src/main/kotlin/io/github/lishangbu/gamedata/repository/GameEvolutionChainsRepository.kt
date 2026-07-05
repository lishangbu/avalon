package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_EVOLUTION_CHAINS_TABLE = GameDataTableSpec(
	tableName = "game_evolution_chain",
	label = "进化链",
	columns = listOf(
		GameDataColumnSpec(name = "baby_trigger_item_id", type = GameDataColumnType.LONG),
	),
	searchColumns = listOf("baby_trigger_item_id"),
)

/**
 * 进化链持久化访问。
 */
@Repository
class GameEvolutionChainsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_EVOLUTION_CHAINS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_EVOLUTION_CHAINS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_EVOLUTION_CHAINS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_EVOLUTION_CHAINS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_EVOLUTION_CHAINS_TABLE, id)
	}
}
