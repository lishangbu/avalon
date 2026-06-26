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
			"common-persistence",
			"migration",
			"security",
			"system",
		)
		private val DISALLOWED_OBJECT_DECLARATION =
			Regex("""^\s*(?:(?:private|internal|public|protected|data)\s+)*object\s+\w+""")
		private val TOP_LEVEL_TYPE_DECLARATION =
			Regex(
				"""^(?:(?:public|internal|private|abstract|final|open|sealed|data|value|annotation|fun)\s+)*""" +
					"""(?:class|interface|object|enum\s+class|sealed\s+(?:class|interface)|data\s+class|value\s+class|annotation\s+class)\s+\w+""",
			)
	}
}
