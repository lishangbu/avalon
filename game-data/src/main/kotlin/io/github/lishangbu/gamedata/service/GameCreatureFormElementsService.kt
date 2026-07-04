package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureFormElementsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureFormElementsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureFormElementsRepository
import org.springframework.stereotype.Service

/**
 * 精灵形态属性 Service。
 */
@Service
class GameCreatureFormElementsService(
	private val repository: GameCreatureFormElementsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureFormElementsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureFormElementsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureFormElementsResponse =
		GameCreatureFormElementsResponse.from(repository.get(id))

	fun create(request: GameCreatureFormElementsRequest): GameCreatureFormElementsResponse =
		GameCreatureFormElementsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureFormElementsRequest): GameCreatureFormElementsResponse =
		GameCreatureFormElementsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureFormElementsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
