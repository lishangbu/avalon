package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameTransferAreaSpeciesRequest
import io.github.lishangbu.gamedata.dto.GameTransferAreaSpeciesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameTransferAreaSpeciesRepository
import org.springframework.stereotype.Service

/**
 * 迁移区域种类 Service。
 */
@Service
class GameTransferAreaSpeciesService(
	private val repository: GameTransferAreaSpeciesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameTransferAreaSpeciesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameTransferAreaSpeciesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameTransferAreaSpeciesResponse =
		GameTransferAreaSpeciesResponse.from(repository.get(id))

	fun create(request: GameTransferAreaSpeciesRequest): GameTransferAreaSpeciesResponse =
		GameTransferAreaSpeciesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameTransferAreaSpeciesRequest): GameTransferAreaSpeciesResponse =
		GameTransferAreaSpeciesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameTransferAreaSpeciesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
