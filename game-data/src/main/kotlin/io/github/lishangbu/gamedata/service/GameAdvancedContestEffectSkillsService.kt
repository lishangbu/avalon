package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectSkillsRequest
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectSkillsResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameAdvancedContestEffectSkillsRepository
import org.springframework.stereotype.Service

/**
 * 高级评价效果技能 Service。
 */
@Service
class GameAdvancedContestEffectSkillsService(
	private val repository: GameAdvancedContestEffectSkillsRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameAdvancedContestEffectSkillsResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameAdvancedContestEffectSkillsResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameAdvancedContestEffectSkillsResponse =
		GameAdvancedContestEffectSkillsResponse.from(repository.get(id))

	fun create(request: GameAdvancedContestEffectSkillsRequest): GameAdvancedContestEffectSkillsResponse =
		GameAdvancedContestEffectSkillsResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameAdvancedContestEffectSkillsRequest): GameAdvancedContestEffectSkillsResponse =
		GameAdvancedContestEffectSkillsResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameAdvancedContestEffectSkillsRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
