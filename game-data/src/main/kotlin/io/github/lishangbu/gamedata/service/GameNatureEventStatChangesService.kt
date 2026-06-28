package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameNatureEventStatChangesRequest
import io.github.lishangbu.gamedata.dto.GameNatureEventStatChangesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameNatureEventStatChangesRepository
import org.springframework.stereotype.Service

/**
 * 性格活动能力变化 Service。
 */
@Service
class GameNatureEventStatChangesService(
	private val repository: GameNatureEventStatChangesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameNatureEventStatChangesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameNatureEventStatChangesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameNatureEventStatChangesResponse =
		GameNatureEventStatChangesResponse.from(repository.get(id))

	fun create(request: GameNatureEventStatChangesRequest): GameNatureEventStatChangesResponse =
		GameNatureEventStatChangesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameNatureEventStatChangesRequest): GameNatureEventStatChangesResponse =
		GameNatureEventStatChangesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameNatureEventStatChangesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
