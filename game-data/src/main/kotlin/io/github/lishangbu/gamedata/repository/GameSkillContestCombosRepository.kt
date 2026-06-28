package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_SKILL_CONTEST_COMBOS_TABLE = GameDataTableSpec(
	tableName = "game_skill_contest_combo",
	label = "技能评价组合",
	columns = listOf(
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "combo_type", type = GameDataColumnType.STRING, required = true, maxLength = 40),
		GameDataColumnSpec(name = "relation_type", type = GameDataColumnType.STRING, required = true, maxLength = 40),
		GameDataColumnSpec(name = "related_skill_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("skill_id", "related_skill_id"),
)

/**
 * 技能评价组合持久化访问。
 */
@Repository
class GameSkillContestCombosRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_SKILL_CONTEST_COMBOS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_SKILL_CONTEST_COMBOS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_SKILL_CONTEST_COMBOS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_SKILL_CONTEST_COMBOS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_SKILL_CONTEST_COMBOS_TABLE, id)
	}
}
