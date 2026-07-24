package io.github.lishangbu.battlerules.coverage

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

/** 为覆盖矩阵索引战斗引擎中的行为测试源码。 */
class BattleBehaviorTestSourceIndex(projectRoot: Path) {
	private val executableSourcesByClass = loadSources(projectRoot.resolve(BATTLE_ENGINE_TEST_ROOT))
		.mapValues { (_, source) -> source.withoutCommentsAndLiterals() }
		.filterValues { TEST_ANNOTATION in it }

	/** 返回包含指定行为标识的测试类。 */
	fun classesContaining(token: String): Set<String> =
		executableSourcesByClass.filterValues { token in it }.keys

	/** 返回实际构造指定效果类型的测试类，避免把导入、类型判断或注释误作行为证据。 */
	fun classesInstantiating(ownerType: String, effectType: String): Set<String> {
		val constructor = Regex("""${Regex.escape(ownerType)}\.${Regex.escape(effectType)}\s*\(""")
		return executableSourcesByClass.filterValues(constructor::containsMatchIn).keys
	}

	private fun String.withoutCommentsAndLiterals(): String {
		val executable = StringBuilder(length)
		var state = LexicalState.CODE
		var blockDepth = 0
		var index = 0
		while (index < length) {
			val current = this[index]
			val next = getOrNull(index + 1)
			when (state) {
				LexicalState.CODE -> when {
					current == '/' && next == '/' -> {
						executable.append("  ")
						index++
						state = LexicalState.LINE_COMMENT
					}
					current == '/' && next == '*' -> {
						executable.append("  ")
						index++
						blockDepth = 1
						state = LexicalState.BLOCK_COMMENT
					}
					startsWith("\"\"\"", index) -> {
						executable.append("   ")
						index += 2
						state = LexicalState.RAW_STRING
					}
					current == '"' -> {
						executable.append(' ')
						state = LexicalState.STRING
					}
					current == '\'' -> {
						executable.append(' ')
						state = LexicalState.CHAR
					}
					current == '`' -> {
						executable.append(' ')
						state = LexicalState.BACKTICK_IDENTIFIER
					}
					else -> executable.append(current)
				}
				LexicalState.LINE_COMMENT -> {
					executable.append(if (current == '\n') '\n' else ' ')
					if (current == '\n') state = LexicalState.CODE
				}
				LexicalState.BLOCK_COMMENT -> when {
					current == '/' && next == '*' -> {
						executable.append("  ")
						index++
						blockDepth++
					}
					current == '*' && next == '/' -> {
						executable.append("  ")
						index++
						blockDepth--
						if (blockDepth == 0) state = LexicalState.CODE
					}
					else -> executable.append(if (current == '\n') '\n' else ' ')
				}
				LexicalState.STRING,
				LexicalState.CHAR,
				-> {
					executable.append(if (current == '\n') '\n' else ' ')
					if (current == '\\' && next != null) {
						executable.append(if (next == '\n') '\n' else ' ')
						index++
					} else if (
						(state == LexicalState.STRING && current == '"') ||
						(state == LexicalState.CHAR && current == '\'')
					) {
						state = LexicalState.CODE
					}
				}
				LexicalState.RAW_STRING -> {
					if (startsWith("\"\"\"", index)) {
						executable.append("   ")
						index += 2
						state = LexicalState.CODE
					} else {
						executable.append(if (current == '\n') '\n' else ' ')
					}
				}
				LexicalState.BACKTICK_IDENTIFIER -> {
					executable.append(if (current == '\n') '\n' else ' ')
					if (current == '`') state = LexicalState.CODE
				}
			}
			index++
		}
		return executable.toString()
	}

	private fun loadSources(sourceRoot: Path): Map<String, String> {
		val paths = Files.walk(sourceRoot)
		return try {
			paths
				.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith("Tests.kt") }
				.toList()
				.associate { path -> path.nameWithoutExtension to Files.readString(path) }
		} finally {
			paths.close()
		}
	}

	private companion object {
		private const val BATTLE_ENGINE_TEST_ROOT = "battle-engine/src/test/kotlin"
		private const val TEST_ANNOTATION = "@Test"
	}

	private enum class LexicalState {
		CODE,
		LINE_COMMENT,
		BLOCK_COMMENT,
		STRING,
		RAW_STRING,
		CHAR,
		BACKTICK_IDENTIFIER,
	}
}
