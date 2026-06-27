package io.github.lishangbu.gamedata.repository

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
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_EVOLUTION_CHAINS_TABLE)
