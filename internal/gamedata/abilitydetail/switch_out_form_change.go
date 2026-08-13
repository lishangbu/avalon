package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// SwitchOutFormChange 是特性持有成员成功离场时执行的确定形态切换资料。
//
// 它与入场形态、天气形态和太晶化各自拥有独立触发窗口。两个字段只保存稳定 Identifier；目标形态的能力、属性、
// 体重和最大生命必须由 Battle 在对局开始时冻结为 FormProfile，Battle Engine 不会查询实时资料。
type SwitchOutFormChange struct {
	// BaseCreatureID 是允许触发离场形态切换的当前基础形态稳定 Identifier。
	BaseCreatureID snowflake.ID
	// AlternateCreatureID 是成功离场后切换到的目标形态稳定 Identifier。
	AlternateCreatureID snowflake.ID
}

// validSwitchOutFormChange 判断离场形态切换资料是否引用两个不同的有效精灵。
func validSwitchOutFormChange(value *SwitchOutFormChange) bool {
	return value == nil || (value.BaseCreatureID != snowflake.ID(0) && value.AlternateCreatureID != snowflake.ID(0) &&
		value.BaseCreatureID != value.AlternateCreatureID)
}

// cloneSwitchOutFormChange 深复制离场形态切换资料，避免管理请求的指针与权威领域值共享。
func cloneSwitchOutFormChange(value *SwitchOutFormChange) *SwitchOutFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// validSwitchOutHealDenominator 判断离场固定比例回复分母是否处于持久化与引擎共同支持的范围。
// 0 是未声明该规则的唯一哨兵；正值必须能在 uint32 生命计算中安全使用。
func validSwitchOutHealDenominator(value int32) bool {
	return value >= 0 && value <= 65_535
}
