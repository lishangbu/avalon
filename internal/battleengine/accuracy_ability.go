package battleengine

import "math/big"

// AccuracyMultiplier 是战斗快照中命中规则使用的精确正整数分数。
//
// 类型仅表示数值倍率；每个 MemberSnapshot 字段都固定了自己的作用对象和触发条件，不能把天气、混乱、技能分类
// 等不同生命周期压缩为自由解释的通用效果集合。
type AccuracyMultiplier struct {
	// Numerator 是倍率的正整数分子，范围为 1 至 65535。
	Numerator uint16 `json:"numerator"`
	// Denominator 是倍率的正整数分母，范围为 1 至 65535。
	Denominator uint16 `json:"denominator"`
}

// cloneAccuracyMultiplier 深拷贝可选命中倍率。
func cloneAccuracyMultiplier(value *AccuracyMultiplier) *AccuracyMultiplier {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// validAccuracyMultiplier 校验精确命中分数；nil 表示没有该条特性规则。
func validAccuracyMultiplier(value *AccuracyMultiplier) bool {
	return value == nil || (value.Numerator != 0 && value.Denominator != 0)
}

// applyAbilityAccuracyMultiplier 将全部已满足条件的特性倍率在一次整数除法中应用到基础命中率。
//
// 使用者任意技能倍率先于物理专用倍率，目标侧规则再按普通天气和混乱分别叠加；所有乘除通过一个分子/分母链在
// 最后一次向下取整，和批准基线的整条公式取整顺序一致，避免每步截断带来的重放差异。
func applyAbilityAccuracyMultiplier(weather *WeatherEffect, actor, target MemberSnapshot, skill SkillSnapshot, accuracy uint8) uint8 {
	if accuracy == 0 {
		return 0
	}
	// 每个数据库字段都允许 65535/1，因此五条规则的中间乘积可能超过 uint64；使用标准库大整数保持规则
	// 基线“全部倍率相乘后一次向下取整”的精确语义，而不是逐项截断或溢出回绕。
	numerator, denominator := big.NewInt(int64(accuracy)), big.NewInt(1)
	appendMultiplier := func(value *AccuracyMultiplier) {
		if value == nil {
			return
		}
		numerator.Mul(numerator, big.NewInt(int64(value.Numerator)))
		denominator.Mul(denominator, big.NewInt(int64(value.Denominator)))
	}
	appendMultiplier(actor.AccuracyMultiplier)
	if skill.DamageClass == DamageClassPhysical {
		appendMultiplier(actor.PhysicalSkillAccuracyMultiplier)
	}
	// 无视目标特性只排除目标侧三类命中倍率；使用者自身的命中倍率仍属于攻击侧规则，必须继续参与计算。
	if !ignoresTargetAbilityEffects(actor, skill) {
		if weather != nil && weather.Kind == WeatherKindSandstorm {
			appendMultiplier(target.OpponentAccuracySandstormMultiplier)
		}
		if weather != nil && weather.Kind == WeatherKindSnow {
			appendMultiplier(target.OpponentAccuracySnowMultiplier)
		}
		if target.ConfusionTurnsRemaining > 0 {
			appendMultiplier(target.OpponentAccuracyConfusionMultiplier)
		}
	}
	numerator.Quo(numerator, denominator)
	if !numerator.IsUint64() || numerator.Uint64() > 100 {
		return 100
	}
	return uint8(max(uint64(1), numerator.Uint64()))
}
