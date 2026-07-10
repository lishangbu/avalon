package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameGenderSpeciesRatesRequest
import io.github.lishangbu.gamedata.dto.GameGenderSpeciesRatesResponse
import io.github.lishangbu.gamedata.entity.GameGenderSpeciesRates
import io.github.lishangbu.gamedata.entity.genderId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.rate
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameGenderSpeciesRatesRepository
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
 * 性别种类比例维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameGenderSpeciesRatesService(
	private val repository: GameGenderSpeciesRatesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameGenderSpeciesRatesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameGenderSpeciesRates::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.genderId) ilike pattern, sql<String>("cast(%e as text)", table.speciesId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"gender_id" -> gameDataLongFilterValue("gender_id", rawValue)?.let { where(table.genderId eq it) }
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"rate" -> gameDataIntFilterValue("rate", rawValue)?.let { where(table.rate eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameGenderSpeciesRatesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameGenderSpeciesRatesRequest): GameGenderSpeciesRatesResponse =
		repository.save(
			GameGenderSpeciesRates {
				genderId = request.genderId ?: invalidValue("gender_id", "gender_id 不能为空")
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				rate = request.rate ?: invalidValue("rate", "rate 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameGenderSpeciesRatesRequest): GameGenderSpeciesRatesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameGenderSpeciesRates {
				this.id = id
				genderId = request.genderId ?: invalidValue("gender_id", "gender_id 不能为空")
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
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

	private fun entityByIdOrNotFound(id: Long): GameGenderSpeciesRates =
		repository.findNullable(id) ?: notFound("id", "性别种类比例不存在: $id")

	private fun GameGenderSpeciesRates.toResponse(): GameGenderSpeciesRatesResponse =
		GameGenderSpeciesRatesResponse {
			id = this@toResponse.id
			genderId = this@toResponse.genderId
			speciesId = this@toResponse.speciesId
			rate = this@toResponse.rate
		}
}
