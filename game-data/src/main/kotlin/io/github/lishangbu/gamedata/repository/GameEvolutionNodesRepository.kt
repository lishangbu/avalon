package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_EVOLUTION_NODES_TABLE = GameDataTableSpec(
	tableName = "game_evolution_node",
	label = "进化链节点",
	columns = listOf(
		GameDataColumnSpec(name = "chain_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "parent_species_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "baby", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "node_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("chain_id", "species_id"),
)

/**
 * 进化链节点持久化访问。
 */
@Repository
class GameEvolutionNodesRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_EVOLUTION_NODES_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_EVOLUTION_NODES_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_EVOLUTION_NODES_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_EVOLUTION_NODES_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_EVOLUTION_NODES_TABLE, id)
	}
}
