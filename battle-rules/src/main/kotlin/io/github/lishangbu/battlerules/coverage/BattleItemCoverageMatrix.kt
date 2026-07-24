package io.github.lishangbu.battlerules.coverage

/** 将携带道具覆盖情况渲染为稳定、便于代码审查的 Markdown 报告。 */
class BattleItemCoverageMatrix {
	private val delegate = BattleEffectCoverageMatrix(
		title = "战斗道具覆盖矩阵",
		generatorTask = ":battle-rules:generateBattleItemCoverage",
		totalLabel = "携带道具",
		subjectLabel = "道具",
	)

	/** 按道具编码排序并生成覆盖矩阵。 */
	fun render(entries: List<BattleItemCoverageEntry>): String = delegate.render(entries)
}
