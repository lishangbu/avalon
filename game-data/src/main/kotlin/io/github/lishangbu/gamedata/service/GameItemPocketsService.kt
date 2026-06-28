package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemPocketsRequest
import io.github.lishangbu.gamedata.dto.GameItemPocketsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemPocketsRepository
import org.springframework.stereotype.Service

/**
 * 道具口袋 Service。
 */
@Service
class GameItemPocketsService(
	private val repository: GameItemPocketsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemPocketsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemPocketsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemPocketsResponse =
		GameItemPocketsResponse.from(repository.get(id))

	fun create(request: GameItemPocketsRequest): GameItemPocketsResponse =
		GameItemPocketsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemPocketsRequest): GameItemPocketsResponse =
		GameItemPocketsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemPocketsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
