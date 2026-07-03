package io.github.lishangbu.security.oauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 验证 Backend 自定义权限 claim 到 Spring Security authority 的转换。
 *
 * 这里不启动授权服务器，也不构造真实 token：本函数的职责只是把已经解析出的 claims 映射成权限对象。
 * 用纯单元测试锁住列表和空格分隔字符串两种输入形态，可以保证 JWT、opaque token 或测试桩在 claim
 * 反序列化细节不同的情况下，后续 RBAC 代码仍然只面对同一种 `GrantedAuthority` 结果。
 */
class TokenAuthoritiesTests {
	@Test
	fun `maps collection claims to access node and role authorities`() {
		val authorities = securityAuthoritiesFromClaims(
			mapOf(
				"access_nodes" to listOf("security:admin", "system.rbac.users"),
				"roles" to listOf("system-admin"),
			),
		)

		assertThat(authorities.map { it.authority }).containsExactly(
			"security:admin",
			"system.rbac.users",
			"ROLE_system-admin",
		)
	}

	@Test
	fun `maps space separated string claims to access node and role authorities`() {
		val authorities = securityAuthoritiesFromClaims(
			mapOf(
				"access_nodes" to "security:admin  system.rbac.users ",
				"roles" to "system-admin audit-admin",
			),
		)

		assertThat(authorities.map { it.authority }).containsExactly(
			"security:admin",
			"system.rbac.users",
			"ROLE_system-admin",
			"ROLE_audit-admin",
		)
	}
}
