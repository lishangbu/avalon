package battleengine

import "fmt"

// TerastallizationStatStageChange 是特性持有成员完成太晶化时立即作用于自身的一条能力阶级变化规则。
//
// 它不复用技能命中后的 StatStageEffect：没有目标选择、没有触发概率，也不会在普通入场时执行。Battle 必须把
// 游戏资料中的能力 Identifier 映射为封闭 Stat 后再写入本结构。
type TerastallizationStatStageChange struct {
	// Stat 是要改变的封闭能力项。
	Stat Stat `json:"stat"`
	// StageDelta 是请求的能力阶级增减，范围为 -6 至 6 且不能为零。
	StageDelta int8 `json:"stageDelta"`
}

// validateTerastallizationStatStageChange 校验可选的太晶化自身能力阶级变化规则。
func validateTerastallizationStatStageChange(value *TerastallizationStatStageChange) error {
	if value == nil {
		return nil
	}
	if !value.Stat.Valid() || value.StageDelta == 0 || value.StageDelta < -6 || value.StageDelta > 6 {
		return fmt.Errorf("太晶化能力阶级变化规则无效")
	}
	return nil
}

// cloneTerastallizationStatStageChange 深复制可选太晶化特性规则，保持 State 快照与调用方输入隔离。
func cloneTerastallizationStatStageChange(value *TerastallizationStatStageChange) *TerastallizationStatStageChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
