package io.github.lishangbu.battlerules.coverage

/**
 * 将战斗特性覆盖情况渲染为稳定、便于代码审查的 Markdown 报告。
 */
class BattleAbilityCoverageMatrix {
	/** 按特性编码排序并生成覆盖矩阵。 */
	fun render(entries: List<BattleAbilityCoverageEntry>): String {
		val sortedEntries = entries.sortedBy(BattleAbilityCoverageEntry::code)
		val gaps = sortedEntries.filter { it.hasGap() }
		return buildString {
			appendLine("# 战斗特性覆盖矩阵")
			appendLine()
			appendLine("> 本文件由 `./gradlew :battle-rules:generateBattleAbilityCoverage` 生成，请勿手工编辑。")
			appendLine()
			appendLine("## 汇总")
			appendLine()
			appendLine("- 主系列特性：${sortedEntries.size}")
			appendLine("- 已启用资料：${sortedEntries.count(BattleAbilityCoverageEntry::enabled)}")
			appendLine("- Jimmer 规则读取完整：${sortedEntries.count(BattleAbilityCoverageEntry::jimmerLoaded)}")
			appendLine("- 运行时策略完整：${sortedEntries.count(BattleAbilityCoverageEntry::runtimeSupported)}")
			appendLine("- 具备行为测试证据：${sortedEntries.count { it.behaviorTestClasses.isNotEmpty() }}")
			appendLine("- 待补缺口：${gaps.size}")
			appendLine()
			appendTable("待补缺口", gaps)
			appendLine()
			appendTable("完整矩阵", sortedEntries)
		}
	}

	private fun StringBuilder.appendTable(
		title: String,
		entries: List<BattleAbilityCoverageEntry>,
	) {
		appendLine("## $title")
		appendLine()
		appendLine("| 特性 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |")
		appendLine("| --- | --- | --- | --- | --- | --- | --- |")
		entries.forEach { entry -> appendLine(entry.toTableRow()) }
	}

	private fun BattleAbilityCoverageEntry.hasGap(): Boolean =
		enabled && (!jimmerLoaded || !runtimeSupported || behaviorTestClasses.isEmpty() || unverifiedPolicies.isNotEmpty())

	private fun BattleAbilityCoverageEntry.toTableRow(): String =
		listOf(
			"`${code.escapeTableCell()}` ${name.escapeTableCell()}",
			if (enabled) "启用" else "停用",
			policies.renderCodeValues(),
			if (jimmerLoaded) "完整" else "缺失",
			if (runtimeSupported) "支持" else "缺失",
			behaviorTestClasses.sorted().renderCodeValues(),
			unverifiedPolicies.renderCodeValues(),
		).joinToString(prefix = "| ", separator = " | ", postfix = " |")

	private fun Collection<String>.renderCodeValues(): String =
		if (isEmpty()) "—" else joinToString(", ") { "`${it.escapeTableCell()}`" }

	private fun String.escapeTableCell(): String = replace("|", "\\|")
}
