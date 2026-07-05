package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_CREATURE_FORM_ELEMENTS_TABLE = GameDataTableSpec(
	tableName = "game_creature_form_element",
	label = "精灵形态属性",
	columns = listOf(
		GameDataColumnSpec(name = "form_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("form_id", "element_id"),
)

/**
 * 精灵形态属性持久化访问。
 */
@Repository
class GameCreatureFormElementsRepository(
	private val operations: GameDataJimmerOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_CREATURE_FORM_ELEMENTS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_CREATURE_FORM_ELEMENTS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_CREATURE_FORM_ELEMENTS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_CREATURE_FORM_ELEMENTS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_CREATURE_FORM_ELEMENTS_TABLE, id)
	}
}
