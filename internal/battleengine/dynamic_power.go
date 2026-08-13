package battleengine

import (
	"errors"
	"fmt"
)

// DynamicPowerKind 是普通伤害公式前重新计算基础威力的封闭规则种类。
//
// 动态威力不是伤害倍率：它在攻击、防御、属性一致加成、要害和属性相性之前，替换本次公式使用的基础威力。
// 每种 kind 都拥有独立的强类型参数，禁止根据技能名称、Stable Code 或自由文本在运行时猜测规则。
type DynamicPowerKind string

const (
	// DynamicPowerKindPositiveStatStageSum 按使用者或目标所有正向能力阶级总和计算威力。
	DynamicPowerKindPositiveStatStageSum DynamicPowerKind = "positiveStatStageSum"
	// DynamicPowerKindUserSpeedRatioThresholds 按使用者有效速度相对目标有效速度的整数倍数选择威力。
	DynamicPowerKindUserSpeedRatioThresholds DynamicPowerKind = "userSpeedRatioThresholds"
	// DynamicPowerKindTargetToUserSpeedRatio 按目标有效速度相对使用者有效速度的比例计算并封顶威力。
	DynamicPowerKindTargetToUserSpeedRatio DynamicPowerKind = "targetToUserSpeedRatio"
	// DynamicPowerKindTargetWeightThresholds 按目标当前体重所在的连续区间选择威力。
	DynamicPowerKindTargetWeightThresholds DynamicPowerKind = "targetWeightThresholds"
	// DynamicPowerKindUserTargetWeightRatioThresholds 按使用者与目标当前体重的整数比例选择威力。
	DynamicPowerKindUserTargetWeightRatioThresholds DynamicPowerKind = "userTargetWeightRatioThresholds"
	// DynamicPowerKindUserHPFractionThresholds 按使用者当前生命占最大生命的离散分档选择威力。
	DynamicPowerKindUserHPFractionThresholds DynamicPowerKind = "userHPFractionThresholds"
)

// valid 报告动态威力规则是否为当前纯战斗引擎支持的稳定值；空值表示该技能使用静态 Power。
func (kind DynamicPowerKind) valid() bool {
	return kind == "" || kind == DynamicPowerKindPositiveStatStageSum ||
		kind == DynamicPowerKindUserSpeedRatioThresholds || kind == DynamicPowerKindTargetToUserSpeedRatio ||
		kind == DynamicPowerKindTargetWeightThresholds || kind == DynamicPowerKindUserTargetWeightRatioThresholds ||
		kind == DynamicPowerKindUserHPFractionThresholds
}

// SpeedPowerThreshold 是按有效速度整数倍数选择动态威力的一档。
type SpeedPowerThreshold struct {
	// MinimumRatio 是分子速度至少达到分母速度多少倍才命中本档；必须为正数。
	MinimumRatio uint16 `json:"minimumRatio"`
	// Power 是命中本档时进入普通伤害公式的正基础威力。
	Power uint16 `json:"power"`
}

// WeightPowerThreshold 是按目标当前体重选择动态威力的一档。
type WeightPowerThreshold struct {
	// MaximumWeightInclusive 是与 MemberSnapshot.Weight 使用同一资料整数刻度的闭区间上界。
	MaximumWeightInclusive uint32 `json:"maximumWeightInclusive"`
	// Power 是目标体重不超过本档上界时使用的正基础威力。
	Power uint16 `json:"power"`
}

// WeightRatioPowerThreshold 是按使用者与目标当前体重比例选择动态威力的一档。
type WeightRatioPowerThreshold struct {
	// MinimumUserToTargetRatio 是使用者体重至少为目标体重多少倍才命中本档；必须为正数。
	MinimumUserToTargetRatio uint16 `json:"minimumUserToTargetRatio"`
	// Power 是命中本档时使用的正基础威力。
	Power uint16 `json:"power"`
}

// HPFractionPowerThreshold 是按使用者当前生命比例选择动态威力的一档。
type HPFractionPowerThreshold struct {
	// MaximumScaledHPInclusive 是 floor(scale * currentHP / maxHP) 的闭区间上界。
	MaximumScaledHPInclusive uint16 `json:"maximumScaledHpInclusive"`
	// Power 是当前缩放生命值不超过本档上界时使用的正基础威力。
	Power uint16 `json:"power"`
}

// DynamicPowerRule 保存一种已冻结、可在逐目标伤害阶段执行的动态基础威力规则。
//
// Kind 决定哪些字段可用。保留各类阈值为独立字段而不是混合“通用阈值”数组，是因为速度、体重和生命比例
// 的比较方向、单位和边界语义不同；validateDynamicPowerRule 会拒绝缺失参数、无序阈值和溢出风险配置。
type DynamicPowerRule struct {
	// Kind 是本规则的封闭种类；空值表示不启用动态威力并读取 SkillSnapshot.Power。
	Kind DynamicPowerKind `json:"kind,omitempty"`
	// Source 仅供 positiveStatStageSum 使用，表示累加使用者或当前实际目标的正向能力阶级。
	Source EffectTarget `json:"source,omitempty"`
	// BasePower 仅供 positiveStatStageSum 使用，是没有任何正向能力阶级时的正基础威力。
	BasePower uint16 `json:"basePower,omitempty"`
	// PowerPerPositiveStage 仅供 positiveStatStageSum 使用，是每一级正向能力阶级增加的威力。
	PowerPerPositiveStage uint16 `json:"powerPerPositiveStage,omitempty"`
	// MaximumPower 供 positiveStatStageSum 与 targetToUserSpeedRatio 使用；0 表示前者没有额外封顶，
	// 而后者必须提供正上限，避免极端速度资料扩大基础威力。
	MaximumPower uint16 `json:"maximumPower,omitempty"`
	// SpeedThresholds 仅供 userSpeedRatioThresholds 使用，必须按 MinimumRatio 从大到小严格排列。
	SpeedThresholds []SpeedPowerThreshold `json:"speedThresholds,omitempty"`
	// FallbackPower 供三类阈值规则使用：没有任何阈值命中时采用该正基础威力。
	FallbackPower uint16 `json:"fallbackPower,omitempty"`
	// SpeedRatioMultiplier 仅供 targetToUserSpeedRatio 使用，表示公式中的正整数倍率。
	SpeedRatioMultiplier uint16 `json:"speedRatioMultiplier,omitempty"`
	// SpeedRatioAdditivePower 仅供 targetToUserSpeedRatio 使用，表示速度比例项之后相加的非负威力。
	SpeedRatioAdditivePower uint16 `json:"speedRatioAdditivePower,omitempty"`
	// WeightThresholds 仅供 targetWeightThresholds 使用，必须按 MaximumWeightInclusive 从小到大严格排列。
	WeightThresholds []WeightPowerThreshold `json:"weightThresholds,omitempty"`
	// WeightRatioThresholds 仅供 userTargetWeightRatioThresholds 使用，必须按 MinimumUserToTargetRatio 从大到小严格排列。
	WeightRatioThresholds []WeightRatioPowerThreshold `json:"weightRatioThresholds,omitempty"`
	// HPFractionScale 仅供 userHPFractionThresholds 使用，是离散生命比例计算中的正缩放常量。
	HPFractionScale uint16 `json:"hpFractionScale,omitempty"`
	// HPFractionThresholds 仅供 userHPFractionThresholds 使用，必须按 MaximumScaledHPInclusive 从小到大严格排列。
	HPFractionThresholds []HPFractionPowerThreshold `json:"hpFractionThresholds,omitempty"`
}

// active 报告技能是否声明了动态威力规则。
func (rule DynamicPowerRule) active() bool { return rule.Kind != "" }

// validateDynamicPowerRule 校验各类动态威力规则的参数、排序与伤害公式安全上界。
func validateDynamicPowerRule(rule DynamicPowerRule) error {
	if !rule.Kind.valid() {
		return errors.New("动态威力规则未知")
	}
	if !rule.active() {
		return nil
	}
	switch rule.Kind {
	case DynamicPowerKindPositiveStatStageSum:
		if !rule.Source.Valid() || rule.BasePower == 0 || rule.PowerPerPositiveStage == 0 ||
			rule.MaximumPower != 0 && rule.MaximumPower < rule.BasePower {
			return errors.New("正向能力阶级动态威力参数无效")
		}
		// 七项能力每项最高 +6 阶。未设置上限时仍要求最坏情况下能放入既有 uint16 基础威力公式。
		if rule.MaximumPower == 0 && uint32(rule.BasePower)+uint32(rule.PowerPerPositiveStage)*42 > uint32(^uint16(0)) {
			return errors.New("正向能力阶级动态威力可能溢出")
		}
	case DynamicPowerKindUserSpeedRatioThresholds:
		if rule.FallbackPower == 0 || len(rule.SpeedThresholds) == 0 {
			return errors.New("使用者速度比例动态威力参数无效")
		}
		previous := ^uint16(0)
		for _, threshold := range rule.SpeedThresholds {
			if threshold.MinimumRatio == 0 || threshold.Power == 0 || threshold.MinimumRatio >= previous {
				return errors.New("使用者速度比例动态威力阈值无效")
			}
			previous = threshold.MinimumRatio
		}
	case DynamicPowerKindTargetToUserSpeedRatio:
		if rule.SpeedRatioMultiplier == 0 || rule.MaximumPower == 0 {
			return errors.New("目标速度比例动态威力参数无效")
		}
	case DynamicPowerKindTargetWeightThresholds:
		if rule.FallbackPower == 0 || len(rule.WeightThresholds) == 0 {
			return errors.New("目标体重动态威力参数无效")
		}
		var previous uint32
		for index, threshold := range rule.WeightThresholds {
			if threshold.MaximumWeightInclusive == 0 || threshold.Power == 0 || index > 0 && threshold.MaximumWeightInclusive <= previous {
				return errors.New("目标体重动态威力阈值无效")
			}
			previous = threshold.MaximumWeightInclusive
		}
	case DynamicPowerKindUserTargetWeightRatioThresholds:
		if rule.FallbackPower == 0 || len(rule.WeightRatioThresholds) == 0 {
			return errors.New("体重比例动态威力参数无效")
		}
		previous := ^uint16(0)
		for _, threshold := range rule.WeightRatioThresholds {
			if threshold.MinimumUserToTargetRatio == 0 || threshold.Power == 0 || threshold.MinimumUserToTargetRatio >= previous {
				return errors.New("体重比例动态威力阈值无效")
			}
			previous = threshold.MinimumUserToTargetRatio
		}
	case DynamicPowerKindUserHPFractionThresholds:
		if rule.HPFractionScale == 0 || rule.FallbackPower == 0 || len(rule.HPFractionThresholds) == 0 {
			return errors.New("使用者生命比例动态威力参数无效")
		}
		var previous uint16
		for index, threshold := range rule.HPFractionThresholds {
			if threshold.Power == 0 || index > 0 && threshold.MaximumScaledHPInclusive <= previous {
				return errors.New("使用者生命比例动态威力阈值无效")
			}
			previous = threshold.MaximumScaledHPInclusive
		}
	default:
		return fmt.Errorf("动态威力规则 %q 未实现", rule.Kind)
	}
	return nil
}

// dynamicPower 返回逐目标普通伤害公式应读取的基础威力。
//
// State 创建时已经验证规则参数，因此本函数只基于行动当前的成员快照计算，不消费随机数，也不会修改状态。
// 速度统一经 effectiveSpeed 读取，保证动态威力与回合排序使用相同的能力阶级和主要异常修正口径。
func dynamicPower(skill SkillSnapshot, user, target MemberSnapshot) uint16 {
	rule := skill.DynamicPower
	if !rule.active() {
		return skill.Power
	}
	switch rule.Kind {
	case DynamicPowerKindPositiveStatStageSum:
		source := user
		if rule.Source == EffectTargetSelected {
			source = target
		}
		positiveStages := uint32(0)
		for _, stage := range source.StatStages {
			if stage > 0 {
				positiveStages += uint32(stage)
			}
		}
		power := uint32(rule.BasePower) + uint32(rule.PowerPerPositiveStage)*positiveStages
		if rule.MaximumPower != 0 && power > uint32(rule.MaximumPower) {
			return rule.MaximumPower
		}
		return uint16(power)
	case DynamicPowerKindUserSpeedRatioThresholds:
		userSpeed, targetSpeed := effectiveSpeed(user), effectiveSpeed(target)
		for _, threshold := range rule.SpeedThresholds {
			if uint64(userSpeed) >= uint64(targetSpeed)*uint64(threshold.MinimumRatio) {
				return threshold.Power
			}
		}
		return rule.FallbackPower
	case DynamicPowerKindTargetToUserSpeedRatio:
		userSpeed, targetSpeed := effectiveSpeed(user), effectiveSpeed(target)
		power := uint64(rule.SpeedRatioMultiplier)*uint64(targetSpeed)/uint64(userSpeed) + uint64(rule.SpeedRatioAdditivePower)
		if power > uint64(rule.MaximumPower) {
			return rule.MaximumPower
		}
		return uint16(power)
	case DynamicPowerKindTargetWeightThresholds:
		targetWeight := effectiveBattleWeight(target)
		for _, threshold := range rule.WeightThresholds {
			if targetWeight <= threshold.MaximumWeightInclusive {
				return threshold.Power
			}
		}
		return rule.FallbackPower
	case DynamicPowerKindUserTargetWeightRatioThresholds:
		userWeight, targetWeight := effectiveBattleWeight(user), effectiveBattleWeight(target)
		for _, threshold := range rule.WeightRatioThresholds {
			if uint64(userWeight) >= uint64(targetWeight)*uint64(threshold.MinimumUserToTargetRatio) {
				return threshold.Power
			}
		}
		return rule.FallbackPower
	case DynamicPowerKindUserHPFractionThresholds:
		scaledHP := uint64(rule.HPFractionScale) * uint64(user.CurrentHP) / uint64(user.MaxHP)
		for _, threshold := range rule.HPFractionThresholds {
			if scaledHP <= uint64(threshold.MaximumScaledHPInclusive) {
				return threshold.Power
			}
		}
		return rule.FallbackPower
	default:
		return skill.Power
	}
}

// effectiveBattleWeight 返回成员参与体重规则时的有效体重。
// 减半体重道具只影响运行时读取，不会改写资料冻结的 Weight；奇数值向下取整但至少保留 1，避免比例规则除以零。
func effectiveBattleWeight(member MemberSnapshot) uint32 {
	if member.ItemID != 0 && member.HeldItemWeightHalf {
		if member.Weight <= 1 {
			return 1
		}
		return member.Weight / 2
	}
	return member.Weight
}

// cloneDynamicPowerRule 深复制阈值数组，避免 State 快照通过嵌套切片被调用方修改。
func cloneDynamicPowerRule(rule DynamicPowerRule) DynamicPowerRule {
	rule.SpeedThresholds = append([]SpeedPowerThreshold(nil), rule.SpeedThresholds...)
	rule.WeightThresholds = append([]WeightPowerThreshold(nil), rule.WeightThresholds...)
	rule.WeightRatioThresholds = append([]WeightRatioPowerThreshold(nil), rule.WeightRatioThresholds...)
	rule.HPFractionThresholds = append([]HPFractionPowerThreshold(nil), rule.HPFractionThresholds...)
	return rule
}
