package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameContestEffectsRequest
import io.github.lishangbu.gamedata.dto.GameContestEffectsResponse
import io.github.lishangbu.gamedata.entity.GameContestEffects
import io.github.lishangbu.gamedata.entity.appeal
import io.github.lishangbu.gamedata.entity.effect
import io.github.lishangbu.gamedata.entity.flavorText
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.jam
import io.github.lishangbu.gamedata.repository.GameContestEffectsRepository
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
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 评价效果维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameContestEffectsService(
	private val repository: GameContestEffectsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameContestEffectsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameContestEffects::class) {
			search.pattern?.let { pattern ->
				where(or(table.effect ilike pattern, table.flavorText ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"appeal" -> gameDataIntFilterValue("appeal", rawValue)?.let { where(table.appeal eq it) }
				"jam" -> gameDataIntFilterValue("jam", rawValue)?.let { where(table.jam eq it) }
				"effect" -> gameDataStringFilterValue("effect", rawValue)?.let { where(table.effect eq it) }
				"flavor_text" -> gameDataStringFilterValue("flavor_text", rawValue)?.let { where(table.flavorText eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameContestEffectsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameContestEffectsRequest): GameContestEffectsResponse =
		repository.save(
			GameContestEffects {
				appeal = request.appeal ?: invalidValue("appeal", "appeal 不能为空")
				jam = request.jam ?: invalidValue("jam", "jam 不能为空")
				effect = gameDataOptionalText(request.effect, "effect", null)
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameContestEffectsRequest): GameContestEffectsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameContestEffects {
				this.id = id
				appeal = request.appeal ?: invalidValue("appeal", "appeal 不能为空")
				jam = request.jam ?: invalidValue("jam", "jam 不能为空")
				effect = gameDataOptionalText(request.effect, "effect", null)
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

	private fun entityByIdOrNotFound(id: Long): GameContestEffects =
		repository.findNullable(id) ?: notFound("id", "评价效果不存在: $id")

	private fun GameContestEffects.toResponse(): GameContestEffectsResponse =
		GameContestEffectsResponse {
			id = this@toResponse.id
			appeal = this@toResponse.appeal
			jam = this@toResponse.jam
			effect = this@toResponse.effect
			flavorText = this@toResponse.flavorText
		}
}
