package battleengine

import "fmt"

// AbilityHPChangedEvent 记录反应型特性造成的一段实际生命回复或伤害。
type AbilityHPChangedEvent struct {
	// Type 是稳定事件种类 abilityHpChanged。
	Type EventKind `json:"kind"`
	// SchemaVersion 是事件载荷版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是事件所属回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是提供特性规则的成员引用。
	Source MemberRef `json:"source"`
	// Target 是生命实际变化的成员引用。
	Target MemberRef `json:"target"`
	// Effect 是封闭的触发语义代码。
	Effect string `json:"effect"`
	// Healing 表示 Amount 是回复而不是伤害。
	Healing bool `json:"healing"`
	// Amount 是实际写入的生命变化量。
	Amount uint32 `json:"amount"`
	// CurrentHP 是写入后的目标当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 abilityHpChanged。
func (event AbilityHPChangedEvent) Kind() EventKind { return event.Type }

// AbilityChargeChangedEvent 记录受伤充能的建立或消费。
type AbilityChargeChangedEvent struct {
	// Type 是稳定事件种类 abilityChargeChanged。
	Type EventKind `json:"kind"`
	// SchemaVersion 是事件载荷版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是事件所属回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是储存或消费充能的成员。
	Member MemberRef `json:"member"`
	// ElementID 是充能匹配的有效属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
	// Consumed 表示该事件消费了已有充能；false 表示建立或刷新充能。
	Consumed bool `json:"consumed"`
}

// Kind 返回 abilityChargeChanged。
func (event AbilityChargeChangedEvent) Kind() EventKind { return event.Type }

// StatStageDelta 描述一项冻结的能力阶级变化。
//
// Stat 必须是 Battle Engine 支持的七项可变能力之一；Delta 为正表示提升、为负表示降低。实际写入仍由
// applyStatStageDelta 夹在 -6..6，资料层不能通过超大变化量绕过引擎边界。
type StatStageDelta struct {
	// Stat 是要改变的能力项。
	Stat Stat `json:"stat"`
	// Delta 是本次声明的能力阶级变化量。
	Delta int8 `json:"delta"`
}

// MajorStatusEndTurnHealing 描述以主要异常替换其原始回合末伤害的固定比例回复规则。
type MajorStatusEndTurnHealing struct {
	// Statuses 是会触发回复的主要异常集合。
	Statuses []MajorStatus `json:"statuses"`
	// Denominator 是以最大生命为分子的正分母。
	Denominator uint16 `json:"denominator"`
}

// WeatherEndTurnDamage 描述仅在指定有效普通天气中对持有者造成的回合末间接伤害。
type WeatherEndTurnDamage struct {
	// Weathers 是允许触发的普通天气集合。
	Weathers []WeatherKind `json:"weathers"`
	// Denominator 是以最大生命为分子的正分母。
	Denominator uint16 `json:"denominator"`
}

// OpponentMajorStatusEndTurnDamage 描述对处于指定主要异常的上场对手造成的回合末间接伤害。
type OpponentMajorStatusEndTurnDamage struct {
	// Statuses 是允许触发的对手主要异常集合。
	Statuses []MajorStatus `json:"statuses"`
	// Denominator 是以目标最大生命为分子的正分母。
	Denominator uint16 `json:"denominator"`
}

// EndTurnMajorStatusCure 描述回合末按概率治愈自身主要异常的规则。
type EndTurnMajorStatusCure struct {
	// ChancePercent 是 1..100 的成功概率。
	ChancePercent uint8 `json:"chancePercent"`
	// RequiredWeathers 是允许触发的有效普通天气集合；空集合表示不要求天气。
	RequiredWeathers []WeatherKind `json:"requiredWeathers,omitempty"`
}

// EndTurnRandomStatStageChange 描述回合末随机选择两个不同能力项进行升降的规则。
type EndTurnRandomStatStageChange struct {
	// RaiseDelta 是随机提升项的正变化量。
	RaiseDelta int8 `json:"raiseDelta"`
	// LowerDelta 是另一随机降低项的负变化量。
	LowerDelta int8 `json:"lowerDelta"`
}

// FaintStatStageBoost 描述倒下事件发生后触发的能力提升。
type FaintStatStageBoost struct {
	// Stat 是被提升的能力项。
	Stat Stat `json:"stat"`
	// Delta 是正能力阶级变化量。
	Delta int8 `json:"delta"`
	// RequiresCausedFaint 为 true 时，仅由持有者本次技能造成的倒下触发。
	RequiresCausedFaint bool `json:"requiresCausedFaint"`
}

// FaintAttackerDamage 描述持有者因技能伤害倒下后对攻击者的反制伤害。
type FaintAttackerDamage struct {
	// RequiresContact 表示原技能必须仍构成有效接触。
	RequiresContact bool `json:"requiresContact"`
	// AttackerMaxHPDenominator 表示按攻击者最大生命计算伤害的正分母；0 表示不用该算法。
	AttackerMaxHPDenominator uint16 `json:"attackerMaxHpDenominator"`
	// UsesDamageTaken 表示改用持有者本次实际损失生命作为伤害。
	UsesDamageTaken bool `json:"usesDamageTaken"`
	// SuppressedByExplosionSuppression 表示场上任一存活成员的爆炸效果封锁可阻止本规则。
	SuppressedByExplosionSuppression bool `json:"suppressedByExplosionSuppression"`
}

// ReceivedDamageStatStageChange 描述承受真实本体技能伤害后的能力阶级变化。
type ReceivedDamageStatStageChange struct {
	// Changes 是按声明顺序执行的能力阶级变化。
	Changes []StatStageDelta `json:"changes"`
	// RequiresContact 表示本次技能必须仍构成有效接触。
	RequiresContact bool `json:"requiresContact"`
	// ChangesAttacker 表示把 Changes 施加给攻击者而不是持有者。
	ChangesAttacker bool `json:"changesAttacker"`
	// ElementIDs 是允许触发的技能有效属性集合；空集合表示不限制属性。
	ElementIDs []Identifier `json:"elementIds,omitempty"`
}

// ReceivedDamageCharge 描述受伤后为下一次指定属性攻击储存一次性伤害倍率的规则。
type ReceivedDamageCharge struct {
	// ElementID 是被强化的技能有效属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
	// Numerator 是精确倍率的正分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正分母。
	Denominator uint16 `json:"denominator"`
}

// ReactiveAbilityRules 汇集按回合末、受伤和倒下窗口结算的冻结特性规则。
//
// 这些规则共享触发窗口但保留各自的强类型字段，避免把效果压缩成自由文本或通用键值集合。结构整体由 Battle
// 在开战时冻结；Battle Engine 在运行中绝不访问实时特性资料。
type ReactiveAbilityRules struct {
	// EndTurnStatStageChanges 是每回合末固定施加给持有者的能力变化。
	EndTurnStatStageChanges []StatStageDelta `json:"endTurnStatStageChanges,omitempty"`
	// MajorStatusEndTurnHealing 是以异常原始伤害替换为回复的规则。
	MajorStatusEndTurnHealing *MajorStatusEndTurnHealing `json:"majorStatusEndTurnHealing,omitempty"`
	// WeatherEndTurnDamage 是匹配天气下对持有者造成伤害的规则。
	WeatherEndTurnDamage *WeatherEndTurnDamage `json:"weatherEndTurnDamage,omitempty"`
	// OpponentMajorStatusEndTurnDamage 是对匹配异常的上场对手造成伤害的规则。
	OpponentMajorStatusEndTurnDamage *OpponentMajorStatusEndTurnDamage `json:"opponentMajorStatusEndTurnDamage,omitempty"`
	// EndTurnMajorStatusCure 是回合末治愈自身主要异常的规则。
	EndTurnMajorStatusCure *EndTurnMajorStatusCure `json:"endTurnMajorStatusCure,omitempty"`
	// EndTurnAllyMajorStatusCureChance 是回合末治愈一名上场伙伴的百分比概率；0 表示没有规则。
	EndTurnAllyMajorStatusCureChance uint8 `json:"endTurnAllyMajorStatusCureChance"`
	// EndTurnRandomStatStageChange 是随机选择不同能力项升降的规则。
	EndTurnRandomStatStageChange *EndTurnRandomStatStageChange `json:"endTurnRandomStatStageChange,omitempty"`
	// OncePerBattleCausedFaintMultiStatBoost 是持有者首次造成倒下时同时提升的能力集合。
	OncePerBattleCausedFaintMultiStatBoost []StatStageDelta `json:"oncePerBattleCausedFaintMultiStatBoost,omitempty"`
	// FaintStatStageBoosts 是倒下事件触发的能力提升集合。
	FaintStatStageBoosts []FaintStatStageBoost `json:"faintStatStageBoosts,omitempty"`
	// FaintHighestStatBoost 表示持有者造成倒下后提升五项原始能力中最高的一项。
	FaintHighestStatBoost bool `json:"faintHighestStatBoost"`
	// FaintAttackerDamage 是持有者因技能伤害倒下后的攻击者反制规则。
	FaintAttackerDamage *FaintAttackerDamage `json:"faintAttackerDamage,omitempty"`
	// ExplosionEffectSuppression 表示该成员存活在场时封锁声明可被爆炸效果封锁的倒下反制。
	ExplosionEffectSuppression bool `json:"explosionEffectSuppression"`
	// CriticalDamageSetStatStage 是持有者存活承受要害本体伤害后直接设置的能力阶级。
	CriticalDamageSetStatStage *StatStageDelta `json:"criticalDamageSetStatStage,omitempty"`
	// DamageCrossedHalfHPStatStageChanges 是生命从半血以上跨至半血或以下时执行的一组能力变化。
	DamageCrossedHalfHPStatStageChanges []StatStageDelta `json:"damageCrossedHalfHpStatStageChanges,omitempty"`
	// ReceivedDamageStatStageChanges 是承受真实本体技能伤害后按顺序执行的能力规则。
	ReceivedDamageStatStageChanges []ReceivedDamageStatStageChange `json:"receivedDamageStatStageChanges,omitempty"`
	// ReceivedDamageCharge 是受伤后储存下一次属性攻击强化的规则。
	ReceivedDamageCharge *ReceivedDamageCharge `json:"receivedDamageCharge,omitempty"`
	// ReceivedDamageAttackerMajorStatus 是持有者承受真实本体技能伤害后施加给攻击者的主要异常。
	// 空值表示没有规则；状态仍经过属性、场地、已有异常和一次性治疗道具的统一校验链路。
	ReceivedDamageAttackerMajorStatus MajorStatus `json:"receivedDamageAttackerMajorStatus,omitempty"`
}

func cloneReactiveAbilityRules(value *ReactiveAbilityRules) *ReactiveAbilityRules {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.EndTurnStatStageChanges = append([]StatStageDelta(nil), value.EndTurnStatStageChanges...)
	cloned.OncePerBattleCausedFaintMultiStatBoost = append([]StatStageDelta(nil), value.OncePerBattleCausedFaintMultiStatBoost...)
	cloned.FaintStatStageBoosts = append([]FaintStatStageBoost(nil), value.FaintStatStageBoosts...)
	cloned.DamageCrossedHalfHPStatStageChanges = append([]StatStageDelta(nil), value.DamageCrossedHalfHPStatStageChanges...)
	cloned.ReceivedDamageStatStageChanges = append([]ReceivedDamageStatStageChange(nil), value.ReceivedDamageStatStageChanges...)
	for index := range cloned.ReceivedDamageStatStageChanges {
		cloned.ReceivedDamageStatStageChanges[index].Changes = append([]StatStageDelta(nil), value.ReceivedDamageStatStageChanges[index].Changes...)
		cloned.ReceivedDamageStatStageChanges[index].ElementIDs = append([]Identifier(nil), value.ReceivedDamageStatStageChanges[index].ElementIDs...)
	}
	if value.MajorStatusEndTurnHealing != nil {
		v := *value.MajorStatusEndTurnHealing
		v.Statuses = append([]MajorStatus(nil), value.MajorStatusEndTurnHealing.Statuses...)
		cloned.MajorStatusEndTurnHealing = &v
	}
	if value.WeatherEndTurnDamage != nil {
		v := *value.WeatherEndTurnDamage
		v.Weathers = append([]WeatherKind(nil), value.WeatherEndTurnDamage.Weathers...)
		cloned.WeatherEndTurnDamage = &v
	}
	if value.OpponentMajorStatusEndTurnDamage != nil {
		v := *value.OpponentMajorStatusEndTurnDamage
		v.Statuses = append([]MajorStatus(nil), value.OpponentMajorStatusEndTurnDamage.Statuses...)
		cloned.OpponentMajorStatusEndTurnDamage = &v
	}
	if value.EndTurnMajorStatusCure != nil {
		v := *value.EndTurnMajorStatusCure
		v.RequiredWeathers = append([]WeatherKind(nil), value.EndTurnMajorStatusCure.RequiredWeathers...)
		cloned.EndTurnMajorStatusCure = &v
	}
	if value.EndTurnRandomStatStageChange != nil {
		v := *value.EndTurnRandomStatStageChange
		cloned.EndTurnRandomStatStageChange = &v
	}
	if value.FaintAttackerDamage != nil {
		v := *value.FaintAttackerDamage
		cloned.FaintAttackerDamage = &v
	}
	if value.CriticalDamageSetStatStage != nil {
		v := *value.CriticalDamageSetStatStage
		cloned.CriticalDamageSetStatStage = &v
	}
	if value.ReceivedDamageCharge != nil {
		v := *value.ReceivedDamageCharge
		cloned.ReceivedDamageCharge = &v
	}
	return &cloned
}

// CloneReactiveAbilityRules 为 Battle 冻结与资料边界提供规则的深复制。
// 返回值与输入完全隔离，调用方修改切片或嵌套结构不会污染已经开始的对局。
func CloneReactiveAbilityRules(value *ReactiveAbilityRules) *ReactiveAbilityRules {
	return cloneReactiveAbilityRules(value)
}

func validateReactiveAbilityRules(value *ReactiveAbilityRules) error {
	if value == nil {
		return nil
	}
	validDelta := func(change StatStageDelta) bool {
		switch change.Stat {
		case StatAttack, StatDefense, StatSpecialAttack, StatSpecialDefense, StatSpeed, StatAccuracy, StatEvasion:
			return change.Delta != 0 && change.Delta >= -6 && change.Delta <= 6
		default:
			return false
		}
	}
	for _, changes := range [][]StatStageDelta{value.EndTurnStatStageChanges, value.OncePerBattleCausedFaintMultiStatBoost, value.DamageCrossedHalfHPStatStageChanges} {
		for _, change := range changes {
			if !validDelta(change) {
				return fmt.Errorf("能力阶级变化无效")
			}
		}
	}
	validStatuses := func(statuses []MajorStatus) bool {
		if len(statuses) == 0 || len(statuses) > 6 {
			return false
		}
		seen := make(map[MajorStatus]struct{}, len(statuses))
		for _, status := range statuses {
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
	validWeathers := func(weathers []WeatherKind, allowEmpty bool) bool {
		if len(weathers) == 0 {
			return allowEmpty
		}
		if len(weathers) > 4 {
			return false
		}
		seen := make(map[WeatherKind]struct{}, len(weathers))
		for _, weather := range weathers {
			if !weather.valid() {
				return false
			}
			if _, duplicate := seen[weather]; duplicate {
				return false
			}
			seen[weather] = struct{}{}
		}
		return true
	}
	if rule := value.MajorStatusEndTurnHealing; rule != nil && (!validStatuses(rule.Statuses) || rule.Denominator == 0) {
		return fmt.Errorf("异常回合末回复规则无效")
	}
	if rule := value.WeatherEndTurnDamage; rule != nil && (!validWeathers(rule.Weathers, false) || rule.Denominator == 0) {
		return fmt.Errorf("天气回合末伤害规则无效")
	}
	if rule := value.OpponentMajorStatusEndTurnDamage; rule != nil && (!validStatuses(rule.Statuses) || rule.Denominator == 0) {
		return fmt.Errorf("对手异常回合末伤害规则无效")
	}
	if rule := value.EndTurnMajorStatusCure; rule != nil && (rule.ChancePercent == 0 || rule.ChancePercent > 100 || !validWeathers(rule.RequiredWeathers, true)) {
		return fmt.Errorf("回合末异常治愈规则无效")
	}
	if value.EndTurnAllyMajorStatusCureChance > 100 {
		return fmt.Errorf("伙伴异常治愈概率必须不大于 100")
	}
	if rule := value.EndTurnRandomStatStageChange; rule != nil && (rule.RaiseDelta <= 0 || rule.LowerDelta >= 0) {
		return fmt.Errorf("随机能力升降方向无效")
	}
	for _, boost := range value.FaintStatStageBoosts {
		if !validDelta(StatStageDelta{Stat: boost.Stat, Delta: boost.Delta}) {
			return fmt.Errorf("倒下能力强化规则无效")
		}
	}
	if rule := value.FaintAttackerDamage; rule != nil {
		usesMaximumHP := rule.AttackerMaxHPDenominator != 0
		if usesMaximumHP == rule.UsesDamageTaken {
			return fmt.Errorf("倒下攻击者反制必须且只能声明一种伤害算法")
		}
	}
	if value.CriticalDamageSetStatStage != nil && !validDelta(*value.CriticalDamageSetStatStage) {
		return fmt.Errorf("要害能力设置规则无效")
	}
	for _, rule := range value.ReceivedDamageStatStageChanges {
		if len(rule.Changes) == 0 {
			return fmt.Errorf("受伤能力变化不能为空")
		}
		for _, change := range rule.Changes {
			if !validDelta(change) {
				return fmt.Errorf("受伤能力变化无效")
			}
		}
		seen := make(map[Identifier]struct{}, len(rule.ElementIDs))
		for _, id := range rule.ElementIDs {
			if id == 0 {
				return fmt.Errorf("受伤能力变化属性为空")
			}
			if _, duplicate := seen[id]; duplicate {
				return fmt.Errorf("受伤能力变化属性重复")
			}
			seen[id] = struct{}{}
		}
	}
	if rule := value.ReceivedDamageCharge; rule != nil && (rule.ElementID == 0 || rule.Numerator == 0 || rule.Denominator == 0) {
		return fmt.Errorf("受伤充能规则不完整")
	}
	if value.ReceivedDamageAttackerMajorStatus != "" && !value.ReceivedDamageAttackerMajorStatus.Valid() {
		return fmt.Errorf("受伤反制主要异常无效")
	}
	return nil
}

// ValidateReactiveAbilityRules 校验资料层准备冻结的反应型特性规则。
func ValidateReactiveAbilityRules(value *ReactiveAbilityRules) error {
	return validateReactiveAbilityRules(value)
}

// reactiveRules 返回只读零值规则，减少各触发阶段对 nil 的重复分支。
func reactiveRules(member MemberSnapshot) ReactiveAbilityRules {
	if member.ReactiveAbilityRules == nil {
		return ReactiveAbilityRules{}
	}
	return *member.ReactiveAbilityRules
}

// applyReceivedDamageAttackerMajorStatus 把受伤反制特性声明的主要异常写入本次伤害来源。
//
// source 是特性持有者，target 是攻击者。该入口复用技能异常相同的已有异常、属性、场地与一次性治疗道具
// 约束，并发布相同结构化事件，使回放不需要通过特性名称解释状态变化。
func applyReceivedDamageAttackerMajorStatus(
	state State,
	source MemberRef,
	targetRef MemberRef,
	status MajorStatus,
) (State, []Event) {
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !found || target.CurrentHP == 0 || status == "" {
		return state, nil
	}
	if target.MajorStatus != "" {
		return state, []Event{MajorStatusBlockedEvent{
			Type: EventKindMajorStatusBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: source, Target: targetRef, Status: status, Reason: MajorStatusBlockReasonExistingStatus,
		}}
	}
	if majorStatusBlockedByElement(state.rules, target, status) {
		return state, []Event{MajorStatusBlockedEvent{
			Type: EventKindMajorStatusBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: source, Target: targetRef, Status: status, Reason: MajorStatusBlockReasonElementImmunity,
		}}
	}
	if terrainBlocksMajorStatus(state.environment.Terrain, state.rules, target, status) {
		return state, []Event{MajorStatusBlockedEvent{
			Type: EventKindMajorStatusBlocked, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: source, Target: targetRef, Status: status, Reason: MajorStatusBlockReasonTerrainImmunity,
		}}
	}
	target.MajorStatus = status
	state.replaceMember(targetRef.Side, target)
	events := []Event{MajorStatusAppliedEvent{
		Type: EventKindMajorStatusApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: source, Target: targetRef, Status: status,
	}}
	var cured []Event
	state, cured = applyHeldItemParalysisCure(state, targetRef, status)
	events = append(events, cured...)
	state, cured = applyHeldItemPoisonCure(state, targetRef, status)
	events = append(events, cured...)
	state, cured = applyHeldItemBurnCure(state, targetRef, status)
	events = append(events, cured...)
	state, cured = applyHeldItemFreezeCure(state, targetRef, status)
	events = append(events, cured...)
	state, cured = applyHeldItemAllMajorStatusCure(state, targetRef, status)
	events = append(events, cured...)
	return state, events
}

func containsMajorStatus(values []MajorStatus, target MajorStatus) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}

func containsWeather(values []WeatherKind, target WeatherKind) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}

// applyReactiveStatChanges 按声明顺序写入能力阶级并发布每项实际变化事件。
func applyReactiveStatChanges(state State, source, target MemberRef, changes []StatStageDelta) (State, []Event) {
	member, ok := state.member(target.Side, target.Position)
	if !ok || member.CurrentHP == 0 {
		return state, nil
	}
	events := make([]Event, 0, len(changes))
	for _, change := range changes {
		before := member.StatStages[change.Stat]
		after := max(int8(-6), min(int8(6), before+change.Delta))
		if before == after {
			continue
		}
		member.StatStages = cloneStatStages(member.StatStages)
		member.StatStages[change.Stat] = after
		events = append(events, StatStageChangedEvent{Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: source, Target: target, Stat: change.Stat, Delta: after - before, CurrentStage: after})
	}
	state.replaceMember(target.Side, member)
	return state, events
}

// resolveEndTurnReactiveAbilities 在环境持续时间递减前按稳定阵营与席位顺序执行回合末特性。
// 这保证天气最后一个有效回合仍能触发依赖天气的伤害与治愈，随后环境阶段才发布天气结束事件。
func resolveEndTurnReactiveAbilities(state State, random RandomSource) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	events := make([]Event, 0, 12)
	trace := make([]RandomTraceEntry, 0, 4)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, ok := state.member(side.Side, position)
			if !ok || member.CurrentHP == 0 || member.ReactiveAbilityRules == nil {
				continue
			}
			ref := MemberRef{Side: side.Side, Position: position}
			rules := reactiveRules(member)
			var changes []Event
			state, changes = applyReactiveStatChanges(state, ref, ref, rules.EndTurnStatStageChanges)
			events = append(events, changes...)

			member, _ = state.member(side.Side, position)
			if rule := rules.WeatherEndTurnDamage; rule != nil && !member.IndirectDamageImmunity {
				weather := effectiveWeather(state)
				if weather != nil && containsWeather(rule.Weathers, weather.Kind) {
					damage := min(max(member.MaxHP/uint32(rule.Denominator), 1), member.CurrentHP)
					member.CurrentHP -= damage
					state.replaceMember(side.Side, member)
					events = append(events, AbilityHPChangedEvent{Type: EventKindAbilityHPChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Source: ref, Target: ref, Effect: "weatherEndTurnDamage", Amount: damage, CurrentHP: member.CurrentHP})
				}
			}

			if rule := rules.OpponentMajorStatusEndTurnDamage; rule != nil {
				for _, opponentSide := range state.sides {
					if opponentSide.Side == side.Side {
						continue
					}
					for _, opponentPosition := range opponentSide.ActiveMembers {
						target, found := state.member(opponentSide.Side, opponentPosition)
						if !found || target.CurrentHP == 0 || target.IndirectDamageImmunity || !containsMajorStatus(rule.Statuses, target.MajorStatus) {
							continue
						}
						damage := min(max(target.MaxHP/uint32(rule.Denominator), 1), target.CurrentHP)
						target.CurrentHP -= damage
						state.replaceMember(opponentSide.Side, target)
						targetRef := MemberRef{Side: opponentSide.Side, Position: opponentPosition}
						events = append(events, AbilityHPChangedEvent{Type: EventKindAbilityHPChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Source: ref, Target: targetRef, Effect: "opponentMajorStatusEndTurnDamage", Amount: damage, CurrentHP: target.CurrentHP})
					}
				}
			}

			member, _ = state.member(side.Side, position)
			if rule := rules.EndTurnMajorStatusCure; rule != nil && member.MajorStatus != "" {
				weatherBattlees := len(rule.RequiredWeathers) == 0
				if weather := effectiveWeather(state); weather != nil && containsWeather(rule.RequiredWeathers, weather.Kind) {
					weatherBattlees = true
				}
				if weatherBattlees {
					success := rule.ChancePercent == 100
					if rule.ChancePercent < 100 {
						roll, next, entry, err := random.Next(100, "end turn major status cure")
						if err != nil {
							return State{}, RandomSource{}, nil, nil, err
						}
						random = next
						trace = append(trace, entry)
						success = roll+1 <= int32(rule.ChancePercent)
					}
					if success {
						status := member.MajorStatus
						member.MajorStatus = ""
						member.SleepTurnsRemaining = 0
						member.BadPoisonCounter = 0
						state.replaceMember(side.Side, member)
						events = append(events, MajorStatusClearedEvent{Type: EventKindMajorStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: ref, Status: status})
					}
				}
			}

			if rules.EndTurnAllyMajorStatusCureChance != 0 {
				for _, allyPosition := range side.ActiveMembers {
					if allyPosition == position {
						continue
					}
					ally, found := state.member(side.Side, allyPosition)
					if !found || ally.CurrentHP == 0 || ally.MajorStatus == "" {
						continue
					}
					roll, next, entry, err := random.Next(100, "end turn ally major status cure")
					if err != nil {
						return State{}, RandomSource{}, nil, nil, err
					}
					random = next
					trace = append(trace, entry)
					if roll+1 > int32(rules.EndTurnAllyMajorStatusCureChance) {
						break
					}
					status := ally.MajorStatus
					ally.MajorStatus = ""
					ally.SleepTurnsRemaining = 0
					ally.BadPoisonCounter = 0
					state.replaceMember(side.Side, ally)
					events = append(events, MajorStatusClearedEvent{Type: EventKindMajorStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber, Target: MemberRef{Side: side.Side, Position: allyPosition}, Status: status})
					break
				}
			}

			if rule := rules.EndTurnRandomStatStageChange; rule != nil {
				stats := []Stat{StatAttack, StatDefense, StatSpecialAttack, StatSpecialDefense, StatSpeed, StatAccuracy, StatEvasion}
				raiseIndex, next, entry, err := random.Next(int32(len(stats)), "end turn random stat raise")
				if err != nil {
					return State{}, RandomSource{}, nil, nil, err
				}
				random = next
				trace = append(trace, entry)
				lowerIndex, next, entry, err := random.Next(int32(len(stats)-1), "end turn random stat lower")
				if err != nil {
					return State{}, RandomSource{}, nil, nil, err
				}
				random = next
				trace = append(trace, entry)
				if lowerIndex >= raiseIndex {
					lowerIndex++
				}
				state, changes = applyReactiveStatChanges(state, ref, ref, []StatStageDelta{{Stat: stats[raiseIndex], Delta: rule.RaiseDelta}, {Stat: stats[lowerIndex], Delta: rule.LowerDelta}})
				events = append(events, changes...)
			}
		}
	}
	return state, random, events, trace, nil
}

// resolveReactiveAbilityAfterAction 扫描当前行动的本体伤害与倒下事件并执行对应触发规则。
func resolveReactiveAbilityAfterAction(state State, actionEvents []Event) (State, []Event) {
	events := make([]Event, 0, 12)
	for _, raw := range actionEvents {
		damage, ok := raw.(DamageAppliedEvent)
		if !ok || damage.Amount == 0 {
			continue
		}
		target, found := state.member(damage.Target.Side, damage.Target.Position)
		attacker, attackerFound := state.member(damage.Actor.Side, damage.Actor.Position)
		if !found || !attackerFound || target.ReactiveAbilityRules == nil || ignoresTargetAbilityEffects(attacker, skillByID(attacker, damage.SkillID)) {
			continue
		}
		rules := reactiveRules(target)
		if rules.ReceivedDamageAttackerMajorStatus != "" {
			var statusEvents []Event
			state, statusEvents = applyReceivedDamageAttackerMajorStatus(
				state, damage.Target, damage.Actor, rules.ReceivedDamageAttackerMajorStatus,
			)
			events = append(events, statusEvents...)
		}
		if target.CurrentHP != 0 && damage.CriticalHit && rules.CriticalDamageSetStatStage != nil {
			change := *rules.CriticalDamageSetStatStage
			before := target.StatStages[change.Stat]
			change.Delta = change.Delta - before
			var changed []Event
			state, changed = applyReactiveStatChanges(state, damage.Target, damage.Target, []StatStageDelta{change})
			events = append(events, changed...)
		}
		target, _ = state.member(damage.Target.Side, damage.Target.Position)
		if target.CurrentHP != 0 && !target.HalfHPThresholdAbilityActivated && damage.CurrentHP*2 <= target.MaxHP && (damage.CurrentHP+damage.Amount)*2 > target.MaxHP && len(rules.DamageCrossedHalfHPStatStageChanges) != 0 {
			target.HalfHPThresholdAbilityActivated = true
			state.replaceMember(damage.Target.Side, target)
			var changed []Event
			state, changed = applyReactiveStatChanges(state, damage.Target, damage.Target, rules.DamageCrossedHalfHPStatStageChanges)
			events = append(events, changed...)
		}
		if target.CurrentHP != 0 {
			skill := skillByID(attacker, damage.SkillID)
			for _, rule := range rules.ReceivedDamageStatStageChanges {
				if rule.RequiresContact && !skillMakesEffectiveContact(attacker, skill) {
					continue
				}
				elementID := effectiveSkillElementForMember(attacker, skill, effectiveSkillWeather(state, attacker))
				if len(rule.ElementIDs) != 0 && !containsString(rule.ElementIDs, elementID) {
					continue
				}
				destination := damage.Target
				if rule.ChangesAttacker {
					destination = damage.Actor
				}
				var changed []Event
				state, changed = applyReactiveStatChanges(state, damage.Target, destination, rule.Changes)
				events = append(events, changed...)
			}
			if rule := rules.ReceivedDamageCharge; rule != nil {
				target, _ = state.member(damage.Target.Side, damage.Target.Position)
				target.ChargedElementID = rule.ElementID
				target.ChargedDamageNumerator = rule.Numerator
				target.ChargedDamageDenominator = rule.Denominator
				state.replaceMember(damage.Target.Side, target)
				events = append(events, AbilityChargeChangedEvent{Type: EventKindAbilityChargeChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Member: damage.Target, ElementID: rule.ElementID})
			}
		}
	}

	for _, raw := range actionEvents {
		fainted, ok := raw.(ParticipantFaintedEvent)
		if !ok {
			continue
		}
		var killer MemberRef
		caused := false
		damageTaken := uint32(0)
		skill := SkillSnapshot{}
		for _, candidate := range actionEvents {
			if hit, hitOK := candidate.(DamageAppliedEvent); hitOK && hit.Target == fainted.Target {
				killer = hit.Actor
				caused = true
				damageTaken += hit.Amount
				if actor, found := state.member(killer.Side, killer.Position); found {
					skill = skillByID(actor, hit.SkillID)
				}
			}
		}
		for _, side := range state.sides {
			for _, position := range side.ActiveMembers {
				holder, found := state.member(side.Side, position)
				if !found || holder.CurrentHP == 0 || holder.ReactiveAbilityRules == nil {
					continue
				}
				ref := MemberRef{Side: side.Side, Position: position}
				rules := reactiveRules(holder)
				for _, boost := range rules.FaintStatStageBoosts {
					if boost.RequiresCausedFaint && (!caused || killer != ref) {
						continue
					}
					var changed []Event
					state, changed = applyReactiveStatChanges(state, ref, ref, []StatStageDelta{{Stat: boost.Stat, Delta: boost.Delta}})
					events = append(events, changed...)
				}
				if caused && killer == ref && !holder.OncePerBattleFaintBoostActivated && len(rules.OncePerBattleCausedFaintMultiStatBoost) != 0 {
					holder.OncePerBattleFaintBoostActivated = true
					state.replaceMember(side.Side, holder)
					var changed []Event
					state, changed = applyReactiveStatChanges(state, ref, ref, rules.OncePerBattleCausedFaintMultiStatBoost)
					events = append(events, changed...)
				}
				if caused && killer == ref && rules.FaintHighestStatBoost {
					var changed []Event
					state, changed = applyReactiveStatChanges(state, ref, ref, []StatStageDelta{{Stat: highestRawBattleStat(holder.Stats), Delta: 1}})
					events = append(events, changed...)
				}
			}
		}
		faintedMember, found := state.member(fainted.Target.Side, fainted.Target.Position)
		if !found || !caused || faintedMember.ReactiveAbilityRules == nil {
			continue
		}
		rule := reactiveRules(faintedMember).FaintAttackerDamage
		if rule == nil || rule.RequiresContact && !skillMakesEffectiveContact(mustMember(state, killer), skill) || rule.SuppressedByExplosionSuppression && explosionEffectsSuppressed(state) {
			continue
		}
		attacker, found := state.member(killer.Side, killer.Position)
		if !found || attacker.CurrentHP == 0 {
			continue
		}
		amount := damageTaken
		if rule.AttackerMaxHPDenominator != 0 {
			amount = max(attacker.MaxHP/uint32(rule.AttackerMaxHPDenominator), 1)
		}
		amount = min(amount, attacker.CurrentHP)
		attacker.CurrentHP -= amount
		state.replaceMember(killer.Side, attacker)
		events = append(events, AbilityHPChangedEvent{Type: EventKindAbilityHPChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Source: fainted.Target, Target: killer, Effect: "faintAttackerDamage", Amount: amount, CurrentHP: attacker.CurrentHP})
	}
	return state, events
}

func skillByID(member MemberSnapshot, skillID Identifier) SkillSnapshot {
	for _, skill := range member.Skills {
		if skill.SkillID == skillID {
			return skill
		}
	}
	return SkillSnapshot{}
}
func mustMember(state State, ref MemberRef) MemberSnapshot {
	member, _ := state.member(ref.Side, ref.Position)
	return member
}
func explosionEffectsSuppressed(state State) bool {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, ok := state.member(side.Side, position)
			if ok && member.CurrentHP != 0 && reactiveRules(member).ExplosionEffectSuppression {
				return true
			}
		}
	}
	return false
}

// consumeReactiveAbilityCharge 在匹配攻击实际造成本体伤害后原子清除一次性充能。
func consumeReactiveAbilityCharge(state State, actorRef MemberRef, skill SkillSnapshot, bodyDamage uint32) (State, []Event) {
	actor, found := state.member(actorRef.Side, actorRef.Position)
	if !found || bodyDamage == 0 || actor.ChargedElementID == 0 || actor.ChargedElementID != effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor)) {
		return state, nil
	}
	elementID := actor.ChargedElementID
	actor.ChargedElementID = 0
	actor.ChargedDamageNumerator = 1
	actor.ChargedDamageDenominator = 1
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{AbilityChargeChangedEvent{Type: EventKindAbilityChargeChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Member: actorRef, ElementID: elementID, Consumed: true}}
}
