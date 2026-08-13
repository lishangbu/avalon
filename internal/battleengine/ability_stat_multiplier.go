package battleengine

import "math/big"

// AttackingStatMultiplier 描述持有者作为攻击方时对公式攻击能力的条件倍率。
//
// 该规则只修正伤害计算使用的临时能力值，不会改写成员的基础能力或能力阶级。所有可选条件同时声明时
// 必须全部满足；倍率在攻击能力阶级之后、灼伤物理攻击减半之前应用。
type AttackingStatMultiplier struct {
	// Stat 是被修正的攻击侧能力，只允许攻击或特攻。
	Stat Stat `json:"stat"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
	// RequiredTerrain 是必须匹配的普通场地；空值表示不要求场地。
	RequiredTerrain TerrainKind `json:"requiredTerrain,omitempty"`
	// RequiredWeather 是必须匹配的有效普通天气；空值表示不要求天气。
	RequiredWeather WeatherKind `json:"requiredWeather,omitempty"`
	// RequiresMajorStatus 表示持有者必须具有任意有效主要异常。
	RequiresMajorStatus bool `json:"requiresMajorStatus"`
	// RequiredMajorStatuses 是允许触发的具体主要异常集合；空集合表示不限定异常种类。
	RequiredMajorStatuses []MajorStatus `json:"requiredMajorStatuses,omitempty"`
	// MaximumHPNumerator 是生命上限条件的分子；0 表示不要求低生命。
	MaximumHPNumerator uint16 `json:"maximumHpNumerator"`
	// MaximumHPDenominator 是生命上限条件的分母；0 表示不要求低生命。
	MaximumHPDenominator uint16 `json:"maximumHpDenominator"`
	// IgnoreBurnAttackReduction 表示本规则激活且修正攻击时跳过灼伤的物理攻击减半。
	IgnoreBurnAttackReduction bool `json:"ignoreBurnAttackReduction"`
}

// OpponentAttackingStatMultiplier 描述持有者作为防守方时对攻击方公式能力的倍率。
//
// 该效果属于防守方特性，因此攻击方无视目标特性时不会生效。
type OpponentAttackingStatMultiplier struct {
	// Stat 是被修正的攻击侧能力，只允许攻击或特攻。
	Stat Stat `json:"stat"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// DefendingStatMultiplier 描述持有者作为防守方时对自身公式防御能力的条件倍率。
//
// 该倍率在防御能力阶级和天气防御修正之后应用；它不会表现为最终伤害减免，也不会改写权威能力值。
type DefendingStatMultiplier struct {
	// Stat 是被修正的防守侧能力，只允许防御或特防。
	Stat Stat `json:"stat"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
	// RequiredTerrain 是必须匹配的普通场地；空值表示不要求场地。
	RequiredTerrain TerrainKind `json:"requiredTerrain,omitempty"`
	// RequiresMajorStatus 表示持有者必须具有任意有效主要异常。
	RequiresMajorStatus bool `json:"requiresMajorStatus"`
}

// OpponentDefendingStatMultiplier 描述持有者作为攻击方时对目标公式防御能力的倍率。
type OpponentDefendingStatMultiplier struct {
	// Stat 是被修正的防守侧能力，只允许防御或特防。
	Stat Stat `json:"stat"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// AllyAbilityPresenceAttackingStatMultiplier 描述指定互助组伙伴在场时对持有者攻击能力的倍率。
type AllyAbilityPresenceAttackingStatMultiplier struct {
	// GroupCode 是伙伴特性必须声明的非空互助组代码。
	GroupCode string `json:"groupCode"`
	// Stat 是被修正的攻击侧能力，只允许攻击或特攻。
	Stat Stat `json:"stat"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// validAbilityStatMultipliers 校验五类彼此独立的公式能力倍率。
func validAbilityStatMultipliers(member MemberSnapshot) bool {
	return validAttackingStatMultiplier(member.AttackingStatMultiplier) &&
		validOpponentAttackingStatMultiplier(member.OpponentAttackingStatMultiplier) &&
		validDefendingStatMultiplier(member.DefendingStatMultiplier) &&
		validOpponentDefendingStatMultiplier(member.OpponentDefendingStatMultiplier) &&
		validAllyAbilityPresenceAttackingStatMultiplier(member.AllyAbilityPresenceAttackingStatMultiplier)
}

// validAttackingStatMultiplier 校验攻击能力、正分数及所有可选条件。
func validAttackingStatMultiplier(value *AttackingStatMultiplier) bool {
	if value == nil {
		return true
	}
	if (value.Stat != StatAttack && value.Stat != StatSpecialAttack) || value.Numerator == 0 || value.Denominator == 0 ||
		value.RequiredTerrain != "" && !value.RequiredTerrain.valid() || value.RequiredWeather != "" && !value.RequiredWeather.valid() ||
		(value.MaximumHPNumerator == 0) != (value.MaximumHPDenominator == 0) ||
		value.MaximumHPDenominator != 0 && value.MaximumHPNumerator > value.MaximumHPDenominator ||
		value.IgnoreBurnAttackReduction && value.Stat != StatAttack {
		return false
	}
	seen := make(map[MajorStatus]struct{}, len(value.RequiredMajorStatuses))
	for _, status := range value.RequiredMajorStatuses {
		if !status.Valid() {
			return false
		}
		if _, duplicate := seen[status]; duplicate {
			return false
		}
		seen[status] = struct{}{}
	}
	return true
}

// validOpponentAttackingStatMultiplier 校验目标特性修正攻击能力的规则。
func validOpponentAttackingStatMultiplier(value *OpponentAttackingStatMultiplier) bool {
	return value == nil || (value.Stat == StatAttack || value.Stat == StatSpecialAttack) && value.Numerator != 0 && value.Denominator != 0
}

// validDefendingStatMultiplier 校验防守能力、正分数及可选场地条件。
func validDefendingStatMultiplier(value *DefendingStatMultiplier) bool {
	return value == nil || (value.Stat == StatDefense || value.Stat == StatSpecialDefense) && value.Numerator != 0 && value.Denominator != 0 &&
		(value.RequiredTerrain == "" || value.RequiredTerrain.valid())
}

// validOpponentDefendingStatMultiplier 校验攻击方特性修正目标防御能力的规则。
func validOpponentDefendingStatMultiplier(value *OpponentDefendingStatMultiplier) bool {
	return value == nil || (value.Stat == StatDefense || value.Stat == StatSpecialDefense) && value.Numerator != 0 && value.Denominator != 0
}

// validAllyAbilityPresenceAttackingStatMultiplier 校验伙伴互助组能力倍率。
func validAllyAbilityPresenceAttackingStatMultiplier(value *AllyAbilityPresenceAttackingStatMultiplier) bool {
	return value == nil || value.GroupCode != "" && (value.Stat == StatAttack || value.Stat == StatSpecialAttack) &&
		value.Numerator != 0 && value.Denominator != 0
}

// attackingStatMultiplierActive 报告攻击方当前状态和有效环境是否同时满足规则条件。
func attackingStatMultiplierActive(value *AttackingStatMultiplier, member MemberSnapshot, weather *WeatherEffect, terrain *TerrainEffect) bool {
	if value == nil {
		return false
	}
	if value.RequiresMajorStatus && member.MajorStatus == "" {
		return false
	}
	if len(value.RequiredMajorStatuses) != 0 && !containsMajorStatus(value.RequiredMajorStatuses, member.MajorStatus) {
		return false
	}
	if value.MaximumHPDenominator != 0 &&
		uint64(member.CurrentHP)*uint64(value.MaximumHPDenominator) > uint64(member.MaxHP)*uint64(value.MaximumHPNumerator) {
		return false
	}
	if value.RequiredWeather != "" && (weather == nil || weather.Kind != value.RequiredWeather) {
		return false
	}
	if value.RequiredTerrain != "" && (terrain == nil || terrain.Kind != value.RequiredTerrain) {
		return false
	}
	return true
}

// defendingStatMultiplierActive 报告防守方是否满足异常和场地条件。
func defendingStatMultiplierActive(value *DefendingStatMultiplier, member MemberSnapshot, terrain *TerrainEffect) bool {
	return value != nil && (!value.RequiresMajorStatus || member.MajorStatus != "") &&
		(value.RequiredTerrain == "" || terrain != nil && terrain.Kind == value.RequiredTerrain)
}

// damageStatMultiplier 保存一次公式能力值修正的精确正分数。
type damageStatMultiplier struct {
	numerator   uint16
	denominator uint16
}

// applyDamageStatMultipliers 把全部匹配能力倍率合并后只取整一次，并对 uint32 结果饱和。
func applyDamageStatMultipliers(value uint32, multipliers ...damageStatMultiplier) uint32 {
	if value == 0 {
		return 1
	}
	numerator := new(big.Int).SetUint64(uint64(value))
	denominator := big.NewInt(1)
	for _, multiplier := range multipliers {
		if multiplier.numerator == 0 || multiplier.denominator == 0 {
			continue
		}
		numerator.Mul(numerator, big.NewInt(int64(multiplier.numerator)))
		denominator.Mul(denominator, big.NewInt(int64(multiplier.denominator)))
	}
	result := numerator.Quo(numerator, denominator)
	if result.Sign() <= 0 {
		return 1
	}
	if !result.IsUint64() || result.Uint64() > uint64(^uint32(0)) {
		return ^uint32(0)
	}
	return uint32(result.Uint64())
}

// attackingStatAfterAbility 返回能力阶级之后、灼伤修正之前的攻击侧能力。
func attackingStatAfterAbility(
	value uint32,
	attacker, defender MemberSnapshot,
	stat Stat,
	weather *WeatherEffect,
	terrain *TerrainEffect,
	ignoreDefenderAbility bool,
	ally *AllyAbilityPresenceAttackingStatMultiplier,
) uint32 {
	multipliers := make([]damageStatMultiplier, 0, 3)
	if rule := attacker.AttackingStatMultiplier; rule != nil && rule.Stat == stat && attackingStatMultiplierActive(rule, attacker, weather, terrain) {
		multipliers = append(multipliers, damageStatMultiplier{rule.Numerator, rule.Denominator})
	}
	if rule := defender.OpponentAttackingStatMultiplier; !ignoreDefenderAbility && rule != nil && rule.Stat == stat {
		multipliers = append(multipliers, damageStatMultiplier{rule.Numerator, rule.Denominator})
	}
	if ally != nil && ally.Stat == stat {
		multipliers = append(multipliers, damageStatMultiplier{ally.Numerator, ally.Denominator})
	}
	return applyDamageStatMultipliers(value, multipliers...)
}

// defendingStatAfterAbility 返回天气防御修正之后的防守侧能力。
func defendingStatAfterAbility(value uint32, attacker, defender MemberSnapshot, stat Stat, terrain *TerrainEffect, ignoreDefenderAbility bool) uint32 {
	multipliers := make([]damageStatMultiplier, 0, 2)
	if rule := defender.DefendingStatMultiplier; !ignoreDefenderAbility && rule != nil && rule.Stat == stat && defendingStatMultiplierActive(rule, defender, terrain) {
		multipliers = append(multipliers, damageStatMultiplier{rule.Numerator, rule.Denominator})
	}
	if rule := attacker.OpponentDefendingStatMultiplier; rule != nil && rule.Stat == stat {
		multipliers = append(multipliers, damageStatMultiplier{rule.Numerator, rule.Denominator})
	}
	return applyDamageStatMultipliers(value, multipliers...)
}

// attackingStatMultiplierIgnoresBurn 报告当前已激活攻击规则是否明确绕过灼伤减半。
func attackingStatMultiplierIgnoresBurn(member MemberSnapshot, weather *WeatherEffect, terrain *TerrainEffect) bool {
	rule := member.AttackingStatMultiplier
	return rule != nil && rule.Stat == StatAttack && rule.IgnoreBurnAttackReduction &&
		attackingStatMultiplierActive(rule, member, weather, terrain)
}

// cloneAttackingStatMultiplier 深复制可选攻击能力倍率及其异常集合。
func cloneAttackingStatMultiplier(value *AttackingStatMultiplier) *AttackingStatMultiplier {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.RequiredMajorStatuses = append([]MajorStatus(nil), value.RequiredMajorStatuses...)
	return &cloned
}

// cloneOpponentAttackingStatMultiplier 深复制可选目标特性攻击能力倍率。
func cloneOpponentAttackingStatMultiplier(value *OpponentAttackingStatMultiplier) *OpponentAttackingStatMultiplier {
	return cloneValue(value)
}

// cloneDefendingStatMultiplier 深复制可选防守能力倍率。
func cloneDefendingStatMultiplier(value *DefendingStatMultiplier) *DefendingStatMultiplier {
	return cloneValue(value)
}

// cloneOpponentDefendingStatMultiplier 深复制可选攻击方特性目标防御倍率。
func cloneOpponentDefendingStatMultiplier(value *OpponentDefendingStatMultiplier) *OpponentDefendingStatMultiplier {
	return cloneValue(value)
}

// cloneAllyAbilityPresenceAttackingStatMultiplier 深复制可选伙伴互助组攻击能力倍率。
func cloneAllyAbilityPresenceAttackingStatMultiplier(value *AllyAbilityPresenceAttackingStatMultiplier) *AllyAbilityPresenceAttackingStatMultiplier {
	return cloneValue(value)
}
