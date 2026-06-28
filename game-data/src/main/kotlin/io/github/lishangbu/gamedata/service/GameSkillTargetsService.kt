package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillTargetsRequest
import io.github.lishangbu.gamedata.dto.GameSkillTargetsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillTargetsRepository
import org.springframework.stereotype.Service

/**
 * 技能目标 Service。
 */
@Service
class GameSkillTargetsService(
	private val repository: GameSkillTargetsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillTargetsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillTargetsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillTargetsResponse =
		GameSkillTargetsResponse.from(repository.get(id))

	fun create(request: GameSkillTargetsRequest): GameSkillTargetsResponse =
		GameSkillTargetsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillTargetsRequest): GameSkillTargetsResponse =
		GameSkillTargetsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillTargetsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
