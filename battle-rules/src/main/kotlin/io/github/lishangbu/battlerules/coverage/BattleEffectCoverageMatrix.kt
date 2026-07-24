package io.github.lishangbu.battlerules.coverage

/** 为不同战斗效果资料生成统一格式的覆盖矩阵。 */
internal class BattleEffectCoverageMatrix(
	private val title: String,
	private val generatorTask: String,
	private val totalLabel: String,
	private val subjectLabel: String,
) {
	fun render(entries: List<BattleEffectCoverageEntry>): String {
		val sortedEntries = entries.sortedBy(BattleEffectCoverageEntry::code)
		val gaps = sortedEntries.filter { it.hasGap() }
		val intentionalNoEffectCount = sortedEntries.count { it.hasIntentionalNoEffectContract() }
		return buildString {
			appendLine("# $title")
			appendLine()
			appendLine("> 本文件由 `./gradlew $generatorTask` 生成，请勿手工编辑。")
			appendLine()
			appendLine("## 汇总")
			appendLine()
			appendLine("- $totalLabel：${sortedEntries.size}")
			appendLine("- 已启用资料：${sortedEntries.count(BattleEffectCoverageEntry::enabled)}")
			appendLine("- Jimmer 规则读取完整：${sortedEntries.count(BattleEffectCoverageEntry::jimmerLoaded)}")
			appendLine("- 运行时策略完整：${sortedEntries.count(BattleEffectCoverageEntry::runtimeSupported)}")
			appendLine("- 具备行为测试证据：${sortedEntries.count { it.behaviorTestClasses.isNotEmpty() }}")
			if (intentionalNoEffectCount > 0) {
				appendLine("- 明确无效果契约：$intentionalNoEffectCount")
			}
			appendLine("- 待补缺口：${gaps.size}")
			appendLine()
			appendTable("待补缺口", gaps)
			appendLine()
			appendTable("完整矩阵", sortedEntries)
		}
	}

	private fun StringBuilder.appendTable(
		tableTitle: String,
		entries: List<BattleEffectCoverageEntry>,
	) {
		appendLine("## $tableTitle")
		appendLine()
		appendLine("| $subjectLabel | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |")
		appendLine("| --- | --- | --- | --- | --- | --- | --- |")
		entries.forEach { entry -> appendLine(entry.toTableRow()) }
	}

	private fun BattleEffectCoverageEntry.hasGap(): Boolean =
		enabled && (
			!jimmerLoaded ||
				!runtimeSupported ||
				(behaviorTestClasses.isEmpty() && !hasIntentionalNoEffectContract()) ||
				unverifiedPolicies.isNotEmpty()
		)

	private fun BattleEffectCoverageEntry.hasIntentionalNoEffectContract(): Boolean =
		policies.isNotEmpty() && intentionalNoEffectPolicies.containsAll(policies)

	private fun BattleEffectCoverageEntry.toTableRow(): String =
		listOf(
			"`${code.escapeTableCell()}` ${name.escapeTableCell()}",
			if (enabled) "启用" else "停用",
			policies.renderCodeValues(),
			if (jimmerLoaded) "完整" else "缺失",
			if (runtimeSupported) "支持" else "缺失",
			if (hasIntentionalNoEffectContract()) {
				"不适用（明确无效果）"
			} else {
				behaviorTestClasses.sorted().renderCodeValues()
			},
			unverifiedPolicies.renderCodeValues(),
		).joinToString(prefix = "| ", separator = " | ", postfix = " |")

	private fun Collection<String>.renderCodeValues(): String =
		if (isEmpty()) "—" else joinToString(", ") { "`${it.escapeTableCell()}`" }

	private fun String.escapeTableCell(): String = replace("|", "\\|")
}
