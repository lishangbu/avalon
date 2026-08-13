package skilldetail

// LeechSeed 描述技能命中后尝试在已选目标本体种下寄生种子的独立资料规则。
//
// 寄生种子的来源是使用者当时的场上槽位，而非稳定成员；目标换下时状态清除，来源换下后由替换成员获得
// 回合末回复。它既不是有固定持续时间的易变状态，也不是通用吸血参数，因此使用独立强类型模型持久化。
type LeechSeed struct {
	// ChancePercent 是种子写入目标的独立触发概率，取值范围为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validLeechSeed 校验可选寄生种子资料的概率边界。
func validLeechSeed(value *LeechSeed) bool {
	return value == nil || value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneLeechSeed 复制可选资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneLeechSeed(value *LeechSeed) *LeechSeed {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
