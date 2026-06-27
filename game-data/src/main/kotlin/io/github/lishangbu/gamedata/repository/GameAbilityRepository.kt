package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ABILITY_TABLE = GameDataTableSpec(
	tableName = "game_ability",
	label = "特性资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "main_series", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 特性资料持久化访问。
 */
@Repository
class GameAbilityRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ABILITY_TABLE)
