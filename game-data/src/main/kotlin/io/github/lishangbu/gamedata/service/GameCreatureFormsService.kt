package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureFormsRequest
import io.github.lishangbu.gamedata.dto.GameCreatureFormsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureFormsRepository
import org.springframework.stereotype.Service

/**
 * 生物形态 Service。
 */
@Service
class GameCreatureFormsService(
	private val repository: GameCreatureFormsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureFormsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureFormsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureFormsResponse =
		GameCreatureFormsResponse.from(repository.get(id))

	fun create(request: GameCreatureFormsRequest): GameCreatureFormsResponse =
		GameCreatureFormsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureFormsRequest): GameCreatureFormsResponse =
		GameCreatureFormsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureFormsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
