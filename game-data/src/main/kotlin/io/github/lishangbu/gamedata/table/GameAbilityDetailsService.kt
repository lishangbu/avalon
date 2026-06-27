package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ABILITY_DETAILS_TABLE = GameDataTableSpec(
	tableName = "game_ability_detail",
	label = "特性详情",
	columns = listOf(
		GameDataColumnSpec(name = "ability_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "short_effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("ability_id", "effect", "flavor_text"),
)

/**
 * 特性详情 Service。
 */
@Service
class GameAbilityDetailsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ABILITY_DETAILS_TABLE)
