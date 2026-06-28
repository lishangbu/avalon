package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameCreatureStatRequest
import io.github.lishangbu.gamedata.dto.GameCreatureStatResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameCreatureStatRepository
import org.springframework.stereotype.Service

/**
 * 生物数值绑定 Service。
 */
@Service
class GameCreatureStatService(
	private val repository: GameCreatureStatRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameCreatureStatResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameCreatureStatResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameCreatureStatResponse =
		GameCreatureStatResponse.from(repository.get(id))

	fun create(request: GameCreatureStatRequest): GameCreatureStatResponse =
		GameCreatureStatResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameCreatureStatRequest): GameCreatureStatResponse =
		GameCreatureStatResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameCreatureStatRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
