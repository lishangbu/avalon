package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameTransferAreaSpeciesRequest
import io.github.lishangbu.gamedata.dto.GameTransferAreaSpeciesResponse
import io.github.lishangbu.gamedata.entity.GameTransferAreaSpecies
import io.github.lishangbu.gamedata.entity.areaId
import io.github.lishangbu.gamedata.entity.baseScore
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.rate
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameTransferAreaSpeciesRepository
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
 * 迁移区域种类维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameTransferAreaSpeciesService(
	private val repository: GameTransferAreaSpeciesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameTransferAreaSpeciesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameTransferAreaSpecies::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.areaId) ilike pattern, sql<String>("cast(%e as text)", table.speciesId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"area_id" -> gameDataLongFilterValue("area_id", rawValue)?.let { where(table.areaId eq it) }
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"base_score" -> gameDataIntFilterValue("base_score", rawValue)?.let { where(table.baseScore eq it) }
				"rate" -> gameDataIntFilterValue("rate", rawValue)?.let { where(table.rate eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameTransferAreaSpeciesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameTransferAreaSpeciesRequest): GameTransferAreaSpeciesResponse =
		repository.save(
			GameTransferAreaSpecies {
				areaId = request.areaId ?: invalidValue("area_id", "area_id 不能为空")
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				baseScore = request.baseScore ?: invalidValue("base_score", "base_score 不能为空")
				rate = request.rate ?: invalidValue("rate", "rate 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameTransferAreaSpeciesRequest): GameTransferAreaSpeciesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameTransferAreaSpecies {
				this.id = id
				areaId = request.areaId ?: invalidValue("area_id", "area_id 不能为空")
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				baseScore = request.baseScore ?: invalidValue("base_score", "base_score 不能为空")
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

	private fun entityByIdOrNotFound(id: Long): GameTransferAreaSpecies =
		repository.findNullable(id) ?: notFound("id", "迁移区域种类不存在: $id")

	private fun GameTransferAreaSpecies.toResponse(): GameTransferAreaSpeciesResponse =
		GameTransferAreaSpeciesResponse {
			id = this@toResponse.id
			areaId = this@toResponse.areaId
			speciesId = this@toResponse.speciesId
			baseScore = this@toResponse.baseScore
			rate = this@toResponse.rate
		}
}
