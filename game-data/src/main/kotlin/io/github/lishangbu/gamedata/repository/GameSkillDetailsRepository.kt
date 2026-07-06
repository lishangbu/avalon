package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

private val GAME_SKILL_DETAILS_TABLE = GameDataTableSpec(
	tableName = "game_skill_detail",
	label = "技能详情",
	columns = listOf(
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "ailment_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "category_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "target_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "contest_type_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "contest_effect_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "advanced_contest_effect_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "min_hits", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "max_hits", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "min_turns", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "max_turns", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "drain", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "healing", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "crit_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "ailment_chance", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "flinch_chance", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "stat_chance", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "short_effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("skill_id", "effect", "flavor_text"),
)

/**
 * 技能详情持久化访问。
 */
@Repository
class GameSkillDetailsRepository(
	sqlClient: KSqlClient,
) : GameDataJimmerRepository(sqlClient, GAME_SKILL_DETAILS_TABLE) {
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
