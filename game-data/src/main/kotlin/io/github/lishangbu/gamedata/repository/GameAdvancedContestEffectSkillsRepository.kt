package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 高级评价效果技能持久化访问。
 */
@Repository
class GameAdvancedContestEffectSkillsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE)
