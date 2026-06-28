package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillRequest
import io.github.lishangbu.gamedata.dto.GameSkillResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillRepository
import org.springframework.stereotype.Service

/**
 * 技能资料 Service。
 */
@Service
class GameSkillService(
	private val repository: GameSkillRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillResponse =
		GameSkillResponse.from(repository.get(id))

	fun create(request: GameSkillRequest): GameSkillResponse =
		GameSkillResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillRequest): GameSkillResponse =
		GameSkillResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
