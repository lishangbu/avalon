package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// SwitchInStatStageTarget 是特性入场能力阶级变化可作用的封闭目标集合。
type SwitchInStatStageTarget string

const (
	// SwitchInStatStageTargetSelf 表示特性持有成员自身。
	SwitchInStatStageTargetSelf SwitchInStatStageTarget = "self"
	// SwitchInStatStageTargetOpponents 表示特性持有成员对侧的所有当前上场成员。
	SwitchInStatStageTargetOpponents SwitchInStatStageTarget = "opponents"
)

// validSwitchInStatStageTarget 报告目标集合是否属于当前资料层支持的封闭值。
func validSwitchInStatStageTarget(value SwitchInStatStageTarget) bool {
	return value == SwitchInStatStageTargetSelf || value == SwitchInStatStageTargetOpponents
}

// SwitchInStatStageChange 是特性持有成员进入场地时立即改变能力阶级的独立资料规则。
//
// 它不复用技能能力变化：没有概率、不读取技能目标，且只在成员实际成功进入场地后触发。StatID 仍引用实时
// 游戏资料中的稳定 Identifier，Battle 启动时会严格映射为引擎封闭 Stat。
type SwitchInStatStageChange struct {
	// Target 是该规则作用于持有成员自身或其当前上场对手。
	Target SwitchInStatStageTarget
	// StatID 是需要增减的能力资料稳定 Identifier。
	StatID snowflake.ID
	// StageDelta 是请求的能力阶级增减，取值为 -6 至 6 且不能为零。
	StageDelta int32
}

// cloneSwitchInStatStageChange 深复制可选的入场能力阶级变化资料。
func cloneSwitchInStatStageChange(value *SwitchInStatStageChange) *SwitchInStatStageChange {
	if value == nil {
		return nil
	}
	return &SwitchInStatStageChange{Target: value.Target, StatID: value.StatID, StageDelta: value.StageDelta}
}

// validSwitchInStatStageChange 校验完整的入场能力阶级变化资料。
func validSwitchInStatStageChange(value *SwitchInStatStageChange) bool {
	return value == nil || (validSwitchInStatStageTarget(value.Target) && value.StatID != snowflake.ID(0) &&
		value.StageDelta >= -6 && value.StageDelta <= 6 && value.StageDelta != 0)
}
