package skilldetail

// Spikes 描述技能成功后尝试在被选中对手一方场地增加一层撒菱的完整资料。
//
// 撒菱最多三层，只伤害接地换入成员。它与按属性倍率结算的隐形岩、施加异常的毒菱和降低速度的黏黏网拥有
// 不同的建立与换入语义，因此必须使用独立资料类型、持久化列和编译分支。
type Spikes struct {
	// ChancePercent 是本次尝试增加一层撒菱的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validSpikes 校验可选撒菱资料的触发概率。
func validSpikes(value *Spikes) bool {
	return value == nil || value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneSpikes 复制可选撒菱资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneSpikes(value *Spikes) *Spikes {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
