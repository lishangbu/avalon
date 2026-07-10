package io.github.lishangbu.system.service

import io.github.lishangbu.system.dto.CreateUserRequest
import io.github.lishangbu.system.dto.ResetUserPasswordRequest
import io.github.lishangbu.system.dto.UpdateUserRolesRequest
import io.github.lishangbu.system.dto.UserResponse
import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.accountNonLocked
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.displayName
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.username
import io.github.lishangbu.security.repository.SecurityUserRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidReference
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.normalizedSlugCodes
import io.github.lishangbu.common.web.requiredPassword
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.requiredUsername
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RBAC 用户系统管理服务。
 *
 * 用户主表通过 Jimmer Repository 写入，用户角色绑定通过 Jimmer association API 维护。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class UserService(
	private val userRepository: SecurityUserRepository,
	private val passwordEncoder: PasswordEncoder,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 分页查询用户及其角色 code。
	 */
	@Transactional(readOnly = true)
	fun listUsers(
		page: Int,
		size: Int,
		query: String?,
		roleCode: String?,
		enabled: Boolean?,
		accountNonLocked: Boolean?,
	): Page<UserResponse> {
		validatePage(page, size)
		val searchFilter = searchFilter(query)
		val roleCodeFilter = filterValue("roleCode", roleCode)
		val userPage = sqlClient.createQuery(SecurityUser::class) {
			searchFilter.pattern?.let { pattern ->
				where(
					or(
						table.username ilike pattern,
						table.displayName ilike pattern,
					),
				)
			}
			roleCodeFilter?.let { code ->
				val role = table.joinList(SecurityUser::roles)
				where(role.code eq code)
			}
			enabled?.let { value ->
				where(table.enabled eq value)
			}
			accountNonLocked?.let { value ->
				where(table.accountNonLocked eq value)
			}
			orderBy(table.username)
			select(table)
		}
			.fetchPage(page, size)
		val roleCodesByUserId = roleCodesByUserId(userPage.rows.map(SecurityUser::id))
		return userPage.mapRows { user ->
			user.toResponse(roleCodesByUserId[user.id].orEmpty())
		}
	}

	/**
	 * 查询单个用户及其角色 code。
	 */
	@Transactional(readOnly = true)
	fun getUser(userId: Long): UserResponse =
		userByIdOrNotFound(userId).toResponse()

	/**
	 * 创建用户并维护用户角色关联表。
	 */
	@Transactional
	fun createUser(request: CreateUserRequest): UserResponse {
		val username = request.username.requiredUsername("username")
		if (userExists(username)) {
			conflict("username", "username 已存在")
		}
		val roleIds = resolveRoleIds(request.roleCodes)
		val user = userRepository.save(
			SecurityUser {
				this.username = username
				passwordHash = checkNotNull(passwordEncoder.encode(request.password.requiredPassword("password"))) {
					"PasswordEncoder returned null"
				}
				displayName = request.displayName.requiredText("displayName", maxLength = 80)
				enabled = true
				accountNonLocked = true
			},
		)
		insertRoleBindings(user.id, roleIds)
		return user.toResponse()
	}

	/**
	 * 启用用户账号。
	 */
	@Transactional
	fun enableUser(userId: Long): UserResponse =
		updateUserStatus(userId, enabled = true)

	/**
	 * 禁用用户账号。
	 */
	@Transactional
	fun disableUser(userId: Long): UserResponse =
		updateUserStatus(userId, enabled = false)

	/**
	 * 锁定用户账号。
	 */
	@Transactional
	fun lockUser(userId: Long): UserResponse =
		updateUserStatus(userId, accountNonLocked = false)

	/**
	 * 解锁用户账号。
	 */
	@Transactional
	fun unlockUser(userId: Long): UserResponse =
		updateUserStatus(userId, accountNonLocked = true)

	/**
	 * 重置用户密码。
	 */
	@Transactional
	fun resetPassword(userId: Long, request: ResetUserPasswordRequest): UserResponse {
		val user = userByIdOrNotFound(userId)
		val passwordHash = checkNotNull(passwordEncoder.encode(request.password.requiredPassword("password"))) {
			"PasswordEncoder returned null"
		}
		return saveUser(user, passwordHash = passwordHash).toResponse()
	}

	/**
	 * 替换用户角色绑定。
	 */
	@Transactional
	fun updateUserRoles(userId: Long, request: UpdateUserRolesRequest): UserResponse {
		val user = userByIdOrNotFound(userId)
		val roleIds = resolveRoleIds(request.roleCodes)
		replaceRoleBindings(user.id, roleIds)
		return saveUser(user).toResponse()
	}

	/**
	 * 按 id 查询用户，不存在时返回稳定 404。
	 */
	private fun userByIdOrNotFound(userId: Long): SecurityUser =
		userRepository.findNullable(userId)
			?: notFound("userId", "用户不存在: $userId")

	/**
	 * 更新用户启用和锁定状态。
	 */
	private fun updateUserStatus(
		userId: Long,
		enabled: Boolean? = null,
		accountNonLocked: Boolean? = null,
	): UserResponse {
		val user = userByIdOrNotFound(userId)
		return saveUser(
			user = user,
			enabled = enabled ?: user.enabled,
			accountNonLocked = accountNonLocked ?: user.accountNonLocked,
		).toResponse()
	}

	/**
	 * 以 Jimmer 保存用户主表变更，并保留不可变字段。
	 */
	private fun saveUser(
		user: SecurityUser,
		passwordHash: String = user.passwordHash,
		enabled: Boolean = user.enabled,
		accountNonLocked: Boolean = user.accountNonLocked,
	): SecurityUser =
		userRepository.save(
			SecurityUser {
				id = user.id
				username = user.username
				this.passwordHash = passwordHash
				displayName = user.displayName
				this.enabled = enabled
				this.accountNonLocked = accountNonLocked
			},
		)

	/**
	 * 批量写入用户角色绑定。
	 */
	private fun insertRoleBindings(userId: Long, roleIds: List<Long>) {
		sqlClient
			.getAssociations(SecurityUser::roles)
			.insertAllIfAbsent(listOf(userId), roleIds)
	}

	/**
	 * 使用 Jimmer association API 替换用户角色绑定。
	 */
	private fun replaceRoleBindings(userId: Long, roleIds: List<Long>) {
		sqlClient
			.getAssociations(SecurityUser::roles)
			.replaceAll(listOf(userId), roleIds)
	}

	/**
	 * 解析角色 code，并按规范化后的稳定顺序返回角色 id。
	 */
	private fun resolveRoleIds(roleCodes: List<String>): List<Long> {
		val requestedCodes = roleCodes.normalizedSlugCodes("roleCodes")
		val rolesByCode = sqlClient.executeQuery(SecurityRole::class) {
			where(table.code valueIn requestedCodes)
			select(table)
		}.associateBy { it.code }
		val missingCodes = requestedCodes.filterNot(rolesByCode::containsKey)
		if (missingCodes.isNotEmpty()) {
			invalidReference("roleCodes", "roleCodes 不存在: ${missingCodes.joinToString()}")
		}
		return requestedCodes.map { rolesByCode.getValue(it).id }
	}

	/**
	 * 使用数据库唯一键前置检查，给管理端返回更明确的冲突错误。
	 */
	private fun userExists(username: String): Boolean =
		sqlClient.createQuery(SecurityUser::class) {
			where(table.username eq username)
			select(table.id)
		}.exists()

	/**
	 * 将持久化用户转换为管理端响应。
	 */
	private fun SecurityUser.toResponse(roleCodes: List<String> = roleCodes(id)): UserResponse =
		UserResponse {
			id = this@toResponse.id
			username = this@toResponse.username
			displayName = this@toResponse.displayName
			enabled = this@toResponse.enabled
			accountNonLocked = this@toResponse.accountNonLocked
			this.roleCodes = roleCodes
		}

	/**
	 * 查询用户绑定的角色 code。
	 */
	private fun roleCodes(userId: Long): List<String> =
		sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			where(table.id eq userId)
			orderBy(role.code)
			select(role.code)
		}

	private fun roleCodesByUserId(userIds: List<Long>): Map<Long, List<String>> {
		if (userIds.isEmpty()) {
			return emptyMap()
		}
		return sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			where(table.id valueIn userIds)
			orderBy(table.id, role.code)
			select(table.id, role.code)
		}.groupBy({ it._1 }, { it._2 })
	}

}
