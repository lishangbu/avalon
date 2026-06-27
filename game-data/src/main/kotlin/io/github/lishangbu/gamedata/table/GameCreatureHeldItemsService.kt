package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_HELD_ITEMS_TABLE = GameDataTableSpec(
	tableName = "game_creature_held_item",
	label = "生物持有道具",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "version_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "rarity", type = GameDataColumnType.INT),
	),
	searchColumns = listOf("creature_id", "item_id", "version_id"),
)

/**
 * 生物持有道具 Service。
 */
@Service
class GameCreatureHeldItemsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_HELD_ITEMS_TABLE)
