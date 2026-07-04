package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureElementRequest
import io.github.lishangbu.gamedata.dto.GameCreatureElementResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureElementRepository
import org.springframework.stereotype.Service

/**
 * 精灵属性绑定 Service。
 */
@Service
class GameCreatureElementService(
	private val repository: GameCreatureElementRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureElementResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureElementResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureElementResponse =
		GameCreatureElementResponse.from(repository.get(id))

	fun create(request: GameCreatureElementRequest): GameCreatureElementResponse =
		GameCreatureElementResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureElementRequest): GameCreatureElementResponse =
		GameCreatureElementResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureElementRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
