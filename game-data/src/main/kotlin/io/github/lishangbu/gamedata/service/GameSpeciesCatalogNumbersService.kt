package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSpeciesCatalogNumbersRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesCatalogNumbersResponse
import io.github.lishangbu.gamedata.entity.GameSpeciesCatalogNumbers
import io.github.lishangbu.gamedata.entity.catalogId
import io.github.lishangbu.gamedata.entity.entryNumber
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameSpeciesCatalogNumbersRepository
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
 * 种类目录编号维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSpeciesCatalogNumbersService(
	private val repository: GameSpeciesCatalogNumbersRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSpeciesCatalogNumbersResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSpeciesCatalogNumbers::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.speciesId) ilike pattern, sql<String>("cast(%e as text)", table.catalogId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"catalog_id" -> gameDataLongFilterValue("catalog_id", rawValue)?.let { where(table.catalogId eq it) }
				"entry_number" -> gameDataIntFilterValue("entry_number", rawValue)?.let { where(table.entryNumber eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSpeciesCatalogNumbersResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSpeciesCatalogNumbersRequest): GameSpeciesCatalogNumbersResponse =
		repository.save(
			GameSpeciesCatalogNumbers {
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				catalogId = request.catalogId ?: invalidValue("catalog_id", "catalog_id 不能为空")
				entryNumber = request.entryNumber ?: invalidValue("entry_number", "entry_number 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSpeciesCatalogNumbersRequest): GameSpeciesCatalogNumbersResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSpeciesCatalogNumbers {
				this.id = id
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				catalogId = request.catalogId ?: invalidValue("catalog_id", "catalog_id 不能为空")
				entryNumber = request.entryNumber ?: invalidValue("entry_number", "entry_number 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSpeciesCatalogNumbers =
		repository.findNullable(id) ?: notFound("id", "种类目录编号不存在: $id")

	private fun GameSpeciesCatalogNumbers.toResponse(): GameSpeciesCatalogNumbersResponse =
		GameSpeciesCatalogNumbersResponse {
			id = this@toResponse.id
			speciesId = this@toResponse.speciesId
			catalogId = this@toResponse.catalogId
			entryNumber = this@toResponse.entryNumber
		}
}
