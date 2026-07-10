package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameLocationAreaMethodRatesRequest
import io.github.lishangbu.gamedata.dto.GameLocationAreaMethodRatesResponse
import io.github.lishangbu.gamedata.entity.GameLocationAreaMethodRates
import io.github.lishangbu.gamedata.entity.areaId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.methodId
import io.github.lishangbu.gamedata.entity.rate
import io.github.lishangbu.gamedata.repository.GameLocationAreaMethodRatesRepository
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
 * 区域遭遇方式概率维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameLocationAreaMethodRatesService(
	private val repository: GameLocationAreaMethodRatesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameLocationAreaMethodRatesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameLocationAreaMethodRates::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.areaId) ilike pattern, sql<String>("cast(%e as text)", table.methodId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"area_id" -> gameDataLongFilterValue("area_id", rawValue)?.let { where(table.areaId eq it) }
				"method_id" -> gameDataLongFilterValue("method_id", rawValue)?.let { where(table.methodId eq it) }
				"rate" -> gameDataIntFilterValue("rate", rawValue)?.let { where(table.rate eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameLocationAreaMethodRatesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameLocationAreaMethodRatesRequest): GameLocationAreaMethodRatesResponse =
		repository.save(
			GameLocationAreaMethodRates {
				areaId = request.areaId ?: invalidValue("area_id", "area_id 不能为空")
				methodId = request.methodId ?: invalidValue("method_id", "method_id 不能为空")
				rate = request.rate ?: invalidValue("rate", "rate 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameLocationAreaMethodRatesRequest): GameLocationAreaMethodRatesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameLocationAreaMethodRates {
				this.id = id
				areaId = request.areaId ?: invalidValue("area_id", "area_id 不能为空")
				methodId = request.methodId ?: invalidValue("method_id", "method_id 不能为空")
				rate = request.rate ?: invalidValue("rate", "rate 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameLocationAreaMethodRates =
		repository.findNullable(id) ?: notFound("id", "区域遭遇方式概率不存在: $id")

	private fun GameLocationAreaMethodRates.toResponse(): GameLocationAreaMethodRatesResponse =
		GameLocationAreaMethodRatesResponse {
			id = this@toResponse.id
			areaId = this@toResponse.areaId
			methodId = this@toResponse.methodId
			rate = this@toResponse.rate
		}
}
