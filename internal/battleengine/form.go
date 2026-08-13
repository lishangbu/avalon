package battleengine

import (
	"fmt"
)

// FormProfile 是成员在本场对局中可以切换到的一份完整形态战斗画像。
//
// 它由 Battle 在对局开始时按照同一培养值和等级从实时精灵资料计算并冻结。运行时绝不根据形态名称、
// Stable Code 或实时数据库重新查询数值，避免资料维护影响正在进行的对局。
type FormProfile struct {
	// CreatureID 是该形态精灵的稳定 Identifier 文本。
	CreatureID Identifier `json:"creatureId"`
	// MaxHP 是该形态按当前成员培养值和本场等级计算出的最大生命值。
	MaxHP uint32 `json:"maxHp"`
	// Stats 是该形态按当前成员培养值和本场等级计算出的五项非生命战斗能力。
	Stats StatBlock `json:"stats"`
	// Weight 是该形态的冻结体重整数刻度。
	Weight uint32 `json:"weight"`
	// ElementIDs 是该形态的一至两个属性稳定 Identifier 文本。
	ElementIDs []Identifier `json:"elementIds"`
}

// SwitchInFormChange 是特性在成员进入场地时触发的一次确定形态切换规则。
//
// 该规则只在成员当前确实处于 BaseCreatureID 时生效。AddsMaximumHPDifference 仅用于少数需要补齐正最大
// 生命差值的入场形态，不能作为任何普通形态切换的默认生命变化策略。
type SwitchInFormChange struct {
	// BaseCreatureID 是允许触发该规则的基础形态稳定 Identifier。
	BaseCreatureID Identifier `json:"baseCreatureId"`
	// AlternateCreatureID 是触发后切换到的目标形态稳定 Identifier。
	AlternateCreatureID Identifier `json:"alternateCreatureId"`
	// AddsMaximumHPDifference 表示目标最大生命更高时，当前生命同步增加两者差额。
	AddsMaximumHPDifference bool `json:"addsMaximumHpDifference"`
}

// SwitchOutFormChange 是特性在成员成功离开场地时触发的一次确定形态切换规则。
//
// 该规则只在成员当前确实处于 BaseCreatureID 时生效。它保留当前生命并在目标最大生命较低时夹取，不附带
// 入场形态特有的最大生命差额补齐语义。
type SwitchOutFormChange struct {
	// BaseCreatureID 是允许触发该规则的基础形态稳定 Identifier。
	BaseCreatureID Identifier `json:"baseCreatureId"`
	// AlternateCreatureID 是成功离场后切换到的目标形态稳定 Identifier。
	AlternateCreatureID Identifier `json:"alternateCreatureId"`
}

// WeatherFormTarget 是一种普通天气与其对应目标形态的强类型绑定。
type WeatherFormTarget struct {
	// Weather 是会选择该目标形态的普通天气。
	Weather WeatherKind `json:"weather"`
	// CreatureID 是该天气下应切换到的目标形态稳定 Identifier。
	CreatureID Identifier `json:"creatureId"`
}

// WeatherFormChange 是特性按当前有效普通天气同步成员形态的规则。
//
// DefaultCreatureID 用于无天气、未映射天气或天气效果被封锁时的默认形态。Targets 是最多四项的封闭天气
// 映射，不是可由资料文本驱动的泛型效果数组。
type WeatherFormChange struct {
	// DefaultCreatureID 是未命中任意天气映射时使用的默认形态稳定 Identifier。
	DefaultCreatureID Identifier `json:"defaultCreatureId"`
	// Targets 是普通天气到目标形态的无重复映射。
	Targets []WeatherFormTarget `json:"targets"`
}

// validateFormProfile 校验成员形态画像自身的封闭不变量。
func validateFormProfile(value FormProfile) error {
	if !value.CreatureID.IsValid() || value.MaxHP == 0 || value.Weight == 0 ||
		value.Stats.Attack == 0 || value.Stats.Defense == 0 || value.Stats.SpecialAttack == 0 ||
		value.Stats.SpecialDefense == 0 || value.Stats.Speed == 0 || len(value.ElementIDs) < 1 ||
		len(value.ElementIDs) > 2 || hasBlankOrDuplicate(value.ElementIDs) {
		return fmt.Errorf("形态画像无效")
	}
	return nil
}

// validateSwitchInFormChange 校验可选的入场形态切换规则。
func validateSwitchInFormChange(value *SwitchInFormChange, profiles []FormProfile) error {
	if value == nil {
		return nil
	}
	if !value.BaseCreatureID.IsValid() || !value.AlternateCreatureID.IsValid() ||
		value.BaseCreatureID == value.AlternateCreatureID || !containsFormProfile(profiles, value.BaseCreatureID) ||
		!containsFormProfile(profiles, value.AlternateCreatureID) {
		return fmt.Errorf("入场形态切换规则无效")
	}
	return nil
}

// validateSwitchOutFormChange 校验可选的成功离场形态切换规则。
func validateSwitchOutFormChange(value *SwitchOutFormChange, profiles []FormProfile) error {
	if value == nil {
		return nil
	}
	if !value.BaseCreatureID.IsValid() || !value.AlternateCreatureID.IsValid() ||
		value.BaseCreatureID == value.AlternateCreatureID || !containsFormProfile(profiles, value.BaseCreatureID) ||
		!containsFormProfile(profiles, value.AlternateCreatureID) {
		return fmt.Errorf("离场形态切换规则无效")
	}
	return nil
}

// validateWeatherFormChange 校验可选的天气形态规则。
func validateWeatherFormChange(value *WeatherFormChange, profiles []FormProfile) error {
	if value == nil {
		return nil
	}
	if !value.DefaultCreatureID.IsValid() || !containsFormProfile(profiles, value.DefaultCreatureID) ||
		len(value.Targets) == 0 || len(value.Targets) > 4 {
		return fmt.Errorf("天气形态规则无效")
	}
	seen := make(map[WeatherKind]struct{}, len(value.Targets))
	for _, target := range value.Targets {
		if !target.Weather.valid() || !target.CreatureID.IsValid() ||
			!containsFormProfile(profiles, target.CreatureID) {
			return fmt.Errorf("天气形态规则无效")
		}
		if _, duplicated := seen[target.Weather]; duplicated {
			return fmt.Errorf("天气形态规则包含重复天气")
		}
		seen[target.Weather] = struct{}{}
	}
	return nil
}

// cloneFormProfiles 深复制成员可切换的全部形态画像。
func cloneFormProfiles(values []FormProfile) []FormProfile {
	cloned := make([]FormProfile, len(values))
	for index, value := range values {
		cloned[index] = value
		cloned[index].ElementIDs = append([]Identifier(nil), value.ElementIDs...)
	}
	return cloned
}

// cloneSwitchInFormChange 深复制可选的入场形态切换规则。
func cloneSwitchInFormChange(value *SwitchInFormChange) *SwitchInFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneSwitchOutFormChange 深复制可选的成功离场形态切换规则。
func cloneSwitchOutFormChange(value *SwitchOutFormChange) *SwitchOutFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneWeatherFormChange 深复制可选的天气形态规则。
func cloneWeatherFormChange(value *WeatherFormChange) *WeatherFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.Targets = append([]WeatherFormTarget(nil), value.Targets...)
	return &cloned
}

// formProfile 查找成员已经冻结的一份目标形态画像。
func (member MemberSnapshot) formProfile(creatureID Identifier) (FormProfile, bool) {
	for _, profile := range member.FormProfiles {
		if profile.CreatureID == creatureID {
			return profile, true
		}
	}
	return FormProfile{}, false
}

func containsFormProfile(profiles []FormProfile, creatureID Identifier) bool {
	for _, profile := range profiles {
		if profile.CreatureID == creatureID {
			return true
		}
	}
	return false
}

// applyFormProfile 将一份冻结形态画像写入成员，同时保留技能、特性、持有物、异常和临时状态。
//
// 普通形态切换保持当前生命，并在新上限更低时夹取。需要特殊补血语义的规则由调用者在本函数后明确处理，
// 防止所有形态切换被误解释为回复。
func applyFormProfile(member MemberSnapshot, profile FormProfile) MemberSnapshot {
	member.CreatureID = profile.CreatureID
	member.MaxHP = profile.MaxHP
	if member.CurrentHP > member.MaxHP {
		member.CurrentHP = member.MaxHP
	}
	member.Stats = profile.Stats
	member.Weight = profile.Weight
	member.NaturalElementIDs = append([]Identifier(nil), profile.ElementIDs...)
	member.ElementIDs = append([]Identifier(nil), profile.ElementIDs...)
	// 太晶化覆盖当前属性而不覆盖形态资料。形态变化仍要更新 NaturalElementIDs，供状态摘要、审计和未来
	// 解除机制准确获取自然基线；但已太晶化的成员在本场余下时间保持唯一太晶属性。
	if member.Terastallized {
		member.ElementIDs = []Identifier{member.TeraElementID}
		return member
	}
	// 道具身份的持续时间覆盖整个连续上场周期，而形态只会更新其下方的自然画像。因此形态变化后必须更新
	// 离场还原来源，并立即恢复道具指定的单属性，不能让天气或入场形态切换意外移除此状态。
	if len(member.HeldItemElementIdentityBaseElementIDs) > 0 {
		member.HeldItemElementIdentityBaseElementIDs = append([]Identifier(nil), profile.ElementIDs...)
		member.ElementIDs = []Identifier{member.HeldItemElementID}
	}
	return member
}
