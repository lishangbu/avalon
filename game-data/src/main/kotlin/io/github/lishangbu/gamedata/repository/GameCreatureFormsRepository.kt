package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_CREATURE_FORMS_TABLE = GameDataTableSpec(
	tableName = "game_creature_form",
	label = "精灵形态",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "form_name", type = GameDataColumnType.STRING, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "form_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "battle_only", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "default_form", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "enhanced_form", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "form_name"),
)

/**
 * 精灵形态持久化访问。
 */
@Repository
class GameCreatureFormsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_CREATURE_FORMS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_CREATURE_FORMS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_CREATURE_FORMS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_CREATURE_FORMS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_CREATURE_FORMS_TABLE, id)
	}
}
