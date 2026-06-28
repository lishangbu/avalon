package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureGameIndicesRequest
import io.github.lishangbu.gamedata.dto.GameCreatureGameIndicesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureGameIndicesRepository
import org.springframework.stereotype.Service

/**
 * 生物索引 Service。
 */
@Service
class GameCreatureGameIndicesService(
	private val repository: GameCreatureGameIndicesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureGameIndicesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureGameIndicesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureGameIndicesResponse =
		GameCreatureGameIndicesResponse.from(repository.get(id))

	fun create(request: GameCreatureGameIndicesRequest): GameCreatureGameIndicesResponse =
		GameCreatureGameIndicesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureGameIndicesRequest): GameCreatureGameIndicesResponse =
		GameCreatureGameIndicesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureGameIndicesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
