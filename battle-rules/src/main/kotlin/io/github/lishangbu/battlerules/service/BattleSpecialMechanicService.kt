package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSpecialMechanicRequest
import io.github.lishangbu.battlerules.dto.BattleSpecialMechanicResponse
import io.github.lishangbu.battlerules.entity.BattleSpecialMechanic
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleSpecialMechanicRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗特殊机制维护服务。
 *
 * 特殊机制本身只是可开关能力的字典，是否在某个赛制中可用由绑定表决定。
 * 这里使用中性 code，避免把来源品牌词固化进后端契约。
 */
@Service
class BattleSpecialMechanicService(
	private val repository: BattleSpecialMechanicRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleSpecialMechanicResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleSpecialMechanic::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleSpecialMechanicResponse =
		mechanicByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleSpecialMechanicRequest): BattleSpecialMechanicResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleSpecialMechanic {
				this.code = code
				name = normalized.name
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleSpecialMechanicRequest): BattleSpecialMechanicResponse {
		mechanicByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleSpecialMechanic {
				this.id = id
				this.code = code
				name = normalized.name
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		mechanicByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun mechanicByIdOrNotFound(id: Long): BattleSpecialMechanic =
		repository.findNullable(id) ?: notFound("id", "特殊机制不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSpecialMechanic::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "特殊机制 code 已存在: $code")
		}
	}

	private fun BattleSpecialMechanicRequest.normalized(): BattleSpecialMechanicRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			description = optionalText(description, "description", 600),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSpecialMechanic.toResponse(): BattleSpecialMechanicResponse =
		BattleSpecialMechanicResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			description = this@toResponse.description
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
