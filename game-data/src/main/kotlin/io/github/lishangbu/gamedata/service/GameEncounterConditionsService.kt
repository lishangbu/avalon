package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEncounterConditionsRequest
import io.github.lishangbu.gamedata.dto.GameEncounterConditionsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEncounterConditionsRepository
import org.springframework.stereotype.Service

/**
 * 遭遇条件 Service。
 */
@Service
class GameEncounterConditionsService(
	private val repository: GameEncounterConditionsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEncounterConditionsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEncounterConditionsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEncounterConditionsResponse =
		GameEncounterConditionsResponse.from(repository.get(id))

	fun create(request: GameEncounterConditionsRequest): GameEncounterConditionsResponse =
		GameEncounterConditionsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEncounterConditionsRequest): GameEncounterConditionsResponse =
		GameEncounterConditionsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEncounterConditionsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
