package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureRequest
import io.github.lishangbu.gamedata.dto.GameCreatureResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureRepository
import org.springframework.stereotype.Service

/**
 * 生物资料 Service。
 */
@Service
class GameCreatureService(
	private val repository: GameCreatureRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureResponse =
		GameCreatureResponse.from(repository.get(id))

	fun create(request: GameCreatureRequest): GameCreatureResponse =
		GameCreatureResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureRequest): GameCreatureResponse =
		GameCreatureResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
