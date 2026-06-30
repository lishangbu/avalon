package io.github.lishangbu.battleengine

import kotlin.test.assertEquals

/**
 * 公开规则对照测试的元数据。
 *
 * 每条战斗规则 fixture 都需要说明名称、输入摘要和期望摘要。这个类型只服务测试层，
 * 不参与生产代码，也不替代具体断言；它的职责是让后续规则实现都能留下稳定的测试事实。
 */
internal data class PublicBattleRuleFixture(
	val name: String,
	val inputSummary: String,
	val expectedSummary: String,
) {
	init {
		require(name.isNotBlank()) { "fixture name must not be blank" }
		require(inputSummary.isNotBlank()) { "fixture inputSummary must not be blank" }
		require(expectedSummary.isNotBlank()) { "fixture expectedSummary must not be blank" }
	}

	/**
	 * 明确断言 fixture 名称，防止复制测试时只改了断言数据却漏改场景标识。
	 */
	fun assertNamed(expected: String) {
		assertEquals(expected, name)
	}
}

/**
 * 创建公开规则对照 fixture。
 *
 * 这个函数保留为普通构造入口，而不是做成通用测试执行器，避免把每条规则独有的断言流程藏进抽象里。
 */
internal fun publicBattleRuleFixture(
	name: String,
	inputSummary: String,
	expectedSummary: String,
): PublicBattleRuleFixture =
	PublicBattleRuleFixture(
		name = name,
		inputSummary = inputSummary,
		expectedSummary = expectedSummary,
	)
