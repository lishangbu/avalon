package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameNaturesRequest
import io.github.lishangbu.gamedata.dto.GameNaturesResponse
import io.github.lishangbu.gamedata.entity.GameNatures
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.decreasedStatId
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.hatesFlavorId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.increasedStatId
import io.github.lishangbu.gamedata.entity.likesFlavorId
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.repository.GameNaturesRepository
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
 * 性格资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameNaturesService(
	private val repository: GameNaturesRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameNaturesResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameNatures::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"increased_stat_id" -> gameDataLongFilterValue("increased_stat_id", rawValue)?.let { where(table.increasedStatId eq it) }
				"decreased_stat_id" -> gameDataLongFilterValue("decreased_stat_id", rawValue)?.let { where(table.decreasedStatId eq it) }
				"likes_flavor_id" -> gameDataLongFilterValue("likes_flavor_id", rawValue)?.let { where(table.likesFlavorId eq it) }
				"hates_flavor_id" -> gameDataLongFilterValue("hates_flavor_id", rawValue)?.let { where(table.hatesFlavorId eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameNaturesResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameNaturesRequest): GameNaturesResponse =
		repository.save(
			GameNatures {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				increasedStatId = request.increasedStatId
				decreasedStatId = request.decreasedStatId
				likesFlavorId = request.likesFlavorId
				hatesFlavorId = request.hatesFlavorId
				enabled = request.enabled ?: invalidValue("enabled", "enabled 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameNaturesRequest): GameNaturesResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameNatures {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				increasedStatId = request.increasedStatId
				decreasedStatId = request.decreasedStatId
				likesFlavorId = request.likesFlavorId
				hatesFlavorId = request.hatesFlavorId
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

	private fun entityByIdOrNotFound(id: Long): GameNatures =
		repository.findNullable(id) ?: notFound("id", "性格资料不存在: $id")

	private fun GameNatures.toResponse(): GameNaturesResponse =
		GameNaturesResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			increasedStatId = this@toResponse.increasedStatId
			decreasedStatId = this@toResponse.decreasedStatId
			likesFlavorId = this@toResponse.likesFlavorId
			hatesFlavorId = this@toResponse.hatesFlavorId
			enabled = this@toResponse.enabled
		}
}
