package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameHabitatRequest
import io.github.lishangbu.gamedata.dto.GameHabitatResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameHabitatRepository
import org.springframework.stereotype.Service

/**
 * 栖息地 Service。
 */
@Service
class GameHabitatService(
	private val repository: GameHabitatRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameHabitatResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameHabitatResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameHabitatResponse =
		GameHabitatResponse.from(repository.get(id))

	fun create(request: GameHabitatRequest): GameHabitatResponse =
		GameHabitatResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameHabitatRequest): GameHabitatResponse =
		GameHabitatResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameHabitatRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
