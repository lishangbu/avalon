package skilldetail

// StealthRock 描述技能成功后尝试在被选中对手一方场地布置隐形岩的完整资料。
//
// 隐形岩没有层数，并对所有换入成员按岩石属性相性造成伤害；它不能复用撒菱层数或其它入场危害资料。
type StealthRock struct {
	// ChancePercent 是本次尝试布置隐形岩的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validStealthRock 校验可选隐形岩资料的触发概率。
func validStealthRock(value *StealthRock) bool {
	return value == nil || value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneStealthRock 复制可选隐形岩资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneStealthRock(value *StealthRock) *StealthRock {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
