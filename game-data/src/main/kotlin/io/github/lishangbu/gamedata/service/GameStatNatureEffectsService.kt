package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameStatNatureEffectsRequest
import io.github.lishangbu.gamedata.dto.GameStatNatureEffectsResponse
import io.github.lishangbu.gamedata.entity.GameStatNatureEffects
import io.github.lishangbu.gamedata.entity.effectType
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.natureId
import io.github.lishangbu.gamedata.entity.statId
import io.github.lishangbu.gamedata.repository.GameStatNatureEffectsRepository
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
 * 数值项性格影响维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameStatNatureEffectsService(
	private val repository: GameStatNatureEffectsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameStatNatureEffectsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameStatNatureEffects::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.statId) ilike pattern, sql<String>("cast(%e as text)", table.natureId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"stat_id" -> gameDataLongFilterValue("stat_id", rawValue)?.let { where(table.statId eq it) }
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
	fun get(id: Long): GameStatNatureEffectsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameStatNatureEffectsRequest): GameStatNatureEffectsResponse =
		repository.save(
			GameStatNatureEffects {
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
				natureId = request.natureId ?: invalidValue("nature_id", "nature_id 不能为空")
				effectType = gameDataRequiredText(request.effectType, "effect_type", 20)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameStatNatureEffectsRequest): GameStatNatureEffectsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameStatNatureEffects {
				this.id = id
				statId = request.statId ?: invalidValue("stat_id", "stat_id 不能为空")
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

	private fun entityByIdOrNotFound(id: Long): GameStatNatureEffects =
		repository.findNullable(id) ?: notFound("id", "数值项性格影响不存在: $id")

	private fun GameStatNatureEffects.toResponse(): GameStatNatureEffectsResponse =
		GameStatNatureEffectsResponse {
			id = this@toResponse.id
			statId = this@toResponse.statId
			natureId = this@toResponse.natureId
			effectType = this@toResponse.effectType
		}
}
