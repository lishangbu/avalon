package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_EVOLUTION_TRIGGERS_TABLE = GameDataTableSpec(
	tableName = "game_evolution_trigger",
	label = "进化触发器",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 进化触发器 Service。
 */
@Service
class GameEvolutionTriggersService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_EVOLUTION_TRIGGERS_TABLE)
