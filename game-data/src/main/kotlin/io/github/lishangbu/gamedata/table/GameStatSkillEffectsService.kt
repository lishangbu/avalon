package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_STAT_SKILL_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_stat_skill_effect",
	label = "数值项技能影响",
	columns = listOf(
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "change_value", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "effect_type", type = GameDataColumnType.STRING, required = true, maxLength = 20),
	),
	searchColumns = listOf("stat_id", "skill_id"),
)

/**
 * 数值项技能影响 Service。
 */
@Service
class GameStatSkillEffectsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_STAT_SKILL_EFFECTS_TABLE)
