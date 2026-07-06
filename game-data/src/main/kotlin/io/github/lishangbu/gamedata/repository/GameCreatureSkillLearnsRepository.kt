package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_CREATURE_SKILL_LEARNS_TABLE = GameDataTableSpec(
	tableName = "game_creature_skill_learn",
	label = "精灵技能学习",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "learn_method_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "level_learned_at", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("creature_id", "skill_id"),
)

/**
 * 精灵技能学习持久化访问。
 */
@Repository
class GameCreatureSkillLearnsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_CREATURE_SKILL_LEARNS_TABLE) {
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
