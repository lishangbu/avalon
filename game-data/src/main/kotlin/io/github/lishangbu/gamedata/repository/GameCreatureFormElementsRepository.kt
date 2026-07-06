package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

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
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_CREATURE_FORM_ELEMENTS_TABLE) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> =
		listRecords(page, size, query, filters)

	@Transactional(readOnly = true)
	fun get(id: Long): GameDataRecordResponse =
		getRecord(id)

	@Transactional
	fun create(request: GameDataRecordRequest): GameDataRecordResponse =
		createRecord(request)

	@Transactional
	fun update(id: Long, request: GameDataRecordRequest): GameDataRecordResponse =
		updateRecord(id, request)

	@Transactional
	fun delete(id: Long) {
		deleteRecord(id)
	}
}
