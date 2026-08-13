package battle

import "math"
import "github.com/lishangbu/avalon/internal/platform/snowflake"
import "github.com/lishangbu/avalon/internal/battleengine"
import "github.com/lishangbu/avalon/internal/gamedata/skilldetail"
import "github.com/lishangbu/avalon/internal/gamedata/skillstatchange"

func (compiler *initialMemberCompiler) skill(skillID snowflake.ID, position battleengine.SkillPosition) (battleengine.SkillSnapshot, error) {
	if skillID == snowflake.ID(0) || position < 1 || position > battleengine.MaximumSkillsPerMember {
		return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
	}
	if cached, found := compiler.skills[skillID]; found {
		cached.Position = position
		return cached, nil
	}
	data, found := compiler.snapshot.skills[skillID]
	if !found || !data.Enabled || data.ElementID == nil || data.DamageClassID == nil || data.PP == nil || *data.PP < 1 || *data.PP > 255 {
		return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*data.ElementID); !enabled {
		return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
	}
	damageClass, err := compiler.damageClass(*data.DamageClassID)
	if err != nil {
		return battleengine.SkillSnapshot{}, err
	}
	if data.Power != nil && (*data.Power < 0 || *data.Power > math.MaxUint16) || data.Accuracy != nil && (*data.Accuracy < 0 || *data.Accuracy > 100) ||
		data.EffectChance != nil && (*data.EffectChance < 0 || *data.EffectChance > 100) || data.Priority < math.MinInt8 || data.Priority > math.MaxInt8 {
		return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
	}
	compiled := battleengine.SkillSnapshot{
		SkillID: skillID, Name: data.Name, ElementID: *data.ElementID, DamageClass: damageClass,
		Priority: int8(data.Priority), MaxPP: uint8(*data.PP), RemainingPP: uint8(*data.PP),
	}
	if data.Power != nil {
		compiled.Power = uint16(*data.Power)
	}
	if data.Accuracy != nil {
		compiled.Accuracy = uint8(*data.Accuracy)
	}
	if compiled.DamageClass == battleengine.DamageClassStatus && compiled.Power != 0 {
		return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
	}
	detail, hasDetail, err := compiler.detail(skillID)
	if err != nil {
		return battleengine.SkillSnapshot{}, err
	}
	targetScope := battleengine.SkillTargetScopeSelectedTarget
	effectTarget := battleengine.EffectTargetSelected
	var statChance *int32
	if hasDetail {
		targetScope, err = compiler.targetScope(detail.TargetID)
		if err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		// 自身技能不会把“已选目标”语义泄漏给附加效果；其它范围技能会在引擎逐一遍历
		// 真实目标，因此仍使用 selectedTarget 让效果落在当前遍历成员上。
		if targetScope == battleengine.SkillTargetScopeSelf {
			effectTarget = battleengine.EffectTargetUser
		}
		if detail.Drain != nil && (*detail.Drain < -100 || *detail.Drain > 100) ||
			detail.Healing != nil && (*detail.Healing < -100 || *detail.Healing > 100) {
			return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
		}
		if detail.Drain != nil {
			compiled.DrainPercent = int8(*detail.Drain)
		}
		if detail.Healing != nil {
			compiled.HealingPercent = int8(*detail.Healing)
		}
		if (detail.TargetHealingNumerator == nil) != (detail.TargetHealingDenominator == nil) ||
			detail.TargetHealingNumerator != nil && (*detail.TargetHealingNumerator < 1 || *detail.TargetHealingDenominator < 1 ||
				*detail.TargetHealingNumerator > *detail.TargetHealingDenominator || *detail.TargetHealingDenominator > math.MaxUint16) {
			return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
		}
		if detail.TargetHealingNumerator != nil {
			compiled.TargetHealingNumerator = uint16(*detail.TargetHealingNumerator)
			compiled.TargetHealingDenominator = uint16(*detail.TargetHealingDenominator)
		}
		if (detail.CuresUserSideMajorStatuses || detail.CuresUserMajorStatus || detail.CuresUserSideActiveMajorStatuses) &&
			compiled.DamageClass != battleengine.DamageClassStatus {
			return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
		}
		compiled.CuresUserSideMajorStatuses = detail.CuresUserSideMajorStatuses
		compiled.CuresUserMajorStatus = detail.CuresUserMajorStatus
		compiled.CuresUserSideActiveMajorStatuses = detail.CuresUserSideActiveMajorStatuses
		if detail.ForceTargetSwitch && targetScope != battleengine.SkillTargetScopeSelectedTarget {
			return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
		}
		compiled.ForceTargetSwitch = detail.ForceTargetSwitch
		compiled.RechargesAfterUse = detail.RechargesAfterUse
		compiled.LocksAccuracyOnTarget = detail.LocksAccuracyOnTarget
		compiled.MakesContact = detail.MakesContact
		compiled.PunchBased = detail.PunchBased
		compiled.SlicingBased = detail.SlicingBased
		compiled.SoundBased = detail.SoundBased
		compiled.PulseBased = detail.PulseBased
		compiled.BiteBased = detail.BiteBased
		compiled.PowderBased = detail.PowderBased
		compiled.WeakenedByGrassyTerrain = detail.WeakenedByGrassyTerrain
		if err := compileWeatherAccuracyOverrides(&compiled, detail.WeatherAccuracyOverrides); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compiler.compileWeatherElementOverrides(&compiled, detail.WeatherElementOverrides); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileWeatherPowerMultipliers(&compiled, detail.WeatherPowerMultipliers); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileChargeSkippedWeathers(&compiled, detail.ChargeSkippedWeathers, detail.VolatileEffects); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileSkillHitProperties(&compiled, detail); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileSkillDamageRule(&compiled, detail); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileDynamicPower(&compiled, detail.DynamicPower); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileFieldSpeedOrderApplication(&compiled, detail.FieldSpeedOrder, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileLeechSeedApplication(&compiled, detail.LeechSeed, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileWeatherApplication(&compiled, detail.Weather, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileTerrainApplication(&compiled, detail.Terrain, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileTailwindApplication(&compiled, detail.Tailwind, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileReflectApplication(&compiled, detail.Reflect, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileLightScreenApplication(&compiled, detail.LightScreen, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileAuroraVeilApplication(&compiled, detail.AuroraVeil, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileSpikesApplication(&compiled, detail.Spikes, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileStealthRockApplication(&compiled, detail.StealthRock, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileToxicSpikesApplication(&compiled, detail.ToxicSpikes, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileStickyWebApplication(&compiled, detail.StickyWeb, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileRapidSpinApplication(&compiled, detail.RapidSpin, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if err := compileDefogApplication(&compiled, detail.Defog, targetScope); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		if detail.FlinchChance != nil {
			if *detail.FlinchChance < 0 || *detail.FlinchChance > 100 {
				return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
			}
			compiled.FlinchChancePercent = uint8(*detail.FlinchChance)
		}
		if err := compileVolatileStatusApplications(&compiled, detail, effectTarget); err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		statChance = detail.StatChance
		if detail.AilmentID != nil {
			status, err := compiler.ailment(*detail.AilmentID)
			if err != nil {
				return battleengine.SkillSnapshot{}, err
			}
			compiled.StatusApplications = []battleengine.MajorStatusApplication{{
				Status: status, Target: effectTarget, ChancePercent: effectChance(detail.AilmentChance, data.EffectChance),
			}}
		}
	}
	if !hasDetail {
		// 没有详情资料的技能按单段普通要害处理，保证冻结快照始终是完整、可回放的执行形状。
		compiled.MinHits = 1
		compiled.MaxHits = 1
	}
	compiled.TargetScope = targetScope
	changes, err := compiler.changes(skillID)
	if err != nil {
		return battleengine.SkillSnapshot{}, err
	}
	compiled.StatStageEffects = make([]battleengine.StatStageEffect, 0, len(changes))
	for _, change := range changes {
		stat, err := compiler.stat(change.StatID)
		if err != nil {
			return battleengine.SkillSnapshot{}, err
		}
		battleStat, ok := battleStatForCode(stat.Code)
		if !ok {
			return battleengine.SkillSnapshot{}, ErrInitialStateCompilation
		}
		compiled.StatStageEffects = append(compiled.StatStageEffects, battleengine.StatStageEffect{
			Stat: battleStat, Target: effectTarget, StageDelta: int8(change.ChangeValue), ChancePercent: effectChance(statChance, data.EffectChance),
		})
	}
	compiler.skills[skillID] = compiled
	compiled.Position = position
	return compiled, nil
}

// compileWeatherAccuracyOverrides 将资料层按天气声明的命中率例外冻结到纯战斗引擎快照。
//
// 覆盖规则不建立天气、也不改变天气持续时间；它仅在对应普通天气已经生效时替换技能基础命中率。0 是
// 明确的必中语义，不能被当成缺失值或在编译时丢弃。资料服务虽然会先校验，Battle 边界仍逐项映射和拒绝
// 未知或重复天气，防止离线 SQL 写入让一场对局带着不确定的命中规则启动。
func compileWeatherAccuracyOverrides(
	compiled *battleengine.SkillSnapshot,
	values []skilldetail.WeatherAccuracyOverride,
) error {
	if len(values) > 4 {
		return ErrInitialStateCompilation
	}
	overrides := make([]battleengine.WeatherAccuracyOverride, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.Valid() || value.AccuracyPercent < 0 || value.AccuracyPercent > 100 {
			return ErrInitialStateCompilation
		}
		if _, duplicated := seen[value.Weather]; duplicated {
			return ErrInitialStateCompilation
		}
		seen[value.Weather] = struct{}{}
		var weather battleengine.WeatherKind
		switch value.Weather {
		case skilldetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case skilldetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case skilldetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case skilldetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return ErrInitialStateCompilation
		}
		overrides = append(overrides, battleengine.WeatherAccuracyOverride{
			Weather: weather, AccuracyPercent: uint8(value.AccuracyPercent),
		})
	}
	compiled.WeatherAccuracyOverrides = overrides
	return nil
}

// compileWeatherElementOverrides 将资料层按天气声明的属性例外冻结到纯战斗引擎快照。
//
// 覆盖规则不建立天气、也不改变天气持续时间；它仅在对应普通天气已生效时替换本次技能属性。除了拒绝未知或
// 重复天气外，编译器还必须确认目标属性仍属于启用资料，避免离线 SQL 写入已禁用或不存在的 Identifier 后让对局
// 以不完整的属性相性表启动。
func (compiler *initialMemberCompiler) compileWeatherElementOverrides(
	compiled *battleengine.SkillSnapshot,
	values []skilldetail.WeatherElementOverride,
) error {
	if len(values) > 4 {
		return ErrInitialStateCompilation
	}
	overrides := make([]battleengine.WeatherElementOverride, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.Valid() || value.ElementID == snowflake.ID(0) {
			return ErrInitialStateCompilation
		}
		if _, duplicated := seen[value.Weather]; duplicated {
			return ErrInitialStateCompilation
		}
		if _, enabled := compiler.elementIDEnabled(value.ElementID); !enabled {
			return ErrInitialStateCompilation
		}
		seen[value.Weather] = struct{}{}
		var weather battleengine.WeatherKind
		switch value.Weather {
		case skilldetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case skilldetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case skilldetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case skilldetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return ErrInitialStateCompilation
		}
		overrides = append(overrides, battleengine.WeatherElementOverride{
			Weather: weather, ElementID: value.ElementID,
		})
	}
	compiled.WeatherElementOverrides = overrides
	return nil
}

// compileWeatherPowerMultipliers 将资料层按天气声明的基础威力分数冻结到纯战斗引擎快照。
//
// 倍率只会在匹配的普通天气已经生效时参与普通伤害公式。这里重复领域层校验，是为了阻止绕过管理 API 的数据库
// 写入带着未知天气、重复天气、零分母或超出上限的整数分数启动对局。
func compileWeatherPowerMultipliers(
	compiled *battleengine.SkillSnapshot,
	values []skilldetail.WeatherPowerMultiplier,
) error {
	if len(values) > 4 {
		return ErrInitialStateCompilation
	}
	multipliers := make([]battleengine.WeatherPowerMultiplier, 0, len(values))
	seen := make(map[skilldetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.Valid() || value.Numerator < 1 || value.Denominator < 1 ||
			value.Numerator > math.MaxUint16 || value.Denominator > math.MaxUint16 ||
			int64(value.Numerator) > int64(value.Denominator)*10 {
			return ErrInitialStateCompilation
		}
		if _, duplicated := seen[value.Weather]; duplicated {
			return ErrInitialStateCompilation
		}
		seen[value.Weather] = struct{}{}
		var weather battleengine.WeatherKind
		switch value.Weather {
		case skilldetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case skilldetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case skilldetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case skilldetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return ErrInitialStateCompilation
		}
		multipliers = append(multipliers, battleengine.WeatherPowerMultiplier{
			Weather: weather, Numerator: uint16(value.Numerator), Denominator: uint16(value.Denominator),
		})
	}
	compiled.WeatherPowerMultipliers = multipliers
	return nil
}

// compileVolatileStatusApplications 将管理端的封闭易变状态资料转换为纯战斗引擎快照。
//
// 这条边界必须显式枚举每种状态和目标；未知 JSON、自由文本或超出 uint8 的数值一律阻止 Battle 启动，
// 不能降级为“忽略效果”的不确定战斗。
func compileVolatileStatusApplications(
	compiled *battleengine.SkillSnapshot,
	detail skilldetail.RuleSet,
	defaultTarget battleengine.EffectTarget,
) error {
	applications := make([]battleengine.VolatileStatusApplication, 0, len(detail.VolatileEffects))
	for _, effect := range detail.VolatileEffects {
		if effect.ChancePercent < 1 || effect.ChancePercent > 100 || effect.MinTurns < 1 || effect.MaxTurns < effect.MinTurns ||
			effect.MaxTurns > math.MaxUint8 || effect.SubstituteCostNumerator < 0 || effect.SubstituteCostNumerator > math.MaxUint8 ||
			effect.SubstituteCostDenominator < 0 || effect.SubstituteCostDenominator > math.MaxUint8 {
			return ErrInitialStateCompilation
		}
		status, ok := battleVolatileStatus(effect.Status)
		if !ok {
			return ErrInitialStateCompilation
		}
		target := defaultTarget
		switch effect.Target {
		case skilldetail.VolatileEffectTargetSelectedTarget:
			// 自身范围技能没有外部已选目标，资料使用 selectedTarget 时按前面已解析的自身语义落到使用者。
			if defaultTarget == battleengine.EffectTargetUser {
				target = battleengine.EffectTargetUser
			}
		case skilldetail.VolatileEffectTargetUser:
			target = battleengine.EffectTargetUser
		default:
			return ErrInitialStateCompilation
		}
		applications = append(applications, battleengine.VolatileStatusApplication{
			Status: status, Target: target, ChancePercent: uint8(effect.ChancePercent),
			MinTurns: uint8(effect.MinTurns), MaxTurns: uint8(effect.MaxTurns),
			SubstituteCostNumerator:   uint8(effect.SubstituteCostNumerator),
			SubstituteCostDenominator: uint8(effect.SubstituteCostDenominator),
		})
	}
	compiled.VolatileStatusApplications = applications
	return nil
}

// battleVolatileStatus 把已在 skilldetail 层校验过的稳定资料代码转换为纯引擎封闭枚举。
func battleVolatileStatus(status skilldetail.VolatileStatus) (battleengine.VolatileStatus, bool) {
	switch status {
	case skilldetail.VolatileStatusConfusion:
		return battleengine.VolatileStatusConfusion, true
	case skilldetail.VolatileStatusBinding:
		return battleengine.VolatileStatusBinding, true
	case skilldetail.VolatileStatusTaunt:
		return battleengine.VolatileStatusTaunt, true
	case skilldetail.VolatileStatusCharging:
		return battleengine.VolatileStatusCharging, true
	case skilldetail.VolatileStatusLockedMove:
		return battleengine.VolatileStatusLockedMove, true
	case skilldetail.VolatileStatusDisable:
		return battleengine.VolatileStatusDisable, true
	case skilldetail.VolatileStatusProtection:
		return battleengine.VolatileStatusProtection, true
	case skilldetail.VolatileStatusSubstitute:
		return battleengine.VolatileStatusSubstitute, true
	default:
		return "", false
	}
}

// compileSkillHitProperties 将实时资料中可选的连续命中次数和要害等级转换为引擎不可空的冻结值。
//
// 资料只填写一个命中次数端点时，它表达固定段数而不是开放区间；两个端点同时存在时才表达随机范围。
// 该规则让管理端可以用最少的字段正确描述固定多段技能，同时不会把缺失的另一端误推断为 1 段。
func compileSkillHitProperties(compiled *battleengine.SkillSnapshot, detail skilldetail.RuleSet) error {
	minimum, maximum := int32(1), int32(1)
	switch {
	case detail.MinHits != nil && detail.MaxHits != nil:
		minimum, maximum = *detail.MinHits, *detail.MaxHits
	case detail.MinHits != nil:
		minimum, maximum = *detail.MinHits, *detail.MinHits
	case detail.MaxHits != nil:
		minimum, maximum = *detail.MaxHits, *detail.MaxHits
	}
	if minimum < 1 || maximum < minimum || maximum > 100 {
		return ErrInitialStateCompilation
	}
	compiled.MinHits = uint8(minimum)
	compiled.MaxHits = uint8(maximum)
	if detail.CritRate == nil {
		return nil
	}
	if *detail.CritRate < 0 || *detail.CritRate > 6 {
		return ErrInitialStateCompilation
	}
	compiled.CriticalHitStage = uint8(*detail.CritRate)
	return nil
}

// compileSkillDamageRule 将资料层的 kebab-case 直接伤害规则转换为纯引擎快照的稳定强类型模式。
//
// 该转换是实时资料进入 Battle 的最后一道边界：即使数据库曾被离线工具错误写入，未知模式、溢出数值或
// 与模式不匹配的参数也会阻止对局启动，而不会在运行时降级为普通公式伤害。
func compileSkillDamageRule(compiled *battleengine.SkillSnapshot, detail skilldetail.RuleSet) error {
	mode := detail.DamageMode
	if mode == "" {
		return ErrInitialStateCompilation
	}
	switch mode {
	case skilldetail.DamageModeFormula:
		compiled.DamageMode = battleengine.SkillDamageModeFormula
	case skilldetail.DamageModeFixedAmount:
		if detail.DamageAmount == nil || *detail.DamageAmount < 1 {
			return ErrInitialStateCompilation
		}
		compiled.DamageMode = battleengine.SkillDamageModeFixedAmount
		compiled.DamageAmount = uint32(*detail.DamageAmount)
	case skilldetail.DamageModeUserLevel:
		compiled.DamageMode = battleengine.SkillDamageModeUserLevel
	case skilldetail.DamageModeTargetCurrentHPFraction:
		if detail.DamageNumerator == nil || detail.DamageDenominator == nil || detail.MinimumDamage == nil ||
			*detail.DamageNumerator < 1 || *detail.DamageNumerator > math.MaxUint16 ||
			*detail.DamageDenominator < 1 || *detail.DamageDenominator > math.MaxUint16 ||
			*detail.DamageNumerator > *detail.DamageDenominator || *detail.MinimumDamage < 1 {
			return ErrInitialStateCompilation
		}
		compiled.DamageMode = battleengine.SkillDamageModeTargetCurrentHPFraction
		compiled.DamageNumerator = uint16(*detail.DamageNumerator)
		compiled.DamageDenominator = uint16(*detail.DamageDenominator)
		compiled.MinimumDamage = uint32(*detail.MinimumDamage)
	case skilldetail.DamageModeTargetCurrentHPMinusUserCurrentHP:
		compiled.DamageMode = battleengine.SkillDamageModeTargetCurrentHPMinusUserCurrentHP
	case skilldetail.DamageModeUserCurrentHPAndUserFaints:
		compiled.DamageMode = battleengine.SkillDamageModeUserCurrentHPAndUserFaints
	case skilldetail.DamageModeAverageUserAndTargetCurrentHP:
		if compiled.DamageClass != battleengine.DamageClassStatus {
			return ErrInitialStateCompilation
		}
		compiled.DamageMode = battleengine.SkillDamageModeAverageUserAndTargetCurrentHP
	case skilldetail.DamageModeOneHitKnockOut:
		if compiled.DamageClass == battleengine.DamageClassStatus || detail.OneHitKnockOutBaseAccuracy == nil ||
			*detail.OneHitKnockOutBaseAccuracy < 1 || *detail.OneHitKnockOutBaseAccuracy > 100 ||
			detail.OneHitKnockOutSameElementUserBaseAccuracy != nil &&
				(*detail.OneHitKnockOutSameElementUserBaseAccuracy < 1 || *detail.OneHitKnockOutSameElementUserBaseAccuracy > 100) {
			return ErrInitialStateCompilation
		}
		compiled.DamageMode = battleengine.SkillDamageModeOneHitKnockOut
		compiled.OneHitKnockOutBaseAccuracy = uint8(*detail.OneHitKnockOutBaseAccuracy)
		if detail.OneHitKnockOutSameElementUserBaseAccuracy != nil {
			compiled.OneHitKnockOutSameElementUserBaseAccuracy = uint8(*detail.OneHitKnockOutSameElementUserBaseAccuracy)
		}
		compiled.OneHitKnockOutBlocksSameElementTarget = detail.OneHitKnockOutBlocksSameElementTarget
	case skilldetail.DamageModeReceivedDamage:
		if compiled.DamageClass == battleengine.DamageClassStatus || detail.ReceivedDamageNumerator == nil ||
			detail.ReceivedDamageDenominator == nil || *detail.ReceivedDamageNumerator < 1 ||
			*detail.ReceivedDamageNumerator > math.MaxUint16 || *detail.ReceivedDamageDenominator < 1 ||
			*detail.ReceivedDamageDenominator > math.MaxUint16 ||
			(!detail.ReceivedDamageAcceptsPhysical && !detail.ReceivedDamageAcceptsSpecial) {
			return ErrInitialStateCompilation
		}
		compiled.DamageMode = battleengine.SkillDamageModeReceivedDamage
		compiled.ReceivedDamageNumerator = uint16(*detail.ReceivedDamageNumerator)
		compiled.ReceivedDamageDenominator = uint16(*detail.ReceivedDamageDenominator)
		compiled.ReceivedDamageAcceptsPhysical = detail.ReceivedDamageAcceptsPhysical
		compiled.ReceivedDamageAcceptsSpecial = detail.ReceivedDamageAcceptsSpecial
		compiled.ReceivedDamageIgnoreNonImmuneElementEffectiveness = detail.ReceivedDamageIgnoreNonImmuneElementEffectiveness
	default:
		return ErrInitialStateCompilation
	}
	return nil
}

// compileDynamicPower 将已经过资料层校验的动态基础威力显式映射为纯引擎规则。
//
// 动态威力只允许普通物理或特殊伤害公式。这里逐种枚举映射，而不把 JSON 或技能名称交给引擎解释；即使数据库
// 被离线工具绕过写入，未知代码、越界整数或不完整规则也会令 Battle 在启动前失败。
func compileDynamicPower(compiled *battleengine.SkillSnapshot, value skilldetail.DynamicPower) error {
	if !value.Valid() {
		return ErrInitialStateCompilation
	}
	if !value.Active() {
		return nil
	}
	if compiled.DamageMode != battleengine.SkillDamageModeFormula || compiled.DamageClass == battleengine.DamageClassStatus {
		return ErrInitialStateCompilation
	}
	rule := battleengine.DynamicPowerRule{}
	switch value.Kind {
	case skilldetail.DynamicPowerKindPositiveStatStageSum:
		rule.Kind = battleengine.DynamicPowerKindPositiveStatStageSum
		switch value.Source {
		case skilldetail.DynamicPowerSourceUser:
			rule.Source = battleengine.EffectTargetUser
		case skilldetail.DynamicPowerSourceSelectedTarget:
			rule.Source = battleengine.EffectTargetSelected
		default:
			return ErrInitialStateCompilation
		}
		basePower, baseOK := positiveDynamicPower(value.BasePower)
		perStage, perStageOK := positiveDynamicPower(value.PowerPerPositiveStage)
		maximumPower, maximumOK := optionalDynamicPower(value.MaximumPower)
		if !baseOK || !perStageOK || !maximumOK {
			return ErrInitialStateCompilation
		}
		rule.BasePower = basePower
		rule.PowerPerPositiveStage = perStage
		rule.MaximumPower = maximumPower
	case skilldetail.DynamicPowerKindUserSpeedRatioThresholds:
		thresholds, err := compileSpeedPowerThresholds(value.SpeedThresholds)
		if err != nil {
			return err
		}
		fallbackPower, fallbackOK := positiveDynamicPower(value.FallbackPower)
		if !fallbackOK {
			return ErrInitialStateCompilation
		}
		rule.Kind = battleengine.DynamicPowerKindUserSpeedRatioThresholds
		rule.FallbackPower = fallbackPower
		rule.SpeedThresholds = thresholds
	case skilldetail.DynamicPowerKindTargetToUserSpeedRatio:
		multiplier, multiplierOK := positiveDynamicPower(value.SpeedRatioMultiplier)
		additivePower, additiveOK := optionalDynamicPower(value.SpeedRatioAdditivePower)
		maximumPower, maximumOK := positiveDynamicPower(value.MaximumPower)
		if !multiplierOK || !additiveOK || !maximumOK {
			return ErrInitialStateCompilation
		}
		rule.Kind = battleengine.DynamicPowerKindTargetToUserSpeedRatio
		rule.SpeedRatioMultiplier = multiplier
		rule.SpeedRatioAdditivePower = additivePower
		rule.MaximumPower = maximumPower
	case skilldetail.DynamicPowerKindTargetWeightThresholds:
		thresholds, err := compileWeightPowerThresholds(value.WeightThresholds)
		if err != nil {
			return err
		}
		fallbackPower, fallbackOK := positiveDynamicPower(value.FallbackPower)
		if !fallbackOK {
			return ErrInitialStateCompilation
		}
		rule.Kind = battleengine.DynamicPowerKindTargetWeightThresholds
		rule.FallbackPower = fallbackPower
		rule.WeightThresholds = thresholds
	case skilldetail.DynamicPowerKindUserTargetWeightRatioThresholds:
		thresholds, err := compileWeightRatioPowerThresholds(value.WeightRatioThresholds)
		if err != nil {
			return err
		}
		fallbackPower, fallbackOK := positiveDynamicPower(value.FallbackPower)
		if !fallbackOK {
			return ErrInitialStateCompilation
		}
		rule.Kind = battleengine.DynamicPowerKindUserTargetWeightRatioThresholds
		rule.FallbackPower = fallbackPower
		rule.WeightRatioThresholds = thresholds
	case skilldetail.DynamicPowerKindUserHPFractionThresholds:
		thresholds, err := compileHPFractionPowerThresholds(value.HPFractionThresholds)
		if err != nil {
			return err
		}
		scale, scaleOK := positiveDynamicPower(value.HPFractionScale)
		fallbackPower, fallbackOK := positiveDynamicPower(value.FallbackPower)
		if !scaleOK || !fallbackOK {
			return ErrInitialStateCompilation
		}
		rule.Kind = battleengine.DynamicPowerKindUserHPFractionThresholds
		rule.HPFractionScale = scale
		rule.FallbackPower = fallbackPower
		rule.HPFractionThresholds = thresholds
	default:
		return ErrInitialStateCompilation
	}
	compiled.DynamicPower = rule
	return nil
}

// compileFieldSpeedOrderApplication 将资料层的全场速度顺序规则映射为纯战斗引擎快照。
//
// 全场效果不是某个目标成员的附加状态，因而只允许自身范围的变化技能建立。未知资料代码、越界数值或把规则挂在
// 伤害技能/外部目标上的组合都会阻止 Battle 启动，不能被静默忽略。
func compileFieldSpeedOrderApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.FieldSpeedOrder,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		!value.Kind.Valid() || value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	var kind battleengine.FieldSpeedOrderKind
	switch value.Kind {
	case skilldetail.FieldSpeedOrderKindTrickRoom:
		kind = battleengine.FieldSpeedOrderKindTrickRoom
	default:
		return ErrInitialStateCompilation
	}
	compiled.FieldSpeedOrderApplication = &battleengine.FieldSpeedOrderApplication{
		Effect:        battleengine.FieldSpeedOrderEffect{Kind: kind, TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileLeechSeedApplication 将资料层的寄生种子规则映射为纯战斗引擎快照。
//
// 种子必须由单体目标变化技能施加；来源槽位、草属性免疫、替身拦截、换人清理以及回合末结算都属于引擎
// 固定语义，资料层只声明该独立规则的触发概率。任何越界概率或错误的技能形状都会阻止 Battle 启动。
func compileLeechSeedApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.LeechSeed,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelectedTarget ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.LeechSeedApplication = &battleengine.LeechSeedApplication{ChancePercent: uint8(value.ChancePercent)}
	return nil
}

// compileWeatherApplication 将资料层的普通天气规则映射为纯战斗引擎快照。
//
// 普通天气是全场环境，不是外部目标的附加状态，因此只允许自身范围的变化技能建立。未知种类、越界回合或概率、
// 以及把规则挂在伤害技能或外部目标上的组合都会阻止 Battle 启动，不能被静默忽略。
func compileWeatherApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.Weather,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		!value.Kind.Valid() || value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	var kind battleengine.WeatherKind
	switch value.Kind {
	case skilldetail.WeatherKindSun:
		kind = battleengine.WeatherKindSun
	case skilldetail.WeatherKindRain:
		kind = battleengine.WeatherKindRain
	case skilldetail.WeatherKindSandstorm:
		kind = battleengine.WeatherKindSandstorm
	case skilldetail.WeatherKindSnow:
		kind = battleengine.WeatherKindSnow
	default:
		return ErrInitialStateCompilation
	}
	compiled.WeatherApplication = &battleengine.WeatherApplication{
		Effect:        battleengine.WeatherEffect{Kind: kind, TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileTerrainApplication 将资料层的普通场地规则映射为纯战斗引擎快照。
//
// 场地是全场环境而非外部目标附加状态，因此只允许自身范围的变化技能建立。未知种类、越界回合或概率，以及把
// 规则挂在伤害技能或外部目标上的组合都会阻止 Battle 启动，不能被静默忽略。
func compileTerrainApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.Terrain,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		!value.Kind.Valid() || value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	var kind battleengine.TerrainKind
	switch value.Kind {
	case skilldetail.TerrainKindElectric:
		kind = battleengine.TerrainKindElectric
	case skilldetail.TerrainKindGrassy:
		kind = battleengine.TerrainKindGrassy
	case skilldetail.TerrainKindMisty:
		kind = battleengine.TerrainKindMisty
	case skilldetail.TerrainKindPsychic:
		kind = battleengine.TerrainKindPsychic
	default:
		return ErrInitialStateCompilation
	}
	compiled.TerrainApplication = &battleengine.TerrainApplication{
		Effect:        battleengine.TerrainEffect{Kind: kind, TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileTailwindApplication 将资料层的顺风规则映射为纯战斗引擎快照。
//
// 顺风固定作用于使用者一方，因而只允许自身范围的变化技能建立。越界回合或概率，以及把规则挂在伤害技能或
// 外部目标上的组合都会阻止 Battle 启动，不能被静默忽略。
func compileTailwindApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.Tailwind,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.TailwindApplication = &battleengine.TailwindApplication{
		Effect:        battleengine.TailwindEffect{TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileReflectApplication 将资料层的反射壁规则映射为纯战斗引擎快照。
//
// 反射壁固定作用于使用者一方，因而只允许自身范围的变化技能建立。越界回合或概率，以及把规则挂在伤害技能或
// 外部目标上的组合都会阻止 Battle 启动，不能被静默忽略或误编译为光墙、极光幕。
func compileReflectApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.Reflect,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.ReflectApplication = &battleengine.ReflectApplication{
		Effect:        battleengine.ReflectEffect{TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileLightScreenApplication 将资料层的光墙规则映射为纯战斗引擎快照。
//
// 光墙固定作用于使用者一方，因而只允许自身范围的变化技能建立。越界回合或概率，以及把规则挂在伤害技能或
// 外部目标上的组合都会阻止 Battle 启动，不能被静默忽略或误编译为反射壁、极光幕。
func compileLightScreenApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.LightScreen,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.LightScreenApplication = &battleengine.LightScreenApplication{
		Effect:        battleengine.LightScreenEffect{TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileAuroraVeilApplication 将资料层的极光幕规则映射为纯战斗引擎快照。
//
// 极光幕固定作用于使用者一方，因而只允许自身范围的变化技能建立。越界回合或概率，以及把规则挂在伤害技能或
// 外部目标上的组合都会阻止 Battle 启动，不能被静默忽略或误编译为仅物理或仅特殊屏障。
func compileAuroraVeilApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.AuroraVeil,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelf ||
		value.TurnsRemaining < 1 || value.TurnsRemaining > math.MaxUint8 ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.AuroraVeilApplication = &battleengine.AuroraVeilApplication{
		Effect:        battleengine.AuroraVeilEffect{TurnsRemaining: uint8(value.TurnsRemaining)},
		ChancePercent: uint8(value.ChancePercent),
	}
	return nil
}

// compileSpikesApplication 将资料层的撒菱规则映射为纯战斗引擎快照。
//
// 撒菱只允许由单体目标变化技能建立；概率必须能够无损转换为 uint8。违反任一约束都会阻止 Battle 启动，避免
// 把入场危害错误编译为使用者侧状态或普通伤害。
func compileSpikesApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.Spikes,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelectedTarget ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.SpikesApplication = &battleengine.SpikesApplication{ChancePercent: uint8(value.ChancePercent)}
	return nil
}

// compileStealthRockApplication 将资料层的隐形岩规则映射为纯战斗引擎快照。
//
// 隐形岩只允许由单体目标变化技能建立；它没有层数并按岩石属性相性结算，因此不能复用撒菱或毒菱编译分支。
func compileStealthRockApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.StealthRock,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelectedTarget ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.StealthRockApplication = &battleengine.StealthRockApplication{ChancePercent: uint8(value.ChancePercent)}
	return nil
}

// compileToxicSpikesApplication 将资料层的毒菱规则映射为纯战斗引擎快照。
//
// 毒菱只允许由单体目标变化技能建立；它在换入时施加异常或被毒属性成员吸收，故保留独立编译分支。
func compileToxicSpikesApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.ToxicSpikes,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelectedTarget ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.ToxicSpikesApplication = &battleengine.ToxicSpikesApplication{ChancePercent: uint8(value.ChancePercent)}
	return nil
}

// compileStickyWebApplication 将资料层的黏黏网规则映射为纯战斗引擎快照。
//
// 黏黏网只允许由单体目标变化技能建立；它在接地成员换入时改变速度能力阶级，不会造成伤害或主要异常。
func compileStickyWebApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.StickyWeb,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelectedTarget ||
		value.ChancePercent < 1 || value.ChancePercent > 100 {
		return ErrInitialStateCompilation
	}
	compiled.StickyWebApplication = &battleengine.StickyWebApplication{ChancePercent: uint8(value.ChancePercent)}
	return nil
}

// compileRapidSpinApplication 将资料层的快速旋转固定规则映射为纯战斗引擎快照。
//
// 快速旋转只能附着在单体目标物理伤害技能上，并要求资料显式启用；它只清除使用者一方的入场危害。
func compileRapidSpinApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.RapidSpin,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if !value.Enabled || compiled.DamageClass != battleengine.DamageClassPhysical || targetScope != battleengine.SkillTargetScopeSelectedTarget {
		return ErrInitialStateCompilation
	}
	compiled.RapidSpinApplication = &battleengine.RapidSpinApplication{}
	return nil
}

// compileDefogApplication 将资料层的清除浓雾固定规则映射为纯战斗引擎快照。
//
// 清除浓雾只能附着在单体目标变化技能上，并要求资料显式启用；引擎会保留顺风、清除目标方屏障与危害及普通场地。
func compileDefogApplication(
	compiled *battleengine.SkillSnapshot,
	value *skilldetail.Defog,
	targetScope battleengine.SkillTargetScope,
) error {
	if value == nil {
		return nil
	}
	if !value.Enabled || compiled.DamageClass != battleengine.DamageClassStatus || targetScope != battleengine.SkillTargetScopeSelectedTarget {
		return ErrInitialStateCompilation
	}
	compiled.DefogApplication = &battleengine.DefogApplication{}
	return nil
}

// positiveDynamicPower 将资料层正基础威力或参数转换为引擎 uint16，拒绝零、负数和截断。
func positiveDynamicPower(value int32) (uint16, bool) {
	if value < 1 || value > math.MaxUint16 {
		return 0, false
	}
	return uint16(value), true
}

// optionalDynamicPower 将允许为零的资料层动态参数转换为引擎 uint16，拒绝负数和截断。
func optionalDynamicPower(value int32) (uint16, bool) {
	if value < 0 || value > math.MaxUint16 {
		return 0, false
	}
	return uint16(value), true
}

// compileSpeedPowerThresholds 将速度比例阈值逐项转换为引擎快照。
func compileSpeedPowerThresholds(values []skilldetail.SpeedPowerThreshold) ([]battleengine.SpeedPowerThreshold, error) {
	result := make([]battleengine.SpeedPowerThreshold, 0, len(values))
	for _, value := range values {
		ratio, ratioOK := positiveDynamicPower(value.MinimumRatio)
		power, powerOK := positiveDynamicPower(value.Power)
		if !ratioOK || !powerOK {
			return nil, ErrInitialStateCompilation
		}
		result = append(result, battleengine.SpeedPowerThreshold{MinimumRatio: ratio, Power: power})
	}
	return result, nil
}

// compileWeightPowerThresholds 将体重区间阈值逐项转换为引擎快照。
func compileWeightPowerThresholds(values []skilldetail.WeightPowerThreshold) ([]battleengine.WeightPowerThreshold, error) {
	result := make([]battleengine.WeightPowerThreshold, 0, len(values))
	for _, value := range values {
		power, powerOK := positiveDynamicPower(value.Power)
		if value.MaximumWeightInclusive < 1 || !powerOK {
			return nil, ErrInitialStateCompilation
		}
		result = append(result, battleengine.WeightPowerThreshold{MaximumWeightInclusive: uint32(value.MaximumWeightInclusive), Power: power})
	}
	return result, nil
}

// compileWeightRatioPowerThresholds 将使用者相对目标体重比例阈值逐项转换为引擎快照。
func compileWeightRatioPowerThresholds(values []skilldetail.WeightRatioPowerThreshold) ([]battleengine.WeightRatioPowerThreshold, error) {
	result := make([]battleengine.WeightRatioPowerThreshold, 0, len(values))
	for _, value := range values {
		ratio, ratioOK := positiveDynamicPower(value.MinimumUserToTargetRatio)
		power, powerOK := positiveDynamicPower(value.Power)
		if !ratioOK || !powerOK {
			return nil, ErrInitialStateCompilation
		}
		result = append(result, battleengine.WeightRatioPowerThreshold{MinimumUserToTargetRatio: ratio, Power: power})
	}
	return result, nil
}

// compileHPFractionPowerThresholds 将生命比例阈值逐项转换为引擎快照。
func compileHPFractionPowerThresholds(values []skilldetail.HPFractionPowerThreshold) ([]battleengine.HPFractionPowerThreshold, error) {
	result := make([]battleengine.HPFractionPowerThreshold, 0, len(values))
	for _, value := range values {
		power, powerOK := positiveDynamicPower(value.Power)
		if value.MaximumScaledHPInclusive < 0 || value.MaximumScaledHPInclusive > math.MaxUint16 || !powerOK {
			return nil, ErrInitialStateCompilation
		}
		result = append(result, battleengine.HPFractionPowerThreshold{MaximumScaledHPInclusive: uint16(value.MaximumScaledHPInclusive), Power: power})
	}
	return result, nil
}

func (compiler *initialMemberCompiler) damageClass(id snowflake.ID) (battleengine.DamageClass, error) {
	if cached, found := compiler.damageClasses[id]; found {
		return cached, nil
	}
	data, found := compiler.snapshot.damageClasses[id]
	if !found || !data.Enabled {
		return "", ErrInitialStateCompilation
	}
	value := battleengine.DamageClass(data.Code)
	if !value.Valid() {
		return "", ErrInitialStateCompilation
	}
	compiler.damageClasses[id] = value
	return value, nil
}

func (compiler *initialMemberCompiler) detail(skillID snowflake.ID) (skilldetail.RuleSet, bool, error) {
	if cached, found := compiler.details[skillID]; found {
		return cached, true, nil
	}
	value, found := compiler.snapshot.details[skillID]
	if !found {
		return skilldetail.RuleSet{}, false, nil
	}
	if value.SkillID != skillID {
		return skilldetail.RuleSet{}, false, ErrInitialStateCompilation
	}
	compiler.details[skillID] = value
	return value, true, nil
}

func (compiler *initialMemberCompiler) ailment(id snowflake.ID) (battleengine.MajorStatus, error) {
	if cached, found := compiler.ailments[id]; found {
		return cached, nil
	}
	data, found := compiler.snapshot.ailments[id]
	if !found || !data.Enabled {
		return "", ErrInitialStateCompilation
	}
	value := battleengine.MajorStatus(data.Code)
	if !value.Valid() {
		return "", ErrInitialStateCompilation
	}
	compiler.ailments[id] = value
	return value, nil
}

func (compiler *initialMemberCompiler) targetScope(id *snowflake.ID) (battleengine.SkillTargetScope, error) {
	if id == nil {
		return battleengine.SkillTargetScopeSelectedTarget, nil
	}
	if cached, found := compiler.targets[*id]; found {
		return cached, nil
	}
	data, found := compiler.snapshot.targets[*id]
	if !found || !data.Enabled {
		return "", ErrInitialStateCompilation
	}
	value, ok := targetScopeForCode(data.Code)
	if !ok {
		return "", ErrInitialStateCompilation
	}
	compiler.targets[*id] = value
	return value, nil
}

func (compiler *initialMemberCompiler) changes(skillID snowflake.ID) ([]skillstatchange.Change, error) {
	if cached, found := compiler.statChanges[skillID]; found {
		return cached, nil
	}
	result := append([]skillstatchange.Change(nil), compiler.snapshot.statChanges[skillID]...)
	seen := make(map[snowflake.ID]struct{}, len(result))
	for _, value := range result {
		if value.SkillID != skillID || value.StatID == snowflake.ID(0) || value.ChangeValue == 0 || value.ChangeValue < -6 || value.ChangeValue > 6 {
			return nil, ErrInitialStateCompilation
		}
		if _, duplicate := seen[value.StatID]; duplicate {
			return nil, ErrInitialStateCompilation
		}
		seen[value.StatID] = struct{}{}
	}
	compiler.statChanges[skillID] = result
	return result, nil
}

func effectChance(detailChance, fallback *int32) uint8 {
	if detailChance != nil {
		return uint8(*detailChance)
	}
	if fallback != nil {
		return uint8(*fallback)
	}
	return 100
}

func targetScopeForCode(code string) (battleengine.SkillTargetScope, bool) {
	switch code {
	case "selected-target", "selected-pokemon", "selectedTarget":
		return battleengine.SkillTargetScopeSelectedTarget, true
	case "user":
		return battleengine.SkillTargetScopeSelf, true
	case "self":
		return battleengine.SkillTargetScopeSelf, true
	case "user-side-active":
		return battleengine.SkillTargetScopeUserSideActive, true
	case "all-opponents":
		return battleengine.SkillTargetScopeAllAdjacentOpponents, true
	case "all-adjacent-participants":
		return battleengine.SkillTargetScopeAllAdjacentParticipants, true
	case "random-opponent":
		return battleengine.SkillTargetScopeRandomAdjacentOpponent, true
	default:
		return "", false
	}
}
