package io.github.lishangbu.gamedata.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.dto.GameSkillRequest
import io.github.lishangbu.gamedata.dto.GameSkillResponse
import io.github.lishangbu.gamedata.entity.GameSkill
import io.github.lishangbu.gamedata.entity.accuracy
import io.github.lishangbu.gamedata.entity.code
import io.github.lishangbu.gamedata.entity.damageClassId
import io.github.lishangbu.gamedata.entity.effectChance
import io.github.lishangbu.gamedata.entity.elementId
import io.github.lishangbu.gamedata.entity.enabled
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.name
import io.github.lishangbu.gamedata.entity.power
import io.github.lishangbu.gamedata.entity.pp
import io.github.lishangbu.gamedata.entity.priority
import io.github.lishangbu.gamedata.repository.GameSkillRepository
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
 * 技能资料维护服务。
 *
 * 查询、校验和写入只依赖当前资料实体，不共享跨表运行时元数据。
 */
@Service
class GameSkillService(
	private val repository: GameSkillRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): Page<GameSkillResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(GameSkill::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			filters.forEach { (field, rawValue) ->
				when (field) {
				"code" -> gameDataStringFilterValue("code", rawValue)?.let { where(table.code eq it) }
				"name" -> gameDataStringFilterValue("name", rawValue)?.let { where(table.name eq it) }
				"element_id" -> gameDataLongFilterValue("element_id", rawValue)?.let { where(table.elementId eq it) }
				"damage_class_id" -> gameDataLongFilterValue("damage_class_id", rawValue)?.let { where(table.damageClassId eq it) }
				"accuracy" -> gameDataIntFilterValue("accuracy", rawValue)?.let { where(table.accuracy eq it) }
				"power" -> gameDataIntFilterValue("power", rawValue)?.let { where(table.power eq it) }
				"pp" -> gameDataIntFilterValue("pp", rawValue)?.let { where(table.pp eq it) }
				"priority" -> gameDataIntFilterValue("priority", rawValue)?.let { where(table.priority eq it) }
				"effect_chance" -> gameDataIntFilterValue("effect_chance", rawValue)?.let { where(table.effectChance eq it) }
				"enabled" -> gameDataBooleanFilterValue("enabled", rawValue)?.let { where(table.enabled eq it) }
					else -> invalidValue(field, "筛选字段不存在: $field")
				}
			}
			orderBy(table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): GameSkillResponse =
		entityByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: GameSkillRequest): GameSkillResponse =
		repository.save(
			GameSkill {
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				elementId = request.elementId
				damageClassId = request.damageClassId
				accuracy = request.accuracy
				power = request.power
				pp = request.pp
				priority = request.priority
				effectChance = request.effectChance
				enabled = request.enabled
			},
			SaveMode.INSERT_ONLY,
		).toResponse()

	@Transactional
	fun update(id: Long, request: GameSkillRequest): GameSkillResponse {
		entityByIdOrNotFound(id)
		return repository.save(
			GameSkill {
				this.id = id
				code = request.code.orEmpty().requiredSlugCode("code")
				name = gameDataRequiredText(request.name, "name", 120)
				elementId = request.elementId
				damageClassId = request.damageClassId
				accuracy = request.accuracy
				power = request.power
				pp = request.pp
				priority = request.priority
				effectChance = request.effectChance
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

	private fun entityByIdOrNotFound(id: Long): GameSkill =
		repository.findNullable(id) ?: notFound("id", "技能资料不存在: $id")

	private fun GameSkill.toResponse(): GameSkillResponse =
		GameSkillResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			elementId = this@toResponse.elementId
			damageClassId = this@toResponse.damageClassId
			accuracy = this@toResponse.accuracy
			power = this@toResponse.power
			pp = this@toResponse.pp
			priority = this@toResponse.priority
			effectChance = this@toResponse.effectChance
			enabled = this@toResponse.enabled
		}
}
