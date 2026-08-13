package skilldetail

// validChargeSkippedWeathers 校验资料层声明的天气跳过蓄力集合。
//
// 此集合仅包含普通天气的封闭代码，且每种天气只能出现一次。它与蓄力易变状态共同组成规则：前者声明何时可以
// 跳过，后者声明技能本身是否存在两段行动，不能以技能名称或效果文本推断。
func validChargeSkippedWeathers(values []WeatherKind) bool {
	if len(values) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Valid() {
			return false
		}
		if _, duplicate := seen[value]; duplicate {
			return false
		}
		seen[value] = struct{}{}
	}
	return true
}

// chargeSkippedWeathersRequireCharging 报告跳过蓄力天气是否只附着在声明蓄力控制状态的技能上。
//
// 空集合始终有效；非空集合必须存在一项 charging 易变状态，避免管理端为普通技能写入永远不会执行的环境规则。
func chargeSkippedWeathersRequireCharging(weathers []WeatherKind, effects []VolatileEffect) bool {
	if len(weathers) == 0 {
		return true
	}
	for _, effect := range effects {
		if effect.Status == VolatileStatusCharging {
			return true
		}
	}
	return false
}

// cloneChargeSkippedWeathers 复制天气切片，隔离管理命令、审计快照和 Battle 编译边界持有的底层数组。
func cloneChargeSkippedWeathers(values []WeatherKind) []WeatherKind {
	if values == nil {
		return []WeatherKind{}
	}
	return append([]WeatherKind(nil), values...)
}
