package abilitydetail

// SwitchInAllyHeal 是特性持有成员进入场地时为同侧其它上场成员回复生命的独立资料规则。
//
// 它不使用技能目标、命中率或概率，也不回复触发成员自身或后备成员。回复量由每个接收者自己的最大生命计算，
// 不能借用天气回复、场地回复或描述文本表达。
type SwitchInAllyHeal struct {
	// HealDenominator 是每名实际接收者按最大生命计算的正回复分母。
	HealDenominator int32
}

// cloneSwitchInAllyHeal 深复制可选的入场同侧回复资料。
func cloneSwitchInAllyHeal(value *SwitchInAllyHeal) *SwitchInAllyHeal {
	if value == nil {
		return nil
	}
	return &SwitchInAllyHeal{HealDenominator: value.HealDenominator}
}

// validSwitchInAllyHeal 校验完整的入场同侧回复资料。
func validSwitchInAllyHeal(value *SwitchInAllyHeal) bool {
	return value == nil || (value.HealDenominator >= 1 && value.HealDenominator <= 65_535)
}
