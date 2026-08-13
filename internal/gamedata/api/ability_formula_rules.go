package api

import (
	"strings"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
)

// formulaAbilityProtoInput 保存一次创建或完整更新请求中规则 112—131 的独立运输消息。
type formulaAbilityProtoInput struct {
	targetGenderDamageMultiplier               *domainv1.GameAbilityTargetGenderDamageMultiplier
	punchBasedSkillDamageBoost                 *domainv1.GameAbilityPunchBasedSkillDamageBoost
	slicingBasedSkillDamageBoost               *domainv1.GameAbilitySlicingBasedSkillDamageBoost
	soundBasedSkillDamageBoost                 *domainv1.GameAbilitySoundBasedSkillDamageBoost
	pulseBasedSkillDamageBoost                 *domainv1.GameAbilityPulseBasedSkillDamageBoost
	biteBasedSkillDamageBoost                  *domainv1.GameAbilityBiteBasedSkillDamageBoost
	secondaryEffectsSuppressedDamageBoost      *domainv1.GameAbilitySecondaryEffectsSuppressedDamageBoost
	soundBasedSkillDamageReduction             *domainv1.GameAbilitySoundBasedSkillDamageReduction
	superEffectiveDamageReduction              *domainv1.GameAbilitySuperEffectiveDamageReduction
	fullHPDamageReduction                      *domainv1.GameAbilityFullHPDamageReduction
	damageClassDamageReduction                 *domainv1.GameAbilityDamageClassDamageReduction
	elementSkillDamageReduction                *domainv1.GameAbilityElementSkillDamageReduction
	contactBasedSkillDamageReduction           *domainv1.GameAbilityContactBasedSkillDamageReduction
	attackingStatMultiplier                    *domainv1.GameAbilityAttackingStatMultiplier
	opponentAttackingStatMultiplier            *domainv1.GameAbilityOpponentAttackingStatMultiplier
	defendingStatMultiplier                    *domainv1.GameAbilityDefendingStatMultiplier
	opponentDefendingStatMultiplier            *domainv1.GameAbilityOpponentDefendingStatMultiplier
	allySkillDamageBoost                       *domainv1.GameAbilityAllySkillDamageBoost
	allyReceivedDamageReduction                *domainv1.GameAbilityAllyReceivedDamageReduction
	allyAbilityGroupCode                       string
	allyAbilityPresenceAttackingStatMultiplier *domainv1.GameAbilityAllyAbilityPresenceAttackingStatMultiplier
}

// formulaAbilityCreateInput 从创建请求逐项读取规则 112—131 的独立字段。
func formulaAbilityCreateInput(body *domainv1.GameAbilityRuleGroup) formulaAbilityProtoInput {
	return formulaAbilityProtoInput{
		targetGenderDamageMultiplier: body.GetTargetGenderDamageMultiplier(), punchBasedSkillDamageBoost: body.GetPunchBasedSkillDamageBoost(),
		slicingBasedSkillDamageBoost: body.GetSlicingBasedSkillDamageBoost(), soundBasedSkillDamageBoost: body.GetSoundBasedSkillDamageBoost(),
		pulseBasedSkillDamageBoost: body.GetPulseBasedSkillDamageBoost(), biteBasedSkillDamageBoost: body.GetBiteBasedSkillDamageBoost(),
		secondaryEffectsSuppressedDamageBoost: body.GetSecondaryEffectsSuppressedDamageBoost(), soundBasedSkillDamageReduction: body.GetSoundBasedSkillDamageReduction(),
		superEffectiveDamageReduction: body.GetSuperEffectiveDamageReduction(), fullHPDamageReduction: body.GetFullHpDamageReduction(),
		damageClassDamageReduction: body.GetDamageClassDamageReduction(), elementSkillDamageReduction: body.GetElementSkillDamageReduction(),
		contactBasedSkillDamageReduction: body.GetContactBasedSkillDamageReduction(), attackingStatMultiplier: body.GetAttackingStatMultiplier(),
		opponentAttackingStatMultiplier: body.GetOpponentAttackingStatMultiplier(), defendingStatMultiplier: body.GetDefendingStatMultiplier(),
		opponentDefendingStatMultiplier: body.GetOpponentDefendingStatMultiplier(), allySkillDamageBoost: body.GetAllySkillDamageBoost(),
		allyReceivedDamageReduction: body.GetAllyReceivedDamageReduction(), allyAbilityGroupCode: body.GetAllyAbilityGroupCode(),
		allyAbilityPresenceAttackingStatMultiplier: body.GetAllyAbilityPresenceAttackingStatMultiplier(),
	}
}

// formulaAbilityRulesFromMessages 将独立运输消息转换为 Current Game Data 的强类型规则字段。
func formulaAbilityRulesFromMessages(input formulaAbilityProtoInput) (abilitydetail.OptionalValues, error) {
	gender, err := targetGenderMultiplierFromMessage(input.targetGenderDamageMultiplier)
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	result := abilitydetail.OptionalValues{TargetGenderDamageMultiplier: gender, AllyAbilityGroupCode: strings.TrimSpace(input.allyAbilityGroupCode)}
	if result.PunchBasedSkillDamageBoost, err = simpleFormulaRule(input.punchBasedSkillDamageBoost, func(n, d uint16) *abilitydetail.PunchBasedSkillDamageBoost {
		return &abilitydetail.PunchBasedSkillDamageBoost{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.SlicingBasedSkillDamageBoost, err = simpleFormulaRule(input.slicingBasedSkillDamageBoost, func(n, d uint16) *abilitydetail.SlicingBasedSkillDamageBoost {
		return &abilitydetail.SlicingBasedSkillDamageBoost{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.SoundBasedSkillDamageBoost, err = simpleFormulaRule(input.soundBasedSkillDamageBoost, func(n, d uint16) *abilitydetail.SoundBasedSkillDamageBoost {
		return &abilitydetail.SoundBasedSkillDamageBoost{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.PulseBasedSkillDamageBoost, err = simpleFormulaRule(input.pulseBasedSkillDamageBoost, func(n, d uint16) *abilitydetail.PulseBasedSkillDamageBoost {
		return &abilitydetail.PulseBasedSkillDamageBoost{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.BiteBasedSkillDamageBoost, err = simpleFormulaRule(input.biteBasedSkillDamageBoost, func(n, d uint16) *abilitydetail.BiteBasedSkillDamageBoost {
		return &abilitydetail.BiteBasedSkillDamageBoost{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.SecondaryEffectsSuppressedDamageBoost, err = simpleFormulaRule(input.secondaryEffectsSuppressedDamageBoost, func(n, d uint16) *abilitydetail.SecondaryEffectsSuppressedDamageBoost {
		return &abilitydetail.SecondaryEffectsSuppressedDamageBoost{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.SoundBasedSkillDamageReduction, err = simpleFormulaRule(input.soundBasedSkillDamageReduction, func(n, d uint16) *abilitydetail.SoundBasedSkillDamageReduction {
		return &abilitydetail.SoundBasedSkillDamageReduction{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.SuperEffectiveDamageReduction, err = simpleFormulaRule(input.superEffectiveDamageReduction, func(n, d uint16) *abilitydetail.SuperEffectiveDamageReduction {
		return &abilitydetail.SuperEffectiveDamageReduction{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.FullHPDamageReduction, err = simpleFormulaRule(input.fullHPDamageReduction, func(n, d uint16) *abilitydetail.FullHPDamageReduction {
		return &abilitydetail.FullHPDamageReduction{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.ContactBasedSkillDamageReduction, err = simpleFormulaRule(input.contactBasedSkillDamageReduction, func(n, d uint16) *abilitydetail.ContactBasedSkillDamageReduction {
		return &abilitydetail.ContactBasedSkillDamageReduction{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.AllyReceivedDamageReduction, err = simpleFormulaRule(input.allyReceivedDamageReduction, func(n, d uint16) *abilitydetail.AllyReceivedDamageReduction {
		return &abilitydetail.AllyReceivedDamageReduction{Numerator: n, Denominator: d}
	}); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.DamageClassDamageReduction, err = damageClassReductionFromMessage(input.damageClassDamageReduction); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.ElementSkillDamageReduction, err = elementReductionFromMessage(input.elementSkillDamageReduction); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.AttackingStatMultiplier, err = attackingStatMultiplierFromMessage(input.attackingStatMultiplier); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.OpponentAttackingStatMultiplier, err = opponentAttackingStatMultiplierFromMessage(input.opponentAttackingStatMultiplier); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.DefendingStatMultiplier, err = defendingStatMultiplierFromMessage(input.defendingStatMultiplier); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.OpponentDefendingStatMultiplier, err = opponentDefendingStatMultiplierFromMessage(input.opponentDefendingStatMultiplier); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.AllySkillDamageBoost, err = allySkillDamageBoostFromMessage(input.allySkillDamageBoost); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if result.AllyAbilityPresenceAttackingStatMultiplier, err = allyPresenceMultiplierFromMessage(input.allyAbilityPresenceAttackingStatMultiplier); err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	// 逐字段运输转换完成后复用领域的最终组合约束，确保互助组长度、生命阈值配对以及条件方向等跨字段
	// 关系不会因为调用方绕过 Proto 中间件而进入资料服务。
	if !result.ValidForBattle() {
		return abilitydetail.OptionalValues{}, kratoserrors.BadRequest("INVALID_ABILITY_FORMULA_RULES", "特性公式规则组合无效")
	}
	return result, nil
}

type formulaFractionMessage interface {
	GetNumerator() int32
	GetDenominator() int32
}

func simpleFormulaRule[M interface {
	formulaFractionMessage
	comparable
}, T any](message M, build func(uint16, uint16) *T) (*T, error) {
	var zero M
	if message == zero {
		return nil, nil
	}
	numerator, denominator, err := formulaFraction(message.GetNumerator(), message.GetDenominator())
	if err != nil {
		return nil, err
	}
	return build(numerator, denominator), nil
}

func formulaFraction(numerator, denominator int32) (uint16, uint16, error) {
	if numerator < 1 || numerator > 65_535 || denominator < 1 || denominator > 65_535 {
		return 0, 0, kratoserrors.BadRequest("INVALID_ABILITY_FORMULA_MULTIPLIER", "特性公式倍率分数无效")
	}
	return uint16(numerator), uint16(denominator), nil
}

func targetGenderMultiplierFromMessage(value *domainv1.GameAbilityTargetGenderDamageMultiplier) (*abilitydetail.TargetGenderDamageMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	sn, sd, err := formulaFraction(value.GetSameGenderNumerator(), value.GetSameGenderDenominator())
	if err != nil {
		return nil, err
	}
	on, od, err := formulaFraction(value.GetOppositeGenderNumerator(), value.GetOppositeGenderDenominator())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.TargetGenderDamageMultiplier{SameGenderNumerator: sn, SameGenderDenominator: sd, OppositeGenderNumerator: on, OppositeGenderDenominator: od}, nil
}

func damageClassReductionFromMessage(value *domainv1.GameAbilityDamageClassDamageReduction) (*abilitydetail.DamageClassDamageReduction, error) {
	if value == nil {
		return nil, nil
	}
	classes, err := formulaDamageClasses(value.GetDamageClasses())
	if err != nil {
		return nil, err
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.DamageClassDamageReduction{DamageClasses: classes, Numerator: n, Denominator: d}, nil
}

func allySkillDamageBoostFromMessage(value *domainv1.GameAbilityAllySkillDamageBoost) (*abilitydetail.AllySkillDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	classes, err := formulaDamageClasses(value.GetDamageClasses())
	if err != nil {
		return nil, err
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.AllySkillDamageBoost{DamageClasses: classes, Numerator: n, Denominator: d}, nil
}

func formulaDamageClasses(values []string) ([]battleengine.DamageClass, error) {
	if len(values) == 0 || len(values) > 2 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_DAMAGE_CLASSES", "特性伤害分类集合无效")
	}
	result := make([]battleengine.DamageClass, 0, len(values))
	seen := map[battleengine.DamageClass]struct{}{}
	for _, raw := range values {
		class := battleengine.DamageClass(raw)
		if class != battleengine.DamageClassPhysical && class != battleengine.DamageClassSpecial {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_DAMAGE_CLASSES", "特性伤害分类无效")
		}
		if _, duplicate := seen[class]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_DAMAGE_CLASSES", "特性伤害分类重复")
		}
		seen[class] = struct{}{}
		result = append(result, class)
	}
	return result, nil
}

func elementReductionFromMessage(value *domainv1.GameAbilityElementSkillDamageReduction) (*abilitydetail.ElementSkillDamageReduction, error) {
	if value == nil {
		return nil, nil
	}
	if len(value.GetElementIds()) == 0 || len(value.GetElementIds()) > 32 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ELEMENT_REDUCTION", "特性属性集合无效")
	}
	elements := make([]battleengine.Identifier, 0, len(value.GetElementIds()))
	seen := map[battleengine.Identifier]struct{}{}
	for _, raw := range value.GetElementIds() {
		id, err := gameDataIdentifier(raw, "INVALID_ABILITY_ELEMENT_REDUCTION")
		if err != nil {
			return nil, err
		}
		if _, duplicate := seen[id]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_ELEMENT_REDUCTION", "特性属性重复")
		}
		seen[id] = struct{}{}
		elements = append(elements, id)
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.ElementSkillDamageReduction{ElementIDs: elements, Numerator: n, Denominator: d}, nil
}

func attackingStatMultiplierFromMessage(value *domainv1.GameAbilityAttackingStatMultiplier) (*abilitydetail.AttackingStatMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	stat, ok := formulaStat(value.GetStat(), true)
	if !ok {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ATTACKING_STAT", "特性攻击能力项无效")
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	weather, err := formulaWeather(value.GetRequiredWeather())
	if err != nil {
		return nil, err
	}
	terrain, err := formulaTerrain(value.GetRequiredTerrain())
	if err != nil {
		return nil, err
	}
	statuses, err := formulaStatuses(value.GetRequiredMajorStatuses())
	if err != nil {
		return nil, err
	}
	maximumHPNumerator, maximumHPDenominator := value.GetMaximumHpNumerator(), value.GetMaximumHpDenominator()
	if maximumHPNumerator < 0 || maximumHPNumerator > 65_535 || maximumHPDenominator < 0 || maximumHPDenominator > 65_535 ||
		(maximumHPNumerator == 0) != (maximumHPDenominator == 0) ||
		maximumHPDenominator != 0 && maximumHPNumerator > maximumHPDenominator {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ATTACKING_HP", "特性生命阈值无效")
	}
	if value.GetIgnoreBurnAttackReduction() && stat != battleengine.StatAttack {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ATTACKING_BURN_REDUCTION", "仅攻击能力倍率可以忽略灼伤减半")
	}
	return &abilitydetail.AttackingStatMultiplier{Stat: stat, Numerator: n, Denominator: d, RequiredWeather: weather, RequiredTerrain: terrain, RequiresMajorStatus: value.GetRequiresMajorStatus(), RequiredMajorStatuses: statuses, MaximumHPNumerator: uint16(maximumHPNumerator), MaximumHPDenominator: uint16(maximumHPDenominator), IgnoreBurnAttackReduction: value.GetIgnoreBurnAttackReduction()}, nil
}

func opponentAttackingStatMultiplierFromMessage(value *domainv1.GameAbilityOpponentAttackingStatMultiplier) (*abilitydetail.OpponentAttackingStatMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	stat, ok := formulaStat(value.GetStat(), true)
	if !ok {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_OPPONENT_ATTACKING_STAT", "特性攻击能力项无效")
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.OpponentAttackingStatMultiplier{Stat: stat, Numerator: n, Denominator: d}, nil
}

func defendingStatMultiplierFromMessage(value *domainv1.GameAbilityDefendingStatMultiplier) (*abilitydetail.DefendingStatMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	stat, ok := formulaStat(value.GetStat(), false)
	if !ok {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_DEFENDING_STAT", "特性防守能力项无效")
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	terrain, err := formulaTerrain(value.GetRequiredTerrain())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.DefendingStatMultiplier{Stat: stat, Numerator: n, Denominator: d, RequiredTerrain: terrain, RequiresMajorStatus: value.GetRequiresMajorStatus()}, nil
}

func opponentDefendingStatMultiplierFromMessage(value *domainv1.GameAbilityOpponentDefendingStatMultiplier) (*abilitydetail.OpponentDefendingStatMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	stat, ok := formulaStat(value.GetStat(), false)
	if !ok {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_OPPONENT_DEFENDING_STAT", "特性防守能力项无效")
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	return &abilitydetail.OpponentDefendingStatMultiplier{Stat: stat, Numerator: n, Denominator: d}, nil
}

func allyPresenceMultiplierFromMessage(value *domainv1.GameAbilityAllyAbilityPresenceAttackingStatMultiplier) (*abilitydetail.AllyAbilityPresenceAttackingStatMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	stat, ok := formulaStat(value.GetStat(), true)
	if !ok {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ALLY_STAT", "特性互助组攻击能力项无效")
	}
	n, d, err := formulaFraction(value.GetNumerator(), value.GetDenominator())
	if err != nil {
		return nil, err
	}
	code := strings.TrimSpace(value.GetGroupCode())
	if code == "" {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ALLY_GROUP", "特性互助组代码不能为空")
	}
	return &abilitydetail.AllyAbilityPresenceAttackingStatMultiplier{GroupCode: code, Stat: stat, Numerator: n, Denominator: d}, nil
}

func formulaStat(value string, attacking bool) (battleengine.Stat, bool) {
	stat := battleengine.Stat(value)
	if attacking {
		return stat, stat == battleengine.StatAttack || stat == battleengine.StatSpecialAttack
	}
	return stat, stat == battleengine.StatDefense || stat == battleengine.StatSpecialDefense
}

func formulaWeather(value domainv1.GameSkillWeatherKind) (battleengine.WeatherKind, error) {
	if value == domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_UNSPECIFIED {
		return "", nil
	}
	domain, err := abilityWeatherKindFromMessage(value, "INVALID_ABILITY_FORMULA_WEATHER")
	return battleengine.WeatherKind(domain), err
}

func formulaTerrain(value domainv1.GameSkillTerrainKind) (battleengine.TerrainKind, error) {
	switch value {
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_UNSPECIFIED:
		return "", nil
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC:
		return battleengine.TerrainKindElectric, nil
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY:
		return battleengine.TerrainKindGrassy, nil
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY:
		return battleengine.TerrainKindMisty, nil
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC:
		return battleengine.TerrainKindPsychic, nil
	default:
		return "", kratoserrors.BadRequest("INVALID_ABILITY_FORMULA_TERRAIN", "特性公式场地无效")
	}
}

func formulaStatuses(values []string) ([]battleengine.MajorStatus, error) {
	result := make([]battleengine.MajorStatus, 0, len(values))
	seen := map[battleengine.MajorStatus]struct{}{}
	for _, raw := range values {
		status := battleengine.MajorStatus(raw)
		if !status.Valid() {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_FORMULA_STATUS", "特性主要异常无效")
		}
		if _, duplicate := seen[status]; duplicate {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_FORMULA_STATUS", "特性主要异常重复")
		}
		seen[status] = struct{}{}
		result = append(result, status)
	}
	return result, nil
}

func targetGenderMultiplierMessage(value *abilitydetail.TargetGenderDamageMultiplier) *domainv1.GameAbilityTargetGenderDamageMultiplier {
	if value == nil || value.SameGenderNumerator == 0 || value.SameGenderDenominator == 0 || value.OppositeGenderNumerator == 0 || value.OppositeGenderDenominator == 0 {
		return nil
	}
	return &domainv1.GameAbilityTargetGenderDamageMultiplier{SameGenderNumerator: int32(value.SameGenderNumerator), SameGenderDenominator: int32(value.SameGenderDenominator), OppositeGenderNumerator: int32(value.OppositeGenderNumerator), OppositeGenderDenominator: int32(value.OppositeGenderDenominator)}
}

func formulaDamageClassStrings(values []battleengine.DamageClass) []string {
	result := make([]string, len(values))
	for i, v := range values {
		result[i] = string(v)
	}
	return result
}
func formulaStatusStrings(values []battleengine.MajorStatus) []string {
	result := make([]string, len(values))
	for i, v := range values {
		result[i] = string(v)
	}
	return result
}

func formulaWeatherMessage(value battleengine.WeatherKind) domainv1.GameSkillWeatherKind {
	switch value {
	case battleengine.WeatherKindSun:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
	case battleengine.WeatherKindRain:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
	case battleengine.WeatherKindSandstorm:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
	case battleengine.WeatherKindSnow:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
	default:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_UNSPECIFIED
	}
}
func formulaTerrainMessage(value battleengine.TerrainKind) domainv1.GameSkillTerrainKind {
	switch value {
	case battleengine.TerrainKindElectric:
		return domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC
	case battleengine.TerrainKindGrassy:
		return domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY
	case battleengine.TerrainKindMisty:
		return domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY
	case battleengine.TerrainKindPsychic:
		return domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC
	default:
		return domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_UNSPECIFIED
	}
}

func punchMultiplierMessage(v *abilitydetail.PunchBasedSkillDamageBoost) *domainv1.GameAbilityPunchBasedSkillDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityPunchBasedSkillDamageBoost{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func slicingMultiplierMessage(v *abilitydetail.SlicingBasedSkillDamageBoost) *domainv1.GameAbilitySlicingBasedSkillDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilitySlicingBasedSkillDamageBoost{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func soundBoostMultiplierMessage(v *abilitydetail.SoundBasedSkillDamageBoost) *domainv1.GameAbilitySoundBasedSkillDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilitySoundBasedSkillDamageBoost{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func pulseMultiplierMessage(v *abilitydetail.PulseBasedSkillDamageBoost) *domainv1.GameAbilityPulseBasedSkillDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityPulseBasedSkillDamageBoost{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func biteMultiplierMessage(v *abilitydetail.BiteBasedSkillDamageBoost) *domainv1.GameAbilityBiteBasedSkillDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityBiteBasedSkillDamageBoost{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func suppressedMultiplierMessage(v *abilitydetail.SecondaryEffectsSuppressedDamageBoost) *domainv1.GameAbilitySecondaryEffectsSuppressedDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilitySecondaryEffectsSuppressedDamageBoost{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func soundReductionMultiplierMessage(v *abilitydetail.SoundBasedSkillDamageReduction) *domainv1.GameAbilitySoundBasedSkillDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilitySoundBasedSkillDamageReduction{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func superReductionMultiplierMessage(v *abilitydetail.SuperEffectiveDamageReduction) *domainv1.GameAbilitySuperEffectiveDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilitySuperEffectiveDamageReduction{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func fullHPMultiplierMessage(v *abilitydetail.FullHPDamageReduction) *domainv1.GameAbilityFullHPDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityFullHPDamageReduction{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func contactReductionMultiplierMessage(v *abilitydetail.ContactBasedSkillDamageReduction) *domainv1.GameAbilityContactBasedSkillDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityContactBasedSkillDamageReduction{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func allyReductionMultiplierMessage(v *abilitydetail.AllyReceivedDamageReduction) *domainv1.GameAbilityAllyReceivedDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityAllyReceivedDamageReduction{Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}

func damageClassReductionMessage(v *abilitydetail.DamageClassDamageReduction) *domainv1.GameAbilityDamageClassDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityDamageClassDamageReduction{DamageClasses: formulaDamageClassStrings(v.DamageClasses), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func elementReductionMessage(v *abilitydetail.ElementSkillDamageReduction) *domainv1.GameAbilityElementSkillDamageReduction {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityElementSkillDamageReduction{ElementIds: identifierStrings(v.ElementIDs), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func attackingStatMultiplierMessage(v *abilitydetail.AttackingStatMultiplier) *domainv1.GameAbilityAttackingStatMultiplier {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityAttackingStatMultiplier{Stat: string(v.Stat), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator), RequiredTerrain: formulaTerrainMessage(v.RequiredTerrain), RequiredWeather: formulaWeatherMessage(v.RequiredWeather), RequiresMajorStatus: v.RequiresMajorStatus, RequiredMajorStatuses: formulaStatusStrings(v.RequiredMajorStatuses), MaximumHpNumerator: int32(v.MaximumHPNumerator), MaximumHpDenominator: int32(v.MaximumHPDenominator), IgnoreBurnAttackReduction: v.IgnoreBurnAttackReduction}
}
func opponentAttackingStatMultiplierMessage(v *abilitydetail.OpponentAttackingStatMultiplier) *domainv1.GameAbilityOpponentAttackingStatMultiplier {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityOpponentAttackingStatMultiplier{Stat: string(v.Stat), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func defendingStatMultiplierMessage(v *abilitydetail.DefendingStatMultiplier) *domainv1.GameAbilityDefendingStatMultiplier {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityDefendingStatMultiplier{Stat: string(v.Stat), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator), RequiredTerrain: formulaTerrainMessage(v.RequiredTerrain), RequiresMajorStatus: v.RequiresMajorStatus}
}
func opponentDefendingStatMultiplierMessage(v *abilitydetail.OpponentDefendingStatMultiplier) *domainv1.GameAbilityOpponentDefendingStatMultiplier {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityOpponentDefendingStatMultiplier{Stat: string(v.Stat), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func allySkillDamageBoostMessage(v *abilitydetail.AllySkillDamageBoost) *domainv1.GameAbilityAllySkillDamageBoost {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityAllySkillDamageBoost{DamageClasses: formulaDamageClassStrings(v.DamageClasses), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
func allyPresenceMultiplierMessage(v *abilitydetail.AllyAbilityPresenceAttackingStatMultiplier) *domainv1.GameAbilityAllyAbilityPresenceAttackingStatMultiplier {
	if v == nil {
		return nil
	}
	return &domainv1.GameAbilityAllyAbilityPresenceAttackingStatMultiplier{GroupCode: v.GroupCode, Stat: string(v.Stat), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
}
