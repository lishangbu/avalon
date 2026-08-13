// Package battlerules 定义 Skill 与 Ability 在 Current Game Data 中持久化的事件型规则文档。
package battlerules

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"reflect"

	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
)

var (
	// ErrInvalidSkill 表示 Skill rules 不是当前编译器接受的规范事件文档。
	ErrInvalidSkill = errors.New("技能战斗规则无效")
	// ErrInvalidAbility 表示 Ability rules 不是当前编译器接受的规范事件文档。
	ErrInvalidAbility = errors.New("特性战斗规则无效")
)

// Skill 是一条 Skill 在使用时交给 Battle Engine 编译的完整规则文档。
// 空文档表示该技能只有主表中的基础战斗事实，没有额外规则。
type Skill struct {
	OnUse *SkillOnUse `json:"onUse,omitempty"`
}

// SkillOnUse 保存技能从选择、命中到后效阶段使用的强类型规则事实。
type SkillOnUse struct {
	skilldetail.OptionalValues
}

// NewSkill 将已经通过领域校验的技能规则事实组织为事件型文档。
func NewSkill(values skilldetail.OptionalValues) (Skill, bool) {
	values.Effect = nil
	values.ShortEffect = nil
	values.Description = nil
	values, valid := skilldetail.NormalizeForRules(values)
	if valid && reflect.DeepEqual(values, skilldetail.OptionalValues{DamageMode: skilldetail.DamageModeFormula}) {
		return Skill{}, true
	}
	return Skill{OnUse: &SkillOnUse{OptionalValues: values}}, valid
}

// Values 返回规则文档供统一 Battle 编译器使用的规范化强类型事实。
func (rules Skill) Values() (skilldetail.OptionalValues, bool) {
	values := skilldetail.OptionalValues{}
	if rules.OnUse != nil {
		values = rules.OnUse.OptionalValues
	}
	values.Effect = nil
	values.ShortEffect = nil
	values.Description = nil
	return skilldetail.NormalizeForRules(values)
}

// ParseSkill 严格解析、校验并规范化持久化的 Skill rules JSON。
func ParseSkill(payload []byte) (Skill, error) {
	var rules Skill
	if err := decodeStrict(payload, &rules); err != nil {
		return Skill{}, ErrInvalidSkill
	}
	values, valid := rules.Values()
	if !valid {
		return Skill{}, ErrInvalidSkill
	}
	canonical, valid := NewSkill(values)
	if !valid || !sameJSON(rules, canonical) {
		return Skill{}, ErrInvalidSkill
	}
	return canonical, nil
}

// Ability 是一条 Ability 按 Battle Engine 执行时机组织的完整规则文档。
type Ability struct {
	Passive             *AbilityPassive             `json:"passive,omitempty"`
	Reactive            *AbilityReactive            `json:"reactive,omitempty"`
	OnSwitchIn          *AbilityOnSwitchIn          `json:"onSwitchIn,omitempty"`
	OnSwitchOut         *AbilityOnSwitchOut         `json:"onSwitchOut,omitempty"`
	OnDamage            *AbilityOnDamage            `json:"onDamage,omitempty"`
	OnTurnEnd           *AbilityOnTurnEnd           `json:"onTurnEnd,omitempty"`
	OnEnvironmentChange *AbilityOnEnvironmentChange `json:"onEnvironmentChange,omitempty"`
	OnTerastallization  *AbilityOnTerastallization  `json:"onTerastallization,omitempty"`
}

// AbilityPassive 保存不创建独立事件、由固定结算阶段直接读取的特性规则。
type AbilityPassive struct{ abilitydetail.OptionalValues }

// AbilityReactive 保存受伤、倒下与其它反应窗口读取的强类型规则。
type AbilityReactive struct{ abilitydetail.OptionalValues }

// AbilityOnSwitchIn 保存成员成功进入场地后执行的规则。
type AbilityOnSwitchIn struct{ abilitydetail.OptionalValues }

// AbilityOnSwitchOut 保存成员成功离场后执行的规则。
type AbilityOnSwitchOut struct{ abilitydetail.OptionalValues }

// AbilityOnDamage 保存一段有效伤害结算后执行的规则。
type AbilityOnDamage struct{ abilitydetail.OptionalValues }

// AbilityOnTurnEnd 保存回合末阶段执行的规则。
type AbilityOnTurnEnd struct{ abilitydetail.OptionalValues }

// AbilityOnEnvironmentChange 保存有效天气变化后同步形态的规则。
type AbilityOnEnvironmentChange struct{ abilitydetail.OptionalValues }

// AbilityOnTerastallization 保存成员完成太晶化后执行的规则。
type AbilityOnTerastallization struct{ abilitydetail.OptionalValues }

// NewAbility 将已经通过领域校验的特性规则事实拆分到稳定执行时机。
func NewAbility(values abilitydetail.OptionalValues) (Ability, bool) {
	values.Effect = nil
	values.ShortEffect = nil
	values.Introduction = nil
	values, valid := abilitydetail.NormalizeForRules(values)

	passive := values
	clearTriggeredAbilityValues(&passive)
	reactive := abilitydetail.OptionalValues{ReactiveAbilityRules: values.ReactiveAbilityRules}
	onSwitchIn := abilitydetail.OptionalValues{
		SwitchInStrongWeather: values.SwitchInStrongWeather, SwitchInWeather: values.SwitchInWeather,
		SwitchInTerrain: values.SwitchInTerrain, SwitchInStatStageChange: values.SwitchInStatStageChange,
		SwitchInAllyHeal:                        values.SwitchInAllyHeal,
		SwitchInOpponentDefenseComparisonBoost:  values.SwitchInOpponentDefenseComparisonBoost,
		SwitchInAllyStatStageCopy:               values.SwitchInAllyStatStageCopy,
		SwitchInAllyStatStageReset:              values.SwitchInAllyStatStageReset,
		SwitchInClearAllSideDamageReductions:    values.SwitchInClearAllSideDamageReductions,
		SwitchInCopyOpponentAbility:             values.SwitchInCopyOpponentAbility,
		SwitchInRevealOpponentHeldItems:         values.SwitchInRevealOpponentHeldItems,
		SwitchInRevealOpponentHighestPowerSkill: values.SwitchInRevealOpponentHighestPowerSkill,
		SwitchInTransformIntoOpponent:           values.SwitchInTransformIntoOpponent,
		SwitchInDetectDangerousOpponentSkill:    values.SwitchInDetectDangerousOpponentSkill,
		SwitchInDisguiseAsLastHealthyAlly:       values.SwitchInDisguiseAsLastHealthyAlly,
		SwitchInHeldItemElementIdentity:         values.SwitchInHeldItemElementIdentity,
		SwitchInFormChange:                      values.SwitchInFormChange,
	}
	onSwitchOut := abilitydetail.OptionalValues{
		SwitchOutMajorStatusCure: values.SwitchOutMajorStatusCure,
		SwitchOutHealDenominator: values.SwitchOutHealDenominator,
		SwitchOutFormChange:      values.SwitchOutFormChange,
	}
	onDamage := abilitydetail.OptionalValues{
		DamageCrossedHalfHPForceSelfSwitch: values.DamageCrossedHalfHPForceSelfSwitch,
		ContactDamageToAttackerDenominator: values.ContactDamageToAttackerDenominator,
	}
	onTurnEnd := abilitydetail.OptionalValues{WeatherEndTurnHeal: values.WeatherEndTurnHeal}
	onEnvironmentChange := abilitydetail.OptionalValues{WeatherFormChange: values.WeatherFormChange}
	onTerastallization := abilitydetail.OptionalValues{
		TerastallizationStatStageChange:  values.TerastallizationStatStageChange,
		TerastallizationEnvironmentClear: values.TerastallizationEnvironmentClear,
	}

	return Ability{
		Passive:     optionalAbilityGroup(passive, func(value abilitydetail.OptionalValues) *AbilityPassive { return &AbilityPassive{value} }),
		Reactive:    optionalAbilityGroup(reactive, func(value abilitydetail.OptionalValues) *AbilityReactive { return &AbilityReactive{value} }),
		OnSwitchIn:  optionalAbilityGroup(onSwitchIn, func(value abilitydetail.OptionalValues) *AbilityOnSwitchIn { return &AbilityOnSwitchIn{value} }),
		OnSwitchOut: optionalAbilityGroup(onSwitchOut, func(value abilitydetail.OptionalValues) *AbilityOnSwitchOut { return &AbilityOnSwitchOut{value} }),
		OnDamage:    optionalAbilityGroup(onDamage, func(value abilitydetail.OptionalValues) *AbilityOnDamage { return &AbilityOnDamage{value} }),
		OnTurnEnd:   optionalAbilityGroup(onTurnEnd, func(value abilitydetail.OptionalValues) *AbilityOnTurnEnd { return &AbilityOnTurnEnd{value} }),
		OnEnvironmentChange: optionalAbilityGroup(onEnvironmentChange, func(value abilitydetail.OptionalValues) *AbilityOnEnvironmentChange {
			return &AbilityOnEnvironmentChange{value}
		}),
		OnTerastallization: optionalAbilityGroup(onTerastallization, func(value abilitydetail.OptionalValues) *AbilityOnTerastallization {
			return &AbilityOnTerastallization{value}
		}),
	}, valid
}

// Values 合并各执行时机并返回统一 Battle 编译器使用的规范化强类型事实。
func (rules Ability) Values() (abilitydetail.OptionalValues, bool) {
	groups := []abilitydetail.OptionalValues{}
	if rules.Passive != nil {
		groups = append(groups, rules.Passive.OptionalValues)
	}
	if rules.Reactive != nil {
		groups = append(groups, rules.Reactive.OptionalValues)
	}
	if rules.OnSwitchIn != nil {
		groups = append(groups, rules.OnSwitchIn.OptionalValues)
	}
	if rules.OnSwitchOut != nil {
		groups = append(groups, rules.OnSwitchOut.OptionalValues)
	}
	if rules.OnDamage != nil {
		groups = append(groups, rules.OnDamage.OptionalValues)
	}
	if rules.OnTurnEnd != nil {
		groups = append(groups, rules.OnTurnEnd.OptionalValues)
	}
	if rules.OnEnvironmentChange != nil {
		groups = append(groups, rules.OnEnvironmentChange.OptionalValues)
	}
	if rules.OnTerastallization != nil {
		groups = append(groups, rules.OnTerastallization.OptionalValues)
	}
	merged := abilitydetail.OptionalValues{}
	for _, group := range groups {
		payload, err := json.Marshal(group)
		if err != nil {
			return abilitydetail.OptionalValues{}, false
		}
		if err := json.Unmarshal(payload, &merged); err != nil {
			return abilitydetail.OptionalValues{}, false
		}
	}
	merged.Effect = nil
	merged.ShortEffect = nil
	merged.Introduction = nil
	return abilitydetail.NormalizeForRules(merged)
}

// ParseAbility 严格解析、校验并规范化持久化的 Ability rules JSON。
func ParseAbility(payload []byte) (Ability, error) {
	var rules Ability
	if err := decodeStrict(payload, &rules); err != nil {
		return Ability{}, ErrInvalidAbility
	}
	values, valid := rules.Values()
	if !valid {
		return Ability{}, ErrInvalidAbility
	}
	canonical, valid := NewAbility(values)
	if !valid || !sameJSON(rules, canonical) {
		return Ability{}, ErrInvalidAbility
	}
	return canonical, nil
}

// SkillJSON 返回经过严格校验且字段顺序稳定的 Skill rules JSON。
func SkillJSON(rules Skill) ([]byte, error) {
	values, valid := rules.Values()
	if !valid {
		return nil, ErrInvalidSkill
	}
	canonical, valid := NewSkill(values)
	if !valid {
		return nil, ErrInvalidSkill
	}
	return json.Marshal(canonical)
}

// AbilityJSON 返回经过严格校验且字段顺序稳定的 Ability rules JSON。
func AbilityJSON(rules Ability) ([]byte, error) {
	values, valid := rules.Values()
	if !valid {
		return nil, ErrInvalidAbility
	}
	canonical, valid := NewAbility(values)
	if !valid {
		return nil, ErrInvalidAbility
	}
	return json.Marshal(canonical)
}

func decodeStrict(payload []byte, target any) error {
	decoder := json.NewDecoder(bytes.NewReader(payload))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(target); err != nil {
		return err
	}
	var trailing any
	if err := decoder.Decode(&trailing); !errors.Is(err, io.EOF) {
		if err == nil {
			return errors.New("规则文档包含多余 JSON 值")
		}
		return err
	}
	return nil
}

func sameJSON(left, right any) bool {
	leftPayload, leftErr := json.Marshal(left)
	rightPayload, rightErr := json.Marshal(right)
	if leftErr != nil || rightErr != nil {
		return false
	}
	var leftValue any
	var rightValue any
	return json.Unmarshal(leftPayload, &leftValue) == nil && json.Unmarshal(rightPayload, &rightValue) == nil &&
		reflect.DeepEqual(leftValue, rightValue)
}

func optionalAbilityGroup[T any](value abilitydetail.OptionalValues, build func(abilitydetail.OptionalValues) *T) *T {
	if reflect.DeepEqual(value, abilitydetail.OptionalValues{}) {
		return nil
	}
	return build(value)
}

func clearTriggeredAbilityValues(values *abilitydetail.OptionalValues) {
	values.ReactiveAbilityRules = nil
	values.SwitchInStrongWeather = nil
	values.SwitchInWeather = nil
	values.SwitchInTerrain = nil
	values.SwitchInStatStageChange = nil
	values.SwitchInAllyHeal = nil
	values.SwitchInOpponentDefenseComparisonBoost = false
	values.SwitchInAllyStatStageCopy = false
	values.SwitchInAllyStatStageReset = false
	values.SwitchInClearAllSideDamageReductions = false
	values.SwitchInCopyOpponentAbility = false
	values.SwitchInRevealOpponentHeldItems = false
	values.SwitchInRevealOpponentHighestPowerSkill = false
	values.SwitchInTransformIntoOpponent = false
	values.SwitchInDetectDangerousOpponentSkill = false
	values.SwitchInDisguiseAsLastHealthyAlly = false
	values.SwitchInHeldItemElementIdentity = false
	values.SwitchInFormChange = nil
	values.SwitchOutMajorStatusCure = false
	values.SwitchOutHealDenominator = 0
	values.SwitchOutFormChange = nil
	values.DamageCrossedHalfHPForceSelfSwitch = false
	values.ContactDamageToAttackerDenominator = 0
	values.WeatherEndTurnHeal = nil
	values.WeatherFormChange = nil
	values.TerastallizationStatStageChange = nil
	values.TerastallizationEnvironmentClear = false
}
