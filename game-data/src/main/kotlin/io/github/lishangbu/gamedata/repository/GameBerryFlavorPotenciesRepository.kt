package io.github.lishangbu.gamedata.repository

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
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_BERRY_FLAVOR_POTENCIES_TABLE)
