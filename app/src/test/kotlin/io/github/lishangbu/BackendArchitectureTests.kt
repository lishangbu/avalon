package io.github.lishangbu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * 后端整体架构边界回归测试。
 *
 * 资料、规则、系统、安全和调度模块现在都以 Jimmer Repository、Jimmer DSL 或 Jimmer 当前事务连接作为持久化入口。
 * 这个测试不判断某个具体查询是否应该写成实体 DSL，因为少量报表型固定 SQL 仍可能需要走 Jimmer connection；
 * 它只禁止真正会把项目带回旧路线的东西：Spring JDBC 模板、直接 JDBC starter，以及已经删除的按资源名分发的
 * 通用资料操作类。这样后续新增模块时，只要误把 `JdbcTemplate` 或旧 Operations 带回来，应用层测试就会先失败。
 */
class BackendArchitectureTests {
	@Test
	fun `backend production code keeps persistence on jimmer path`() {
		val backendRoot = backendRoot()
		val forbiddenTokens = listOf(
			"JdbcTemplate",
			"NamedParameterJdbcTemplate",
			"org.springframework.jdbc.core",
			"libs.spring.boot.starter.jdbc",
			"spring-boot-starter-jdbc",
			"GameDataJimmerOperations",
			"GameDataTableService",
			"BattleRulesOperations",
		)
		val hits = productionSourceAndBuildFiles(backendRoot).flatMap { file ->
			val text = file.readText()
			forbiddenTokens
				.filter(text::contains)
				.map { token -> "${backendRoot.relativize(file)} contains $token" }
		}

		assertThat(hits)
			.withFailMessage(
				"后端持久层必须继续走 Jimmer，不应重新引入 Spring JDBC 模板或旧动态资源操作入口：\n${
					hits.joinToString("\n")
				}",
			)
			.isEmpty()
	}

	private fun backendRoot(): Path =
		listOf(Path.of("."), Path.of(".."))
			.map(Path::toAbsolutePath)
			.map(Path::normalize)
			.firstOrNull { root -> Files.exists(root.resolve("settings.gradle.kts")) }
			?: error("Cannot locate avalon backend root from ${Path.of(".").toAbsolutePath().normalize()}")

	private fun productionSourceAndBuildFiles(backendRoot: Path): List<Path> =
		Files.walk(backendRoot).use { paths ->
			paths
				.filter(Files::isRegularFile)
				.filter { file -> file.isProductionKotlinFile(backendRoot) || file.name == "build.gradle.kts" }
				.filter { file -> !backendRoot.relativize(file).toString().startsWith("buildSrc/") }
				.sorted()
				.toList()
		}

	private fun Path.isProductionKotlinFile(backendRoot: Path): Boolean {
		val relativePath = backendRoot.relativize(this).toString().replace('\\', '/')
		return relativePath.endsWith(".kt") && "/src/main/kotlin/" in relativePath
	}
}
