package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// OpponentSwitchRestriction 描述特性对敌方成员主动换人的独立限制规则。
//
// 规则是否存在由外层指针表达，因此三个条件都取零值时仍可合法表示“限制所有对手主动换人”。这保留了
// 不要求属性或接地的限制特性，不能用“字段全部为零即没有规则”的隐式约定替代。
type OpponentSwitchRestriction struct {
	// RequiredTargetElementID 是受限制目标必须拥有的可选属性稳定 Identifier。
	// nil 表示不按属性筛选；实际属性是否启用由 Battle 启动时的资料冻结边界校验。
	RequiredTargetElementID *snowflake.ID
	// RequiresGroundedTarget 表示只有当前接地的目标会被限制主动换人。
	// 接地的完整定义属于 Battle Engine；资料层只保存这一独立条件，不能借飞行属性名称推断。
	RequiresGroundedTarget bool
	// SameEffectGrantsImmunity 表示目标自身具有与本规则完整相同的主动换人限制时，不受本规则限制。
	// 它只影响同一种规则间的免疫，不会替代持有道具提供的明确豁免。
	SameEffectGrantsImmunity bool
}

func cloneOpponentSwitchRestriction(value *OpponentSwitchRestriction) *OpponentSwitchRestriction {
	if value == nil {
		return nil
	}
	cloned := *value
	if value.RequiredTargetElementID != nil {
		requiredTargetElementID := *value.RequiredTargetElementID
		cloned.RequiredTargetElementID = &requiredTargetElementID
	}
	return &cloned
}

func validOpponentSwitchRestriction(value *OpponentSwitchRestriction) bool {
	return value == nil || value.RequiredTargetElementID == nil || *value.RequiredTargetElementID != snowflake.ID(0)
}
