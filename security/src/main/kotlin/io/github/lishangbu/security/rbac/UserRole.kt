package io.github.lishangbu.security.rbac

/**
 * 认证主体持有的角色快照。
 */
data class UserRole(
	val id: Long,
	val code: String,
	val name: String,
)
