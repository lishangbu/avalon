package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_SKILL_TABLE = GameDataTableSpec(
	tableName = "game_skill",
	label = "技能资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "damage_class_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "accuracy", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "power", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "pp", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "priority", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "effect_chance", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 技能资料持久化访问。
 */
@Repository
class GameSkillRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_SKILL_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_SKILL_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_SKILL_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_SKILL_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_SKILL_TABLE, id)
	}
}
