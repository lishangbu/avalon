package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameTransferAreasRequest
import io.github.lishangbu.gamedata.dto.GameTransferAreasResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameTransferAreasRepository
import org.springframework.stereotype.Service

/**
 * 迁移区域 Service。
 */
@Service
class GameTransferAreasService(
	private val repository: GameTransferAreasRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameTransferAreasResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameTransferAreasResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameTransferAreasResponse =
		GameTransferAreasResponse.from(repository.get(id))

	fun create(request: GameTransferAreasRequest): GameTransferAreasResponse =
		GameTransferAreasResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameTransferAreasRequest): GameTransferAreasResponse =
		GameTransferAreasResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameTransferAreasRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
