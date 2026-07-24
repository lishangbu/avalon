package io.github.lishangbu.battlerules.coverage

/** 描述单个携带道具从资料读取到运行时行为验证的覆盖情况。 */
data class BattleItemCoverageEntry(
	override val code: String,
	override val name: String,
	override val enabled: Boolean,
	override val policies: List<String>,
	override val jimmerLoaded: Boolean,
	override val runtimeSupported: Boolean,
	override val behaviorTestClasses: Set<String>,
	override val unverifiedPolicies: List<String>,
	override val intentionalNoEffectPolicies: List<String> = emptyList(),
) : BattleEffectCoverageEntry
