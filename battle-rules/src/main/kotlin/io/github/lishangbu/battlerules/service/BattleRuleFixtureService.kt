package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleRuleFixtureRequest
import io.github.lishangbu.battlerules.dto.BattleRuleFixtureResponse
import io.github.lishangbu.battlerules.entity.BattleRuleFixture
import io.github.lishangbu.battlerules.entity.category
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.expectedSummary
import io.github.lishangbu.battlerules.entity.fixtureType
import io.github.lishangbu.battlerules.entity.formatCode
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.inputSummary
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleRuleFixtureRepository
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
 * 战斗规则公开对照 Fixture 维护服务。
 *
 * 主表只维护可检索的场景元数据，不试图执行测试，也不保存运行结果。公开来源保留在测试 fixture 附近，
 * 管理端只负责维护用例元数据和查看运行历史。
 */
@Service
class BattleRuleFixtureService(
	private val repository: BattleRuleFixtureRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?, category: String?, enabled: Boolean?): Page<BattleRuleFixtureResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		val normalizedCategory = category?.takeIf { it.isNotBlank() }?.let { requiredUpperText(it, "category", 60) }
		return sqlClient.createQuery(BattleRuleFixture::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			normalizedCategory?.let { where(table.category eq it) }
			enabled?.let { where(table.enabled eq it) }
			orderBy(table.category, table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleRuleFixtureResponse =
		fixtureByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleRuleFixtureRequest): BattleRuleFixtureResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleRuleFixture {
				this.code = code
				name = normalized.name
				category = normalized.category
				fixtureType = normalized.fixtureType
				formatCode = normalized.formatCode
				description = normalized.description
				inputSummary = normalized.inputSummary
				expectedSummary = normalized.expectedSummary
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleRuleFixtureRequest): BattleRuleFixtureResponse {
		fixtureByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleRuleFixture {
				this.id = id
				this.code = code
				name = normalized.name
				category = normalized.category
				fixtureType = normalized.fixtureType
				formatCode = normalized.formatCode
				description = normalized.description
				inputSummary = normalized.inputSummary
				expectedSummary = normalized.expectedSummary
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		fixtureByIdOrNotFound(id)
		repository.deleteById(id)
	}

	internal fun fixtureByIdOrNotFound(id: Long): BattleRuleFixture =
		repository.findNullable(id) ?: notFound("id", "战斗规则 Fixture 不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleRuleFixture::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "Fixture code 已存在: $code")
		}
	}

	private fun BattleRuleFixtureRequest.normalized(): BattleRuleFixtureRequest =
		copy(
			name = name.requiredText("name", 120),
			category = requiredUpperText(category, "category", 60),
			fixtureType = requiredUpperText(fixtureType, "fixtureType", 40),
			formatCode = optionalText(formatCode, "formatCode", 80),
			description = optionalText(description, "description", 800),
			inputSummary = inputSummary.requiredText("inputSummary", 1000),
			expectedSummary = expectedSummary.requiredText("expectedSummary", 1000),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleRuleFixture.toResponse(): BattleRuleFixtureResponse =
		BattleRuleFixtureResponse(
			id = id,
			code = code,
			name = name,
			category = category,
			fixtureType = fixtureType,
			formatCode = formatCode,
			description = description,
			inputSummary = inputSummary,
			expectedSummary = expectedSummary,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
