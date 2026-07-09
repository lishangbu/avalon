package io.github.lishangbu.battlerules

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battlerules.service.BattleSandboxRuleHitMapper
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
			.filterNot { it.name == "BattleSandboxReplayRepository.kt" }

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
	fun `battle rule resource layers stay aligned`() {
		// 这里固定的是可维护规则资源的三层配套关系：页面 CRUD 入口对应一个 Controller，
		// Controller 只调用自己的 Service，Service 只通过自己的 Jimmer Repository 访问对应规则表。
		// 运行时快照和沙盒是执行接口，不属于规则资料维护资源，所以不会参与三层数量对齐。
		val controllerNames = sourceFiles("controller", "Controller.kt")
			.map { it.name.removeSuffix("Controller.kt") }
			.filterNot { it in setOf("BattleRuntimeSnapshot", "BattleSandbox") }
			.sorted()
		val serviceNames = sourceFiles("service", "Service.kt")
			.map { it.name.removeSuffix("Service.kt") }
			.filterNot { it in setOf("BattleRuntimeSnapshot", "BattleSandboxReplay") }
			.sorted()
		val repositoryNames = sourceFiles("repository", "Repository.kt")
			.map { it.name.removeSuffix("Repository.kt") }
			.filterNot { it == "BattleSandboxReplay" }
			.sorted()

		assertThat(controllerNames).containsExactlyElementsOf(repositoryNames)
		assertThat(serviceNames).containsExactlyElementsOf(repositoryNames)
	}

	@Test
	fun `sandbox event type labels cover every battle event`() {
		val eventFile = existingPath(
			"../battle-engine/src/main/kotlin/io/github/lishangbu/battleengine/model/BattleEvent.kt",
			"battle-engine/src/main/kotlin/io/github/lishangbu/battleengine/model/BattleEvent.kt",
		)
		val snapshotServiceFile = mainSourceRoot.resolve(
			"io/github/lishangbu/battlerules/service/BattleRuntimeSnapshotService.kt",
		)
		val eventTypePattern = Regex("^\\s+data class\\s+(\\w+)\\(")
		val labelPattern = Regex("\"(\\w+)\"\\s*->\\s*\"([^\"]+)\"")

		// 沙盒事件响应已经把 `typeLabel` 作为前端表格的权威中文名。新增 BattleEvent 时如果这里只靠
		// `else -> this` 退回事件类名，管理页会重新出现英文事件编码；如果映射里残留已删除事件，也说明
		// 前端可读文案和引擎事件模型已经漂移。本测试用源码级扫描固定这两个边界，避免为每个事件构造实例。
		val eventTypes = eventFile.readText()
			.lineSequence()
			.mapNotNull { line -> eventTypePattern.find(line)?.groupValues?.get(1) }
			.sorted()
			.toList()
		val labelTypes = labelPattern
			.findAll(snapshotServiceFile.readText())
			.associate { match -> match.groupValues[1] to match.groupValues[2] }
		val missingLabels = eventTypes.filter { labelTypes[it].isNullOrBlank() || labelTypes[it] == it }
		val obsoleteLabels = labelTypes.keys.minus(eventTypes.toSet()).sorted()

		assertThat(missingLabels).isEmpty()
		assertThat(obsoleteLabels).isEmpty()
	}

	@Test
	fun `sandbox rule hit families cover every battle event`() {
		val mapper = BattleSandboxRuleHitMapper()
		val eventTypes = BattleEvent::class.sealedSubclasses.mapNotNull { it.simpleName }.sorted()
		val missingFamilies = eventTypes.filter { mapper.familyCodeForEventType(it).isNullOrBlank() }

		assertThat(missingFamilies)
			.withFailMessage("新增 BattleEvent 后必须登记到沙盒规则命中规则族，避免管理页和覆盖报告漏掉事件：$missingFamilies")
			.isEmpty()
		assertThat(mapper.ruleHitFamilyCodes()).containsExactly(
			"format-and-team-validation",
			"lifecycle-switch-faint-result",
			"turn-flow-action-ordering",
			"target-scope-redirection",
			"hit-protect-substitute-immunity-reflect",
			"damage-formula-stat-element-rounding",
			"major-volatile-persistent-status",
			"weather-terrain-field-side-condition",
			"skill-effect-family",
			"ability-effect-family",
			"item-effect-family",
			"random-replay-public-reference",
		)
	}

	@Test
	fun `battle rules build keeps jimmer persistence without direct jdbc starter`() {
		val buildFile = projectRoot.resolve("build.gradle.kts").readText()

		assertThat(buildFile)
			.contains("implementation(libs.jimmer.spring.boot.starter)")
			.doesNotContain("implementation(libs.spring.boot.starter.jdbc)")
	}

	@Test
	fun `battle rules declarations keep production grade kdoc`() {
		val declarationPattern =
			Regex("^(?:(?:public|internal|private|protected)\\s+)?(?:(?:data|sealed)\\s+)?(?:class|object|interface|enum class)\\s+\\w+")
		val missingKdoc = kotlinFiles(mainSourceRoot).flatMap { file ->
			val lines = Files.readAllLines(file)
			lines.mapIndexedNotNull { index, line ->
				val declaration = line.trim()
				if (!declarationPattern.containsMatchIn(declaration) || hasKdocBeforeDeclaration(lines, index)) {
					null
				} else {
					"${mainSourceRoot.relativize(file)}:${index + 1} missing KDoc for $declaration"
				}
			}
		}

		assertThat(missingKdoc)
			.withFailMessage(
				"战斗规则接口、DTO、运行时装配模型和内部查询行都必须保留生产级 KDoc，说明字段边界、续算职责和不可承担的职责：\n${
					missingKdoc.joinToString("\n")
				}",
			)
			.isEmpty()
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

	private fun hasKdocBeforeDeclaration(lines: List<String>, declarationLineIndex: Int): Boolean {
		var index = declarationLineIndex - 1
		while (index >= 0 && (lines[index].isBlank() || lines[index].trimStart().startsWith("@"))) {
			index -= 1
		}
		if (index < 0 || !lines[index].trim().endsWith("*/")) {
			return false
		}
		while (index >= 0) {
			if (lines[index].trim().startsWith("/**")) {
				return true
			}
			index -= 1
		}
		return false
	}
}
