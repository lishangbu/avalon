package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemFlingEffectsRequest
import io.github.lishangbu.gamedata.dto.GameItemFlingEffectsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemFlingEffectsRepository
import org.springframework.stereotype.Service

/**
 * 道具投掷效果 Service。
 */
@Service
class GameItemFlingEffectsService(
	private val repository: GameItemFlingEffectsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemFlingEffectsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemFlingEffectsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemFlingEffectsResponse =
		GameItemFlingEffectsResponse.from(repository.get(id))

	fun create(request: GameItemFlingEffectsRequest): GameItemFlingEffectsResponse =
		GameItemFlingEffectsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemFlingEffectsRequest): GameItemFlingEffectsResponse =
		GameItemFlingEffectsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemFlingEffectsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
