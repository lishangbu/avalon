package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.dto.GameDataPageResponse
import io.github.lishangbu.gamedata.dto.GameSkillBattleStylesRequest
import io.github.lishangbu.gamedata.dto.GameSkillBattleStylesResponse
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.repository.GameSkillBattleStylesRepository
import org.springframework.stereotype.Service

/**
 * 技能战斗风格 Service。
 */
@Service
class GameSkillBattleStylesService(
	private val repository: GameSkillBattleStylesRepository,
) {
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPageResponse<GameSkillBattleStylesResponse> {
		val records = repository.list(page, size, query, filters)
		return GameDataPageResponse(
			rows = records.rows.map(GameSkillBattleStylesResponse::from),
			totalRowCount = records.totalRowCount,
			totalPageCount = records.totalPageCount,
			page = records.page,
			size = records.size,
		)
	}

	fun get(id: Long): GameSkillBattleStylesResponse =
		GameSkillBattleStylesResponse.from(repository.get(id))

	fun create(request: GameSkillBattleStylesRequest): GameSkillBattleStylesResponse =
		GameSkillBattleStylesResponse.from(repository.create(request.toRecordRequest()))

	fun update(id: Long, request: GameSkillBattleStylesRequest): GameSkillBattleStylesResponse =
		GameSkillBattleStylesResponse.from(repository.update(id, request.toRecordRequest()))

	fun delete(id: Long) {
		repository.delete(id)
	}

	private fun GameSkillBattleStylesRequest.toRecordRequest(): GameDataRecordRequest =
		GameDataRecordRequest(fields = toFields())
}
