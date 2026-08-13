package skilldetail

// StickyWeb 描述技能成功后尝试在被选中对手一方场地布置黏黏网的完整资料。
//
// 黏黏网没有层数、不会造成伤害或施加主要异常；接地成员换入时会降低速度能力阶级，故必须与其它入场危害
// 使用独立的资料和持久化字段。
type StickyWeb struct {
	// ChancePercent 是本次尝试布置黏黏网的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validStickyWeb 校验可选黏黏网资料的触发概率。
func validStickyWeb(value *StickyWeb) bool {
	return value == nil || value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneStickyWeb 复制可选黏黏网资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneStickyWeb(value *StickyWeb) *StickyWeb {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
