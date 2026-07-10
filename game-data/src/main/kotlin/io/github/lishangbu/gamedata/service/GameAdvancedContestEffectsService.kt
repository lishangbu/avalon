package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectsRequest
import io.github.lishangbu.gamedata.dto.GameAdvancedContestEffectsResponse
import io.github.lishangbu.gamedata.entity.GameAdvancedContestEffects
import io.github.lishangbu.gamedata.entity.appeal
import io.github.lishangbu.gamedata.entity.flavorText
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.repository.GameAdvancedContestEffectsRepository
import io.github.lishangbu.gamedata.support.gameDataBooleanFilterValue
import io.github.lishangbu.gamedata.support.gameDataIntFilterValue
import io.github.lishangbu.gamedata.support.gameDataLongFilterValue
import io.github.lishangbu.gamedata.support.gameDataOptionalText
import io.github.lishangbu.gamedata.support.gameDataRequiredText
import io.github.lishangbu.gamedata.support.gameDataStringFilterValue
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 高级评价效果维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameAdvancedContestEffectsService(
	private val repository: GameAdvancedContestEffectsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameAdvancedContestEffectsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameAdvancedContestEffects::class) {
			search.pattern?.let { pattern ->
				where(table.flavorText ilike pattern)
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"appeal" -> gameDataIntFilterValue("appeal", rawValue)?.let { where(table.appeal eq it) }
				"flavor_text" -> gameDataStringFilterValue("flavor_text", rawValue)?.let { where(table.flavorText eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameAdvancedContestEffectsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameAdvancedContestEffectsRequest): GameAdvancedContestEffectsResponse =
		repository.save(
			GameAdvancedContestEffects {
				appeal = request.appeal ?: invalidValue("appeal", "appeal 不能为空")
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameAdvancedContestEffectsRequest): GameAdvancedContestEffectsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameAdvancedContestEffects {
				this.id = id
				appeal = request.appeal ?: invalidValue("appeal", "appeal 不能为空")
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameAdvancedContestEffects =
		repository.findNullable(id) ?: notFound("id", "高级评价效果不存在: $id")

	private fun GameAdvancedContestEffects.toResponse(): GameAdvancedContestEffectsResponse =
		GameAdvancedContestEffectsResponse {
			id = this@toResponse.id
			appeal = this@toResponse.appeal
			flavorText = this@toResponse.flavorText
		}
}
