package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

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
 * 树果口味强度 Service。
 */
@Service
class GameBerryFlavorPotenciesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_BERRY_FLAVOR_POTENCIES_TABLE)
