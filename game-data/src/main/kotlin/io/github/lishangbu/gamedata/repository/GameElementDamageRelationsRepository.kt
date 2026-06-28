package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.springframework.stereotype.Repository

private val GAME_ELEMENT_DAMAGE_RELATIONS_TABLE = GameDataTableSpec(
	tableName = "game_element_damage_relation",
	label = "属性克制关系",
	columns = listOf(
		GameDataColumnSpec(name = "source_element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "target_element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "relation_type", type = GameDataColumnType.STRING, required = true, maxLength = 40),
	),
	searchColumns = listOf("source_element_id", "target_element_id", "relation_type"),
)

/**
 * 属性克制关系持久化访问。
 */
@Repository
class GameElementDamageRelationsRepository(
	private val operations: GameDataJdbcOperations,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		operations.list(GAME_ELEMENT_DAMAGE_RELATIONS_TABLE, page, size, query, filters)

	fun get(id: Long): GameDataRecordResponse =
		operations.get(GAME_ELEMENT_DAMAGE_RELATIONS_TABLE, id)

	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		operations.create(GAME_ELEMENT_DAMAGE_RELATIONS_TABLE, request)

	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		operations.update(GAME_ELEMENT_DAMAGE_RELATIONS_TABLE, id, request)

	fun delete(id: Long) {
		operations.delete(GAME_ELEMENT_DAMAGE_RELATIONS_TABLE, id)
	}
}
