package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SKILL_BATTLE_STYLES_TABLE = GameDataTableSpec(
	tableName = "game_skill_battle_style",
	label = "技能战斗风格",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 技能战斗风格 Service。
 */
@Service
class GameSkillBattleStylesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SKILL_BATTLE_STYLES_TABLE)
