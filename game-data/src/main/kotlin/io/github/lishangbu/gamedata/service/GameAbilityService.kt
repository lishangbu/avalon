package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameAbilityRequest
import io.github.lishangbu.gamedata.dto.GameAbilityResponse
import io.github.lishangbu.gamedata.entity.GameAbility
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.mainSeries
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.repository.GameAbilityRepository
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
 * 特性资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameAbilityService(
	private val repository: GameAbilityRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameAbilityResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameAbility::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"main_series" -> gameDataBooleanFilterValue("main_series", rawValue)?.let { where(table.mainSeries eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameAbilityResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameAbilityRequest): GameAbilityResponse =
		repository.save(
			GameAbility {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				mainSeries = request.mainSeries
				enabled = request.enabled
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameAbilityRequest): GameAbilityResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameAbility {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				mainSeries = request.mainSeries
				enabled = request.enabled
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameAbility =
		repository.findNullable(id) ?: notFound("id", "特性资料不存在: $id")

	private fun GameAbility.toResponse(): GameAbilityResponse =
		GameAbilityResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			mainSeries = this@toResponse.mainSeries
			enabled = this@toResponse.enabled
		}
}
