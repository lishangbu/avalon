package api

import (
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
)

func createSkillDetailValues(body *domainv1.GameSkillOnUseRules) (skilldetail.OptionalValues, error) {
	ailmentID, err := optionalGameDataIdentifier(body.GetAilmentId(), "INVALID_SKILL_AILMENT_ID")
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	categoryID, err := optionalGameDataIdentifier(body.GetCategoryId(), "INVALID_SKILL_CATEGORY_ID")
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	targetID, err := optionalGameDataIdentifier(body.GetTargetId(), "INVALID_SKILL_TARGET_ID")
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	damageMode := skilldetail.DamageMode(body.GetDamageMode())
	if damageMode == "" {
		damageMode = skilldetail.DamageModeFormula
	}
	volatileEffects, err := volatileEffectsFromMessages(body.GetVolatileEffects())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	dynamicPower, err := dynamicPowerFromMessage(body.GetDynamicPower())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	fieldSpeedOrder, err := fieldSpeedOrderFromMessage(body.GetFieldSpeedOrder())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	leechSeed, err := leechSeedFromMessage(body.GetLeechSeed())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	weather, err := weatherFromMessage(body.GetWeather())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	weatherAccuracyOverrides, err := weatherAccuracyOverridesFromMessages(body.GetWeatherAccuracyOverrides())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	weatherElementOverrides, err := weatherElementOverridesFromMessages(body.GetWeatherElementOverrides())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	weatherPowerMultipliers, err := weatherPowerMultipliersFromMessages(body.GetWeatherPowerMultipliers())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	chargeSkippedWeathers, err := chargeSkippedWeathersFromMessages(body.GetChargeSkippedWeathers())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	terrain, err := terrainFromMessage(body.GetTerrain())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	tailwind, err := tailwindFromMessage(body.GetTailwind())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	reflect, err := reflectFromMessage(body.GetReflect())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	lightScreen, err := lightScreenFromMessage(body.GetLightScreen())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	auroraVeil, err := auroraVeilFromMessage(body.GetAuroraVeil())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	spikes, err := spikesFromMessage(body.GetSpikes())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	stealthRock, err := stealthRockFromMessage(body.GetStealthRock())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	toxicSpikes, err := toxicSpikesFromMessage(body.GetToxicSpikes())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	stickyWeb, err := stickyWebFromMessage(body.GetStickyWeb())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	rapidSpin, err := rapidSpinFromMessage(body.GetRapidSpin())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	defog, err := defogFromMessage(body.GetDefog())
	if err != nil {
		return skilldetail.OptionalValues{}, err
	}
	return skilldetail.OptionalValues{
		AilmentID: ailmentID, CategoryID: categoryID, TargetID: targetID,
		MinHits: optionalInt32(body.GetMinHits()), MaxHits: optionalInt32(body.GetMaxHits()),
		MinTurns: optionalInt32(body.GetMinTurns()), MaxTurns: optionalInt32(body.GetMaxTurns()),
		Drain: optionalInt32(body.GetDrain()), Healing: optionalInt32(body.GetHealing()),
		TargetHealingNumerator:   optionalInt32(body.GetTargetHealingNumerator()),
		TargetHealingDenominator: optionalInt32(body.GetTargetHealingDenominator()),
		CritRate:                 optionalInt32(body.GetCritRate()),
		DamageMode:               damageMode, DamageAmount: optionalInt32(body.GetDamageAmount()),
		DamageNumerator: optionalInt32(body.GetDamageNumerator()), DamageDenominator: optionalInt32(body.GetDamageDenominator()),
		MinimumDamage: optionalInt32(body.GetMinimumDamage()), AilmentChance: optionalInt32(body.GetAilmentChance()),
		OneHitKnockOutBaseAccuracy:                        optionalInt32(body.GetOneHitKnockOutBaseAccuracy()),
		OneHitKnockOutSameElementUserBaseAccuracy:         optionalInt32(body.GetOneHitKnockOutSameElementUserBaseAccuracy()),
		OneHitKnockOutBlocksSameElementTarget:             body.GetOneHitKnockOutBlocksSameElementTarget(),
		ReceivedDamageNumerator:                           optionalInt32(body.GetReceivedDamageNumerator()),
		ReceivedDamageDenominator:                         optionalInt32(body.GetReceivedDamageDenominator()),
		ReceivedDamageAcceptsPhysical:                     body.GetReceivedDamageAcceptsPhysical(),
		ReceivedDamageAcceptsSpecial:                      body.GetReceivedDamageAcceptsSpecial(),
		ReceivedDamageIgnoreNonImmuneElementEffectiveness: body.GetReceivedDamageIgnoreNonImmuneElementEffectiveness(),
		WeakenedByGrassyTerrain:                           body.GetWeakenedByGrassyTerrain(),
		WeatherAccuracyOverrides:                          weatherAccuracyOverrides,
		WeatherElementOverrides:                           weatherElementOverrides,
		WeatherPowerMultipliers:                           weatherPowerMultipliers,
		ChargeSkippedWeathers:                             chargeSkippedWeathers,
		DynamicPower:                                      dynamicPower,
		FieldSpeedOrder:                                   fieldSpeedOrder,
		LeechSeed:                                         leechSeed,
		Weather:                                           weather,
		Terrain:                                           terrain,
		Tailwind:                                          tailwind,
		Reflect:                                           reflect,
		LightScreen:                                       lightScreen,
		AuroraVeil:                                        auroraVeil,
		Spikes:                                            spikes,
		StealthRock:                                       stealthRock,
		ToxicSpikes:                                       toxicSpikes,
		StickyWeb:                                         stickyWeb,
		RapidSpin:                                         rapidSpin,
		Defog:                                             defog,
		ForceTargetSwitch:                                 body.GetForceTargetSwitch(),
		RechargesAfterUse:                                 body.GetRechargesAfterUse(),
		LocksAccuracyOnTarget:                             body.GetLocksAccuracyOnTarget(),
		MakesContact:                                      body.GetMakesContact(),
		PunchBased:                                        body.GetPunchBased(),
		SlicingBased:                                      body.GetSlicingBased(),
		SoundBased:                                        body.GetSoundBased(),
		PulseBased:                                        body.GetPulseBased(),
		BiteBased:                                         body.GetBiteBased(),
		PowderBased:                                       body.GetPowderBased(),
		VolatileEffects:                                   volatileEffects,
		CuresUserSideMajorStatuses:                        body.GetCuresUserSideMajorStatuses(),
		CuresUserMajorStatus:                              body.GetCuresUserMajorStatus(),
		CuresUserSideActiveMajorStatuses:                  body.GetCuresUserSideActiveMajorStatuses(),
		FlinchChance:                                      optionalInt32(body.GetFlinchChance()), StatChance: optionalInt32(body.GetStatChance()),
	}, nil
}

func volatileEffectsFromMessages(values []*domainv1.GameSkillVolatileEffect) ([]skilldetail.VolatileEffect, error) {
	result := make([]skilldetail.VolatileEffect, 0, len(values))
	for _, value := range values {
		if value == nil {
			return nil, kratoserrors.BadRequest("INVALID_VOLATILE_EFFECTS", "易变状态效果不能为空")
		}
		status, statusOK := skillDetailVolatileStatus(value.GetStatus())
		target, targetOK := skillDetailVolatileTarget(value.GetTarget())
		if !statusOK || !targetOK {
			return nil, kratoserrors.BadRequest("INVALID_VOLATILE_EFFECTS", "易变状态类别或目标无效")
		}
		result = append(result, skilldetail.VolatileEffect{
			Status: status, Target: target, ChancePercent: value.GetChancePercent(),
			MinTurns: value.GetMinTurns(), MaxTurns: value.GetMaxTurns(),
			SubstituteCostNumerator:   value.GetSubstituteCostNumerator(),
			SubstituteCostDenominator: value.GetSubstituteCostDenominator(),
		})
	}
	return result, nil
}

// dynamicPowerFromMessage 将 Protobuf 的封闭动态威力联合类型转换为资料领域模型。
//
// 传输契约按规则种类拆分消息，领域层也保留各类阈值的独立类型；这里绝不接受任意 JSON、技能名称或自由文本。
func dynamicPowerFromMessage(value *domainv1.GameSkillDynamicPower) (skilldetail.DynamicPower, error) {
	if value == nil || value.GetRule() == nil {
		return skilldetail.DynamicPower{}, nil
	}
	var result skilldetail.DynamicPower
	switch rule := value.GetRule().(type) {
	case *domainv1.GameSkillDynamicPower_PositiveStatStageSum:
		if rule.PositiveStatStageSum == nil {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		source, found := dynamicPowerSourceFromMessage(rule.PositiveStatStageSum.GetSource())
		if !found {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		result = skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindPositiveStatStageSum, Source: source,
			BasePower: rule.PositiveStatStageSum.GetBasePower(), PowerPerPositiveStage: rule.PositiveStatStageSum.GetPowerPerPositiveStage(),
			MaximumPower: rule.PositiveStatStageSum.GetMaximumPower(),
		}
	case *domainv1.GameSkillDynamicPower_UserSpeedRatioThresholds:
		if rule.UserSpeedRatioThresholds == nil {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		thresholds, err := speedPowerThresholdsFromMessages(rule.UserSpeedRatioThresholds.GetThresholds())
		if err != nil {
			return skilldetail.DynamicPower{}, err
		}
		result = skilldetail.DynamicPower{Kind: skilldetail.DynamicPowerKindUserSpeedRatioThresholds,
			SpeedThresholds: thresholds, FallbackPower: rule.UserSpeedRatioThresholds.GetFallbackPower()}
	case *domainv1.GameSkillDynamicPower_TargetToUserSpeedRatio:
		if rule.TargetToUserSpeedRatio == nil {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		result = skilldetail.DynamicPower{Kind: skilldetail.DynamicPowerKindTargetToUserSpeedRatio,
			SpeedRatioMultiplier: rule.TargetToUserSpeedRatio.GetMultiplier(), SpeedRatioAdditivePower: rule.TargetToUserSpeedRatio.GetAdditivePower(),
			MaximumPower: rule.TargetToUserSpeedRatio.GetMaximumPower()}
	case *domainv1.GameSkillDynamicPower_TargetWeightThresholds:
		if rule.TargetWeightThresholds == nil {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		thresholds, err := weightPowerThresholdsFromMessages(rule.TargetWeightThresholds.GetThresholds())
		if err != nil {
			return skilldetail.DynamicPower{}, err
		}
		result = skilldetail.DynamicPower{Kind: skilldetail.DynamicPowerKindTargetWeightThresholds,
			WeightThresholds: thresholds, FallbackPower: rule.TargetWeightThresholds.GetFallbackPower()}
	case *domainv1.GameSkillDynamicPower_UserTargetWeightRatioThresholds:
		if rule.UserTargetWeightRatioThresholds == nil {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		thresholds, err := weightRatioPowerThresholdsFromMessages(rule.UserTargetWeightRatioThresholds.GetThresholds())
		if err != nil {
			return skilldetail.DynamicPower{}, err
		}
		result = skilldetail.DynamicPower{Kind: skilldetail.DynamicPowerKindUserTargetWeightRatioThresholds,
			WeightRatioThresholds: thresholds, FallbackPower: rule.UserTargetWeightRatioThresholds.GetFallbackPower()}
	case *domainv1.GameSkillDynamicPower_UserHpFractionThresholds:
		if rule.UserHpFractionThresholds == nil {
			return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
		}
		thresholds, err := hpFractionPowerThresholdsFromMessages(rule.UserHpFractionThresholds.GetThresholds())
		if err != nil {
			return skilldetail.DynamicPower{}, err
		}
		result = skilldetail.DynamicPower{Kind: skilldetail.DynamicPowerKindUserHPFractionThresholds,
			HPFractionScale: rule.UserHpFractionThresholds.GetScale(), HPFractionThresholds: thresholds,
			FallbackPower: rule.UserHpFractionThresholds.GetFallbackPower()}
	default:
		return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
	}
	if !result.Valid() {
		return skilldetail.DynamicPower{}, invalidDynamicPowerMessage()
	}
	return result, nil
}

// invalidDynamicPowerMessage 创建统一的 HTTP 参数错误，避免暴露内部领域校验细节。
func invalidDynamicPowerMessage() error {
	return kratoserrors.BadRequest("INVALID_DYNAMIC_POWER", "动态基础威力规则无效")
}

// fieldSpeedOrderFromMessage 将外部 Protobuf 的全场速度顺序资料映射为领域强类型模型。该边界明确拒绝未知
// 枚举与非法数值，避免把未识别 JSON、技能名称或前端展示文本带入实时战斗资料。
func fieldSpeedOrderFromMessage(value *domainv1.GameSkillFieldSpeedOrder) (*skilldetail.FieldSpeedOrder, error) {
	if value == nil {
		return nil, nil
	}
	var kind skilldetail.FieldSpeedOrderKind
	switch value.GetKind() {
	case domainv1.GameSkillFieldSpeedOrderKind_GAME_SKILL_FIELD_SPEED_ORDER_KIND_TRICK_ROOM:
		kind = skilldetail.FieldSpeedOrderKindTrickRoom
	default:
		return nil, kratoserrors.BadRequest("INVALID_FIELD_SPEED_ORDER", "全场速度顺序效果无效")
	}
	result := &skilldetail.FieldSpeedOrder{
		Kind: kind, TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent(),
	}
	if result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_FIELD_SPEED_ORDER", "全场速度顺序效果无效")
	}
	return result, nil
}

// leechSeedFromMessage 将 Protobuf 寄生种子消息映射为领域强类型规则，并在 HTTP 边界拒绝无效概率。
func leechSeedFromMessage(value *domainv1.GameSkillLeechSeed) (*skilldetail.LeechSeed, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.LeechSeed{ChancePercent: value.GetChancePercent()}
	if result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_LEECH_SEED", "寄生种子触发概率无效")
	}
	return result, nil
}

// weatherFromMessage 将 Protobuf 普通天气消息映射为领域强类型规则，并在 HTTP 边界拒绝未知枚举或非法数值。
func weatherFromMessage(value *domainv1.GameSkillWeather) (*skilldetail.Weather, error) {
	if value == nil {
		return nil, nil
	}
	var kind skilldetail.WeatherKind
	switch value.GetKind() {
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
		kind = skilldetail.WeatherKindSun
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
		kind = skilldetail.WeatherKindRain
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
		kind = skilldetail.WeatherKindSandstorm
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
		kind = skilldetail.WeatherKindSnow
	default:
		return nil, kratoserrors.BadRequest("INVALID_WEATHER", "普通天气种类无效")
	}
	result := &skilldetail.Weather{Kind: kind, TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent()}
	if !result.Kind.Valid() || result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_WEATHER", "普通天气规则无效")
	}
	return result, nil
}

// weatherAccuracyOverridesFromMessages 将管理 API 的天气命中覆盖数组转换为资料层强类型规则。
//
// 0 是明确的必中覆盖，必须原样保留。重复天气、未指定天气和越界命中率会在 HTTP 边界以稳定的
// BadRequest 拒绝，避免管理端把互相竞争的规则写进实时资料。
func weatherAccuracyOverridesFromMessages(values []*domainv1.GameSkillWeatherAccuracyOverride) ([]skilldetail.WeatherAccuracyOverride, error) {
	if len(values) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_WEATHER_ACCURACY_OVERRIDES", "天气命中覆盖数量超过上限")
	}
	result := make([]skilldetail.WeatherAccuracyOverride, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if value == nil {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ACCURACY_OVERRIDES", "天气命中覆盖不能为空")
		}
		var weather skilldetail.WeatherKind
		switch value.GetWeather() {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = skilldetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = skilldetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = skilldetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = skilldetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ACCURACY_OVERRIDES", "天气命中覆盖天气无效")
		}
		if value.GetAccuracyPercent() < 0 || value.GetAccuracyPercent() > 100 {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ACCURACY_OVERRIDES", "天气命中覆盖命中率无效")
		}
		if _, duplicate := seen[weather]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ACCURACY_OVERRIDES", "天气命中覆盖天气重复")
		}
		seen[weather] = struct{}{}
		result = append(result, skilldetail.WeatherAccuracyOverride{
			Weather: weather, AccuracyPercent: value.GetAccuracyPercent(),
		})
	}
	return result, nil
}

// weatherElementOverridesFromMessages 将管理 API 的天气属性覆盖数组转换为资料层强类型规则。
//
// 每条规则都必须声明当前二进制支持的普通天气以及一个规范 Identifier；重复天气、空消息和未知天气会在 HTTP
// 边界被拒绝，避免把相互竞争的有效属性写进实时资料。目标属性是否启用由 Battle 编译器在启动对局时复核。
func weatherElementOverridesFromMessages(values []*domainv1.GameSkillWeatherElementOverride) ([]skilldetail.WeatherElementOverride, error) {
	if len(values) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_WEATHER_ELEMENT_OVERRIDES", "天气属性覆盖数量超过上限")
	}
	result := make([]skilldetail.WeatherElementOverride, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if value == nil {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ELEMENT_OVERRIDES", "天气属性覆盖不能为空")
		}
		var weather skilldetail.WeatherKind
		switch value.GetWeather() {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = skilldetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = skilldetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = skilldetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = skilldetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ELEMENT_OVERRIDES", "天气属性覆盖天气无效")
		}
		elementID, err := gameDataIdentifier(value.GetElementId(), "INVALID_WEATHER_ELEMENT_OVERRIDE_ELEMENT_ID")
		if err != nil {
			return nil, err
		}
		if _, duplicate := seen[weather]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_ELEMENT_OVERRIDES", "天气属性覆盖天气重复")
		}
		seen[weather] = struct{}{}
		result = append(result, skilldetail.WeatherElementOverride{Weather: weather, ElementID: elementID})
	}
	return result, nil
}

// weatherPowerMultipliersFromMessages 将管理 API 的天气威力倍率数组转换为资料层强类型规则。
//
// 每条倍率必须使用当前二进制支持的普通天气、正整数分子与分母，且同一天气只能出现一次。十倍上限与领域层和
// Battle Engine 状态校验保持一致，避免无效资料在 Battle 启动后造成无意义的整数计算或不可重放的行为。
func weatherPowerMultipliersFromMessages(values []*domainv1.GameSkillWeatherPowerMultiplier) ([]skilldetail.WeatherPowerMultiplier, error) {
	if len(values) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_WEATHER_POWER_MULTIPLIERS", "天气威力倍率数量超过上限")
	}
	result := make([]skilldetail.WeatherPowerMultiplier, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if value == nil {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_POWER_MULTIPLIERS", "天气威力倍率不能为空")
		}
		var weather skilldetail.WeatherKind
		switch value.GetWeather() {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = skilldetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = skilldetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = skilldetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = skilldetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_POWER_MULTIPLIERS", "天气威力倍率天气无效")
		}
		if value.GetNumerator() < 1 || value.GetDenominator() < 1 ||
			int64(value.GetNumerator()) > int64(value.GetDenominator())*10 {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_POWER_MULTIPLIERS", "天气威力倍率分数无效")
		}
		if _, duplicate := seen[weather]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_WEATHER_POWER_MULTIPLIERS", "天气威力倍率天气重复")
		}
		seen[weather] = struct{}{}
		result = append(result, skilldetail.WeatherPowerMultiplier{
			Weather: weather, Numerator: value.GetNumerator(), Denominator: value.GetDenominator(),
		})
	}
	return result, nil
}

// chargeSkippedWeathersFromMessages 将管理 API 的跳过蓄力天气枚举转换为资料层封闭天气代码。
//
// 该函数只负责数组的枚举、数量和唯一性边界；“必须同时声明 charging 易变状态”的跨字段约束由领域服务统一
// 校验，保证 HTTP、命令和测试夹具三个入口使用同一规则。
func chargeSkippedWeathersFromMessages(values []domainv1.GameSkillWeatherKind) ([]skilldetail.WeatherKind, error) {
	if len(values) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_CHARGE_SKIPPED_WEATHERS", "跳过蓄力天气数量超过上限")
	}
	result := make([]skilldetail.WeatherKind, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather skilldetail.WeatherKind
		switch value {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = skilldetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = skilldetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = skilldetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = skilldetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_CHARGE_SKIPPED_WEATHERS", "跳过蓄力天气无效")
		}
		if _, duplicate := seen[weather]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_CHARGE_SKIPPED_WEATHERS", "跳过蓄力天气重复")
		}
		seen[weather] = struct{}{}
		result = append(result, weather)
	}
	return result, nil
}

// terrainFromMessage 将 Protobuf 普通场地消息映射为领域强类型规则，并在 HTTP 边界拒绝未知枚举或非法数值。
func terrainFromMessage(value *domainv1.GameSkillTerrain) (*skilldetail.Terrain, error) {
	if value == nil {
		return nil, nil
	}
	var kind skilldetail.TerrainKind
	switch value.GetKind() {
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC:
		kind = skilldetail.TerrainKindElectric
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY:
		kind = skilldetail.TerrainKindGrassy
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY:
		kind = skilldetail.TerrainKindMisty
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC:
		kind = skilldetail.TerrainKindPsychic
	default:
		return nil, kratoserrors.BadRequest("INVALID_TERRAIN", "普通场地种类无效")
	}
	result := &skilldetail.Terrain{Kind: kind, TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent()}
	if !result.Kind.Valid() || result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_TERRAIN", "普通场地规则无效")
	}
	return result, nil
}

// tailwindFromMessage 将 Protobuf 顺风消息映射为领域强类型规则，并在 HTTP 边界拒绝非法持续回合和触发概率。
func tailwindFromMessage(value *domainv1.GameSkillTailwind) (*skilldetail.Tailwind, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.Tailwind{TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent()}
	if result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_TAILWIND", "顺风规则无效")
	}
	return result, nil
}

// reflectFromMessage 将 Protobuf 反射壁消息映射为领域强类型规则，并在 HTTP 边界拒绝非法持续回合和触发概率。
func reflectFromMessage(value *domainv1.GameSkillReflect) (*skilldetail.Reflect, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.Reflect{TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent()}
	if result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_REFLECT", "反射壁规则无效")
	}
	return result, nil
}

// lightScreenFromMessage 将 Protobuf 光墙消息映射为领域强类型规则，并在 HTTP 边界拒绝非法持续回合和触发概率。
func lightScreenFromMessage(value *domainv1.GameSkillLightScreen) (*skilldetail.LightScreen, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.LightScreen{TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent()}
	if result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_LIGHT_SCREEN", "光墙规则无效")
	}
	return result, nil
}

// auroraVeilFromMessage 将 Protobuf 极光幕消息映射为领域强类型规则，并在 HTTP 边界拒绝非法持续回合和触发概率。
func auroraVeilFromMessage(value *domainv1.GameSkillAuroraVeil) (*skilldetail.AuroraVeil, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.AuroraVeil{TurnsRemaining: value.GetTurnsRemaining(), ChancePercent: value.GetChancePercent()}
	if result.TurnsRemaining < 1 || result.TurnsRemaining > 100 || result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_AURORA_VEIL", "极光幕规则无效")
	}
	return result, nil
}

// spikesFromMessage 将 Protobuf 撒菱消息映射为领域强类型规则，并在 HTTP 边界拒绝非法触发概率。
func spikesFromMessage(value *domainv1.GameSkillSpikes) (*skilldetail.Spikes, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.Spikes{ChancePercent: value.GetChancePercent()}
	if result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_SPIKES", "撒菱规则无效")
	}
	return result, nil
}

// stealthRockFromMessage 将 Protobuf 隐形岩消息映射为领域强类型规则，并在 HTTP 边界拒绝非法触发概率。
func stealthRockFromMessage(value *domainv1.GameSkillStealthRock) (*skilldetail.StealthRock, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.StealthRock{ChancePercent: value.GetChancePercent()}
	if result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_STEALTH_ROCK", "隐形岩规则无效")
	}
	return result, nil
}

// toxicSpikesFromMessage 将 Protobuf 毒菱消息映射为领域强类型规则，并在 HTTP 边界拒绝非法触发概率。
func toxicSpikesFromMessage(value *domainv1.GameSkillToxicSpikes) (*skilldetail.ToxicSpikes, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.ToxicSpikes{ChancePercent: value.GetChancePercent()}
	if result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_TOXIC_SPIKES", "毒菱规则无效")
	}
	return result, nil
}

// stickyWebFromMessage 将 Protobuf 黏黏网消息映射为领域强类型规则，并在 HTTP 边界拒绝非法触发概率。
func stickyWebFromMessage(value *domainv1.GameSkillStickyWeb) (*skilldetail.StickyWeb, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.StickyWeb{ChancePercent: value.GetChancePercent()}
	if result.ChancePercent < 1 || result.ChancePercent > 100 {
		return nil, kratoserrors.BadRequest("INVALID_STICKY_WEB", "黏黏网规则无效")
	}
	return result, nil
}

// rapidSpinFromMessage 将 Protobuf 快速旋转消息映射为领域强类型规则。无参数固定规则必须显式传入
// enabled=true，避免空消息被误认为已启用。
func rapidSpinFromMessage(value *domainv1.GameSkillRapidSpin) (*skilldetail.RapidSpin, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.RapidSpin{Enabled: value.GetEnabled()}
	if !result.Enabled {
		return nil, kratoserrors.BadRequest("INVALID_RAPID_SPIN", "快速旋转规则必须启用")
	}
	return result, nil
}

// defogFromMessage 将 Protobuf 清除浓雾消息映射为领域强类型规则。无参数固定规则必须显式传入 enabled=true，
// 避免空消息被误认为已启用。
func defogFromMessage(value *domainv1.GameSkillDefog) (*skilldetail.Defog, error) {
	if value == nil {
		return nil, nil
	}
	result := &skilldetail.Defog{Enabled: value.GetEnabled()}
	if !result.Enabled {
		return nil, kratoserrors.BadRequest("INVALID_DEFOG", "清除浓雾规则必须启用")
	}
	return result, nil
}

// dynamicPowerSourceFromMessage 显式映射正向能力阶级规则的 Protobuf 枚举。
func dynamicPowerSourceFromMessage(value domainv1.GameSkillDynamicPowerSource) (skilldetail.DynamicPowerSource, bool) {
	switch value {
	case domainv1.GameSkillDynamicPowerSource_GAME_SKILL_DYNAMIC_POWER_SOURCE_USER:
		return skilldetail.DynamicPowerSourceUser, true
	case domainv1.GameSkillDynamicPowerSource_GAME_SKILL_DYNAMIC_POWER_SOURCE_SELECTED_TARGET:
		return skilldetail.DynamicPowerSourceSelectedTarget, true
	default:
		return "", false
	}
}

// speedPowerThresholdsFromMessages 转换使用者速度比例专用阈值数组。
func speedPowerThresholdsFromMessages(values []*domainv1.GameSkillSpeedPowerThreshold) ([]skilldetail.SpeedPowerThreshold, error) {
	result := make([]skilldetail.SpeedPowerThreshold, 0, len(values))
	for _, value := range values {
		if value == nil {
			return nil, invalidDynamicPowerMessage()
		}
		result = append(result, skilldetail.SpeedPowerThreshold{MinimumRatio: value.GetMinimumRatio(), Power: value.GetPower()})
	}
	return result, nil
}

// weightPowerThresholdsFromMessages 转换目标体重区间专用阈值数组。
func weightPowerThresholdsFromMessages(values []*domainv1.GameSkillWeightPowerThreshold) ([]skilldetail.WeightPowerThreshold, error) {
	result := make([]skilldetail.WeightPowerThreshold, 0, len(values))
	for _, value := range values {
		if value == nil {
			return nil, invalidDynamicPowerMessage()
		}
		result = append(result, skilldetail.WeightPowerThreshold{MaximumWeightInclusive: value.GetMaximumWeightInclusive(), Power: value.GetPower()})
	}
	return result, nil
}

// weightRatioPowerThresholdsFromMessages 转换使用者相对目标体重比例专用阈值数组。
func weightRatioPowerThresholdsFromMessages(values []*domainv1.GameSkillWeightRatioPowerThreshold) ([]skilldetail.WeightRatioPowerThreshold, error) {
	result := make([]skilldetail.WeightRatioPowerThreshold, 0, len(values))
	for _, value := range values {
		if value == nil {
			return nil, invalidDynamicPowerMessage()
		}
		result = append(result, skilldetail.WeightRatioPowerThreshold{MinimumUserToTargetRatio: value.GetMinimumUserToTargetRatio(), Power: value.GetPower()})
	}
	return result, nil
}

// hpFractionPowerThresholdsFromMessages 转换使用者当前生命比例专用阈值数组。
func hpFractionPowerThresholdsFromMessages(values []*domainv1.GameSkillHPFractionPowerThreshold) ([]skilldetail.HPFractionPowerThreshold, error) {
	result := make([]skilldetail.HPFractionPowerThreshold, 0, len(values))
	for _, value := range values {
		if value == nil {
			return nil, invalidDynamicPowerMessage()
		}
		result = append(result, skilldetail.HPFractionPowerThreshold{MaximumScaledHPInclusive: value.GetMaximumScaledHpInclusive(), Power: value.GetPower()})
	}
	return result, nil
}

// skillDetailVolatileStatus 显式映射外部 Protobuf 枚举到领域稳定代码。
func skillDetailVolatileStatus(value domainv1.GameSkillVolatileStatus) (skilldetail.VolatileStatus, bool) {
	switch value {
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_CONFUSION:
		return skilldetail.VolatileStatusConfusion, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_BINDING:
		return skilldetail.VolatileStatusBinding, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_TAUNT:
		return skilldetail.VolatileStatusTaunt, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_CHARGING:
		return skilldetail.VolatileStatusCharging, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_LOCKED_MOVE:
		return skilldetail.VolatileStatusLockedMove, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_DISABLE:
		return skilldetail.VolatileStatusDisable, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_PROTECTION:
		return skilldetail.VolatileStatusProtection, true
	case domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_SUBSTITUTE:
		return skilldetail.VolatileStatusSubstitute, true
	default:
		return "", false
	}
}

// skillDetailVolatileTarget 显式映射外部 Protobuf 枚举到领域稳定目标代码。
func skillDetailVolatileTarget(value domainv1.GameSkillVolatileTarget) (skilldetail.VolatileEffectTarget, bool) {
	switch value {
	case domainv1.GameSkillVolatileTarget_GAME_SKILL_VOLATILE_TARGET_SELECTED_TARGET:
		return skilldetail.VolatileEffectTargetSelectedTarget, true
	case domainv1.GameSkillVolatileTarget_GAME_SKILL_VOLATILE_TARGET_USER:
		return skilldetail.VolatileEffectTargetUser, true
	default:
		return "", false
	}
}

// gameSkillVolatileEffects 将已持久化的领域规则转换为管理端可直接编辑的显式 Protobuf 数组。
func gameSkillVolatileEffects(values []skilldetail.VolatileEffect) []*domainv1.GameSkillVolatileEffect {
	result := make([]*domainv1.GameSkillVolatileEffect, 0, len(values))
	for _, value := range values {
		status, statusOK := gameSkillVolatileStatus(value.Status)
		target, targetOK := gameSkillVolatileTarget(value.Target)
		if !statusOK || !targetOK {
			// 领域服务和资料读取边界已经拒绝此类值；保留防御性跳过，避免错误数据泄露为伪有效 API 响应。
			continue
		}
		result = append(result, &domainv1.GameSkillVolatileEffect{
			Status: status, Target: target, ChancePercent: value.ChancePercent,
			MinTurns: value.MinTurns, MaxTurns: value.MaxTurns,
			SubstituteCostNumerator:   value.SubstituteCostNumerator,
			SubstituteCostDenominator: value.SubstituteCostDenominator,
		})
	}
	return result
}

// gameSkillVolatileStatus 将领域稳定代码映射为公开契约枚举。
func gameSkillVolatileStatus(value skilldetail.VolatileStatus) (domainv1.GameSkillVolatileStatus, bool) {
	switch value {
	case skilldetail.VolatileStatusConfusion:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_CONFUSION, true
	case skilldetail.VolatileStatusBinding:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_BINDING, true
	case skilldetail.VolatileStatusTaunt:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_TAUNT, true
	case skilldetail.VolatileStatusCharging:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_CHARGING, true
	case skilldetail.VolatileStatusLockedMove:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_LOCKED_MOVE, true
	case skilldetail.VolatileStatusDisable:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_DISABLE, true
	case skilldetail.VolatileStatusProtection:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_PROTECTION, true
	case skilldetail.VolatileStatusSubstitute:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_SUBSTITUTE, true
	default:
		return domainv1.GameSkillVolatileStatus_GAME_SKILL_VOLATILE_STATUS_UNSPECIFIED, false
	}
}

// gameSkillVolatileTarget 将领域稳定目标映射为公开契约枚举。
func gameSkillVolatileTarget(value skilldetail.VolatileEffectTarget) (domainv1.GameSkillVolatileTarget, bool) {
	switch value {
	case skilldetail.VolatileEffectTargetSelectedTarget:
		return domainv1.GameSkillVolatileTarget_GAME_SKILL_VOLATILE_TARGET_SELECTED_TARGET, true
	case skilldetail.VolatileEffectTargetUser:
		return domainv1.GameSkillVolatileTarget_GAME_SKILL_VOLATILE_TARGET_USER, true
	default:
		return domainv1.GameSkillVolatileTarget_GAME_SKILL_VOLATILE_TARGET_UNSPECIFIED, false
	}
}

// gameSkillDynamicPower 将已持久化的动态基础威力规则转换为管理端可直接编辑的封闭 Protobuf 联合类型。
// 无效的离线数据库记录不会被此函数伪装成有效规则；Battle 启动边界会对此类记录明确拒绝。
func gameSkillDynamicPower(value skilldetail.DynamicPower) *domainv1.GameSkillDynamicPower {
	if !value.Active() {
		return nil
	}
	switch value.Kind {
	case skilldetail.DynamicPowerKindPositiveStatStageSum:
		source, found := gameSkillDynamicPowerSource(value.Source)
		if !found {
			return nil
		}
		return &domainv1.GameSkillDynamicPower{Rule: &domainv1.GameSkillDynamicPower_PositiveStatStageSum{
			PositiveStatStageSum: &domainv1.GameSkillPositiveStatStageSumDynamicPower{Source: source,
				BasePower: value.BasePower, PowerPerPositiveStage: value.PowerPerPositiveStage, MaximumPower: value.MaximumPower},
		}}
	case skilldetail.DynamicPowerKindUserSpeedRatioThresholds:
		thresholds := make([]*domainv1.GameSkillSpeedPowerThreshold, 0, len(value.SpeedThresholds))
		for _, threshold := range value.SpeedThresholds {
			thresholds = append(thresholds, &domainv1.GameSkillSpeedPowerThreshold{MinimumRatio: threshold.MinimumRatio, Power: threshold.Power})
		}
		return &domainv1.GameSkillDynamicPower{Rule: &domainv1.GameSkillDynamicPower_UserSpeedRatioThresholds{
			UserSpeedRatioThresholds: &domainv1.GameSkillUserSpeedRatioThresholdsDynamicPower{Thresholds: thresholds, FallbackPower: value.FallbackPower},
		}}
	case skilldetail.DynamicPowerKindTargetToUserSpeedRatio:
		return &domainv1.GameSkillDynamicPower{Rule: &domainv1.GameSkillDynamicPower_TargetToUserSpeedRatio{
			TargetToUserSpeedRatio: &domainv1.GameSkillTargetToUserSpeedRatioDynamicPower{Multiplier: value.SpeedRatioMultiplier,
				AdditivePower: value.SpeedRatioAdditivePower, MaximumPower: value.MaximumPower},
		}}
	case skilldetail.DynamicPowerKindTargetWeightThresholds:
		thresholds := make([]*domainv1.GameSkillWeightPowerThreshold, 0, len(value.WeightThresholds))
		for _, threshold := range value.WeightThresholds {
			thresholds = append(thresholds, &domainv1.GameSkillWeightPowerThreshold{MaximumWeightInclusive: threshold.MaximumWeightInclusive, Power: threshold.Power})
		}
		return &domainv1.GameSkillDynamicPower{Rule: &domainv1.GameSkillDynamicPower_TargetWeightThresholds{
			TargetWeightThresholds: &domainv1.GameSkillTargetWeightThresholdsDynamicPower{Thresholds: thresholds, FallbackPower: value.FallbackPower},
		}}
	case skilldetail.DynamicPowerKindUserTargetWeightRatioThresholds:
		thresholds := make([]*domainv1.GameSkillWeightRatioPowerThreshold, 0, len(value.WeightRatioThresholds))
		for _, threshold := range value.WeightRatioThresholds {
			thresholds = append(thresholds, &domainv1.GameSkillWeightRatioPowerThreshold{MinimumUserToTargetRatio: threshold.MinimumUserToTargetRatio, Power: threshold.Power})
		}
		return &domainv1.GameSkillDynamicPower{Rule: &domainv1.GameSkillDynamicPower_UserTargetWeightRatioThresholds{
			UserTargetWeightRatioThresholds: &domainv1.GameSkillUserTargetWeightRatioThresholdsDynamicPower{Thresholds: thresholds, FallbackPower: value.FallbackPower},
		}}
	case skilldetail.DynamicPowerKindUserHPFractionThresholds:
		thresholds := make([]*domainv1.GameSkillHPFractionPowerThreshold, 0, len(value.HPFractionThresholds))
		for _, threshold := range value.HPFractionThresholds {
			thresholds = append(thresholds, &domainv1.GameSkillHPFractionPowerThreshold{MaximumScaledHpInclusive: threshold.MaximumScaledHPInclusive, Power: threshold.Power})
		}
		return &domainv1.GameSkillDynamicPower{Rule: &domainv1.GameSkillDynamicPower_UserHpFractionThresholds{
			UserHpFractionThresholds: &domainv1.GameSkillUserHPFractionThresholdsDynamicPower{Scale: value.HPFractionScale,
				Thresholds: thresholds, FallbackPower: value.FallbackPower},
		}}
	default:
		return nil
	}
}

// gameSkillDynamicPowerSource 显式映射领域层正向能力阶级规则的取值对象。
func gameSkillDynamicPowerSource(value skilldetail.DynamicPowerSource) (domainv1.GameSkillDynamicPowerSource, bool) {
	switch value {
	case skilldetail.DynamicPowerSourceUser:
		return domainv1.GameSkillDynamicPowerSource_GAME_SKILL_DYNAMIC_POWER_SOURCE_USER, true
	case skilldetail.DynamicPowerSourceSelectedTarget:
		return domainv1.GameSkillDynamicPowerSource_GAME_SKILL_DYNAMIC_POWER_SOURCE_SELECTED_TARGET, true
	default:
		return domainv1.GameSkillDynamicPowerSource_GAME_SKILL_DYNAMIC_POWER_SOURCE_UNSPECIFIED, false
	}
}

// gameSkillFieldSpeedOrder 将已持久化的领域全场速度顺序资料转换为管理端可编辑的显式消息。无效数据库资料
// 由 Battle 编译边界拒绝；本函数不会把未知种类伪装为 TRICK_ROOM。
func gameSkillFieldSpeedOrder(value *skilldetail.FieldSpeedOrder) *domainv1.GameSkillFieldSpeedOrder {
	if value == nil {
		return nil
	}
	var kind domainv1.GameSkillFieldSpeedOrderKind
	switch value.Kind {
	case skilldetail.FieldSpeedOrderKindTrickRoom:
		kind = domainv1.GameSkillFieldSpeedOrderKind_GAME_SKILL_FIELD_SPEED_ORDER_KIND_TRICK_ROOM
	default:
		return nil
	}
	return &domainv1.GameSkillFieldSpeedOrder{
		Kind: kind, TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent,
	}
}

// gameSkillLeechSeed 将已持久化的领域寄生种子资料转换为管理端可编辑的显式消息。无效数据库资料会返回 nil，
// 随后的 Battle 编译边界仍会拒绝该详情，避免响应层猜测或修复损坏规则。
func gameSkillLeechSeed(value *skilldetail.LeechSeed) *domainv1.GameSkillLeechSeed {
	if value == nil || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillLeechSeed{ChancePercent: value.ChancePercent}
}

// gameSkillWeather 将已持久化的领域普通天气资料转换为管理端可编辑的显式消息。无效数据库资料返回 nil；
// Battle 编译边界仍会拒绝该详情，响应层不会猜测或修复损坏的天气规则。
func gameSkillWeather(value *skilldetail.Weather) *domainv1.GameSkillWeather {
	if value == nil {
		return nil
	}
	var kind domainv1.GameSkillWeatherKind
	switch value.Kind {
	case skilldetail.WeatherKindSun:
		kind = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
	case skilldetail.WeatherKindRain:
		kind = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
	case skilldetail.WeatherKindSandstorm:
		kind = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
	case skilldetail.WeatherKindSnow:
		kind = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
	default:
		return nil
	}
	if value.TurnsRemaining < 1 || value.TurnsRemaining > 100 || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillWeather{Kind: kind, TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent}
}

// gameSkillWeatherAccuracyOverrides 将资料层天气命中覆盖映射为管理 API 的强类型响应。
//
// 资料损坏时返回空数组而不猜测未知天气；Battle 编译边界会继续拒绝该条详情。0 命中率作为必中语义被
// 显式写回响应，避免客户端把它误解为未配置。
func gameSkillWeatherAccuracyOverrides(values []skilldetail.WeatherAccuracyOverride) []*domainv1.GameSkillWeatherAccuracyOverride {
	result := make([]*domainv1.GameSkillWeatherAccuracyOverride, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather domainv1.GameSkillWeatherKind
		switch value.Weather {
		case skilldetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case skilldetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case skilldetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case skilldetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return []*domainv1.GameSkillWeatherAccuracyOverride{}
		}
		if value.AccuracyPercent < 0 || value.AccuracyPercent > 100 {
			return []*domainv1.GameSkillWeatherAccuracyOverride{}
		}
		if _, duplicate := seen[value.Weather]; duplicate {
			return []*domainv1.GameSkillWeatherAccuracyOverride{}
		}
		seen[value.Weather] = struct{}{}
		result = append(result, &domainv1.GameSkillWeatherAccuracyOverride{
			Weather: weather, AccuracyPercent: value.AccuracyPercent,
		})
	}
	return result
}

// gameSkillWeatherElementOverrides 将资料层天气属性覆盖映射为管理 API 的强类型响应。
//
// 数据库资料出现未知天气、空 Identifier 或重复天气时返回空数组而不猜测可执行规则；Battle 编译边界仍会拒绝该详情，
// 因此响应层不会把损坏资料静默修复为无覆盖。
func gameSkillWeatherElementOverrides(values []skilldetail.WeatherElementOverride) []*domainv1.GameSkillWeatherElementOverride {
	result := make([]*domainv1.GameSkillWeatherElementOverride, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather domainv1.GameSkillWeatherKind
		switch value.Weather {
		case skilldetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case skilldetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case skilldetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case skilldetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return []*domainv1.GameSkillWeatherElementOverride{}
		}
		if value.ElementID == snowflake.ID(0) {
			return []*domainv1.GameSkillWeatherElementOverride{}
		}
		if _, duplicate := seen[value.Weather]; duplicate {
			return []*domainv1.GameSkillWeatherElementOverride{}
		}
		seen[value.Weather] = struct{}{}
		result = append(result, &domainv1.GameSkillWeatherElementOverride{
			Weather: weather, ElementId: value.ElementID.String(),
		})
	}
	return result
}

// gameSkillWeatherPowerMultipliers 将资料层天气威力倍率映射为管理 API 的强类型响应。
//
// 无效数据库资料会返回空数组而不会由响应层猜测默认值；Battle 编译器仍会拒绝对应资料，以使管理端看到的配置不
// 会掩盖实际不能启动对局的损坏状态。
func gameSkillWeatherPowerMultipliers(values []skilldetail.WeatherPowerMultiplier) []*domainv1.GameSkillWeatherPowerMultiplier {
	result := make([]*domainv1.GameSkillWeatherPowerMultiplier, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather domainv1.GameSkillWeatherKind
		switch value.Weather {
		case skilldetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case skilldetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case skilldetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case skilldetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return []*domainv1.GameSkillWeatherPowerMultiplier{}
		}
		if value.Numerator < 1 || value.Denominator < 1 || int64(value.Numerator) > int64(value.Denominator)*10 {
			return []*domainv1.GameSkillWeatherPowerMultiplier{}
		}
		if _, duplicate := seen[value.Weather]; duplicate {
			return []*domainv1.GameSkillWeatherPowerMultiplier{}
		}
		seen[value.Weather] = struct{}{}
		result = append(result, &domainv1.GameSkillWeatherPowerMultiplier{
			Weather: weather, Numerator: value.Numerator, Denominator: value.Denominator,
		})
	}
	return result
}

// gameSkillChargeSkippedWeathers 将资料层跳过蓄力天气集合映射为管理 API 的封闭枚举。
//
// 无效或重复的数据库天气会返回空数组；Battle 编译器仍会拒绝该详情，响应层不会通过排序、去重或默认值掩盖
// 损坏资料。
func gameSkillChargeSkippedWeathers(values []skilldetail.WeatherKind) []domainv1.GameSkillWeatherKind {
	result := make([]domainv1.GameSkillWeatherKind, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather domainv1.GameSkillWeatherKind
		switch value {
		case skilldetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case skilldetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case skilldetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case skilldetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return []domainv1.GameSkillWeatherKind{}
		}
		if _, duplicate := seen[value]; duplicate {
			return []domainv1.GameSkillWeatherKind{}
		}
		seen[value] = struct{}{}
		result = append(result, weather)
	}
	return result
}

// gameSkillTerrain 将已持久化的领域普通场地资料转换为管理端可编辑的显式消息。无效数据库资料返回 nil；
// Battle 编译边界仍会拒绝该详情，响应层不会猜测或修复损坏的场地规则。
func gameSkillTerrain(value *skilldetail.Terrain) *domainv1.GameSkillTerrain {
	if value == nil {
		return nil
	}
	var kind domainv1.GameSkillTerrainKind
	switch value.Kind {
	case skilldetail.TerrainKindElectric:
		kind = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC
	case skilldetail.TerrainKindGrassy:
		kind = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY
	case skilldetail.TerrainKindMisty:
		kind = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY
	case skilldetail.TerrainKindPsychic:
		kind = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC
	default:
		return nil
	}
	if value.TurnsRemaining < 1 || value.TurnsRemaining > 100 || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillTerrain{Kind: kind, TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent}
}

// gameSkillTailwind 将已持久化的领域顺风资料转换为管理端可编辑的显式消息。无效数据库资料返回 nil；
// Battle 编译边界仍会拒绝该详情，响应层不会猜测或修复损坏的顺风规则。
func gameSkillTailwind(value *skilldetail.Tailwind) *domainv1.GameSkillTailwind {
	if value == nil || value.TurnsRemaining < 1 || value.TurnsRemaining > 100 || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillTailwind{TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent}
}

// gameSkillReflect 将已持久化的领域反射壁资料转换为管理端可编辑的显式消息。无效数据库资料返回 nil；
// Battle 编译边界仍会拒绝该详情，响应层不会猜测或修复损坏的物理屏障规则。
func gameSkillReflect(value *skilldetail.Reflect) *domainv1.GameSkillReflect {
	if value == nil || value.TurnsRemaining < 1 || value.TurnsRemaining > 100 || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillReflect{TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent}
}

// gameSkillLightScreen 将已持久化的领域光墙资料转换为管理端可编辑的显式消息。无效数据库资料返回 nil；
// Battle 编译边界仍会拒绝该详情，响应层不会猜测或修复损坏的特殊屏障规则。
func gameSkillLightScreen(value *skilldetail.LightScreen) *domainv1.GameSkillLightScreen {
	if value == nil || value.TurnsRemaining < 1 || value.TurnsRemaining > 100 || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillLightScreen{TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent}
}

// gameSkillAuroraVeil 将已持久化的领域极光幕资料转换为管理端可编辑的显式消息。无效数据库资料返回 nil；
// Battle 编译边界仍会拒绝该详情，响应层不会猜测或修复损坏的双防屏障规则。
func gameSkillAuroraVeil(value *skilldetail.AuroraVeil) *domainv1.GameSkillAuroraVeil {
	if value == nil || value.TurnsRemaining < 1 || value.TurnsRemaining > 100 || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillAuroraVeil{TurnsRemaining: value.TurnsRemaining, ChancePercent: value.ChancePercent}
}

// gameSkillSpikes 将已持久化的领域撒菱资料转换为管理端可编辑消息。无效资料返回 nil；Battle 编译边界仍会
// 拒绝对应详情，不会由响应层静默修复。
func gameSkillSpikes(value *skilldetail.Spikes) *domainv1.GameSkillSpikes {
	if value == nil || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillSpikes{ChancePercent: value.ChancePercent}
}

// gameSkillStealthRock 将已持久化的领域隐形岩资料转换为管理端可编辑消息。无效资料返回 nil。
func gameSkillStealthRock(value *skilldetail.StealthRock) *domainv1.GameSkillStealthRock {
	if value == nil || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillStealthRock{ChancePercent: value.ChancePercent}
}

// gameSkillToxicSpikes 将已持久化的领域毒菱资料转换为管理端可编辑消息。无效资料返回 nil。
func gameSkillToxicSpikes(value *skilldetail.ToxicSpikes) *domainv1.GameSkillToxicSpikes {
	if value == nil || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillToxicSpikes{ChancePercent: value.ChancePercent}
}

// gameSkillStickyWeb 将已持久化的领域黏黏网资料转换为管理端可编辑消息。无效资料返回 nil。
func gameSkillStickyWeb(value *skilldetail.StickyWeb) *domainv1.GameSkillStickyWeb {
	if value == nil || value.ChancePercent < 1 || value.ChancePercent > 100 {
		return nil
	}
	return &domainv1.GameSkillStickyWeb{ChancePercent: value.ChancePercent}
}

// gameSkillRapidSpin 将已持久化的领域快速旋转资料转换为管理端可编辑消息。未显式启用的损坏资料返回 nil。
func gameSkillRapidSpin(value *skilldetail.RapidSpin) *domainv1.GameSkillRapidSpin {
	if value == nil || !value.Enabled {
		return nil
	}
	return &domainv1.GameSkillRapidSpin{Enabled: true}
}

// gameSkillDefog 将已持久化的领域清除浓雾资料转换为管理端可编辑消息。未显式启用的损坏资料返回 nil。
func gameSkillDefog(value *skilldetail.Defog) *domainv1.GameSkillDefog {
	if value == nil || !value.Enabled {
		return nil
	}
	return &domainv1.GameSkillDefog{Enabled: true}
}

func gameSkillOnUseRulesMessage(value skilldetail.RuleSet) *domainv1.GameSkillOnUseRules {
	message := &domainv1.GameSkillOnUseRules{
		DamageMode:                            string(value.DamageMode),
		CuresUserSideMajorStatuses:            value.CuresUserSideMajorStatuses,
		CuresUserMajorStatus:                  value.CuresUserMajorStatus,
		CuresUserSideActiveMajorStatuses:      value.CuresUserSideActiveMajorStatuses,
		ForceTargetSwitch:                     value.ForceTargetSwitch,
		RechargesAfterUse:                     value.RechargesAfterUse,
		LocksAccuracyOnTarget:                 value.LocksAccuracyOnTarget,
		MakesContact:                          value.MakesContact,
		PunchBased:                            value.PunchBased,
		SlicingBased:                          value.SlicingBased,
		SoundBased:                            value.SoundBased,
		PulseBased:                            value.PulseBased,
		BiteBased:                             value.BiteBased,
		PowderBased:                           value.PowderBased,
		OneHitKnockOutBlocksSameElementTarget: value.OneHitKnockOutBlocksSameElementTarget,
		ReceivedDamageAcceptsPhysical:         value.ReceivedDamageAcceptsPhysical,
		ReceivedDamageAcceptsSpecial:          value.ReceivedDamageAcceptsSpecial,
		ReceivedDamageIgnoreNonImmuneElementEffectiveness: value.ReceivedDamageIgnoreNonImmuneElementEffectiveness,
		WeakenedByGrassyTerrain:                           value.WeakenedByGrassyTerrain,
	}
	message.VolatileEffects = gameSkillVolatileEffects(value.VolatileEffects)
	message.WeatherAccuracyOverrides = gameSkillWeatherAccuracyOverrides(value.WeatherAccuracyOverrides)
	message.WeatherElementOverrides = gameSkillWeatherElementOverrides(value.WeatherElementOverrides)
	message.WeatherPowerMultipliers = gameSkillWeatherPowerMultipliers(value.WeatherPowerMultipliers)
	message.ChargeSkippedWeathers = gameSkillChargeSkippedWeathers(value.ChargeSkippedWeathers)
	message.DynamicPower = gameSkillDynamicPower(value.DynamicPower)
	message.FieldSpeedOrder = gameSkillFieldSpeedOrder(value.FieldSpeedOrder)
	message.LeechSeed = gameSkillLeechSeed(value.LeechSeed)
	message.Weather = gameSkillWeather(value.Weather)
	message.Terrain = gameSkillTerrain(value.Terrain)
	message.Tailwind = gameSkillTailwind(value.Tailwind)
	message.Reflect = gameSkillReflect(value.Reflect)
	message.LightScreen = gameSkillLightScreen(value.LightScreen)
	message.AuroraVeil = gameSkillAuroraVeil(value.AuroraVeil)
	message.Spikes = gameSkillSpikes(value.Spikes)
	message.StealthRock = gameSkillStealthRock(value.StealthRock)
	message.ToxicSpikes = gameSkillToxicSpikes(value.ToxicSpikes)
	message.StickyWeb = gameSkillStickyWeb(value.StickyWeb)
	message.RapidSpin = gameSkillRapidSpin(value.RapidSpin)
	message.Defog = gameSkillDefog(value.Defog)
	if value.AilmentID != nil {
		message.AilmentId = value.AilmentID.String()
	}
	if value.CategoryID != nil {
		message.CategoryId = value.CategoryID.String()
	}
	if value.TargetID != nil {
		message.TargetId = value.TargetID.String()
	}
	if value.MinHits != nil {
		message.MinHits = *value.MinHits
	}
	if value.MaxHits != nil {
		message.MaxHits = *value.MaxHits
	}
	if value.MinTurns != nil {
		message.MinTurns = *value.MinTurns
	}
	if value.MaxTurns != nil {
		message.MaxTurns = *value.MaxTurns
	}
	if value.Drain != nil {
		message.Drain = *value.Drain
	}
	if value.Healing != nil {
		message.Healing = *value.Healing
	}
	if value.TargetHealingNumerator != nil {
		message.TargetHealingNumerator = *value.TargetHealingNumerator
	}
	if value.TargetHealingDenominator != nil {
		message.TargetHealingDenominator = *value.TargetHealingDenominator
	}
	if value.CritRate != nil {
		message.CritRate = *value.CritRate
	}
	if value.DamageAmount != nil {
		message.DamageAmount = *value.DamageAmount
	}
	if value.DamageNumerator != nil {
		message.DamageNumerator = *value.DamageNumerator
	}
	if value.DamageDenominator != nil {
		message.DamageDenominator = *value.DamageDenominator
	}
	if value.MinimumDamage != nil {
		message.MinimumDamage = *value.MinimumDamage
	}
	if value.OneHitKnockOutBaseAccuracy != nil {
		message.OneHitKnockOutBaseAccuracy = *value.OneHitKnockOutBaseAccuracy
	}
	if value.OneHitKnockOutSameElementUserBaseAccuracy != nil {
		message.OneHitKnockOutSameElementUserBaseAccuracy = *value.OneHitKnockOutSameElementUserBaseAccuracy
	}
	if value.ReceivedDamageNumerator != nil {
		message.ReceivedDamageNumerator = *value.ReceivedDamageNumerator
	}
	if value.ReceivedDamageDenominator != nil {
		message.ReceivedDamageDenominator = *value.ReceivedDamageDenominator
	}
	if value.AilmentChance != nil {
		message.AilmentChance = *value.AilmentChance
	}
	if value.FlinchChance != nil {
		message.FlinchChance = *value.FlinchChance
	}
	if value.StatChance != nil {
		message.StatChance = *value.StatChance
	}
	return message
}

func skillRulesFromMessage(message *domainv1.GameSkillRules) (battlerules.Skill, error) {
	if message == nil || message.GetOnUse() == nil {
		return battlerules.Skill{}, nil
	}
	values, err := createSkillDetailValues(message.GetOnUse())
	if err != nil {
		return battlerules.Skill{}, err
	}
	values.Effect = nil
	values.ShortEffect = nil
	values.Description = nil
	rules, valid := battlerules.NewSkill(values)
	if !valid {
		return battlerules.Skill{}, kratoserrors.BadRequest("INVALID_GAME_SKILL_RULES", "技能战斗规则无效")
	}
	return rules, nil
}

func skillRulesMessage(rules battlerules.Skill) *domainv1.GameSkillRules {
	values, valid := rules.Values()
	if !valid {
		return &domainv1.GameSkillRules{}
	}
	return &domainv1.GameSkillRules{OnUse: gameSkillOnUseRulesMessage(skilldetail.RuleSet{OptionalValues: values})}
}
