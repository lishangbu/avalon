package skilldetail

// LightScreen 描述技能成功后尝试在使用者一方建立光墙的完整资料。
//
// 光墙只减免普通特殊伤害，是与反射壁和极光幕不同的阵营侧状态。单独的资料类型保证管理端、资料编译和回放
// 可以保留该减伤范围，而不会将其误写为物理屏障或双防屏障。
type LightScreen struct {
	// TurnsRemaining 是光墙建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是光墙建立的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validLightScreen 校验可选光墙资料的持续回合与触发概率。
func validLightScreen(value *LightScreen) bool {
	return value == nil || value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneLightScreen 复制可选光墙资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneLightScreen(value *LightScreen) *LightScreen {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
