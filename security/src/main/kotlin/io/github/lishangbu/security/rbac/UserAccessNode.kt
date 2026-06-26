package io.github.lishangbu.security.rbac

/**
 * 认证主体持有的访问节点快照。
 */
data class UserAccessNode(
	val id: Long,
	val code: String,
	val name: String,
)
