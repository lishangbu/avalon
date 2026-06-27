package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_EVOLUTION_CHAINS_TABLE = GameDataTableSpec(
	tableName = "game_evolution_chain",
	label = "进化链",
	columns = listOf(
		GameDataColumnSpec(name = "baby_trigger_item_id", type = GameDataColumnType.LONG),
	),
	searchColumns = listOf("baby_trigger_item_id"),
)

/**
 * 进化链 Service。
 */
@Service
class GameEvolutionChainsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_EVOLUTION_CHAINS_TABLE)
