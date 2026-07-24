package io.github.lishangbu.battlerules.coverage

/** 将启用技能覆盖情况渲染为稳定、便于代码审查的 Markdown 报告。 */
class BattleSkillCoverageMatrix {
	private val delegate = BattleEffectCoverageMatrix(
		title = "战斗技能覆盖矩阵",
		generatorTask = ":battle-rules:generateBattleSkillCoverage",
		totalLabel = "启用技能",
		subjectLabel = "技能",
	)

	/** 按技能编码排序并生成覆盖矩阵。 */
	fun render(entries: List<BattleSkillCoverageEntry>): String = delegate.render(entries)
}
