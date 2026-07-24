package io.github.lishangbu.battlerules.coverage

/** 将战斗特性覆盖情况渲染为稳定、便于代码审查的 Markdown 报告。 */
class BattleAbilityCoverageMatrix {
	private val delegate = BattleEffectCoverageMatrix(
		title = "战斗特性覆盖矩阵",
		generatorTask = ":battle-rules:generateBattleAbilityCoverage",
		totalLabel = "主系列特性",
		subjectLabel = "特性",
	)

	/** 按特性编码排序并生成覆盖矩阵。 */
	fun render(entries: List<BattleAbilityCoverageEntry>): String = delegate.render(entries)
}
