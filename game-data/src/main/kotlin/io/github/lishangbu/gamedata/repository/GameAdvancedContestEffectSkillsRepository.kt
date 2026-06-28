package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
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
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ADVANCED_CONTEST_EFFECT_SKILLS_TABLE, id)
	}
}
