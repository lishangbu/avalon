package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_CREATURE_HELD_ITEMS_TABLE = GameDataTableSpec(
	tableName = "game_creature_held_item",
	label = "生物持有道具",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "rarity", type = GameDataColumnType.INT),
	),
	searchColumns = listOf("creature_id", "item_id"),
)

/**
 * 生物持有道具持久化访问。
 */
@Repository
class GameCreatureHeldItemsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CREATURE_HELD_ITEMS_TABLE)
