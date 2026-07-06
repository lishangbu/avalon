package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

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
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_EVOLUTION_NODES_TABLE) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		listRecords(page, size, query, filters)

	@Transactional(readOnly = true)
	fun get(id: Long): GameDataRecordResponse =
		getRecord(id)

	@Transactional
	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		createRecord(request)

	@Transactional
	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		updateRecord(id, request)

	@Transactional
	fun delete(id: Long) {
		deleteRecord(id)
	}
}
