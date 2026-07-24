package io.github.lishangbu.battlerules.coverage

/**
 * 描述单个战斗特性从资料读取到运行时行为验证的覆盖情况。
 */
data class BattleAbilityCoverageEntry(
	val code: String,
	val name: String,
	val enabled: Boolean,
	val policies: List<String>,
	val jimmerLoaded: Boolean,
	val runtimeSupported: Boolean,
	val behaviorTestClasses: Set<String>,
	val unverifiedPolicies: List<String>,
	val intentionalNoEffectPolicies: List<String> = emptyList(),
)
