package battleengine

import (
	"fmt"
	"sort"
)

// resolveForcedTargetSwitch 在技能已完成普通伤害、异常、能力阶级和易变状态等目标向效果之后，尝试让仍存活的
// 被选中对手强制换人。替身和目标特性免疫都会阻止该规则；没有健康后备时技能的其它已结算效果保持有效，但不会产生选择、随机或
// 换入事件。候选成员按稳定成员位置排序，以保证实时资料、内存切片顺序和重放实现均不能影响随机轨迹。
func resolveForcedTargetSwitch(
	state State,
	actor MemberRef,
	targetSlot SlotRef,
	targetRef MemberRef,
	skill SkillSnapshot,
	targetHadSubstitute bool,
	random RandomSource,
) (State, RandomSource, []Event, []RandomTraceEntry, error) {
	if targetHadSubstitute {
		return state, random, nil, nil, nil
	}
	target, exists := state.ActiveMember(targetSlot)
	if !exists || target.CurrentHP == 0 || target.Position != targetRef.Position {
		return state, random, nil, nil, nil
	}
	actorMember, actorExists := state.member(actor.Side, actor.Position)
	if target.ForcedSwitchImmunity && (!actorExists || !ignoresTargetAbilityEffects(actorMember, skill)) {
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
			int32(len(reserves)), fmt.Sprintf("force target switch selection for %s", skill.SkillID),
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
	selection := ForcedTargetSwitchSelectedEvent{
		Type: EventKindForcedTargetSwitchSelected, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actor, TargetSlot: targetSlot, Target: targetRef, SkillPosition: skill.Position, SkillID: skill.SkillID,
		Candidates: candidates, SelectedMember: selected,
	}
	state, random, switchEvents, switchTrace, err := resolveMemberSwitch(
		state, targetSlot, selected.Position, random, true,
	)
	if err != nil {
		return State{}, RandomSource{}, nil, nil, err
	}
	return state, random, append([]Event{selection}, switchEvents...), append(trace, switchTrace...), nil
}

// healthyReservePositions 返回一方全部尚可战斗、且没有占据当前任一场上槽位的后备成员位置。
//
// 成员位置由对局快照定义为稳定标识，而原始 Members 切片仅是存储形态；因此这里显式按位置升序排列，既保证
// 单一候选不消耗随机数，也使多个候选的随机索引在重放、不同数据库驱动和资料编译顺序下保持一致。
func healthyReservePositions(state State, sideID Side) []MemberPosition {
	reserves := make([]MemberPosition, 0)
	for _, side := range state.sides {
		if side.Side != sideID {
			continue
		}
		for _, member := range side.Members {
			if member.CurrentHP > 0 && !state.isActive(sideID, member.Position) {
				reserves = append(reserves, member.Position)
			}
		}
		break
	}
	sort.Slice(reserves, func(left, right int) bool {
		return reserves[left] < reserves[right]
	})
	return reserves
}
