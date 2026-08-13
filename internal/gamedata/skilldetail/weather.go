package skilldetail

// WeatherKind 是技能详情能够建立的普通全场天气的封闭资料代码。
//
// 天气与场地速度顺序、成员易变状态、侧状态分别持久化。资料层只声明建立何种普通天气以及持续回合和概率；
// 火、水伤害修正与沙暴回合末伤害由纯战斗引擎根据已冻结的快照解释，不能从技能名称或说明文本推断。
type WeatherKind string

const (
	// WeatherKindSun 表示日照：强化火属性技能并削弱水属性技能。
	WeatherKindSun WeatherKind = "sun"
	// WeatherKindRain 表示降雨：强化水属性技能并削弱火属性技能。
	WeatherKindRain WeatherKind = "rain"
	// WeatherKindSandstorm 表示沙暴：会在回合末伤害不具岩石、地面或钢属性的场上成员。
	WeatherKindSandstorm WeatherKind = "sandstorm"
	// WeatherKindSnow 表示降雪。雪的防御加成属于引擎后续独立能力，不能在资料层伪装为已经实现。
	WeatherKindSnow WeatherKind = "snow"
)

// Valid 报告天气种类是否能够由当前资料服务和纯战斗引擎共同解释。
func (kind WeatherKind) Valid() bool {
	return kind == WeatherKindSun || kind == WeatherKindRain || kind == WeatherKindSandstorm || kind == WeatherKindSnow
}

// Weather 描述技能成功后尝试建立普通全场天气的完整资料。
//
// 再次成功使用同一种普通天气不会刷新持续回合，而是产生明确的技能失败事件；不同天气会覆盖当前天气。持续回合
// 和触发概率都是资料事实，不能与戏法空间或未来地形共用泛型效果 JSON。
type Weather struct {
	// Kind 是技能尝试建立的封闭普通天气种类。
	Kind WeatherKind `json:"kind"`
	// TurnsRemaining 是天气建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是天气建立的独立触发概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validWeather 校验可选天气资料的封闭种类和数值边界。
func validWeather(value *Weather) bool {
	return value == nil || value.Kind.Valid() && value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneWeather 复制可选天气资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneWeather(value *Weather) *Weather {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
