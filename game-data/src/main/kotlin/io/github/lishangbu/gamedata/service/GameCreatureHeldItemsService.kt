package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureHeldItemsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureHeldItemsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureHeldItemsRepository
import org.springframework.stereotype.Service

/**
 * 精灵持有道具 Service。
 */
@Service
class GameCreatureHeldItemsService(
	private val repository: GameCreatureHeldItemsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureHeldItemsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureHeldItemsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureHeldItemsResponse =
		GameCreatureHeldItemsResponse.from(repository.get(id))

	fun create(request: GameCreatureHeldItemsRequest): GameCreatureHeldItemsResponse =
		GameCreatureHeldItemsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureHeldItemsRequest): GameCreatureHeldItemsResponse =
		GameCreatureHeldItemsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureHeldItemsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
