package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SKILL_STAT_CHANGES_TABLE = GameDataTableSpec(
	tableName = "game_skill_stat_change",
	label = "技能数值变化",
	columns = listOf(
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "change_value", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("skill_id", "stat_id"),
)

/**
 * 技能数值变化 Service。
 */
@Service
class GameSkillStatChangesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SKILL_STAT_CHANGES_TABLE)
