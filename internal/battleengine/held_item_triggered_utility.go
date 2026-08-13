package battleengine

import (
	"fmt"
	"sort"
)

// HeldItemTrigger 是一次性道具消费事件的稳定触发原因。
type HeldItemTrigger string

const (
	// HeldItemTriggerAccuracyMiss 表示技能因普通命中判定落空。
	HeldItemTriggerAccuracyMiss HeldItemTrigger = "accuracyMiss"
	// HeldItemTriggerSuperEffectiveDamage 表示持有者承受效果绝佳的真实本体技能伤害。
	HeldItemTriggerSuperEffectiveDamage HeldItemTrigger = "superEffectiveDamage"
	// HeldItemTriggerWaterDamage 表示持有者承受水属性真实本体技能伤害。
	HeldItemTriggerWaterDamage HeldItemTrigger = "waterDamage"
	// HeldItemTriggerElectricDamage 表示持有者承受电属性真实本体技能伤害。
	HeldItemTriggerElectricDamage HeldItemTrigger = "electricDamage"
	// HeldItemTriggerWaterDamageSpecialDefense 表示持有者承受水属性伤害后提升特防。
	HeldItemTriggerWaterDamageSpecialDefense HeldItemTrigger = "waterDamageSpecialDefense"
	// HeldItemTriggerIceDamage 表示持有者承受冰属性伤害后提升攻击。
	HeldItemTriggerIceDamage HeldItemTrigger = "iceDamage"
	// HeldItemTriggerFieldSpeedOrderStarted 表示戏法空间已成功建立。
	HeldItemTriggerFieldSpeedOrderStarted HeldItemTrigger = "fieldSpeedOrderStarted"
	// HeldItemTriggerLowHPActionOrder 表示低生命行动顺序道具在排序前触发。
	HeldItemTriggerLowHPActionOrder HeldItemTrigger = "lowHpActionOrder"
)

// HeldItemTriggeredConsumedEvent 记录触发型道具完成效果后清空完整运行态的事实。
type HeldItemTriggeredConsumedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是事件所属回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Holder 是消费道具的成员稳定引用。
	Holder MemberRef `json:"holder"`
	// ItemID 是消费前持有的道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// SkillID 是引发触发的技能稳定 Identifier；环境触发时也保留建立环境的技能。
	SkillID Identifier `json:"skillId,omitempty"`
	// Trigger 是本次触发的稳定原因代码。
	Trigger HeldItemTrigger `json:"trigger"`
}

// Kind 返回 heldItemTriggeredConsumed。
func (event HeldItemTriggeredConsumedEvent) Kind() EventKind { return event.Type }

// HeldItemActionOrderAppliedEvent 记录持有道具对本回合技能行动施加的可观察排序层。
type HeldItemActionOrderAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是排序规则所属回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Holder 是持有道具并提交技能行动的成员稳定引用。
	Holder MemberRef `json:"holder"`
	// ItemID 是本次排序规则的道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// OrderBracket 是最终施加的排序层；正值先行，负值后行。
	OrderBracket int8 `json:"orderBracket"`
}

// Kind 返回 heldItemActionOrderApplied。
func (event HeldItemActionOrderAppliedEvent) Kind() EventKind { return event.Type }

// applyHeldItemAccuracyMissStatStageBoost 结算技能普通命中失败后的使用者侧速度强化道具。
func applyHeldItemAccuracyMissStatStageBoost(state State, actorRef MemberRef, skillID Identifier) (State, []Event) {
	actor, found := state.member(actorRef.Side, actorRef.Position)
	if !found || actor.CurrentHP == 0 || actor.ItemID == 0 || actor.HeldItemAccuracyMissStatStageBoostDelta <= 0 || !actor.HeldItemAccuracyMissStatStageBoostStat.Valid() {
		return state, nil
	}
	stat := actor.HeldItemAccuracyMissStatStageBoostStat
	before := actor.StatStages[stat]
	after := clampHeldItemStatStage(before, actor.HeldItemAccuracyMissStatStageBoostDelta)
	if after == before {
		return state, nil
	}
	itemID := actor.ItemID
	actor.StatStages = cloneStatStages(actor.StatStages)
	actor.StatStages[stat] = after
	actor = clearHeldItemRuntimeState(actor)
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{
		StatStageChangedEvent{Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: actorRef, Target: actorRef, Stat: stat, Delta: after - before, CurrentStage: after},
		HeldItemTriggeredConsumedEvent{Type: EventKindHeldItemTriggeredConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber, Holder: actorRef, ItemID: itemID, SkillID: skillID, Trigger: HeldItemTriggerAccuracyMiss},
	}
}

// applyHeldItemReceivedDamageStatBoost 结算三类真实本体受伤后能力强化道具。
//
// superEffective 已由伤害公式使用同一份冻结相性和强天气规则计算；bodyDamage 为零时明确排除替身、免疫和零伤害。
func applyHeldItemReceivedDamageStatBoost(state State, targetRef MemberRef, skillID, elementID Identifier, superEffective bool, bodyDamage uint32) (State, []Event) {
	// 该类道具只响应具有有效属性的真实本体技能伤害。无属性伤害即使调用方错误地携带了
	// “克制”标记也不能触发，避免 typeless 伤害进入弱点保险或属性反应道具分支。
	if bodyDamage == 0 || elementID == 0 {
		return state, nil
	}
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !found || target.ItemID == 0 {
		return state, nil
	}
	trigger := HeldItemTrigger("")
	changes := []StatStageDelta(nil)
	switch {
	case target.HeldItemWeaknessPolicy && superEffective:
		trigger = HeldItemTriggerSuperEffectiveDamage
		changes = []StatStageDelta{{Stat: StatAttack, Delta: 2}, {Stat: StatSpecialAttack, Delta: 2}}
	case target.HeldItemWaterDamageSpecialAttackBoostElementID != 0 && elementID == target.HeldItemWaterDamageSpecialAttackBoostElementID:
		trigger = HeldItemTriggerWaterDamage
		changes = []StatStageDelta{{Stat: StatSpecialAttack, Delta: 1}}
	case target.HeldItemElectricDamageAttackBoostElementID != 0 && elementID == target.HeldItemElectricDamageAttackBoostElementID:
		trigger = HeldItemTriggerElectricDamage
		changes = []StatStageDelta{{Stat: StatAttack, Delta: 1}}
	case target.HeldItemWaterDamageSpecialDefenseBoostElementID != 0 && elementID == target.HeldItemWaterDamageSpecialDefenseBoostElementID:
		trigger = HeldItemTriggerWaterDamageSpecialDefense
		changes = []StatStageDelta{{Stat: StatSpecialDefense, Delta: 1}}
	case target.HeldItemIceDamageAttackBoostElementID != 0 && elementID == target.HeldItemIceDamageAttackBoostElementID:
		trigger = HeldItemTriggerIceDamage
		changes = []StatStageDelta{{Stat: StatAttack, Delta: 1}}
	default:
		return state, nil
	}
	itemID := target.ItemID
	state, events := applyReactiveStatChanges(state, targetRef, targetRef, changes)
	if len(events) == 0 {
		return state, nil
	}
	target, _ = state.member(targetRef.Side, targetRef.Position)
	target = clearHeldItemRuntimeState(target)
	state.replaceMember(targetRef.Side, target)
	events = append(events, HeldItemTriggeredConsumedEvent{Type: EventKindHeldItemTriggeredConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber, Holder: targetRef, ItemID: itemID, SkillID: skillID, Trigger: trigger})
	return state, events
}

// skillIsSuperEffective 使用与普通伤害相同的有效属性、目标当前属性、标靶和强风口径判断最终相性是否严格大于一。

func skillIsSuperEffective(state State, actorRef, targetRef MemberRef, skill SkillSnapshot) bool {
	actor, actorFound := state.member(actorRef.Side, actorRef.Position)
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !actorFound || !found {
		return false
	}
	elementID := effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor))
	numerator, denominator := uint64(1), uint64(1)
	strongWeather := effectiveStrongWeather(state)
	for _, defenseElementID := range target.ElementIDs {
		partNumerator, partDenominator := state.rules.effectiveness(elementID, defenseElementID)
		if partNumerator == 0 && target.ItemID != 0 && target.HeldItemTypeImmunitySuppression {
			partNumerator, partDenominator = 1, 1
		}
		if strongWeather != nil && strongWindsNeutralizeFlyingWeakness(*strongWeather, state.rules, elementID, defenseElementID) {
			partNumerator, partDenominator = 1, 1
		}
		numerator *= uint64(partNumerator)
		denominator *= uint64(partDenominator)
	}
	return numerator > denominator
}

// effectiveFlinchChance 返回本次畏缩接点使用的概率。
//
// 技能自身已经声明畏缩时不再执行道具的额外接点，避免把两个独立概率错误相加；变化技能同样不触发。
func effectiveFlinchChance(actor MemberSnapshot, skill SkillSnapshot) uint8 {
	if skill.FlinchChancePercent != 0 || actor.ItemID == 0 || skill.DamageClass == DamageClassStatus {
		return skill.FlinchChancePercent
	}
	return actor.HeldItemAdditionalFlinchChancePercent
}

// hasActualSkillDamage 判断当前目标链路是否已记录由本次技能造成的正实际伤害。
func hasActualSkillDamage(events []Event, actor, target MemberRef, skillID Identifier) bool {
	for _, event := range events {
		damage, ok := event.(DamageAppliedEvent)
		if ok && damage.Actor == actor && damage.Target == target && damage.SkillID == skillID && damage.Amount > 0 {
			return true
		}
	}
	return false
}

// applyHeldItemRandomActionOrderBoost 为每项具备概率先行道具的技能计划消费一次显式随机轨迹。
func applyHeldItemRandomActionOrderBoost(plans []actionPlan, random RandomSource) ([]actionPlan, RandomSource, []RandomTraceEntry, error) {
	trace := make([]RandomTraceEntry, 0, len(plans))
	// 客户端提交的行动数组不承担随机消费顺序。先构造稳定的阵营/槽位索引，再逐项掷骰，确保同一回合
	// 在命令数组重排后仍生成相同的先行判定和随机轨迹。
	indices := make([]int, len(plans))
	for index := range plans {
		indices[index] = index
	}
	sort.Slice(indices, func(left, right int) bool {
		leftActor, rightActor := plans[indices[left]].action.Actor, plans[indices[right]].action.Actor
		if leftActor.Side != rightActor.Side {
			return leftActor.Side < rightActor.Side
		}
		return leftActor.Position < rightActor.Position
	})
	for _, index := range indices {
		plan := &plans[index]
		chance := plan.member.HeldItemRandomActionOrderBoostChancePercent
		if plan.action.Kind != ActionKindUseSkill || plan.member.ItemID == 0 || chance == 0 {
			continue
		}
		activated := chance >= 100
		if !activated {
			reason := fmt.Sprintf("held item action order for side %d member %d", plan.action.Actor.Side, plan.member.Position)
			roll, nextRandom, entry, err := random.Next(100, reason)
			if err != nil {
				return nil, RandomSource{}, nil, err
			}
			random = nextRandom
			trace = append(trace, entry)
			activated = roll+1 <= int32(chance)
		}
		if activated {
			plan.orderBracket = 1
		}
	}
	return plans, random, trace, nil
}

// applyHeldItemActionOrderEffects 在排序前统一结算低生命消费先行与概率非消费先行，返回可直接排序的计划。
func applyHeldItemActionOrderEffects(state State, plans []actionPlan, random RandomSource, turnNumber uint32) (State, []actionPlan, RandomSource, []Event, []RandomTraceEntry, error) {
	events := make([]Event, 0)
	beforeRandomBrackets := make([]int8, len(plans))
	for index := range plans {
		plan := &plans[index]
		beforeRandomBrackets[index] = plan.orderBracket
		if plan.action.Kind == ActionKindUseSkill && plan.member.ItemID != 0 && plan.member.HeldItemForcedLastActionOrder {
			events = append(events, HeldItemActionOrderAppliedEvent{Type: EventKindHeldItemActionOrderApplied, SchemaVersion: 1, TurnNumber: turnNumber, Holder: MemberRef{Side: plan.action.Actor.Side, Position: plan.member.Position}, ItemID: plan.member.ItemID, OrderBracket: plan.orderBracket})
		}
		member := plan.member
		if plan.action.Kind != ActionKindUseSkill || member.ItemID == 0 || !member.HeldItemLowHPActionOrderBoost || member.CurrentHP == 0 || member.MaxHP == 0 || uint64(member.CurrentHP)*4 > uint64(member.MaxHP) {
			continue
		}
		member = clearHeldItemRuntimeState(member)
		state.replaceMember(plan.action.Actor.Side, member)
		plan.member = member
		plan.orderBracket = 1
		// 排序计划只把“排序前消费”固化进状态，不产生独立消费事件；道具清空后不向事件流
		// 追加伪造的消费事实。
	}
	nextPlans, nextRandom, trace, err := applyHeldItemRandomActionOrderBoost(plans, random)
	if err != nil {
		return State{}, nil, RandomSource{}, nil, nil, err
	}
	for index := range nextPlans {
		if beforeRandomBrackets[index] == 1 || nextPlans[index].orderBracket != 1 || nextPlans[index].member.ItemID == 0 {
			continue
		}
		events = append(events, HeldItemActionOrderAppliedEvent{Type: EventKindHeldItemActionOrderApplied, SchemaVersion: 1, TurnNumber: turnNumber, Holder: MemberRef{Side: nextPlans[index].action.Actor.Side, Position: nextPlans[index].member.Position}, ItemID: nextPlans[index].member.ItemID, OrderBracket: 1})
	}
	return state, nextPlans, nextRandom, events, trace, nil
}

// applyHeldItemFieldSpeedOrderStatDrop 响应成功建立的戏法空间，对场上客房服务持有者逐一降速并消费。
func applyHeldItemFieldSpeedOrderStatDrop(state State, kind FieldSpeedOrderKind, skillID Identifier) (State, []Event) {
	if kind != FieldSpeedOrderKindTrickRoom {
		return state, nil
	}
	events := make([]Event, 0)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			ref := MemberRef{Side: side.Side, Position: position}
			member, found := state.member(ref.Side, ref.Position)
			if !found || member.CurrentHP == 0 || member.ItemID == 0 || !member.HeldItemFieldSpeedOrderSpeedStageDrop {
				continue
			}
			before := member.StatStages[StatSpeed]
			after := clampHeldItemStatStage(before, -1)
			if after == before {
				continue
			}
			itemID := member.ItemID
			member.StatStages = cloneStatStages(member.StatStages)
			member.StatStages[StatSpeed] = after
			member = clearHeldItemRuntimeState(member)
			state.replaceMember(ref.Side, member)
			events = append(events,
				StatStageChangedEvent{Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Actor: ref, Target: ref, Stat: StatSpeed, Delta: after - before, CurrentStage: after},
				HeldItemTriggeredConsumedEvent{Type: EventKindHeldItemTriggeredConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber, Holder: ref, ItemID: itemID, SkillID: skillID, Trigger: HeldItemTriggerFieldSpeedOrderStarted},
			)
		}
	}
	return state, events
}

// heldItemConsecutiveSkillDamageMultiplier 返回节拍器在最终伤害倍率链中使用的精确整数分数。
//
// 第一次宣告保持 1 倍；第二至第六次每次增加五分之一，第六次以后稳定为 2 倍。把该规则放在最终
// 分子/分母链而不是提前截断基础威力，才能与最终道具倍率语义一致，并避免低威力技能丢精度。
func heldItemConsecutiveSkillDamageMultiplier(actor MemberSnapshot, skill SkillSnapshot) (uint64, uint64) {
	if actor.ItemID == 0 || !actor.HeldItemConsecutiveSkillDamageBoost || actor.LastDeclaredSkillID != skill.SkillID || actor.ConsecutiveDeclaredSkillUses == 0 {
		return 1, 1
	}
	steps := min(actor.ConsecutiveDeclaredSkillUses-1, uint16(5))
	return uint64(5 + steps), 5
}

// recordDeclaredSkillUse 更新成员最近一次已宣告技能及连续次数；换招宣告会从一次重新计数。
func recordDeclaredSkillUse(member MemberSnapshot, skillID Identifier) MemberSnapshot {
	if member.LastDeclaredSkillID == skillID {
		if member.ConsecutiveDeclaredSkillUses < ^uint16(0) {
			member.ConsecutiveDeclaredSkillUses++
		}
	} else {
		member.LastDeclaredSkillID = skillID
		member.ConsecutiveDeclaredSkillUses = 1
	}
	return member
}

// clampHeldItemStatStage 把道具产生的能力阶级变化夹在引擎统一的 -6 至 6 边界内。
func clampHeldItemStatStage(current, delta int8) int8 {
	next := current + delta
	if next < -6 {
		return -6
	}
	if next > 6 {
		return 6
	}
	return next
}

// validOptionalHeldItemStatStageBoost 验证可选道具能力提升必须同时声明合法能力项和正变化量。
func validOptionalHeldItemStatStageBoost(stat Stat, delta int8) bool {
	if stat == "" || delta == 0 {
		return stat == "" && delta == 0
	}
	return stat.Valid() && delta > 0
}
