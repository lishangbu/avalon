package battle

import (
	"math"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ability 读取并缓存一条必须启用的特性主资料。
//
// Team 只保存稳定 Identifier，不能因为当前还没有可执行详情就跳过主资料存在性和启用状态检查；否则禁用特性仍能被
// 新对局引用，并与实时资料维护边界相冲突。
func (compiler *initialMemberCompiler) ability(abilityID snowflake.ID) (ability.Ability, error) {
	if abilityID == snowflake.ID(0) {
		return ability.Ability{}, ErrInitialStateCompilation
	}
	if cached, found := compiler.abilities[abilityID]; found {
		return cached, nil
	}
	data, found := compiler.snapshot.abilities[abilityID]
	if !found || !data.Enabled || data.ID != abilityID {
		return ability.Ability{}, ErrInitialStateCompilation
	}
	compiler.abilities[abilityID] = data
	return data, nil
}

// abilityDetail 读取并缓存某一启用特性的可选详情。
//
// 特性可以暂时只有主资料而没有详情；nil 因而是合法的“尚无可执行附加规则”状态。存在详情时必须恰好一条且归属
// 当前特性，防止绕过唯一约束的数据库记录让 Battle 在多个规则定义中任选其一。
func (compiler *initialMemberCompiler) abilityDetail(abilityID snowflake.ID) (*abilitydetail.RuleSet, error) {
	if detail, found := compiler.abilityDetails[abilityID]; found {
		return detail, nil
	}
	detail, found := compiler.snapshot.abilityDetail[abilityID]
	if !found || detail == nil {
		compiler.abilityDetails[abilityID] = nil
		return nil, nil
	}
	if detail.AbilityID != abilityID {
		return nil, ErrInitialStateCompilation
	}
	compiler.abilityDetails[abilityID] = detail
	return detail, nil
}

// abilityWeatherDamageImmunities 编译一条特性详情的普通天气回合末伤害免疫规则。
//
// 缺少详情是允许的，表示该启用特性目前没有这类效果。存在详情时必须恰好一条、归属当前特性且符合封闭天气集合；
// 读取时绝不通过去重、排序或默认值修复损坏资料，否则同一 Team 的对局行为可能随数据库数组顺序变化。
func (compiler *initialMemberCompiler) abilityWeatherDamageImmunities(abilityID snowflake.ID) ([]battleengine.WeatherKind, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil {
		return []battleengine.WeatherKind{}, nil
	}
	if len(detail.WeatherDamageImmunities) > 4 {
		return nil, ErrInitialStateCompilation
	}
	result := make([]battleengine.WeatherKind, 0, len(detail.WeatherDamageImmunities))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(detail.WeatherDamageImmunities))
	for _, source := range detail.WeatherDamageImmunities {
		if _, duplicated := seen[source]; duplicated {
			return nil, ErrInitialStateCompilation
		}
		seen[source] = struct{}{}
		var weather battleengine.WeatherKind
		switch source {
		case abilitydetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case abilitydetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case abilitydetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case abilitydetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return nil, ErrInitialStateCompilation
		}
		result = append(result, weather)
	}
	return result, nil
}

// abilityWeatherEffectsSuppressed 编译一条特性详情的普通天气封锁开关。
//
// 该开关与天气伤害免疫共用同一条特性详情读取，但在 MemberSnapshot 中保留独立字段：免疫只跳过回合末环境
// 扣血，封锁则会暂停普通天气的多种可执行效果，二者不能通过一个泛型特性效果列表合并。
func (compiler *initialMemberCompiler) abilityWeatherEffectsSuppressed(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.WeatherEffectsSuppressed, nil
}

// abilityReactiveAbilityRules 读取并深复制回合末、受伤与倒下触发特性规则。
//
// Current Game Data 只保存强类型规则；Battle 在创建时取得独立副本，后续资料维护或测试夹具修改不能改变
// 已经冻结的 Participant 事实。缺少详情或规则时返回 nil，绝不从特性名称和说明文本推断行为。
func (compiler *initialMemberCompiler) abilityReactiveAbilityRules(abilityID snowflake.ID) (*battleengine.ReactiveAbilityRules, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.ReactiveAbilityRules == nil {
		return nil, nil
	}
	if err := battleengine.ValidateReactiveAbilityRules(detail.ReactiveAbilityRules); err != nil {
		return nil, ErrInitialStateCompilation
	}
	return battleengine.CloneReactiveAbilityRules(detail.ReactiveAbilityRules), nil
}

// abilityAccuracyRules 编译一条特性详情的全部命中相关规则。
//
// 缺少详情合法地表示没有任何命中规则；存在详情时每条分数都必须严格处于正整数范围，变化技能命中上限只能是
// 0 或 1 至 100。读取不会把损坏分数改为 1/1，避免实时资料异常被静默降级后改变已开始对局的结果。
func (compiler *initialMemberCompiler) abilityAccuracyRules(abilityID snowflake.ID) (abilityAccuracyRules, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return abilityAccuracyRules{}, err
	}
	if detail == nil {
		return abilityAccuracyRules{}, nil
	}
	compileMultiplier := func(value *abilitydetail.AccuracyMultiplier) (*battleengine.AccuracyMultiplier, error) {
		if value == nil {
			return nil, nil
		}
		if value.Numerator < 1 || value.Numerator > 65_535 || value.Denominator < 1 || value.Denominator > 65_535 {
			return nil, ErrInitialStateCompilation
		}
		return &battleengine.AccuracyMultiplier{Numerator: uint16(value.Numerator), Denominator: uint16(value.Denominator)}, nil
	}
	rules := abilityAccuracyRules{
		accuracyAlwaysHits:                          detail.AccuracyAlwaysHits,
		ignoreOpponentAccuracyStatStages:            detail.IgnoreOpponentAccuracyStatStages,
		criticalHitImmunity:                         detail.CriticalHitImmunity,
		skillRecoilDamageImmunity:                   detail.SkillRecoilDamageImmunity,
		indirectDamageImmunity:                      detail.IndirectDamageImmunity,
		contactDamageToAttackerDenominator:          uint16(detail.ContactDamageToAttackerDenominator),
		ignoreOpponentDamageStatStages:              detail.IgnoreOpponentDamageStatStages,
		ignoreTargetAbilityEffects:                  detail.IgnoreTargetAbilityEffects,
		surviveFatalDamageAtFullHP:                  detail.SurviveFatalDamageAtFullHP,
		opponentStatusSkillImmunity:                 detail.OpponentStatusSkillImmunity,
		nonSuperEffectiveDamageImmunity:             detail.NonSuperEffectiveDamageImmunity,
		multiHitMaximum:                             detail.MultiHitMaximum,
		damagingSkillSecondaryEffectImmunity:        detail.DamagingSkillSecondaryEffectImmunity,
		priorityMoveImmunityForSideEnabled:          detail.PriorityMoveImmunityForSideEnabled,
		priorityMoveImmunityForSideProtectsAllies:   detail.PriorityMoveImmunityForSideProtectsAllies,
		statusSkillMovesLastAndIgnoresTargetAbility: detail.StatusSkillMovesLastAndIgnoresTargetAbility,
		contactSkillProtectionBypass:                detail.ContactSkillProtectionBypass,
		contactSuppression:                          detail.ContactSuppression,
		receivedContactDamageHalved:                 detail.ReceivedContactDamageHalved,
		receivedFireDamageDoubled:                   detail.ReceivedFireDamageDoubled,
	}
	if detail.StatusSkillAccuracyCap < 0 || detail.StatusSkillAccuracyCap > 100 {
		return abilityAccuracyRules{}, ErrInitialStateCompilation
	}
	if detail.CriticalHitStageBoost < 0 || detail.CriticalHitStageBoost > 6 {
		return abilityAccuracyRules{}, ErrInitialStateCompilation
	}
	if detail.ContactDamageToAttackerDenominator < 0 || detail.ContactDamageToAttackerDenominator > 65535 {
		return abilityAccuracyRules{}, ErrInitialStateCompilation
	}
	rules.statusSkillAccuracyCap = uint8(detail.StatusSkillAccuracyCap)
	rules.criticalHitStageBoost = uint8(detail.CriticalHitStageBoost)
	if rules.accuracyMultiplier, err = compileMultiplier(detail.AccuracyMultiplier); err != nil {
		return abilityAccuracyRules{}, err
	}
	if rules.physicalSkillAccuracyMultiplier, err = compileMultiplier(detail.PhysicalSkillAccuracyMultiplier); err != nil {
		return abilityAccuracyRules{}, err
	}
	if rules.opponentAccuracySandstormMultiplier, err = compileMultiplier(detail.OpponentAccuracySandstormMultiplier); err != nil {
		return abilityAccuracyRules{}, err
	}
	if rules.opponentAccuracySnowMultiplier, err = compileMultiplier(detail.OpponentAccuracySnowMultiplier); err != nil {
		return abilityAccuracyRules{}, err
	}
	if rules.opponentAccuracyConfusionMultiplier, err = compileMultiplier(detail.OpponentAccuracyConfusionMultiplier); err != nil {
		return abilityAccuracyRules{}, err
	}
	return rules, nil
}

// abilityForcedSwitchImmunity 编译一条特性详情的强制换人免疫开关。
//
// 缺少详情或关闭开关都表示没有规则。该开关只由 Battle Engine 在技能或道具尝试替换当前目标时读取，不能用特性
// 名称、说明文本、替身状态或主动换人规则推断，以保证已开始对局始终使用启动时冻结的资料事实。
func (compiler *initialMemberCompiler) abilityForcedSwitchImmunity(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.ForcedSwitchImmunity, nil
}

// abilityOpponentSwitchRestriction 将特性详情中的敌方主动换人限制规则编译为纯引擎快照。
//
// 无规则时返回 nil；规则存在但没有属性、接地或同类规则条件时仍返回非 nil，表达无条件限制。若资料引用的属性
// 为空 Identifier 或不属于本次读取的启用属性集合，则拒绝启动新对局，避免运行中根据名称、数组顺序或失效资料猜测行为。
func (compiler *initialMemberCompiler) abilityOpponentSwitchRestriction(abilityID snowflake.ID) (*battleengine.OpponentSwitchRestriction, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.OpponentSwitchRestriction == nil {
		return nil, nil
	}
	source := detail.OpponentSwitchRestriction
	result := &battleengine.OpponentSwitchRestriction{
		RequiresGroundedTarget:   source.RequiresGroundedTarget,
		SameEffectGrantsImmunity: source.SameEffectGrantsImmunity,
	}
	if source.RequiredTargetElementID == nil {
		return result, nil
	}
	if *source.RequiredTargetElementID == snowflake.ID(0) {
		return nil, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*source.RequiredTargetElementID); !enabled {
		return nil, ErrInitialStateCompilation
	}
	result.RequiredTargetElementID = *source.RequiredTargetElementID
	return result, nil
}

// abilityDamageCrossedHalfHPForceSelfSwitch 编译一条特性详情的半血跨越强制自换开关。
//
// 缺少详情或关闭开关都表示没有规则。该事实在开局冻结，Battle Engine 只按成员实际本体生命变化判断阈值，
// 绝不根据特性名称、说明文本或替身生命值推断触发。
func (compiler *initialMemberCompiler) abilityDamageCrossedHalfHPForceSelfSwitch(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.DamageCrossedHalfHPForceSelfSwitch, nil
}

// abilitySwitchOutMajorStatusCure 编译一条特性详情的成功离场主要异常净化开关。
//
// 缺少详情或关闭开关都表示没有规则。是否属于成功离场由 Battle Engine 按换人路径和成员存活状态判断，Battle
// 只冻结资料事实，不能在这里把倒下补位或主动换人混为同一种资料语义。
func (compiler *initialMemberCompiler) abilitySwitchOutMajorStatusCure(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchOutMajorStatusCure, nil
}

// abilitySwitchOutHealDenominator 编译一条特性详情的成功离场固定比例回复分母。
//
// 0 表示没有规则；一旦声明则必须落在引擎与数据库共同支持的正整数范围内，不能把损坏资料降级为默认分母。
func (compiler *initialMemberCompiler) abilitySwitchOutHealDenominator(abilityID snowflake.ID) (uint16, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return 0, err
	}
	if detail.SwitchOutHealDenominator == 0 {
		return 0, nil
	}
	if detail.SwitchOutHealDenominator < 1 || detail.SwitchOutHealDenominator > math.MaxUint16 {
		return 0, ErrInitialStateCompilation
	}
	return uint16(detail.SwitchOutHealDenominator), nil
}

// abilityWeatherEndTurnHealing 编译一条特性详情的普通天气回合末固定比例回复规则。
//
// 缺少详情或未声明此规则都表示没有天气回复；一旦资料声明了规则，天气集合、正分母和每个封闭枚举都必须
// 完整有效。读取边界绝不以默认分母、去重或过滤未知天气修复数据库值，避免同一 Team 的对局行为被悄然改变。
func (compiler *initialMemberCompiler) abilityWeatherEndTurnHealing(abilityID snowflake.ID) (*battleengine.WeatherEndTurnHealing, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil || detail.WeatherEndTurnHeal == nil {
		return nil, err
	}
	source := detail.WeatherEndTurnHeal
	if len(source.Weathers) == 0 || len(source.Weathers) > 4 || source.HealDenominator < 1 || source.HealDenominator > 65_535 {
		return nil, ErrInitialStateCompilation
	}
	result := &battleengine.WeatherEndTurnHealing{
		Weathers:        make([]battleengine.WeatherKind, 0, len(source.Weathers)),
		HealDenominator: uint32(source.HealDenominator),
	}
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(source.Weathers))
	for _, sourceWeather := range source.Weathers {
		if _, duplicated := seen[sourceWeather]; duplicated {
			return nil, ErrInitialStateCompilation
		}
		seen[sourceWeather] = struct{}{}
		var weather battleengine.WeatherKind
		switch sourceWeather {
		case abilitydetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case abilitydetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case abilitydetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case abilitydetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return nil, ErrInitialStateCompilation
		}
		result.Weathers = append(result.Weathers, weather)
	}
	return result, nil
}

// abilitySwitchInWeather 编译一条特性详情的入场普通天气规则。
//
// 缺少详情或规则表示没有入场普通天气；普通天气与强天气不能在同一特性详情并存，未知枚举和损坏持续回合
// 必须阻止新对局，不能退化为技能天气或未声明规则。
func (compiler *initialMemberCompiler) abilitySwitchInWeather(abilityID snowflake.ID) (*battleengine.SwitchInWeather, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.SwitchInWeather == nil {
		return nil, nil
	}
	if detail.SwitchInStrongWeather != nil || detail.SwitchInWeather.TurnsRemaining < 1 || detail.SwitchInWeather.TurnsRemaining > 100 {
		return nil, ErrInitialStateCompilation
	}
	var weather battleengine.WeatherKind
	switch detail.SwitchInWeather.Weather {
	case abilitydetail.WeatherKindSun:
		weather = battleengine.WeatherKindSun
	case abilitydetail.WeatherKindRain:
		weather = battleengine.WeatherKindRain
	case abilitydetail.WeatherKindSandstorm:
		weather = battleengine.WeatherKindSandstorm
	case abilitydetail.WeatherKindSnow:
		weather = battleengine.WeatherKindSnow
	default:
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{
		Kind: weather, TurnsRemaining: uint8(detail.SwitchInWeather.TurnsRemaining),
	}}, nil
}

// abilitySwitchInTerrain 编译一条特性详情的入场普通场地规则。
//
// 缺少详情或规则表示没有入场场地；场地可与天气、强天气同时声明，但场地种类和持续回合必须完整有效。
// 读取边界不会以默认场地或默认持续时间修复损坏资料，避免相同冻结 Team 因资料异常得到不确定的战斗环境。
func (compiler *initialMemberCompiler) abilitySwitchInTerrain(abilityID snowflake.ID) (*battleengine.SwitchInTerrain, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.SwitchInTerrain == nil {
		return nil, nil
	}
	if detail.SwitchInTerrain.TurnsRemaining < 1 || detail.SwitchInTerrain.TurnsRemaining > 100 {
		return nil, ErrInitialStateCompilation
	}
	var terrain battleengine.TerrainKind
	switch detail.SwitchInTerrain.Terrain {
	case abilitydetail.TerrainKindElectric:
		terrain = battleengine.TerrainKindElectric
	case abilitydetail.TerrainKindGrassy:
		terrain = battleengine.TerrainKindGrassy
	case abilitydetail.TerrainKindMisty:
		terrain = battleengine.TerrainKindMisty
	case abilitydetail.TerrainKindPsychic:
		terrain = battleengine.TerrainKindPsychic
	default:
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.SwitchInTerrain{Effect: battleengine.TerrainEffect{
		Kind: terrain, TurnsRemaining: uint8(detail.SwitchInTerrain.TurnsRemaining),
	}}, nil
}

// abilitySwitchInStatStageChange 编译一条特性详情的入场能力阶级变化规则。
//
// 缺少详情或规则表示没有入场能力阶级变化；存在规则时必须显式引用一项启用的能力资料，并映射到引擎封闭
// Stat。未知目标、损坏 Identifier、无效阶段变化或未启用能力资料都会阻止新对局，不能退化为默认目标。
func (compiler *initialMemberCompiler) abilitySwitchInStatStageChange(abilityID snowflake.ID) (*battleengine.SwitchInStatStageChange, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.SwitchInStatStageChange == nil {
		return nil, nil
	}
	source := detail.SwitchInStatStageChange
	if source.StatID == snowflake.ID(0) || source.StageDelta < -6 || source.StageDelta > 6 || source.StageDelta == 0 {
		return nil, ErrInitialStateCompilation
	}
	var target battleengine.SwitchInStatStageTarget
	switch source.Target {
	case abilitydetail.SwitchInStatStageTargetSelf:
		target = battleengine.SwitchInStatStageTargetSelf
	case abilitydetail.SwitchInStatStageTargetOpponents:
		target = battleengine.SwitchInStatStageTargetOpponents
	default:
		return nil, ErrInitialStateCompilation
	}
	statData, err := compiler.stat(source.StatID)
	if err != nil {
		return nil, err
	}
	statValue, valid := battleStatForCode(statData.Code)
	if !valid {
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.SwitchInStatStageChange{
		Target: target, Stat: statValue, StageDelta: int8(source.StageDelta),
	}, nil
}

// abilityTerastallizationStatStageChange 编译一条特性详情的太晶化能力阶级变化规则。
//
// 缺少详情或规则表示没有太晶化能力变化。存在规则时，能力资料必须启用并映射到 Battle Engine 支持的封闭 Stat；
// 不允许把未知、禁用或损坏资料降级为默认能力项，以免已开始对局之外的新 Battle 产生不可审计的行为。
func (compiler *initialMemberCompiler) abilityTerastallizationStatStageChange(abilityID snowflake.ID) (*battleengine.TerastallizationStatStageChange, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.TerastallizationStatStageChange == nil {
		return nil, nil
	}
	source := detail.TerastallizationStatStageChange
	if source.StatID == snowflake.ID(0) || source.StageDelta < -6 || source.StageDelta > 6 || source.StageDelta == 0 {
		return nil, ErrInitialStateCompilation
	}
	statData, err := compiler.stat(source.StatID)
	if err != nil {
		return nil, err
	}
	statValue, valid := battleStatForCode(statData.Code)
	if !valid {
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.TerastallizationStatStageChange{Stat: statValue, StageDelta: int8(source.StageDelta)}, nil
}

// abilityTerastallizationEnvironmentClear 编译一条特性详情的太晶化普通环境清除开关。
//
// 缺少特性详情表示没有清场规则；开关本身不代表强天气清除，运行期仍由 Battle Engine 按明确的环境分类处理。
func (compiler *initialMemberCompiler) abilityTerastallizationEnvironmentClear(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return false, err
	}
	return detail != nil && detail.TerastallizationEnvironmentClear, nil
}

// abilitySwitchInAllyHeal 编译一条特性详情的入场同侧回复规则。
//
// 缺少详情或规则表示没有入场回复；分母必须处于封闭范围，不能退化为默认比例。引擎会根据每名同侧其它
// 当前上场成员自身的最大生命计算实际回复，并跳过倒下、满生命或后备成员。
func (compiler *initialMemberCompiler) abilitySwitchInAllyHeal(abilityID snowflake.ID) (*battleengine.SwitchInAllyHeal, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.SwitchInAllyHeal == nil {
		return nil, nil
	}
	if detail.SwitchInAllyHeal.HealDenominator < 1 || detail.SwitchInAllyHeal.HealDenominator > 65_535 {
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.SwitchInAllyHeal{HealDenominator: uint32(detail.SwitchInAllyHeal.HealDenominator)}, nil
}

// abilitySwitchInOpponentDefenseComparisonBoost 编译特性详情的入场对手防御比较强化开关。
//
// 缺少详情或关闭开关都表示没有规则；开启时引擎会在实际入场后读取已冻结的当前场上对手基础防御，绝不在
// 回合内反查实时资料或依赖特性名称。
func (compiler *initialMemberCompiler) abilitySwitchInOpponentDefenseComparisonBoost(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInOpponentDefenseComparisonBoost, nil
}

// abilitySwitchInAllyStatStageCopy 编译特性详情的入场同侧能力阶级复制开关。
//
// 缺少详情或关闭开关都表示没有规则；开启时引擎会在实际入场后仅从本场已冻结的其它存活上场队友复制七项能力阶级，
// 绝不在回合内反查实时资料或根据特性名称推断行为。
func (compiler *initialMemberCompiler) abilitySwitchInAllyStatStageCopy(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInAllyStatStageCopy, nil
}

// abilitySwitchInAllyStatStageReset 编译特性详情的入场同侧能力阶级重置开关。
//
// 缺少详情或关闭开关都表示没有规则；开启时引擎只会重置本场已冻结的其它当前上场队友，绝不在回合内反查
// 实时资料或通过特性名称推断目标集合。
func (compiler *initialMemberCompiler) abilitySwitchInAllyStatStageReset(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInAllyStatStageReset, nil
}

// abilitySwitchInClearAllSideDamageReductions 编译特性详情的入场全阵营减伤屏障清除开关。
//
// 缺少详情或关闭开关都表示没有规则；开启时引擎只清除双方冻结侧状态中的三种屏障，绝不依赖特性名称或技能资料推断。
func (compiler *initialMemberCompiler) abilitySwitchInClearAllSideDamageReductions(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInClearAllSideDamageReductions, nil
}

// abilitySwitchInCopyOpponentAbility 编译特性详情的入场复制对手特性开关。
//
// 缺少详情或关闭开关都表示没有规则；开启时引擎只从本场已冻结的存活上场对手选择来源，并复制其当前
// 强类型特性规则，绝不在回合内反查实时资料或根据特性名称推断行为。
func (compiler *initialMemberCompiler) abilitySwitchInCopyOpponentAbility(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInCopyOpponentAbility, nil
}

// abilitySwitchInRevealOpponentHeldItems 编译特性详情的入场公开对手道具开关。
//
// 缺少详情或关闭开关都表示没有规则；开启时引擎只公开本场已冻结、存活且持有道具的当前上场对手，
// 绝不在回合内反查实时资料或根据特性名称推断行为。
func (compiler *initialMemberCompiler) abilitySwitchInRevealOpponentHeldItems(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInRevealOpponentHeldItems, nil
}

// abilitySwitchInRevealOpponentHighestPowerSkill 编译特性详情的入场公开对手最高威力技能开关。
//
// 缺少详情或关闭开关都表示没有规则；开启后引擎只在本场冻结的存活上场对手技能中选择最高基础威力，
// 并在威力相同情况下使用 SkillID 的稳定顺序，绝不按资料名称或数据库返回顺序推断。
func (compiler *initialMemberCompiler) abilitySwitchInRevealOpponentHighestPowerSkill(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInRevealOpponentHighestPowerSkill, nil
}

// abilitySwitchInTransformIntoOpponent 编译特性详情的入场复制对手战斗画像开关。
//
// 缺少详情或关闭开关都表示没有规则；开启后引擎只从本场冻结的存活上场对手选择来源，并在成员离场时
// 还原触发者原始画像，绝不在回合内反查实时资料。
func (compiler *initialMemberCompiler) abilitySwitchInTransformIntoOpponent(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInTransformIntoOpponent, nil
}

// abilitySwitchInDetectDangerousOpponentSkill 编译特性详情的入场危险技能侦测开关。
func (compiler *initialMemberCompiler) abilitySwitchInDetectDangerousOpponentSkill(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInDetectDangerousOpponentSkill, nil
}

// abilitySwitchInDisguiseAsLastHealthyAlly 编译特性详情的入场视觉伪装开关。
func (compiler *initialMemberCompiler) abilitySwitchInDisguiseAsLastHealthyAlly(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInDisguiseAsLastHealthyAlly, nil
}

// abilitySwitchInHeldItemElementIdentity 编译特性详情的入场携带道具属性身份替换开关。
//
// 缺少详情或关闭开关都表示没有规则。引擎只读取该冻结布尔值和同场冻结的道具属性 Identifier，绝不再根据
// 特性名称、道具名称或说明文本推断属性替换行为。
func (compiler *initialMemberCompiler) abilitySwitchInHeldItemElementIdentity(abilityID snowflake.ID) (bool, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return false, err
	}
	return detail.SwitchInHeldItemElementIdentity, nil
}

// abilityEnvironmentHighestStatMultiplier 将特性详情中的环境最高原始能力强化规则编译为纯引擎快照。
//
// 资料层使用可空指针区分“天气条件”“场地条件”和“没有规则”；引擎则使用封闭枚举的零值表达未选择条件。
// 因此这里必须拒绝双条件、空条件和未知枚举，不能将损坏资料降级为没有强化，以免新对局静默改变规则。
func (compiler *initialMemberCompiler) abilityEnvironmentHighestStatMultiplier(abilityID snowflake.ID) (*battleengine.EnvironmentHighestStatMultiplier, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.EnvironmentHighestStatMultiplier == nil {
		return nil, nil
	}
	source := detail.EnvironmentHighestStatMultiplier
	if (source.RequiredWeather == nil) == (source.RequiredTerrain == nil) {
		return nil, ErrInitialStateCompilation
	}
	result := &battleengine.EnvironmentHighestStatMultiplier{}
	if source.RequiredWeather != nil {
		switch *source.RequiredWeather {
		case abilitydetail.WeatherKindSun:
			result.RequiredWeather = battleengine.WeatherKindSun
		case abilitydetail.WeatherKindRain:
			result.RequiredWeather = battleengine.WeatherKindRain
		case abilitydetail.WeatherKindSandstorm:
			result.RequiredWeather = battleengine.WeatherKindSandstorm
		case abilitydetail.WeatherKindSnow:
			result.RequiredWeather = battleengine.WeatherKindSnow
		default:
			return nil, ErrInitialStateCompilation
		}
		return result, nil
	}
	switch *source.RequiredTerrain {
	case abilitydetail.TerrainKindElectric:
		result.RequiredTerrain = battleengine.TerrainKindElectric
	case abilitydetail.TerrainKindGrassy:
		result.RequiredTerrain = battleengine.TerrainKindGrassy
	case abilitydetail.TerrainKindMisty:
		result.RequiredTerrain = battleengine.TerrainKindMisty
	case abilitydetail.TerrainKindPsychic:
		result.RequiredTerrain = battleengine.TerrainKindPsychic
	default:
		return nil, ErrInitialStateCompilation
	}
	return result, nil
}

// abilitySwitchInFormChange 编译特性详情的入场确定形态切换规则。
//
// 资料层只保存精灵 Identifier 和生命补齐语义；属性、数值和体重必须由同一个编译器随后冻结为 FormProfile，避免
// 引擎在实际换入时再查询可被维护修改的资料。
func (compiler *initialMemberCompiler) abilitySwitchInFormChange(abilityID snowflake.ID) (*battleengine.SwitchInFormChange, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.SwitchInFormChange == nil {
		return nil, nil
	}
	source := detail.SwitchInFormChange
	if source.BaseCreatureID == snowflake.ID(0) || source.AlternateCreatureID == snowflake.ID(0) ||
		source.BaseCreatureID == source.AlternateCreatureID {
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.SwitchInFormChange{
		BaseCreatureID: source.BaseCreatureID, AlternateCreatureID: source.AlternateCreatureID,
		AddsMaximumHPDifference: source.AddsMaximumHPDifference,
	}, nil
}

// abilitySwitchOutFormChange 编译特性详情的成功离场确定形态切换规则。
//
// 资料层只保存两个精灵 Identifier；属性、数值和体重必须由同一个编译器随后冻结为 FormProfile，避免引擎在实际
// 换出时重新查询可被维护修改的实时资料。
func (compiler *initialMemberCompiler) abilitySwitchOutFormChange(abilityID snowflake.ID) (*battleengine.SwitchOutFormChange, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.SwitchOutFormChange == nil {
		return nil, nil
	}
	source := detail.SwitchOutFormChange
	if source.BaseCreatureID == snowflake.ID(0) || source.AlternateCreatureID == snowflake.ID(0) ||
		source.BaseCreatureID == source.AlternateCreatureID {
		return nil, ErrInitialStateCompilation
	}
	return &battleengine.SwitchOutFormChange{
		BaseCreatureID: source.BaseCreatureID, AlternateCreatureID: source.AlternateCreatureID,
	}, nil
}

// abilityWeatherFormChange 编译特性详情的有效普通天气形态规则。
//
// 每种天气必须恰好映射一个启用目标精灵；未知天气、重复映射或缺失默认形态都会拒绝新 Battle，不能在运行期
// 通过数组顺序或默认值决定形态。
func (compiler *initialMemberCompiler) abilityWeatherFormChange(abilityID snowflake.ID) (*battleengine.WeatherFormChange, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil || detail.WeatherFormChange == nil {
		return nil, nil
	}
	source := detail.WeatherFormChange
	if source.DefaultCreatureID == snowflake.ID(0) || len(source.Targets) < 1 || len(source.Targets) > 4 {
		return nil, ErrInitialStateCompilation
	}
	result := &battleengine.WeatherFormChange{
		DefaultCreatureID: source.DefaultCreatureID,
		Targets:           make([]battleengine.WeatherFormTarget, 0, len(source.Targets)),
	}
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(source.Targets))
	for _, target := range source.Targets {
		if target.CreatureID == snowflake.ID(0) {
			return nil, ErrInitialStateCompilation
		}
		if _, duplicated := seen[target.Weather]; duplicated {
			return nil, ErrInitialStateCompilation
		}
		seen[target.Weather] = struct{}{}
		var weather battleengine.WeatherKind
		switch target.Weather {
		case abilitydetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case abilitydetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case abilitydetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case abilitydetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return nil, ErrInitialStateCompilation
		}
		result.Targets = append(result.Targets, battleengine.WeatherFormTarget{
			Weather: weather, CreatureID: target.CreatureID,
		})
	}
	return result, nil
}

// abilitySwitchInStrongWeather 编译一条特性详情的入场强天气规则。
//
// 缺少详情或规则表示没有强天气；未知枚举必须阻止新对局，不能被降级为普通天气、无限天气或未声明规则。
func (compiler *initialMemberCompiler) abilitySwitchInStrongWeather(abilityID snowflake.ID) (battleengine.StrongWeatherKind, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return "", err
	}
	if detail == nil || detail.SwitchInStrongWeather == nil {
		return "", nil
	}
	if detail.SwitchInWeather != nil {
		return "", ErrInitialStateCompilation
	}
	switch detail.SwitchInStrongWeather.Weather {
	case abilitydetail.StrongWeatherKindHarshSunlight:
		return battleengine.StrongWeatherKindHarshSunlight, nil
	case abilitydetail.StrongWeatherKindHeavyRain:
		return battleengine.StrongWeatherKindHeavyRain, nil
	case abilitydetail.StrongWeatherKindStrongWinds:
		return battleengine.StrongWeatherKindStrongWinds, nil
	default:
		return "", ErrInitialStateCompilation
	}
}

// abilityWeatherSpeedMultipliers 编译一条特性详情的普通天气行动速度整数分数倍率集合。
//
// 缺少详情表示没有倍率；存在资料时每种天气只能有一项，且两个分数参数必须为正。编译边界不会将浮点倍率、
// 重复天气或未知枚举归一化，以免数组顺序或运行平台影响同优先度行动顺序。
func (compiler *initialMemberCompiler) abilityWeatherSpeedMultipliers(abilityID snowflake.ID) ([]battleengine.WeatherSpeedMultiplier, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil {
		return nil, err
	}
	if detail == nil {
		return []battleengine.WeatherSpeedMultiplier{}, nil
	}
	if len(detail.WeatherSpeedMultipliers) > 4 {
		return nil, ErrInitialStateCompilation
	}
	result := make([]battleengine.WeatherSpeedMultiplier, 0, len(detail.WeatherSpeedMultipliers))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(detail.WeatherSpeedMultipliers))
	for _, source := range detail.WeatherSpeedMultipliers {
		if source.Numerator < 1 || source.Numerator > 65_535 || source.Denominator < 1 || source.Denominator > 65_535 {
			return nil, ErrInitialStateCompilation
		}
		if _, duplicated := seen[source.Weather]; duplicated {
			return nil, ErrInitialStateCompilation
		}
		seen[source.Weather] = struct{}{}
		var weather battleengine.WeatherKind
		switch source.Weather {
		case abilitydetail.WeatherKindSun:
			weather = battleengine.WeatherKindSun
		case abilitydetail.WeatherKindRain:
			weather = battleengine.WeatherKindRain
		case abilitydetail.WeatherKindSandstorm:
			weather = battleengine.WeatherKindSandstorm
		case abilitydetail.WeatherKindSnow:
			weather = battleengine.WeatherKindSnow
		default:
			return nil, ErrInitialStateCompilation
		}
		result = append(result, battleengine.WeatherSpeedMultiplier{
			Weather: weather, Numerator: uint32(source.Numerator), Denominator: uint32(source.Denominator),
		})
	}
	return result, nil
}
