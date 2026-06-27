package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_TABLE = GameDataTableSpec(
	tableName = "game_creature",
	label = "生物资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "height", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "weight", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "base_experience", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "default_form", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 生物资料 Service。
 */
@Service
class GameCreatureService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_TABLE)
