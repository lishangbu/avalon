package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEncounterConditionValuesRequest
import io.github.lishangbu.gamedata.dto.GameEncounterConditionValuesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEncounterConditionValuesRepository
import org.springframework.stereotype.Service

/**
 * 遭遇条件值 Service。
 */
@Service
class GameEncounterConditionValuesService(
	private val repository: GameEncounterConditionValuesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEncounterConditionValuesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEncounterConditionValuesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEncounterConditionValuesResponse =
		GameEncounterConditionValuesResponse.from(repository.get(id))

	fun create(request: GameEncounterConditionValuesRequest): GameEncounterConditionValuesResponse =
		GameEncounterConditionValuesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEncounterConditionValuesRequest): GameEncounterConditionValuesResponse =
		GameEncounterConditionValuesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEncounterConditionValuesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
