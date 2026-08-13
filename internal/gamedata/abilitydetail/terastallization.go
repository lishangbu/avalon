package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// TerastallizationStatStageChange 是成员完成太晶化后立即施加给自身的能力阶级变化资料规则。
//
// 它不复用入场能力阶级变化：触发时机受赛制机制、每方次数和成员太晶状态共同约束，且目标始终是完成太晶化
// 的成员自身。StatID 引用实时游戏资料中的稳定 Identifier，Battle 创建时必须转换为引擎封闭 Stat。
type TerastallizationStatStageChange struct {
	// StatID 是太晶化后需要增减的能力资料稳定 Identifier。
	StatID snowflake.ID
	// StageDelta 是太晶化完成时请求的能力阶级增减，取值为 -6 至 6 且不能为零。
	StageDelta int32
}

// cloneTerastallizationStatStageChange 深复制可选的太晶化能力阶级变化资料。
func cloneTerastallizationStatStageChange(value *TerastallizationStatStageChange) *TerastallizationStatStageChange {
	if value == nil {
		return nil
	}
	return &TerastallizationStatStageChange{StatID: value.StatID, StageDelta: value.StageDelta}
}

// validTerastallizationStatStageChange 校验完整的太晶化能力阶级变化资料。
func validTerastallizationStatStageChange(value *TerastallizationStatStageChange) bool {
	return value == nil || (value.StatID != snowflake.ID(0) && value.StageDelta >= -6 && value.StageDelta <= 6 && value.StageDelta != 0)
}
