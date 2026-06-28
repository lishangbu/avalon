package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameGenderEvolutionRequirementsRequest
import io.github.lishangbu.gamedata.dto.GameGenderEvolutionRequirementsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameGenderEvolutionRequirementsRepository
import org.springframework.stereotype.Service

/**
 * 性别进化要求 Service。
 */
@Service
class GameGenderEvolutionRequirementsService(
	private val repository: GameGenderEvolutionRequirementsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameGenderEvolutionRequirementsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameGenderEvolutionRequirementsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameGenderEvolutionRequirementsResponse =
		GameGenderEvolutionRequirementsResponse.from(repository.get(id))

	fun create(request: GameGenderEvolutionRequirementsRequest): GameGenderEvolutionRequirementsResponse =
		GameGenderEvolutionRequirementsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameGenderEvolutionRequirementsRequest): GameGenderEvolutionRequirementsResponse =
		GameGenderEvolutionRequirementsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameGenderEvolutionRequirementsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
