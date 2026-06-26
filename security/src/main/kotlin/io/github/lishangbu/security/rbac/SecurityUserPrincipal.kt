package io.github.lishangbu.security.rbac

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * RBAC 用户认证主体。
 *
 * 角色会转换为 `ROLE_` 前缀 authority，访问节点 code 直接作为 authority，用于资源端细粒度鉴权。
 */
data class SecurityUserPrincipal(
	val id: Long,
	private val usernameValue: String,
	private val passwordHash: String,
	val displayName: String,
	val roles: List<UserRole>,
	val accessNodes: List<UserAccessNode>,
	private val enabledValue: Boolean,
	private val accountNonLockedValue: Boolean,
) : UserDetails {
	override fun getAuthorities(): Collection<GrantedAuthority> =
		roles.map { SimpleGrantedAuthority("ROLE_${it.code}") } +
			accessNodes.map { SimpleGrantedAuthority(it.code) }

	override fun getPassword(): String = passwordHash

	override fun getUsername(): String = usernameValue

	override fun isEnabled(): Boolean = enabledValue

	override fun isAccountNonLocked(): Boolean = accountNonLockedValue

	override fun isAccountNonExpired(): Boolean = true

	override fun isCredentialsNonExpired(): Boolean = true
}
