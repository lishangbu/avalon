package skilldetail

import "math"

// DynamicPowerKind 是公式伤害在结算前重新计算基础威力的封闭资料种类。
//
// 动态威力不属于伤害倍率：它在攻击、防御、属性一致加成、要害和属性相性之前，替换本次普通伤害公式的
// 基础威力。资料层使用独立稳定代码和强类型参数，禁止通过技能名称或展示文本猜测行为。
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

// DynamicPowerSource 是正向能力阶级动态威力相对于技能使用者的取值对象。
type DynamicPowerSource string

const (
	// DynamicPowerSourceUser 表示累加技能使用者的正向能力阶级。
	DynamicPowerSourceUser DynamicPowerSource = "user"
	// DynamicPowerSourceSelectedTarget 表示累加本次实际被命中的目标成员的正向能力阶级。
	DynamicPowerSourceSelectedTarget DynamicPowerSource = "selectedTarget"
)

// SpeedPowerThreshold 是按有效速度整数倍数选择动态基础威力的一档。
type SpeedPowerThreshold struct {
	// MinimumRatio 是分子速度至少达到分母速度多少倍才命中本档；必须是正整数。
	MinimumRatio int32 `json:"minimumRatio"`
	// Power 是命中本档时进入普通伤害公式的正基础威力。
	Power int32 `json:"power"`
}

// WeightPowerThreshold 是按目标当前体重选择动态基础威力的一档。
type WeightPowerThreshold struct {
	// MaximumWeightInclusive 是使用资料体重整数刻度表达的闭区间上界。
	MaximumWeightInclusive int32 `json:"maximumWeightInclusive"`
	// Power 是目标体重不超过本档上界时进入普通伤害公式的正基础威力。
	Power int32 `json:"power"`
}

// WeightRatioPowerThreshold 是按使用者与目标当前体重比例选择动态基础威力的一档。
type WeightRatioPowerThreshold struct {
	// MinimumUserToTargetRatio 是使用者体重至少为目标体重多少倍才命中本档；必须是正整数。
	MinimumUserToTargetRatio int32 `json:"minimumUserToTargetRatio"`
	// Power 是命中本档时进入普通伤害公式的正基础威力。
	Power int32 `json:"power"`
}

// HPFractionPowerThreshold 是按使用者当前生命比例选择动态基础威力的一档。
type HPFractionPowerThreshold struct {
	// MaximumScaledHPInclusive 是 floor(scale * currentHP / maxHP) 的闭区间上界。
	MaximumScaledHPInclusive int32 `json:"maximumScaledHpInclusive"`
	// Power 是当前缩放生命值不超过本档上界时进入普通伤害公式的正基础威力。
	Power int32 `json:"power"`
}

// DynamicPower 保存一项可冻结到对战快照的动态基础威力资料。
//
// Kind 决定哪些字段可以出现。速度、体重和生命比例阈值使用彼此独立的强类型数组，不能压缩为无语义的
// 通用 JSON 数组，因为它们的单位、比较方向和边界含义不同。
type DynamicPower struct {
	// Kind 是规则的封闭种类；空值表示不启用动态威力并读取技能静态威力。
	Kind DynamicPowerKind `json:"kind,omitempty"`
	// Source 仅供 positiveStatStageSum 使用，表示累加使用者或当前实际目标的正向能力阶级。
	Source DynamicPowerSource `json:"source,omitempty"`
	// BasePower 仅供 positiveStatStageSum 使用，是没有任何正向能力阶级时的正基础威力。
	BasePower int32 `json:"basePower,omitempty"`
	// PowerPerPositiveStage 仅供 positiveStatStageSum 使用，是每一级正向能力阶级增加的正威力。
	PowerPerPositiveStage int32 `json:"powerPerPositiveStage,omitempty"`
	// MaximumPower 供 positiveStatStageSum 和 targetToUserSpeedRatio 使用；前者的 0 表示不设额外上限。
	MaximumPower int32 `json:"maximumPower,omitempty"`
	// SpeedThresholds 仅供 userSpeedRatioThresholds 使用，必须按 MinimumRatio 从大到小严格排列。
	SpeedThresholds []SpeedPowerThreshold `json:"speedThresholds,omitempty"`
	// FallbackPower 供三类阈值规则使用，是没有任何阈值命中时采用的正基础威力。
	FallbackPower int32 `json:"fallbackPower,omitempty"`
	// SpeedRatioMultiplier 仅供 targetToUserSpeedRatio 使用，是速度比例公式中的正整数倍率。
	SpeedRatioMultiplier int32 `json:"speedRatioMultiplier,omitempty"`
	// SpeedRatioAdditivePower 仅供 targetToUserSpeedRatio 使用，是速度比例项之后相加的非负威力。
	SpeedRatioAdditivePower int32 `json:"speedRatioAdditivePower,omitempty"`
	// WeightThresholds 仅供 targetWeightThresholds 使用，必须按 MaximumWeightInclusive 从小到大严格排列。
	WeightThresholds []WeightPowerThreshold `json:"weightThresholds,omitempty"`
	// WeightRatioThresholds 仅供 userTargetWeightRatioThresholds 使用，必须按 MinimumUserToTargetRatio 从大到小严格排列。
	WeightRatioThresholds []WeightRatioPowerThreshold `json:"weightRatioThresholds,omitempty"`
	// HPFractionScale 仅供 userHPFractionThresholds 使用，是离散生命比例计算中的正缩放常量。
	HPFractionScale int32 `json:"hpFractionScale,omitempty"`
	// HPFractionThresholds 仅供 userHPFractionThresholds 使用，必须按 MaximumScaledHPInclusive 从小到大严格排列。
	HPFractionThresholds []HPFractionPowerThreshold `json:"hpFractionThresholds,omitempty"`
}

// Active 报告该资料是否声明了一种可执行的动态基础威力规则。
func (value DynamicPower) Active() bool {
	return value.Kind != ""
}

// Valid 报告动态基础威力资料是否是当前服务支持的一条完整规则。
//
// 此方法也由 Battle 资料编译边界调用，用于拒绝绕过管理服务直接写入数据库的未知字段、无效阈值或错误模式组合。
func (value DynamicPower) Valid() bool {
	return validDynamicPower(value)
}

func validDynamicPower(value DynamicPower) bool {
	if !value.Active() {
		return value.Source == "" && value.BasePower == 0 && value.PowerPerPositiveStage == 0 && value.MaximumPower == 0 &&
			value.FallbackPower == 0 && value.SpeedRatioMultiplier == 0 && value.SpeedRatioAdditivePower == 0 &&
			value.HPFractionScale == 0 && len(value.SpeedThresholds) == 0 && len(value.WeightThresholds) == 0 &&
			len(value.WeightRatioThresholds) == 0 && len(value.HPFractionThresholds) == 0
	}
	noThresholds := len(value.SpeedThresholds) == 0 && len(value.WeightThresholds) == 0 &&
		len(value.WeightRatioThresholds) == 0 && len(value.HPFractionThresholds) == 0
	switch value.Kind {
	case DynamicPowerKindPositiveStatStageSum:
		return (value.Source == DynamicPowerSourceUser || value.Source == DynamicPowerSourceSelectedTarget) &&
			positivePower(value.BasePower) && positivePower(value.PowerPerPositiveStage) &&
			(value.MaximumPower == 0 || value.MaximumPower >= value.BasePower && validPower(value.MaximumPower)) &&
			(value.MaximumPower != 0 || int64(value.BasePower)+int64(value.PowerPerPositiveStage)*42 <= math.MaxUint16) &&
			value.FallbackPower == 0 && value.SpeedRatioMultiplier == 0 && value.SpeedRatioAdditivePower == 0 &&
			value.HPFractionScale == 0 && noThresholds
	case DynamicPowerKindUserSpeedRatioThresholds:
		return value.Source == "" && value.BasePower == 0 && value.PowerPerPositiveStage == 0 && value.MaximumPower == 0 &&
			positivePower(value.FallbackPower) && value.SpeedRatioMultiplier == 0 && value.SpeedRatioAdditivePower == 0 &&
			value.HPFractionScale == 0 && validSpeedThresholds(value.SpeedThresholds) && len(value.WeightThresholds) == 0 &&
			len(value.WeightRatioThresholds) == 0 && len(value.HPFractionThresholds) == 0
	case DynamicPowerKindTargetToUserSpeedRatio:
		return value.Source == "" && value.BasePower == 0 && value.PowerPerPositiveStage == 0 &&
			positivePower(value.MaximumPower) && value.FallbackPower == 0 && positivePower(value.SpeedRatioMultiplier) &&
			validNonNegativePower(value.SpeedRatioAdditivePower) && value.HPFractionScale == 0 && noThresholds
	case DynamicPowerKindTargetWeightThresholds:
		return value.Source == "" && value.BasePower == 0 && value.PowerPerPositiveStage == 0 && value.MaximumPower == 0 &&
			positivePower(value.FallbackPower) && value.SpeedRatioMultiplier == 0 && value.SpeedRatioAdditivePower == 0 &&
			value.HPFractionScale == 0 && len(value.SpeedThresholds) == 0 && validWeightThresholds(value.WeightThresholds) &&
			len(value.WeightRatioThresholds) == 0 && len(value.HPFractionThresholds) == 0
	case DynamicPowerKindUserTargetWeightRatioThresholds:
		return value.Source == "" && value.BasePower == 0 && value.PowerPerPositiveStage == 0 && value.MaximumPower == 0 &&
			positivePower(value.FallbackPower) && value.SpeedRatioMultiplier == 0 && value.SpeedRatioAdditivePower == 0 &&
			value.HPFractionScale == 0 && len(value.SpeedThresholds) == 0 && len(value.WeightThresholds) == 0 &&
			validWeightRatioThresholds(value.WeightRatioThresholds) && len(value.HPFractionThresholds) == 0
	case DynamicPowerKindUserHPFractionThresholds:
		return value.Source == "" && value.BasePower == 0 && value.PowerPerPositiveStage == 0 && value.MaximumPower == 0 &&
			positivePower(value.FallbackPower) && value.SpeedRatioMultiplier == 0 && value.SpeedRatioAdditivePower == 0 &&
			positivePower(value.HPFractionScale) && len(value.SpeedThresholds) == 0 && len(value.WeightThresholds) == 0 &&
			len(value.WeightRatioThresholds) == 0 && validHPFractionThresholds(value.HPFractionThresholds)
	default:
		return false
	}
}

func validSpeedThresholds(values []SpeedPowerThreshold) bool {
	if len(values) == 0 {
		return false
	}
	previous := int32(math.MaxInt32)
	for _, value := range values {
		if !positivePower(value.MinimumRatio) || !positivePower(value.Power) || value.MinimumRatio >= previous {
			return false
		}
		previous = value.MinimumRatio
	}
	return true
}

func validWeightThresholds(values []WeightPowerThreshold) bool {
	if len(values) == 0 {
		return false
	}
	var previous int32
	for index, value := range values {
		if value.MaximumWeightInclusive <= 0 || !positivePower(value.Power) || index > 0 && value.MaximumWeightInclusive <= previous {
			return false
		}
		previous = value.MaximumWeightInclusive
	}
	return true
}

func validWeightRatioThresholds(values []WeightRatioPowerThreshold) bool {
	if len(values) == 0 {
		return false
	}
	previous := int32(math.MaxInt32)
	for _, value := range values {
		if !positivePower(value.MinimumUserToTargetRatio) || !positivePower(value.Power) || value.MinimumUserToTargetRatio >= previous {
			return false
		}
		previous = value.MinimumUserToTargetRatio
	}
	return true
}

func validHPFractionThresholds(values []HPFractionPowerThreshold) bool {
	if len(values) == 0 {
		return false
	}
	var previous int32 = -1
	for _, value := range values {
		if value.MaximumScaledHPInclusive < 0 || !positivePower(value.Power) || value.MaximumScaledHPInclusive <= previous {
			return false
		}
		previous = value.MaximumScaledHPInclusive
	}
	return true
}

func positivePower(value int32) bool {
	return value >= 1 && value <= math.MaxUint16
}

func validPower(value int32) bool {
	return value >= 0 && value <= math.MaxUint16
}

func validNonNegativePower(value int32) bool {
	return validPower(value)
}

func cloneDynamicPower(value DynamicPower) DynamicPower {
	value.SpeedThresholds = append([]SpeedPowerThreshold(nil), value.SpeedThresholds...)
	value.WeightThresholds = append([]WeightPowerThreshold(nil), value.WeightThresholds...)
	value.WeightRatioThresholds = append([]WeightRatioPowerThreshold(nil), value.WeightRatioThresholds...)
	value.HPFractionThresholds = append([]HPFractionPowerThreshold(nil), value.HPFractionThresholds...)
	return value
}
