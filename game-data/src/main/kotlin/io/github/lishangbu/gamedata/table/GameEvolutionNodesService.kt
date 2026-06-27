package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

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
 * 进化链节点 Service。
 */
@Service
class GameEvolutionNodesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_EVOLUTION_NODES_TABLE)
