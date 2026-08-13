package battleengine

import "fmt"

// resolveDamagedForcedSwitchItem 在一项技能已造成正实际伤害后，结算持有者受伤触发的两种一次性强制换人道具。
//
// 持有者自换与攻击者被换下具有不同目标和免疫语义：前者不检查持有者的 ForcedSwitchImmunity，后者必须检查
// 攻击者的免疫。只有目标仍存活、仍占据场上槽位且存在健康后备时，才会消耗持有者道具并产生选择与换人事件。
func resolveDamagedForcedSwitchItem(
	state State,
	actorSlot SlotRef,
	targetSlot SlotRef,
	actorRef MemberRef,
	targetRef MemberRef,
	actualDamage uint32,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	if actualDamage == 0 {
		return state, random, nil, nil, nil
	}
	holder, found := state.member(targetRef.Side, targetRef.Position)
	if !found || holder.CurrentHP == 0 || !state.isActive(targetRef.Side, targetRef.Position) || holder.ItemID == 0 {
		return state, random, nil, nil, nil
	}
	source := MemberRef{Side: targetRef.Side, Position: holder.Position}
	switchTargetSlot := targetSlot
	switchTarget := source
	if holder.DamagedForceSelfSwitch {
		return resolveHeldItemForcedSwitch(state, source, switchTargetSlot, switchTarget, holder.ItemID, random)
	}
	if !holder.DamagedForceAttackerSwitch {
		return state, random, nil, nil, nil
	}
	attacker, found := state.ActiveMember(actorSlot)
	if !found || attacker.CurrentHP == 0 || attacker.Position != actorRef.Position {
		return state, random, nil, nil, nil
	}
	return resolveHeldItemForcedSwitch(
		state, source, actorSlot, MemberRef{Side: actorSlot.Side, Position: attacker.Position}, holder.ItemID, random,
	)
}

// resolveNegativeStatStageForcedSwitchItems 在一项技能的能力阶级效果完成后，按实际能力下降事件顺序结算持有者
// 的一次性自换道具。
//
// 同一成员在一招内可能被降低多个能力；其身份只结算一次。成员若已经因更早事件离场、倒下、没有后备或不再持有
// 道具则跳过，确保不会虚构消耗或额外消耗随机数。
func resolveNegativeStatStageForcedSwitchItems(
	state State,
	events []Event,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	seen := make(map[MemberRef]struct{})
	resolvedEvents := make([]Event, 0)
	trace := make([]RandomTraceEntry, 0)
	for _, event := range events {
		change, ok := event.(StatStageChangedEvent)
		if !ok || change.Delta >= 0 {
			continue
		}
		if _, duplicated := seen[change.Target]; duplicated {
			continue
		}
		seen[change.Target] = struct{}{}
		slot, _, active := activeSlotForMember(state, change.Target)
		if !active {
			continue
		}
		holder, found := state.ActiveMember(slot)
		if !found || holder.CurrentHP == 0 || !holder.NegativeStatStageForceSelfSwitch || holder.ItemID == 0 {
			continue
		}
		var itemEvents []Event
		var itemTrace []RandomTraceEntry
		var err error
		state, random, itemEvents, itemTrace, err = resolveHeldItemForcedSwitch(
			state, change.Target, slot, change.Target, holder.ItemID, random,
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		resolvedEvents = append(resolvedEvents, itemEvents...)
		trace = append(trace, itemTrace...)
	}
	return state, random, resolvedEvents, trace, nil
}

// resolveHeldItemForcedSwitch 使用持有道具的明确来源和被替换目标，选择健康后备、消耗道具并复用统一完整换入生命周期。
//
// source 与 target 相同表示持有者自身换下，此时不应用 ForcedSwitchImmunity；它们不同时表示道具迫使另一名成员
// 换下，目标自身的特性免疫必须先阻止结算。多个后备仅消费一次可重放随机数，单个后备不消费随机数。
func resolveHeldItemForcedSwitch(
	state State,
	source MemberRef,
	targetSlot SlotRef,
	target MemberRef,
	itemID Identifier,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	holder, holderFound := state.member(source.Side, source.Position)
	replacementTarget, targetFound := state.ActiveMember(targetSlot)
	if !holderFound || holder.ItemID != itemID || holder.CurrentHP == 0 || !state.isActive(source.Side, source.Position) ||
		!targetFound || replacementTarget.CurrentHP == 0 || replacementTarget.Position != target.Position {
		return state, random, nil, nil, nil
	}
	if source != target && replacementTarget.ForcedSwitchImmunity {
		return state, random, nil, nil, nil
	}
	reserves := healthyReservePositions(state, targetSlot.Side)
	if len(reserves) == 0 {
		return state, random, nil, nil, nil
	}
	selectedIndex := 0
	trace := make([]RandomTraceEntry, 0, 1)
	if len(reserves) > 1 {
		choice, nextRandom, entry, err := random.Next(
			int32(len(reserves)), fmt.Sprintf("item forced switch selection for %s", itemID),
		)
		if err != nil {
			return State{}, RandomSource{}, nil, nil, err
		}
		random = nextRandom
		trace = append(trace, entry)
		selectedIndex = int(choice)
	}
	candidates := make([]MemberRef, 0, len(reserves))
	for _, position := range reserves {
		candidates = append(candidates, MemberRef{Side: targetSlot.Side, Position: position})
	}
	selected := candidates[selectedIndex]
	holder.ItemID = 0
	state.replaceMember(source.Side, holder)
	selection := ItemForcedSwitchSelectedEvent{
		Type: EventKindItemForcedSwitchSelected, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Source: source, Target: target, ItemID: itemID, Candidates: candidates, SelectedMember: selected,
	}
	state, random, switchEvents, switchTrace, err := resolveMemberSwitch(state, targetSlot, selected.Position, random, true)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, err
	}
	return state, random, append([]Event{selection}, switchEvents...), append(trace, switchTrace...), nil
}

// skillDamageDealt 汇总本次单一目标技能对指定目标造成的正实际伤害。
//
// 普通伤害、直接伤害与连续命中都使用结构化 DamageAppliedEvent；替身伤害也必须计入，以保持一次技能的实际扣除
// 语义完整。函数故意只读取同一 Actor、Target、SkillID 的事件，避免吸取、反作用、环境伤害或其它目标的事件误触发道具。
func skillDamageDealt(events []Event, actor, target MemberRef, skill SkillSnapshot) uint32 {
	var total uint32
	for _, event := range events {
		switch value := event.(type) {
		case DamageAppliedEvent:
			if value.Actor == actor && value.Target == target && value.SkillID == skill.SkillID {
				total += value.Amount
			}
		case SubstituteDamageAppliedEvent:
			if value.Actor == actor && value.Target == target && value.SkillID == skill.SkillID {
				total += value.Amount
			}
		}
	}
	return total
}
