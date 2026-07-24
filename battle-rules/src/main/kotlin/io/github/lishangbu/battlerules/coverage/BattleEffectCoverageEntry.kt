package io.github.lishangbu.battlerules.coverage

/** 统一描述战斗特性或携带道具从资料读取到行为验证的覆盖事实。 */
interface BattleEffectCoverageEntry {
	val code: String
	val name: String
	val enabled: Boolean
	val policies: List<String>
	val jimmerLoaded: Boolean
	val runtimeSupported: Boolean
	val behaviorTestClasses: Set<String>
	val unverifiedPolicies: List<String>
	val intentionalNoEffectPolicies: List<String>
}
