package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameEventStatNatureEffectsRequest
import io.github.lishangbu.gamedata.dto.GameEventStatNatureEffectsResponse
import io.github.lishangbu.gamedata.entity.GameEventStatNatureEffects
import io.github.lishangbu.gamedata.entity.effectType
import io.github.lishangbu.gamedata.entity.eventStatId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.natureId
import io.github.lishangbu.gamedata.repository.GameEventStatNatureEffectsRepository
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
import org.babyfish.jimmer.sql.kt.ast.expression.sql
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 活动能力性格影响维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameEventStatNatureEffectsService(
	private val repository: GameEventStatNatureEffectsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameEventStatNatureEffectsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameEventStatNatureEffects::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.eventStatId) ilike pattern, sql<String>("cast(%e as text)", table.natureId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"event_stat_id" -> gameDataLongFilterValue("event_stat_id", rawValue)?.let { where(table.eventStatId eq it) }
				"nature_id" -> gameDataLongFilterValue("nature_id", rawValue)?.let { where(table.natureId eq it) }
				"effect_type" -> gameDataStringFilterValue("effect_type", rawValue)?.let { where(table.effectType eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameEventStatNatureEffectsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameEventStatNatureEffectsRequest): GameEventStatNatureEffectsResponse =
		repository.save(
			GameEventStatNatureEffects {
				eventStatId = request.eventStatId ?: invalidValue("event_stat_id", "event_stat_id 不能为空")
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				effectType = gameDataRequiredText(request.effectType, "effect_type", 20)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameEventStatNatureEffectsRequest): GameEventStatNatureEffectsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameEventStatNatureEffects {
				this.id = id
				eventStatId = request.eventStatId ?: invalidValue("event_stat_id", "event_stat_id 不能为空")
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				effectType = gameDataRequiredText(request.effectType, "effect_type", 20)
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameEventStatNatureEffects =
		repository.findNullable(id) ?: notFound("id", "活动能力性格影响不存在: $id")

	private fun GameEventStatNatureEffects.toResponse(): GameEventStatNatureEffectsResponse =
		GameEventStatNatureEffectsResponse {
			id = this@toResponse.id
			eventStatId = this@toResponse.eventStatId
			natureId = this@toResponse.natureId
			effectType = this@toResponse.effectType
		}
}
