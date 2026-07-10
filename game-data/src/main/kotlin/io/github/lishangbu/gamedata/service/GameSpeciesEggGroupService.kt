package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSpeciesEggGroupRequest
import io.github.lishangbu.gamedata.dto.GameSpeciesEggGroupResponse
import io.github.lishangbu.gamedata.entity.GameSpeciesEggGroup
import io.github.lishangbu.gamedata.entity.eggGroupId
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.slotOrder
import io.github.lishangbu.gamedata.entity.speciesId
import io.github.lishangbu.gamedata.repository.GameSpeciesEggGroupRepository
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
 * 种类分组绑定维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSpeciesEggGroupService(
	private val repository: GameSpeciesEggGroupRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSpeciesEggGroupResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSpeciesEggGroup::class) {
			search.pattern?.let { pattern ->
				where(or(sql<String>("cast(%e as text)", table.speciesId) ilike pattern, sql<String>("cast(%e as text)", table.eggGroupId) ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"species_id" -> gameDataLongFilterValue("species_id", rawValue)?.let { where(table.speciesId eq it) }
				"egg_group_id" -> gameDataLongFilterValue("egg_group_id", rawValue)?.let { where(table.eggGroupId eq it) }
				"slot_order" -> gameDataIntFilterValue("slot_order", rawValue)?.let { where(table.slotOrder eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSpeciesEggGroupResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSpeciesEggGroupRequest): GameSpeciesEggGroupResponse =
		repository.save(
			GameSpeciesEggGroup {
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				eggGroupId = request.eggGroupId ?: invalidValue("egg_group_id", "egg_group_id 不能为空")
				slotOrder = request.slotOrder ?: invalidValue("slot_order", "slot_order 不能为空")
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSpeciesEggGroupRequest): GameSpeciesEggGroupResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSpeciesEggGroup {
				this.id = id
				speciesId = request.speciesId ?: invalidValue("species_id", "species_id 不能为空")
				eggGroupId = request.eggGroupId ?: invalidValue("egg_group_id", "egg_group_id 不能为空")
				slotOrder = request.slotOrder ?: invalidValue("slot_order", "slot_order 不能为空")
			},
			SaveMode.UPDATE_ONLY,
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		entityByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun entityByIdOrNotFound(id: Long): GameSpeciesEggGroup =
		repository.findNullable(id) ?: notFound("id", "种类分组绑定不存在: $id")

	private fun GameSpeciesEggGroup.toResponse(): GameSpeciesEggGroupResponse =
		GameSpeciesEggGroupResponse {
			id = this@toResponse.id
			speciesId = this@toResponse.speciesId
			eggGroupId = this@toResponse.eggGroupId
			slotOrder = this@toResponse.slotOrder
		}
}
