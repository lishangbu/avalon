package io.github.lishangbu.battleengine

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * 战斗引擎生产化结构回归测试。
 *
 * 战斗规则已经不再依赖“可在管理系统里维护的测试场景表”。生产代码只需要读取规则快照并执行现代主系列规则，
 * 规则正确性的事实源放在单元测试和公开对照场景里。这个测试只固定边界，不验证具体战斗数值：
 * - 主代码不能重新引入 `sourceUrl`、`rawJson` 这类外部资料源字段。
 * - Liquibase 迁移不能重新出现 `battle_fixture` 之类的数据库夹具表。
 * - 战斗规则运行时模块不能退回把测试事实源存进数据库的设计。
 *
 * 这样后续新增技能、道具、特性或规则时，只需要补充领域数据和行为测试；如果某个公开规则用例要保留来源说明，
 * 应写在测试方法或 `assertNamed` 场景注释里，而不是扩展生产表结构。
 */
class BattleEngineArchitectureTests {
	@Test
	fun `battle runtime and migrations do not store public reference metadata`() {
		val forbiddenTokens = listOf(
			"sourceUrl",
			"source_url",
			"rawJson",
			"raw_json",
			"battle_fixture",
			"battle_fixtures",
		)
		val scannedRoots = listOf(
			existingPath("src/main/kotlin", "battle-engine/src/main/kotlin"),
			existingPath("../battle-rules/src/main/kotlin", "battle-rules/src/main/kotlin"),
			existingPath("../migration/src/main/resources/db/changelog", "migration/src/main/resources/db/changelog"),
		)
		val hits = scannedRoots.flatMap { root ->
			sourceFiles(root).flatMap { file ->
				val text = Files.readString(file)
				forbiddenTokens
					.filter(text::contains)
					.map { token -> "${root.relativize(file)} contains $token" }
			}
		}

		assertTrue(
			hits.isEmpty(),
			"战斗生产代码和迁移不应保存公开资料源或数据库夹具字段：\n${hits.joinToString("\n")}",
		)
	}

	@Test
	fun `battle engine production code stays deterministic and independent from tests`() {
		val forbiddenTokens = listOf(
			"kotlin.random.Random",
			"Random.Default",
			"System.currentTimeMillis",
			"Instant.now",
			"LocalDateTime.now",
			"PublicBattleRuleScenario",
			"publicBattleRuleScenario",
		)
		val mainSourceRoot = existingPath("src/main/kotlin", "battle-engine/src/main/kotlin")
		val hits = sourceFiles(mainSourceRoot).flatMap { file ->
			val text = Files.readString(file)
			forbiddenTokens
				.filter(text::contains)
				.map { token -> "${mainSourceRoot.relativize(file)} contains $token" }
		}

		assertTrue(
			hits.isEmpty(),
			"战斗引擎生产代码必须保持可回放、可测试：随机数只能通过 BattleRandom 注入，时间和公开场景元数据只能留在测试层。\n${
				hits.joinToString("\n")
			}",
		)
	}

	private fun existingPath(vararg candidates: String): Path =
		candidates
			.map(Path::of)
			.firstOrNull(Files::exists)
			?: error("Cannot find any of: ${candidates.joinToString()}")

	private fun sourceFiles(root: Path): List<Path> {
		val paths = Files.walk(root)
		return try {
			paths
				.filter(Files::isRegularFile)
				.filter { path ->
					val fileName = path.toString()
					fileName.endsWith(".kt") || fileName.endsWith(".yaml") || fileName.endsWith(".csv")
				}
				.sorted()
				.toList()
		} finally {
			paths.close()
		}
	}
}
