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
 * 同时兼容空格分隔字符串和集合形式的 claim。
 */
private fun Map<String, Any>.claimValues(name: String): List<String> =
	when (val value = this[name]) {
		is String -> value.split(" ").filter { it.isNotBlank() }
		is Collection<*> -> value.mapNotNull { it?.toString() }
		else -> emptyList()
	}
