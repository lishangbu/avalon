package io.github.lishangbu.security.rbac

import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.roles
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
	classes = [SecurityRbacTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=0",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [SecurityRbacPostgresTestContainer::class])
/**
 * 验证 Jimmer 用户详情服务能从 RBAC 表组装用户、角色和权限。
 */
class JimmerSecurityUserDetailsServiceTests(
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val userDetailsService: JimmerSecurityUserDetailsService,
) {
	@Test
	fun `loads user with roles and access nodes as authorities`() {
		val userId = 30001L
		sqlClient.save(
			SecurityUser {
				id = userId
				username = "rbac-admin"
				passwordHash = "{noop}secret"
				displayName = "管理员"
				enabled = true
				accountNonLocked = true
			},
		)
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, 201L)

		val user = userDetailsService.loadUserByUsername("rbac-admin") as SecurityUserPrincipal

		assertThat(user.username).isEqualTo("rbac-admin")
		assertThat(user.password).isEqualTo("{noop}secret")
		assertThat(user.displayName).isEqualTo("管理员")
		val expectedAccessNodeCodes = listOf(
			"security:admin",
			"system",
			"system.oauth",
			"system.oauth.clients",
			"system.oauth.jwks",
			"system.rbac",
			"system.rbac.access-nodes",
			"system.rbac.roles",
			"system.rbac.users",
			"system.scheduler",
			"system.scheduler.tasks",
		)
		assertThat(user.accessNodes.map { it.code }).containsExactlyInAnyOrderElementsOf(expectedAccessNodeCodes)
		assertThat(user.authorities.map { it.authority })
			.containsExactlyInAnyOrderElementsOf(listOf("ROLE_system-admin") + expectedAccessNodeCodes)
	}

	@Test
	fun `rejects missing user`() {
		org.junit.jupiter.api.assertThrows<UsernameNotFoundException> {
			userDetailsService.loadUserByUsername("missing")
		}
	}
}
