package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameStatRequest
import io.github.lishangbu.gamedata.dto.GameStatResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameStatRepository
import org.springframework.stereotype.Service

/**
 * 数值项 Service。
 */
@Service
class GameStatService(
	private val repository: GameStatRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameStatResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameStatResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameStatResponse =
		GameStatResponse.from(repository.get(id))

	fun create(request: GameStatRequest): GameStatResponse =
		GameStatResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameStatRequest): GameStatResponse =
		GameStatResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameStatRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
