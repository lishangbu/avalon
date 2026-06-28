package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEncounterMethodsRequest
import io.github.lishangbu.gamedata.dto.GameEncounterMethodsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEncounterMethodsRepository
import org.springframework.stereotype.Service

/**
 * 遭遇方式 Service。
 */
@Service
class GameEncounterMethodsService(
	private val repository: GameEncounterMethodsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEncounterMethodsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEncounterMethodsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEncounterMethodsResponse =
		GameEncounterMethodsResponse.from(repository.get(id))

	fun create(request: GameEncounterMethodsRequest): GameEncounterMethodsResponse =
		GameEncounterMethodsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEncounterMethodsRequest): GameEncounterMethodsResponse =
		GameEncounterMethodsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEncounterMethodsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
