package skilldetail

// ToxicSpikes 描述技能成功后尝试在被选中对手一方场地增加一层毒菱的完整资料。
//
// 毒菱最多两层。接地毒属性成员换入会吸收全部层数，其他接地成员会被施加普通中毒或剧毒，因此不能与撒菱或
// 隐形岩使用同一资料结构。
type ToxicSpikes struct {
	// ChancePercent 是本次尝试增加一层毒菱的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validToxicSpikes 校验可选毒菱资料的触发概率。
func validToxicSpikes(value *ToxicSpikes) bool {
	return value == nil || value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneToxicSpikes 复制可选毒菱资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneToxicSpikes(value *ToxicSpikes) *ToxicSpikes {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
