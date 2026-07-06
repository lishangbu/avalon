package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE = GameDataTableSpec(
	tableName = "game_gender_evolution_requirement",
	label = "性别进化要求",
	columns = listOf(
		GameDataColumnSpec(name = "gender_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("gender_id", "species_id"),
)

/**
 * 性别进化要求持久化访问。
 */
@Repository
class GameGenderEvolutionRequirementsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_GENDER_EVOLUTION_REQUIREMENTS_TABLE) {
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
