package battleengine

import "fmt"

// EnvironmentSnapshot 是战斗中不属于任一阵营或成员的全场运行态。
//
// 环境与成员状态一样属于权威 State：它会进入 Turn Record 和离线重放快照。当前只承载全场速度顺序效果；
// 天气、场地、屏障和侧状态将在各自的强类型字段中陆续加入，不能被压缩为无约束的泛型效果列表。
type EnvironmentSnapshot struct {
	// StrongWeather 是可选的全场强天气；空值表示当前没有强天气。强天气独立于普通天气：它没有持续回合，
	// 仅能被其它强天气覆盖，并由最后一个持有来源离场或倒下时结束。
	StrongWeather *StrongWeatherState `json:"strongWeather,omitempty"`
	// Terrain 是可选的普通全场场地；空值表示当前没有场地。场地独立于天气、速度顺序和未来侧状态，
	// 会在持续时间耗尽后自然清除；接地判定、伤害修正、异常阻止和回合末回复由各自明确的结算阶段读取。
	Terrain *TerrainEffect `json:"terrain,omitempty"`
	// Weather 是可选的普通全场天气；空值表示当前没有天气。天气独立于场地、侧状态和成员易变状态，
	// 会影响伤害与回合末结算，并在持续时间耗尽后自然清除。
	Weather *WeatherEffect `json:"weather,omitempty"`
	// FieldSpeedOrder 是改变同一行动优先度内速度比较方向的可选全场效果；空值表示按通常高速优先排序。
	FieldSpeedOrder *FieldSpeedOrderEffect `json:"fieldSpeedOrder,omitempty"`
}

// FieldSpeedOrderKind 是引擎支持的全场速度顺序效果种类。
//
// 该枚举不表示成员的速度数值修正。它仅定义同一优先度的行动计划应以正常还是反向方向比较已经计算好的
// 有效速度，因此不会与能力阶级、主要异常及未来道具或天气的速度修正混淆。
type FieldSpeedOrderKind string

const (
	// FieldSpeedOrderKindTrickRoom 表示戏法空间：同一行动优先度内有效速度较低的成员先行动。
	FieldSpeedOrderKindTrickRoom FieldSpeedOrderKind = "trickRoom"
)

// reversesSpeedOrder 报告该效果是否反转同优先度的有效速度比较方向。
func (kind FieldSpeedOrderKind) reversesSpeedOrder() bool {
	return kind == FieldSpeedOrderKindTrickRoom
}

// valid 报告全场速度顺序效果是否为当前纯战斗引擎的封闭支持值。
func (kind FieldSpeedOrderKind) valid() bool {
	return kind == FieldSpeedOrderKindTrickRoom
}

// FieldSpeedOrderEffect 是已经写入战斗环境、会跨回合生效的全场速度顺序规则。
type FieldSpeedOrderEffect struct {
	// Kind 是决定同一优先度内行动速度比较方向的封闭效果种类。
	Kind FieldSpeedOrderKind `json:"kind"`
	// TurnsRemaining 是包含当前结算回合在内的剩余完整回合数，必须为正数。
	//
	// 技能在本回合建立该效果后，回合末会立刻递减一次；因此标准五回合戏法空间在建立事件中记录 5，
	// 本回合结束后的状态记录 4。这个口径与天气和未来场地效果保持一致。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// advanceTurn 推进一个完整回合后的效果状态。
//
// 返回 nil 表示持续回合已经耗尽，调用方必须恢复普通速度排序并写入结束事件。
func (effect FieldSpeedOrderEffect) advanceTurn() *FieldSpeedOrderEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// FieldSpeedOrderApplication 描述某个技能命中后尝试建立的全场速度顺序效果。
//
// application 与 Effect 分离后，资料可以清晰表达“这次技能如何触发”与“建立后如何影响后续行动排序”两类
// 语义。再次成功使用与现有效果同 kind 的 application 会解除该效果，不会刷新持续回合。
type FieldSpeedOrderApplication struct {
	// Effect 是成功建立时写入 EnvironmentSnapshot 的完整持续效果。
	Effect FieldSpeedOrderEffect `json:"effect"`
	// ChancePercent 是本项全场效果的独立触发概率；100 表示必定且不会消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// validateEnvironment 校验初始或重放快照中全场环境的已知字段。
func validateEnvironment(environment EnvironmentSnapshot) error {
	if environment.StrongWeather != nil {
		if environment.Weather != nil {
			return fmt.Errorf("%w: 强天气不能与普通天气同时存在", ErrInvalidInitialState)
		}
		if err := validateStrongWeatherState(*environment.StrongWeather); err != nil {
			return fmt.Errorf("%w: %v", ErrInvalidInitialState, err)
		}
	}
	if environment.Terrain != nil {
		if err := validateTerrainEffect(*environment.Terrain); err != nil {
			return err
		}
	}
	if environment.Weather != nil {
		if err := validateWeatherEffect(*environment.Weather); err != nil {
			return err
		}
	}
	if environment.FieldSpeedOrder != nil {
		return validateFieldSpeedOrderEffect(*environment.FieldSpeedOrder)
	}
	return nil
}

// validateFieldSpeedOrderEffect 校验一个已经生效或即将生效的全场速度顺序效果。
func validateFieldSpeedOrderEffect(effect FieldSpeedOrderEffect) error {
	if !effect.Kind.valid() || effect.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 全场速度顺序效果无效", ErrInvalidInitialState)
	}
	return nil
}

// validateFieldSpeedOrderApplication 校验资料编译后冻结到技能快照的效果建立规则。
func validateFieldSpeedOrderApplication(application FieldSpeedOrderApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 全场速度顺序效果触发概率无效", ErrInvalidInitialState)
	}
	return validateFieldSpeedOrderEffect(application.Effect)
}

// cloneEnvironment 深复制环境中未来可能继续扩展的指针与集合字段。
func cloneEnvironment(environment EnvironmentSnapshot) EnvironmentSnapshot {
	if environment.StrongWeather != nil {
		effect := *environment.StrongWeather
		environment.StrongWeather = &effect
	}
	if environment.Terrain != nil {
		effect := *environment.Terrain
		environment.Terrain = &effect
	}
	if environment.Weather != nil {
		effect := *environment.Weather
		environment.Weather = &effect
	}
	if environment.FieldSpeedOrder != nil {
		effect := *environment.FieldSpeedOrder
		environment.FieldSpeedOrder = &effect
	}
	return environment
}
