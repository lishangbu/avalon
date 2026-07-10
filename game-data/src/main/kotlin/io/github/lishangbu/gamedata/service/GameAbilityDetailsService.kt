package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameAbilityDetailsRequest
import io.github.lishangbu.gamedata.dto.GameAbilityDetailsResponse
import io.github.lishangbu.gamedata.entity.GameAbilityDetails
import io.github.lishangbu.gamedata.entity.abilityId
import io.github.lishangbu.gamedata.entity.effect
import io.github.lishangbu.gamedata.entity.flavorText
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.shortEffect
import io.github.lishangbu.gamedata.repository.GameAbilityDetailsRepository
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
 * 特性详情维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameAbilityDetailsService(
	private val repository: GameAbilityDetailsRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameAbilityDetailsResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameAbilityDetails::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.abilityId) ilike pattern, table.effect ilike pattern, table.flavorText ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"ability_id" -> gameDataLongFilterValue("ability_id", rawValue)?.let { where(table.abilityId eq it) }
				"effect" -> gameDataStringFilterValue("effect", rawValue)?.let { where(table.effect eq it) }
				"short_effect" -> gameDataStringFilterValue("short_effect", rawValue)?.let { where(table.shortEffect eq it) }
				"flavor_text" -> gameDataStringFilterValue("flavor_text", rawValue)?.let { where(table.flavorText eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameAbilityDetailsResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameAbilityDetailsRequest): GameAbilityDetailsResponse =
		repository.save(
			GameAbilityDetails {
				abilityId = request.abilityId ?: invalidValue("ability_id", "ability_id 不能为空")
				effect = gameDataOptionalText(request.effect, "effect", null)
				shortEffect = gameDataOptionalText(request.shortEffect, "short_effect", null)
				flavorText = gameDataOptionalText(request.flavorText, "flavor_text", null)
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameAbilityDetailsRequest): GameAbilityDetailsResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameAbilityDetails {
				this.id = id
				abilityId = request.abilityId ?: invalidValue("ability_id", "ability_id 不能为空")
				effect = gameDataOptionalText(request.effect, "effect", null)
				shortEffect = gameDataOptionalText(request.shortEffect, "short_effect", null)
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

	private fun entityByIdOrNotFound(id: Long): GameAbilityDetails =
		repository.findNullable(id) ?: notFound("id", "特性详情不存在: $id")

	private fun GameAbilityDetails.toResponse(): GameAbilityDetailsResponse =
		GameAbilityDetailsResponse {
			id = this@toResponse.id
			abilityId = this@toResponse.abilityId
			effect = this@toResponse.effect
			shortEffect = this@toResponse.shortEffect
			flavorText = this@toResponse.flavorText
		}
}
