package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE = GameDataTableSpec(
	tableName = "game_nature_battle_style_preference",
	label = "性格战斗风格偏好",
	columns = listOf(
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "battle_style_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "low_hp_preference", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "high_hp_preference", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("nature_id", "battle_style_id"),
)

/**
 * 性格战斗风格偏好 Service。
 */
@Service
class GameNatureBattleStylePreferencesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_NATURE_BATTLE_STYLE_PREFERENCES_TABLE)
