package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameStatSkillEffectsRequest
import io.github.lishangbu.gamedata.dto.GameStatSkillEffectsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameStatSkillEffectsRepository
import org.springframework.stereotype.Service

/**
 * 数值项技能影响 Service。
 */
@Service
class GameStatSkillEffectsService(
	private val repository: GameStatSkillEffectsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameStatSkillEffectsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameStatSkillEffectsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameStatSkillEffectsResponse =
		GameStatSkillEffectsResponse.from(repository.get(id))

	fun create(request: GameStatSkillEffectsRequest): GameStatSkillEffectsResponse =
		GameStatSkillEffectsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameStatSkillEffectsRequest): GameStatSkillEffectsResponse =
		GameStatSkillEffectsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameStatSkillEffectsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
