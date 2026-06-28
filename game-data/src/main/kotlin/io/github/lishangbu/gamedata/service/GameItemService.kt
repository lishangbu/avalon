package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameItemRequest
import io.github.lishangbu.gamedata.dto.GameItemResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameItemRepository
import org.springframework.stereotype.Service

/**
 * 道具资料 Service。
 */
@Service
class GameItemService(
	private val repository: GameItemRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameItemResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameItemResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameItemResponse =
		GameItemResponse.from(repository.get(id))

	fun create(request: GameItemRequest): GameItemResponse =
		GameItemResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameItemRequest): GameItemResponse =
		GameItemResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameItemRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
