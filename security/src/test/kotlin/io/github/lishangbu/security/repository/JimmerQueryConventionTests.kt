package io.github.lishangbu.security.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * 固化 security 和 system 模块的 Jimmer 查询约束。
 *
 * 系统管理和 SAS 运行时路径不能用 `findAll()` 再在内存中过滤真实业务集合；需要改用
 * Jimmer SQL DSL 或边界明确的 SQL 查询。
 */
class JimmerQueryConventionTests {
	@Test
	fun `runtime security paths do not filter repository findAll results`() {
		val violations = monitoredSources()
			.flatMap { source ->
				source.readLines().mapIndexedNotNull { index, line ->
					source.toString().line(index + 1).takeIf { "findAll()" in line }
				}
			}

		assertThat(violations)
			.describedAs("Use KSqlClient or explicit SQL instead of findAll() on runtime security paths")
			.isEmpty()
	}

	@Test
	fun `runtime security paths do not bypass jimmer with jdbc operations`() {
		val forbiddenFragments = listOf(
			"JdbcTemplate",
			"connectionManager",
			"prepareStatement",
			"java.sql.",
		)
		val violations = monitoredSources()
			.flatMap { source ->
				source.readLines().mapIndexedNotNull { index, line ->
					source.toString().line(index + 1).takeIf { current ->
						forbiddenFragments.any(current::contains)
					}
				}
			}

		assertThat(violations)
			.describedAs("Use Jimmer Repository or KSqlClient instead of direct JDBC operations")
			.isEmpty()
	}

	private fun monitoredSources(): List<Path> =
		listOf(
			"../system/src/main/kotlin/io/github/lishangbu/system/service/OAuthClientService.kt",
			"../system/src/main/kotlin/io/github/lishangbu/system/service/OAuthJwkService.kt",
			"../system/src/main/kotlin/io/github/lishangbu/system/service/AccessNodeService.kt",
			"../system/src/main/kotlin/io/github/lishangbu/system/service/RoleService.kt",
			"../system/src/main/kotlin/io/github/lishangbu/system/service/UserService.kt",
			"src/main/kotlin/io/github/lishangbu/security/oauth/JwkSource.kt",
			"src/main/kotlin/io/github/lishangbu/security/repository/JimmerRegisteredClientRepository.kt",
		).map(Path::of)

	private fun String.line(lineNumber: Int): String =
		"$this:$lineNumber"
}
