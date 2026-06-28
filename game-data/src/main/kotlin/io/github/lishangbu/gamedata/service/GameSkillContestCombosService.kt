package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillContestCombosRequest
import io.github.lishangbu.gamedata.dto.GameSkillContestCombosResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillContestCombosRepository
import org.springframework.stereotype.Service

/**
 * 技能评价组合 Service。
 */
@Service
class GameSkillContestCombosService(
	private val repository: GameSkillContestCombosRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillContestCombosResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillContestCombosResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillContestCombosResponse =
		GameSkillContestCombosResponse.from(repository.get(id))

	fun create(request: GameSkillContestCombosRequest): GameSkillContestCombosResponse =
		GameSkillContestCombosResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillContestCombosRequest): GameSkillContestCombosResponse =
		GameSkillContestCombosResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillContestCombosRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
