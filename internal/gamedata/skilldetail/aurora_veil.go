package skilldetail

// AuroraVeil 描述技能成功后尝试在使用者一方建立极光幕的完整资料。
//
// 极光幕同时减免普通物理和特殊伤害，因而不能复用反射壁或光墙的资料类型。独立类型和独立列保证三种屏障
// 在管理、资料冻结和战斗回放中均维持各自规则语义。
type AuroraVeil struct {
	// TurnsRemaining 是极光幕建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是极光幕建立的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validAuroraVeil 校验可选极光幕资料的持续回合与触发概率。
func validAuroraVeil(value *AuroraVeil) bool {
	return value == nil || value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneAuroraVeil 复制可选极光幕资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneAuroraVeil(value *AuroraVeil) *AuroraVeil {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
