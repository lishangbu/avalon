package io.github.lishangbu.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * 固化 Backend Kotlin 代码组织约束。
 *
 * 普通 `object` 聚合器会隐藏依赖边界，多顶层类型文件会弱化按类型命名和查找的约定。
 */
class KotlinCodeOrganizationConventionTests {
	@Test
	fun `kotlin object declarations are limited to companion objects`() {
		val root = repositoryRoot()
		val violations = projectKotlinFiles(root)
			.flatMap { source ->
				source.readLines().mapIndexedNotNull { index, line ->
					if (DISALLOWED_OBJECT_DECLARATION.containsMatchIn(line)) {
						"${root.relativize(source)}:${index + 1}: ${line.trim()}"
					} else {
						null
					}
				}
			}

		assertThat(violations)
			.describedAs("Use classes, Spring beans or top-level declarations; only companion object is allowed")
			.isEmpty()
	}

	@Test
	fun `kotlin files declare at most one top level type`() {
		val root = repositoryRoot()
		val violations = projectKotlinFiles(root)
			.mapNotNull { source ->
				val declarations = source.readLines().mapIndexedNotNull { index, line ->
					if (TOP_LEVEL_TYPE_DECLARATION.containsMatchIn(line)) {
						"${index + 1}: ${line.trim()}"
					} else {
						null
					}
				}
				if (declarations.size > 1) {
					"${root.relativize(source)}\n${declarations.joinToString("\n")}"
				} else {
					null
				}
			}

		assertThat(violations)
			.describedAs("Use one top-level Kotlin type per file; companion objects and nested types stay inside the owner")
			.isEmpty()
	}

	@Test
	fun `production kotlin code uses jimmer instead of direct jdbc`() {
		val root = repositoryRoot()
		val violations = productionKotlinFiles(root).flatMap { source ->
			source.readLines().mapIndexedNotNull { index, line ->
				if (FORBIDDEN_DATABASE_ACCESS.any(line::contains)) {
					"${root.relativize(source)}:${index + 1}: ${line.trim()}"
				} else {
					null
				}
			}
		}

		assertThat(violations)
			.describedAs("Use Jimmer Repository or KSqlClient for production database operations")
			.isEmpty()
	}

	@Test
	fun `testcontainer images use reproducible versions`() {
		val root = repositoryRoot()
		val mutableImageTag = ":la" + "test"
		val violations = projectKotlinFiles(root).flatMap { source ->
			source.readLines().mapIndexedNotNull { index, line ->
				if (mutableImageTag in line) {
					"${root.relativize(source)}:${index + 1}: ${line.trim()}"
				} else {
					null
				}
			}
		}

		assertThat(violations)
			.describedAs("Pin Testcontainers images to reproducible tags")
			.isEmpty()
	}

	@Test
	fun `project code uses jackson 3 mapper api`() {
		val root = repositoryRoot()
		val sources = projectKotlinFiles(root) + projectBuildFiles(root)
		val violations = sources.flatMap { source ->
			source.readLines().mapIndexedNotNull { index, line ->
				if (FORBIDDEN_JACKSON_2_API.any(line::contains)) {
					"${root.relativize(source)}:${index + 1}: ${line.trim()}"
				} else {
					null
				}
			}
		}

		assertThat(violations)
			.describedAs("Use tools.jackson APIs and Jimmer ImmutableModuleV3; Jackson annotations keep their shared package")
			.isEmpty()
	}

	@Test
	fun `response long identifiers use jimmer string converters`() {
		val root = repositoryRoot()
		val violations = productionKotlinFiles(root)
			.filter { source -> source.fileName.toString().endsWith("Response.kt") }
			.flatMap { source ->
				val lines = source.readLines()
				lines.mapIndexedNotNull { index, line ->
					if (!LONG_ID_PROPERTY.matches(line)) {
						return@mapIndexedNotNull null
					}
					val annotationStart = (index - 3).coerceAtLeast(0)
					val annotations = lines.subList(annotationStart, index)
					if (annotations.any { annotation -> "JsonConverter(LongToStringConverter::class)" in annotation }) {
						null
					} else {
						"${root.relativize(source)}:${index + 1}: ${line.trim()}"
					}
				}
			}

		assertThat(violations)
			.describedAs("Use Jimmer LongToStringConverter for Long identifiers exposed by response DTOs")
			.isEmpty()
	}

	private fun repositoryRoot(): Path {
		val current = Path.of("").toAbsolutePath().normalize()
		return generateSequence(current) { it.parent }
			.first { Files.exists(it.resolve("settings.gradle.kts")) }
	}

	private fun projectKotlinFiles(root: Path): List<Path> =
		MODULES
			.map(root::resolve)
			.filter(Files::exists)
				.flatMap(::kotlinFiles)

	private fun productionKotlinFiles(root: Path): List<Path> =
		MODULES
			.map { module -> root.resolve(module).resolve("src/main/kotlin") }
			.filter(Files::exists)
			.flatMap(::kotlinFiles)

	private fun projectBuildFiles(root: Path): List<Path> =
		buildList {
			add(root.resolve("gradle/libs.versions.toml"))
			add(root.resolve("build.gradle.kts"))
			MODULES.mapTo(this) { module -> root.resolve(module).resolve("build.gradle.kts") }
		}.filter(Files::exists)

	private fun kotlinFiles(module: Path): List<Path> =
		Files.walk(module).let { paths ->
			try {
				paths
					.filter(Files::isRegularFile)
					.filter { path -> path.fileName.toString().endsWith(".kt") }
					.filter { path -> !path.toString().contains("${module.fileSystem.separator}build${module.fileSystem.separator}") }
					.toList()
			} finally {
				paths.close()
			}
		}

	private companion object {
		private val MODULES = listOf(
			"app",
			"battle-engine",
			"battle-session",
			"battle-rules",
			"common-persistence",
			"common-web",
			"game-data",
			"migration",
			"match",
			"s3-core",
			"s3-spring-boot-autoconfigure",
			"s3-spring-boot-starter",
			"scheduler",
			"security",
			"system",
		)
		private val FORBIDDEN_DATABASE_ACCESS = listOf(
			"JdbcTemplate",
			"NamedParameterJdbcTemplate",
			"javaClient.connectionManager",
			"prepareStatement(",
			"import java.sql.Connection",
			"import java.sql.PreparedStatement",
			"import java.sql.ResultSet",
		)
		private val FORBIDDEN_JACKSON_2_API = listOf(
			"com.fasterxml.jackson." + "databind",
			"com.fasterxml.jackson." + "core.type",
			"com.fasterxml.jackson." + "module",
			"ImmutableModule" + "V2",
		)
		private val DISALLOWED_OBJECT_DECLARATION =
			Regex("""^\s*(?:(?:private|internal|public|protected|data)\s+)*object\s+\w+""")
		private val TOP_LEVEL_TYPE_DECLARATION =
			Regex(
				"""^(?:(?:public|internal|private|abstract|final|open|sealed|data|value|annotation|fun)\s+)*""" +
					"""(?:class|interface|object|enum\s+class|sealed\s+(?:class|interface)|data\s+class|value\s+class|annotation\s+class)\s+\w+""",
				)
		private val LONG_ID_PROPERTY = Regex("""\s*val\s+\w*[Ii]d\s*:\s*Long\??\s*,?\s*""")
	}
}
