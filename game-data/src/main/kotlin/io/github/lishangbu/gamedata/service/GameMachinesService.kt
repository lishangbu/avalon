package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameMachinesRequest
import io.github.lishangbu.gamedata.dto.GameMachinesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameMachinesRepository
import org.springframework.stereotype.Service

/**
 * 机器资料 Service。
 */
@Service
class GameMachinesService(
	private val repository: GameMachinesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameMachinesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameMachinesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameMachinesResponse =
		GameMachinesResponse.from(repository.get(id))

	fun create(request: GameMachinesRequest): GameMachinesResponse =
		GameMachinesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameMachinesRequest): GameMachinesResponse =
		GameMachinesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameMachinesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
