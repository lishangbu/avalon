package io.github.lishangbu.battlerules

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * 战斗规则模块结构回归测试。
 *
 * 规则维护接口已经拆成独立 Controller、Service、Repository 和 DTO。本测试只固定模块边界：
 * - 持久层继续使用 Jimmer，不允许退回 Spring JDBC 模板。
 * - Repository 必须是每张规则表自己的 `KRepository`，不能重新出现按资源名分发的万能持久层。
 * - Controller 必须是独立 REST 入口，不能把多个规则资源收进一个动态资源控制器。
 *
 * 运行时快照装配允许使用 `BattleJimmerSql` 通过 Jimmer connection 执行固定 SQL；这不是 JDBC 模板回退，
 * 也不接收页面传来的动态表名或资源 key。
 */
class BattleRulesArchitectureTests {
	private val mainSourceRoot = existingPath("src/main/kotlin", "battle-rules/src/main/kotlin")
	private val projectRoot = existingPath(".", "battle-rules")

	@Test
	fun `battle rules module does not use jdbc template or dynamic resource dispatch`() {
		val forbiddenTokens = listOf(
			"JdbcTemplate",
			"NamedParameterJdbcTemplate",
			"BattleRuleCrud",
			"BattleRulesOperations",
			"resourceKey",
			"resourceName",
			"Map<String, BattleRule",
		)
		val hits = kotlinFiles(mainSourceRoot).flatMap { file ->
			val text = file.readText()
			forbiddenTokens
				.filter(text::contains)
				.map { token -> "${mainSourceRoot.relativize(file)} contains $token" }
		}

		assertThat(hits).isEmpty()
	}

	@Test
	fun `battle rule repositories stay concrete jimmer repositories`() {
		val repositoryFiles = sourceFiles("repository", "Repository.kt")

		assertThat(repositoryFiles).hasSize(24)
		assertThat(repositoryFiles).allSatisfy { file ->
			val repositoryName = file.name.removeSuffix(".kt")
			val text = file.readText()

			assertThat(text)
				.withFailMessage("${file.name} 必须声明自己的 Jimmer Repository")
				.contains("interface $repositoryName : KRepository<")
			assertThat(repositoryName)
				.withFailMessage("${file.name} 不能使用通用 Repository 命名")
				.doesNotContain("Generic")
				.doesNotContain("Operations")
				.doesNotContain("TableService")
		}
	}

	@Test
	fun `battle rule controllers stay concrete rest controllers`() {
		val controllerFiles = sourceFiles("controller", "Controller.kt")

		assertThat(controllerFiles).hasSize(26)
		assertThat(controllerFiles).allSatisfy { file ->
			val controllerName = file.name.removeSuffix(".kt")
			val text = file.readText()

			assertThat(text)
				.withFailMessage("${file.name} 必须是独立 REST Controller")
				.contains("@RestController")
				.contains("@RequestMapping(")
				.contains("class $controllerName(")
			assertThat(controllerName)
				.withFailMessage("${file.name} 不能使用通用 Controller 命名")
				.doesNotContain("Generic")
				.doesNotContain("Crud")
				.doesNotContain("Table")
		}
	}

	@Test
	fun `battle rules build keeps jimmer persistence without direct jdbc starter`() {
		val buildFile = projectRoot.resolve("build.gradle.kts").readText()

		assertThat(buildFile)
			.contains("implementation(libs.jimmer.spring.boot.starter)")
			.doesNotContain("implementation(libs.spring.boot.starter.jdbc)")
	}

	private fun sourceFiles(directoryName: String, suffix: String): List<Path> =
		Files.list(mainSourceRoot.resolve("io/github/lishangbu/battlerules/$directoryName")).use { paths ->
			paths
				.filter { Files.isRegularFile(it) }
				.filter { it.name.endsWith(suffix) }
				.sorted()
				.toList()
		}

	private fun existingPath(vararg candidates: String): Path =
		candidates
			.map(Path::of)
			.firstOrNull(Files::exists)
			?: error("Cannot find any of: ${candidates.joinToString()}")

	private fun kotlinFiles(root: Path): List<Path> =
		Files.walk(root).use { paths ->
			paths
				.filter { Files.isRegularFile(it) }
				.filter { it.name.endsWith(".kt") }
				.sorted()
				.toList()
		}
}
