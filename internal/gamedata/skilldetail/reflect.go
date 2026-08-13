package skilldetail

// Reflect 描述技能成功后尝试在使用者一方建立反射壁的完整资料。
//
// 反射壁是只减免普通物理伤害的阵营侧状态。它与光墙、极光幕拥有不同的伤害适用范围，即使三者的持续回合和
// 概率字段外形相同，也必须分别持久化、校验和编译，不能降级为通用屏障 JSON。
type Reflect struct {
	// TurnsRemaining 是反射壁建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是反射壁建立的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validReflect 校验可选反射壁资料的持续回合与触发概率。
func validReflect(value *Reflect) bool {
	return value == nil || value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneReflect 复制可选反射壁资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneReflect(value *Reflect) *Reflect {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
