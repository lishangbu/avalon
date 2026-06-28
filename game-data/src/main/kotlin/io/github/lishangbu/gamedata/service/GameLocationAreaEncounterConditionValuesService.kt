package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncounterConditionValuesRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaEncounterConditionValuesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameLocationAreaEncounterConditionValuesRepository
import org.springframework.stereotype.Service

/**
 * 区域遭遇条件绑定 Service。
 */
@Service
class GameLocationAreaEncounterConditionValuesService(
	private val repository: GameLocationAreaEncounterConditionValuesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameLocationAreaEncounterConditionValuesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameLocationAreaEncounterConditionValuesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameLocationAreaEncounterConditionValuesResponse =
		GameLocationAreaEncounterConditionValuesResponse.from(repository.get(id))

	fun create(request: GameLocationAreaEncounterConditionValuesRequest): GameLocationAreaEncounterConditionValuesResponse =
		GameLocationAreaEncounterConditionValuesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameLocationAreaEncounterConditionValuesRequest): GameLocationAreaEncounterConditionValuesResponse =
		GameLocationAreaEncounterConditionValuesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameLocationAreaEncounterConditionValuesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
