package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameCreatureAbilityRequest
import io.github.lishangbu.gamedata.dto.GameCreatureAbilityResponse
import io.github.lishangbu.gamedata.entity.GameCreatureAbility
import io.github.lishangbu.gamedata.entity.abilityId
import io.github.lishangbu.gamedata.entity.creatureId
import io.github.lishangbu.gamedata.entity.hidden
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.slotOrder
import io.github.lishangbu.gamedata.repository.GameCreatureAbilityRepository
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
 * 精灵特性绑定维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameCreatureAbilityService(
	private val repository: GameCreatureAbilityRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameCreatureAbilityResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameCreatureAbility::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.creatureId) ilike pattern, sql<String>("cast(%e as text)", table.abilityId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"creature_id" -> gameDataLongFilterValue("creature_id", rawValue)?.let { where(table.creatureId eq it) }
				"ability_id" -> gameDataLongFilterValue("ability_id", rawValue)?.let { where(table.abilityId eq it) }
				"slot_order" -> gameDataIntFilterValue("slot_order", rawValue)?.let { where(table.slotOrder eq it) }
				"hidden" -> gameDataBooleanFilterValue("hidden", rawValue)?.let { where(table.hidden eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameCreatureAbilityResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameCreatureAbilityRequest): GameCreatureAbilityResponse =
		repository.save(
			GameCreatureAbility {
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				abilityId = request.abilityId ?: invalidValue("ability_id", "ability_id 不能为空")
				slotOrder = request.slotOrder ?: invalidValue("slot_order", "slot_order 不能为空")
				hidden = request.hidden
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameCreatureAbilityRequest): GameCreatureAbilityResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameCreatureAbility {
				this.id = id
				creatureId = request.creatureId ?: invalidValue("creature_id", "creature_id 不能为空")
				abilityId = request.abilityId ?: invalidValue("ability_id", "ability_id 不能为空")
				slotOrder = request.slotOrder ?: invalidValue("slot_order", "slot_order 不能为空")
				hidden = request.hidden
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameCreatureAbility =
		repository.findNullable(id) ?: notFound("id", "精灵特性绑定不存在: $id")

	private fun GameCreatureAbility.toResponse(): GameCreatureAbilityResponse =
		GameCreatureAbilityResponse {
			id = this@toResponse.id
			creatureId = this@toResponse.creatureId
			abilityId = this@toResponse.abilityId
			slotOrder = this@toResponse.slotOrder
			hidden = this@toResponse.hidden
		}
}
