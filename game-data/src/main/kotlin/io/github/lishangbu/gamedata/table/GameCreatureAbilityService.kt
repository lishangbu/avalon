package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_ABILITY_TABLE = GameDataTableSpec(
	tableName = "game_creature_ability",
	label = "生物特性绑定",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "ability_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "hidden", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("creature_id", "ability_id"),
)

/**
 * 生物特性绑定 Service。
 */
@Service
class GameCreatureAbilityService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_ABILITY_TABLE)
