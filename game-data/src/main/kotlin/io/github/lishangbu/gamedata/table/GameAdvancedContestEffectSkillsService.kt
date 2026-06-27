package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE = GameDataTableSpec(
	tableName = "game_advanced_contest_effect_skill",
	label = "高级评价效果技能",
	columns = listOf(
		GameDataColumnSpec(name = "advanced_contest_effect_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("advanced_contest_effect_id", "skill_id"),
)

/**
 * 高级评价效果技能 Service。
 */
@Service
class GameAdvancedContestEffectSkillsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE)
