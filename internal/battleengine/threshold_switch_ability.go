package battleengine

import "fmt"

// resolveDamageCrossedHalfHPForcedSwitch 在技能已对本体造成实际伤害后，结算生命跨越半血阈值的强制自换特性。
//
// 触发条件是生命从严格高于最大生命二分之一首次降至二分之一或以下。调用者只传入 DamageAppliedEvent 聚合的
// 本体伤害，因此替身承伤不会触发；没有健康后备时函数不会产生选择事件或消费随机数。
func resolveDamageCrossedHalfHPForcedSwitch(
	state State,
	targetSlot SlotRef,
	target MemberRef,
	damage uint32,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	if damage == 0 {
		return state, random, nil, nil, nil
	}
	holder, found := state.ActiveMember(targetSlot)
	if !found || holder.Position != target.Position || holder.CurrentHP == 0 || !holder.DamageCrossedHalfHPForceSelfSwitch {
		return state, random, nil, nil, nil
	}
	before := min(uint64(holder.MaxHP), uint64(holder.CurrentHP)+uint64(damage))
	if before*2 <= uint64(holder.MaxHP) || uint64(holder.CurrentHP)*2 > uint64(holder.MaxHP) {
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
			int32(len(reserves)), fmt.Sprintf("ability forced switch selection for %s", holder.AbilityID),
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
	selection := AbilityForcedSwitchSelectedEvent{
		Type: EventKindAbilityForcedSwitchSelected, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Source: target, TargetSlot: targetSlot, Candidates: candidates, SelectedMember: selected,
	}
	state, random, switchEvents, switchTrace, err := resolveMemberSwitch(state, targetSlot, selected.Position, random, true)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, err
	}
	return state, random, append([]Event{selection}, switchEvents...), append(trace, switchTrace...), nil
}

// skillBodyDamageDealt 汇总当前技能对指定成员本体写入的实际伤害。
//
// 与携带道具的伤害触发不同，本函数故意不计入 SubstituteDamageAppliedEvent，确保仅本体生命跨过阈值时才触发
// 半血强制自换特性。它同样按行动者、目标和技能稳定标识筛选，避免吸取、反作用或其它目标的伤害污染本次判断。
func skillBodyDamageDealt(events []Event, actor, target MemberRef, skill SkillSnapshot) uint32 {
	var total uint32
	for _, event := range events {
		value, ok := event.(DamageAppliedEvent)
		if ok && value.Actor == actor && value.Target == target && value.SkillID == skill.SkillID {
			total += value.Amount
		}
	}
	return total
}
