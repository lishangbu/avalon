package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameEvolutionNodesRequest
import io.github.lishangbu.gamedata.dto.GameEvolutionNodesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameEvolutionNodesRepository
import org.springframework.stereotype.Service

/**
 * 进化链节点 Service。
 */
@Service
class GameEvolutionNodesService(
	private val repository: GameEvolutionNodesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameEvolutionNodesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameEvolutionNodesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameEvolutionNodesResponse =
		GameEvolutionNodesResponse.from(repository.get(id))

	fun create(request: GameEvolutionNodesRequest): GameEvolutionNodesResponse =
		GameEvolutionNodesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameEvolutionNodesRequest): GameEvolutionNodesResponse =
		GameEvolutionNodesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameEvolutionNodesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
