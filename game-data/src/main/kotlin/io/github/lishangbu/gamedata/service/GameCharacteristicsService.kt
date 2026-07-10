package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCharacteristicsRequest
import io.github.lishangbu.gamedata.dto.GameCharacteristicsResponse
import io.github.lishangbu.gamedata.entity.GameCharacteristics
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.description
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.geneModulo
import io.github.lishangbu.gamedata.entity.highestStatId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.repository.GameCharacteristicsRepository
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
 * 个体特征维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCharacteristicsService(
	private val repository: GameCharacteristicsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCharacteristicsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCharacteristics::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern, table.description ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"highest_stat_id" -> gameDataLongFilterValue("highest_stat_id", rawValue)?.let { where(table.highestStatId eq it) }
				"gene_modulo" -> gameDataIntFilterValue("gene_modulo", rawValue)?.let { where(table.geneModulo eq it) }
				"description" -> gameDataStringFilterValue("description", rawValue)?.let { where(table.description eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCharacteristicsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCharacteristicsRequest): GameCharacteristicsResponse =
		repository.save(
			GameCharacteristics {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 200)
				highestStatId = request.highestStatId
				geneModulo = request.geneModulo ?: invalidValue("gene_modulo", "gene_modulo 不能为空")
				description = gameDataOptionalText(request.description, "description", null)
				enabled = request.enabled ?: invalidValue("enabled", "enabled 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCharacteristicsRequest): GameCharacteristicsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCharacteristics {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 200)
				highestStatId = request.highestStatId
				geneModulo = request.geneModulo ?: invalidValue("gene_modulo", "gene_modulo 不能为空")
				description = gameDataOptionalText(request.description, "description", null)
				enabled = request.enabled ?: invalidValue("enabled", "enabled 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCharacteristics =
		repository.findNullable(id) ?: notFound("id", "个体特征不存在: $id")

	private fun GameCharacteristics.toResponse(): GameCharacteristicsResponse =
		GameCharacteristicsResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			highestStatId = this@toResponse.highestStatId
			geneModulo = this@toResponse.geneModulo
			description = this@toResponse.description
			enabled = this@toResponse.enabled
		}
}
