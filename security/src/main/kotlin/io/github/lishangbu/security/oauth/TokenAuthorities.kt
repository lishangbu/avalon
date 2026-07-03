package io.github.lishangbu.security.oauth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * 从 Backend token claims 中恢复 Spring Security 权限。
 */
fun securityAuthoritiesFromClaims(claims: Map<String, Any>): Collection<GrantedAuthority> {
	val accessNodes = claims.claimValues("access_nodes")
	val roles = claims.claimValues("roles")
	return accessNodes.map { SimpleGrantedAuthority(it) } +
		roles.map { SimpleGrantedAuthority("ROLE_$it") }
}

/**
 * 读取权限 claim 的两种常见反序列化形态。
 *
 * Backend 自己签发 `access_nodes` 和 `roles` 时写入集合；但资源服务器从 JWT、opaque token 或测试桩恢复
 * claims 时，安全框架也可能把同类权限表示为空格分隔字符串。这里把差异收敛在安全模块边界，业务层只接收
 * 标准的 [GrantedAuthority]。
 */
private fun Map<String, Any>.claimValues(name: String): List<String> =
	when (val value = this[name]) {
		is String -> value.split(" ").filter { it.isNotBlank() }
		is Collection<*> -> value.mapNotNull { it?.toString() }
		else -> emptyList()
	}
