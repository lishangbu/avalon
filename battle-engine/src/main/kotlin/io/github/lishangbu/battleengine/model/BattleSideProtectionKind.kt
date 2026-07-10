package io.github.lishangbu.battleengine.model

/**
 * 一侧防护效果种类。
 *
 * 枚举值表达引擎已经显式支持的规则语义，不使用资料表 code。新增防护类型时必须补充对应的技能运行态映射、
 * 阻止入口和公开规则对照测试，避免资料里新增 code 后被静默当成普通无效果技能。
 */
enum class BattleSideProtectionKind {
	STAT_STAGE_REDUCTION,
	STATUS_CONDITION,
	MULTI_TARGET_SKILL,
	PRIORITY_SKILL,
}
