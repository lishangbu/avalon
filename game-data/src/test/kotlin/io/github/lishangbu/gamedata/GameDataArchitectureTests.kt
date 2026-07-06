package io.github.lishangbu.gamedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * 游戏资料模块的结构回归测试。
 *
 * 资料维护已经拆成独立 Controller、Service、Repository 和 DTO。这里不测试某一条业务数据，而是固定模块边界：
 * 第一，资料持久层必须继续走 Jimmer，不允许退回 JdbcTemplate 或旧的通用 Operations；第二，每张资料表都必须
 * 有自己的 Spring Repository，并在自己的文件里声明表字段白名单；第三，通用 Jimmer 基类只能承载 SQL 安全边界，
 * 不能重新变成按资源名分发表操作的大一统入口。
 */
class GameDataArchitectureTests {
	private val mainSourceRoot = existingPath("src/main/kotlin", "game-data/src/main/kotlin")
	private val projectRoot = existingPath(".", "game-data")

	@Test
	fun `game data module does not use jdbc template or removed generic operations`() {
		val forbiddenTokens = listOf(
			"JdbcTemplate",
			"NamedParameterJdbcTemplate",
			"GameDataJimmerOperations",
			"GameDataTableService",
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
	fun `game data repositories stay concrete and table scoped`() {
		val repositoryRoot = mainSourceRoot.resolve("io/github/lishangbu/gamedata/repository")
		val repositoryFiles = Files.list(repositoryRoot).use { paths ->
			paths
				.filter { Files.isRegularFile(it) }
				.filter { it.name.endsWith("Repository.kt") }
				.filter { it.name != "GameDataJimmerRepository.kt" }
				.sorted()
				.toList()
		}

		assertThat(repositoryFiles).isNotEmpty()
		assertThat(repositoryFiles).allSatisfy { file ->
			val className = file.name.removeSuffix(".kt")
			val text = file.readText()

			assertThat(text)
				.withFailMessage("${file.name} 必须是独立 Spring Repository")
				.contains("@Repository")
				.contains("class $className(")
			assertThat(text)
				.withFailMessage("${file.name} 必须绑定自己的 GameDataTableSpec，不能按运行时资源名分发表")
				.contains("private val GAME_")
				.contains("GameDataTableSpec(")
				.contains(": GameDataJimmerRepository(sqlClient,")
		}
	}

	@Test
	fun `game data build keeps jimmer persistence without direct jdbc starter`() {
		val buildFile = projectRoot.resolve("build.gradle.kts").readText()

		assertThat(buildFile)
			.contains("implementation(libs.jimmer.spring.boot.starter)")
			.doesNotContain("implementation(libs.spring.boot.starter.jdbc)")
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
