package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemAttributesRequest
import io.github.lishangbu.gamedata.dto.GameItemAttributesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemAttributesRepository
import org.springframework.stereotype.Service

/**
 * 道具属性 Service。
 */
@Service
class GameItemAttributesService(
	private val repository: GameItemAttributesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemAttributesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemAttributesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemAttributesResponse =
		GameItemAttributesResponse.from(repository.get(id))

	fun create(request: GameItemAttributesRequest): GameItemAttributesResponse =
		GameItemAttributesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemAttributesRequest): GameItemAttributesResponse =
		GameItemAttributesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemAttributesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
