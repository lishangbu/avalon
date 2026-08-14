package abilitydetail

// AccuracyMultiplier 是特性命中规则使用的精确正整数分数。
//
// 它只表达数值倍率，不携带目标、天气或技能分类等语义；每个使用它的特性详情字段都显式声明自己的触发条件，
// 从而避免把生命周期不同的规则收敛为难以校验的泛型效果列表。
type AccuracyMultiplier struct {
	// Numerator 是倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// cloneAccuracyMultiplier 深拷贝可选命中倍率，保留 nil 表示未声明该规则。
func cloneAccuracyMultiplier(value *AccuracyMultiplier) *AccuracyMultiplier {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// validAccuracyMultiplier 校验可选命中倍率的封闭数值范围。
//
// nil 是合法的“没有该规则”；1/1 也是合法的显式中性倍率，持久化适配器会以同一个数据库中性值持久化，避免 null
// 与零值进入数值分母。
func validAccuracyMultiplier(value *AccuracyMultiplier) bool {
	return value == nil || (value.Numerator >= 1 && value.Numerator <= 65_535 &&
		value.Denominator >= 1 && value.Denominator <= 65_535)
}
