package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// SwitchInFormChange 是特性持有成员进入场地时一次性切换形态的资料规则。
//
// 两个形态均使用 Creature Data Projection 的稳定 Identifier。具体能力、属性、体重和最大生命由 Battle 在对局开始时按成员
// 培养值计算并冻结，不能把这些运行时数值、形态名称或 Stable Code 写入特性详情。
type SwitchInFormChange struct {
	// BaseCreatureID 是允许本规则触发的基础形态稳定 Identifier。
	BaseCreatureID snowflake.ID
	// AlternateCreatureID 是触发后切换到的目标形态稳定 Identifier。
	AlternateCreatureID snowflake.ID
	// AddsMaximumHPDifference 表示目标最大生命更高时补齐两种形态的正差额。
	AddsMaximumHPDifference bool
}

// validSwitchInFormChange 校验可选的入场形态切换资料。
func validSwitchInFormChange(value *SwitchInFormChange) bool {
	return value == nil || (value.BaseCreatureID != snowflake.ID(0) && value.AlternateCreatureID != snowflake.ID(0) &&
		value.BaseCreatureID != value.AlternateCreatureID)
}

// cloneSwitchInFormChange 深复制可选的入场形态切换资料。
func cloneSwitchInFormChange(value *SwitchInFormChange) *SwitchInFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
