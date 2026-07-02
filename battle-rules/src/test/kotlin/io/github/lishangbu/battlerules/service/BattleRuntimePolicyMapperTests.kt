package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 验证运行时 policy mapper 的纯字符串映射边界。
 *
 * 集成测试已经会从 Liquibase 种子数据读取启用 policy，并走正式快照装配入口；本测试只覆盖不需要数据库的 mapper
 * 细节，尤其是前缀解析和“启用规则必须显式声明目标”的约束。这样拆分 mapper 文件或新增同构 policy 时，可以用
 * 最小测试快速发现字符串解析被改坏。
 */
class BattleRuntimePolicyMapperTests {
	private val elementIds = mapOf(
		"normal" to 1L,
		"fire" to 10L,
		"water" to 11L,
		"grass" to 12L,
	)

	@Test
	fun `skill target mapper requires explicit policy and rejects unknown policy`() {
		assertThat("selected-target".toBattleSkillTargetScope()).isEqualTo(BattleSkillTargetScope.SELECTED_TARGET)
		assertThat("self".toBattleSkillTargetScope()).isEqualTo(BattleSkillTargetScope.SELF)
		assertThat("unknown-target".isBattleSkillRuntimeTargetPolicySupported()).isFalse()
		assertThat("all-opponents".isBattleSkillRuntimeTargetPolicySupported()).isTrue()

		val exception = assertThrows<ApiException> {
			"unknown-target".toBattleSkillTargetScope()
		}

		assertThat(exception.field).isEqualTo("targetPolicy")
		assertThat(exception.message).isEqualTo("不支持的技能目标策略: unknown-target")
	}

	@Test
	fun `item element boost policy is parsed from element suffix`() {
		val effect = "element-damage-boost-fire".toBattleItemEffect(elementIds)

		assertThat(effect).isInstanceOf(BattleItemEffect.ElementDamageBoost::class.java)
		effect as BattleItemEffect.ElementDamageBoost
		assertThat(effect.elementId).isEqualTo(10L)
		assertThat(effect.multiplier).isEqualTo(1.2)
	}

	@Test
	fun `item element reduction keeps normal element exception`() {
		val normalReduction = "element-damage-reduction-normal".toBattleItemEffect(elementIds)
		val waterReduction = "element-damage-reduction-water".toBattleItemEffect(elementIds)

		assertThat(normalReduction).isInstanceOf(BattleItemEffect.ElementDamageReduction::class.java)
		assertThat((normalReduction as BattleItemEffect.ElementDamageReduction).requiresSuperEffective).isFalse()
		assertThat(waterReduction).isInstanceOf(BattleItemEffect.ElementDamageReduction::class.java)
		assertThat((waterReduction as BattleItemEffect.ElementDamageReduction).requiresSuperEffective).isTrue()
	}

	@Test
	fun `ability grounding policy is supported without creating effect object`() {
		assertThat("ground-immunity".toBattleAbilityEffect(elementIds)).isNull()
		assertThat("ground-immunity".isBattleAbilityRuntimePolicySupported(elementIds)).isTrue()
		assertThat("missing-policy".isBattleAbilityRuntimePolicySupported(elementIds)).isFalse()
	}
}
