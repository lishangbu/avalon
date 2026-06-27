package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_CREATURE_ELEMENT_TABLE = GameDataTableSpec(
	tableName = "game_creature_element",
	label = "生物属性绑定",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id", "element_id"),
)

/**
 * 生物属性绑定持久化访问。
 */
@Repository
class GameCreatureElementRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CREATURE_ELEMENT_TABLE)
