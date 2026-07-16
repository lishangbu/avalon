package io.github.lishangbu.gamedata

import io.github.lishangbu.gamedata.dto.GameAbilityResponse
import org.assertj.core.api.Assertions.assertThat
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.io.path.readText

/**
 * 固化游戏资料模块的 Jimmer 持久化边界。
 *
 * 每项资料必须拥有独立实体与 Repository，Long 主键由 CosId 生成并通过 Jimmer 转换为 JSON 字符串。
 * 此测试同时约束实体注释完整性，防止后续重新引入动态表描述、通用记录模型或直接 JDBC 操作。
 */
class GameDataPersistenceConventionTests {
	@Test
	fun `jimmer immutable response serializes long identifier as json string`() {
		val response = GameAbilityResponse {
			id = JAVASCRIPT_UNSAFE_LONG
			code = "test-ability"
			name = "测试特性"
			mainSeries = true
			enabled = true
		}
		val json = JsonMapper.builder()
			.addModule(ImmutableModuleV3())
			.build()
			.writeValueAsString(response)

		assertThat(json)
			.contains("\"id\":\"$JAVASCRIPT_UNSAFE_LONG\"")
			.contains("\"main_series\":true")
	}

	@Test
	fun `game data responses use jimmer immutable long conversion`() {
		val violations = responseSources().flatMap { source ->
			val text = source.readText()
			buildList {
				if ("@Immutable" !in text || "interface " !in text) {
					add(source.violation("响应必须使用 Jimmer immutable 接口"))
				}
				if ("@JsonConverter(LongToStringConverter::class)" !in text) {
					add(source.violation("响应 Long 主键必须使用 Jimmer LongToStringConverter"))
				}
				if ("data class " in text) {
					add(source.violation("普通 data class 会绕开 Jimmer 主键转换器"))
				}
			}
		}

		assertThat(responseSources()).hasSize(EXPECTED_FEATURE_COUNT)
		assertThat(violations).isEmpty()
	}

	@Test
	fun `game data entities use documented cosid long identifiers`() {
		val violations = entitySources().flatMap { source ->
			buildList {
				val text = source.readText()
				if ("@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)" !in text) {
					add(source.violation("主键必须使用 CosIdLongUserIdGenerator"))
				}
				if ("@JsonConverter(LongToStringConverter::class)" !in text) {
					add(source.violation("Long 主键必须使用 Jimmer LongToStringConverter"))
				}
				if ("GenerationType" in text) {
					add(source.violation("主键不能使用数据库自增策略"))
				}
				if ("对应 `" !in text || "新增记录的主键统一由 CosId 生成" !in text) {
					add(source.violation("实体接口必须说明对应表、聚合边界和主键策略"))
				}
			}
		}

		assertThat(entitySources()).hasSize(EXPECTED_ENTITY_COUNT)
		assertThat(violations).isEmpty()
	}

	@Test
	fun `every game data entity property has detailed kdoc`() {
		val violations = entitySources().flatMap { source ->
			val lines = source.readLines()
			lines.mapIndexedNotNull { index, line ->
				if (!PERSISTENT_PROPERTY.matches(line)) {
					return@mapIndexedNotNull null
				}
				val commentEnd = lines.subList(0, index)
					.indexOfLast { candidate -> candidate.trim().let { it.isNotEmpty() && !it.startsWith("@") } }
				val commentLine = lines.getOrNull(commentEnd)?.trim().orEmpty()
				if (commentEnd < 0 || (
					commentLine != "*/" && !(commentLine.startsWith("/**") && commentLine.endsWith("*/"))
				)) {
					source.violation("${index + 1} 行属性缺少 KDoc")
				} else {
					null
				}
			}
		}

		assertThat(violations).isEmpty()
	}

	@Test
	fun `game data production code has no dynamic table infrastructure or direct jdbc`() {
		val mainSourceRoot = Path.of("src/main/kotlin")
		val forbiddenFiles = FORBIDDEN_FILE_NAMES
			.map(mainSourceRoot::resolve)
			.filter(Files::exists)
		val forbiddenReferences = kotlinSources(mainSourceRoot).flatMap { source ->
			val text = source.readText()
			FORBIDDEN_SOURCE_REFERENCES
				.filter(text::contains)
				.map { reference -> source.violation("禁止依赖 $reference") }
		}

		assertThat(forbiddenFiles).isEmpty()
		assertThat(forbiddenReferences).isEmpty()
	}

	@Test
	fun `feature services declare insert and update save modes explicitly`() {
		val serviceSources = kotlinSources(Path.of("src/main/kotlin/io/github/lishangbu/gamedata/service"))
			.filter { source -> source.fileName.toString().matches(Regex("Game.*Service\\.kt")) }
		val violations = serviceSources.flatMap { source ->
			val text = source.readText()
			buildList {
				if ("SaveMode.INSERT_ONLY" !in text) {
					add(source.violation("新增操作必须显式使用 INSERT_ONLY"))
				}
				if ("SaveMode.UPDATE_ONLY" !in text) {
					add(source.violation("更新操作必须显式使用 UPDATE_ONLY"))
				}
				if ("repository.insert(" in text || "repository.update(" in text) {
					add(source.violation("使用带 SaveMode 的 repository.save，避免调用已弃用快捷方法"))
				}
			}
		}

		assertThat(serviceSources).hasSize(EXPECTED_FEATURE_COUNT)
		assertThat(violations).isEmpty()
	}

	private fun entitySources(): List<Path> =
		kotlinSources(Path.of("src/main/kotlin/io/github/lishangbu/gamedata/entity"))
			.filter { source -> "@Entity" in source.readText() }

	private fun responseSources(): List<Path> =
		kotlinSources(Path.of("src/main/kotlin/io/github/lishangbu/gamedata/dto"))
			.filter { source -> source.fileName.toString().matches(Regex("Game.*Response\\.kt")) }

	private fun kotlinSources(root: Path): List<Path> =
		Files.walk(root).use { paths ->
			paths
				.filter(Files::isRegularFile)
				.filter { path -> path.fileName.toString().endsWith(".kt") }
				.sorted()
				.toList()
		}

	private fun Path.violation(reason: String): String =
		"$this: $reason"

	private companion object {
		private const val EXPECTED_ENTITY_COUNT = 54
		private const val EXPECTED_FEATURE_COUNT = 52
		private const val JAVASCRIPT_UNSAFE_LONG = 9_007_199_254_740_993L
		private val PERSISTENT_PROPERTY = Regex("""\s*val\s+\w+\s*:\s*.+""")
		private val FORBIDDEN_FILE_NAMES = listOf(
			"io/github/lishangbu/gamedata/repository/GameDataTableSpec.kt",
			"io/github/lishangbu/gamedata/repository/GameDataJimmerRepository.kt",
			"io/github/lishangbu/gamedata/model/GameDataRecordRequest.kt",
			"io/github/lishangbu/gamedata/model/GameDataRecordResponse.kt",
		)
		private val FORBIDDEN_SOURCE_REFERENCES = listOf(
			"GameDataTableSpec",
			"GameDataJimmerRepository",
			"GameDataRecordRequest",
			"GameDataRecordResponse",
			"JdbcTemplate",
			"java.sql.",
			"connectionManager",
		)
	}
}
