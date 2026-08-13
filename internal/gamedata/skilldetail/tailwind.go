package skilldetail

// Tailwind 描述技能成功后尝试在使用者一方建立顺风的完整资料。
//
// 顺风是阵营侧状态：成员换下后仍会影响同侧后续上场成员。它不属于天气、普通场地或成员易变状态，因此单独
// 持久化为技能详情的具名字段；战斗引擎只读取这里冻结后的时长与概率，不从技能名称或效果文本推断规则。
type Tailwind struct {
	// TurnsRemaining 是顺风建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是顺风建立的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validTailwind 校验可选顺风资料的持续回合与触发概率。
func validTailwind(value *Tailwind) bool {
	return value == nil || value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneTailwind 复制可选顺风资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneTailwind(value *Tailwind) *Tailwind {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
