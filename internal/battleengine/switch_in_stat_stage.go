package battleengine

import "fmt"

// SwitchInStatStageTarget 是入场特性能力阶级变化可作用的封闭成员集合。
type SwitchInStatStageTarget string

const (
	// SwitchInStatStageTargetSelf 表示入场成员自身。
	SwitchInStatStageTargetSelf SwitchInStatStageTarget = "self"
	// SwitchInStatStageTargetOpponents 表示入场成员对侧所有仍在场且可战斗的成员。
	SwitchInStatStageTargetOpponents SwitchInStatStageTarget = "opponents"
)

// valid 报告入场特性能力阶级目标是否属于引擎能够解释的封闭集合。
func (target SwitchInStatStageTarget) valid() bool {
	return target == SwitchInStatStageTargetSelf || target == SwitchInStatStageTargetOpponents
}

// SwitchInStatStageChange 是成员进入场地后立即执行的独立特性能力阶级变化规则。
//
// 它与技能命中后的 StatStageEffect 不共享生命周期：没有概率、不会读取技能目标，且仅在成员实际成功入场后
// 执行。目标必须由封闭枚举明确指定，避免以特性文本或任意效果数组推断影响范围。
type SwitchInStatStageChange struct {
	// Target 是本规则作用于入场成员自身还是所有场上对手。
	Target SwitchInStatStageTarget `json:"target"`
	// Stat 是需要增减的稳定能力项。
	Stat Stat `json:"stat"`
	// StageDelta 是请求的能力阶级增减，取值为 -6 至 6 且不能为零。
	StageDelta int8 `json:"stageDelta"`
}

// validateSwitchInStatStageChange 校验成员冻结的入场能力阶级变化规则。
func validateSwitchInStatStageChange(value *SwitchInStatStageChange) error {
	if value == nil {
		return nil
	}
	if !value.Target.valid() || !value.Stat.Valid() || value.StageDelta == 0 || value.StageDelta < -6 || value.StageDelta > 6 {
		return fmt.Errorf("入场能力阶级变化无效: %w", ErrInvalidInitialState)
	}
	return nil
}

// cloneSwitchInStatStageChange 深复制可选的入场能力阶级变化规则。
func cloneSwitchInStatStageChange(value *SwitchInStatStageChange) *SwitchInStatStageChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// resolveSwitchInStatStageChange 结算成员实际换入且入场危害结束后的特性能力阶级变化。
func resolveSwitchInStatStageChange(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || member.SwitchInStatStageChange == nil {
		return state, nil
	}
	return applySwitchInStatStageChange(state, MemberRef{Side: slot.Side, Position: member.Position}, *member.SwitchInStatStageChange)
}

// initializeSwitchInStatStageChanges 按冻结阵营和槽位顺序处理双方初始上场成员的能力阶级特性。
//
// 第 0 回合只写入权威快照，不向尚未开始的回合事件流补写 StatStageChangedEvent；后续实际换入才产生事件。
func initializeSwitchInStatStageChanges(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.SwitchInStatStageChange == nil {
				continue
			}
			state, _ = applySwitchInStatStageChange(state, MemberRef{Side: side.Side, Position: member.Position}, *member.SwitchInStatStageChange)
		}
	}
	return state
}

// applySwitchInStatStageChange 对规则声明的固定目标集合应用一次能力阶级变化。
//
// 每个目标分别在 -6 至 6 边界夹取；没有实际变化的目标不产生事件。目标顺序固定为阵营与槽位顺序，确保
// 多目标降阶在事件重放中不依赖 map 遍历或客户端数组顺序。
func applySwitchInStatStageChange(state State, source MemberRef, rule SwitchInStatStageChange) (State, []Event) {
	targets := []MemberRef{}
	switch rule.Target {
	case SwitchInStatStageTargetSelf:
		targets = append(targets, source)
	case SwitchInStatStageTargetOpponents:
		for _, side := range state.sides {
			if side.Side == source.Side {
				continue
			}
			for _, position := range side.ActiveMembers {
				targets = append(targets, MemberRef{Side: side.Side, Position: position})
			}
		}
	default:
		return state, nil
	}
	events := make([]Event, 0, len(targets))
	for _, targetRef := range targets {
		target, found := state.member(targetRef.Side, targetRef.Position)
		if !found || target.CurrentHP == 0 {
			continue
		}
		before := target.StatStages[rule.Stat]
		after := max(int8(-6), min(int8(6), before+rule.StageDelta))
		if before == after {
			continue
		}
		if rule.StageDelta < 0 && targetRef.Side != source.Side && target.ItemID != 0 && target.HeldItemOpponentStatStageReductionImmunity {
			continue
		}
		stages := make(map[Stat]int8, len(target.StatStages)+1)
		for stat, stage := range target.StatStages {
			stages[stat] = stage
		}
		stages[rule.Stat] = after
		target.StatStages = stages
		state.replaceMember(targetRef.Side, target)
		events = append(events, StatStageChangedEvent{
			Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: source, Target: targetRef, Stat: rule.Stat, Delta: after - before, CurrentStage: after,
		})
		if rule.StageDelta < 0 && targetRef.Side != source.Side {
			if target.ItemID != 0 && target.HeldItemNegativeStatStageReset {
				var resetEvents []Event
				state, resetEvents = applyHeldItemNegativeStatStageReset(state, targetRef)
				events = append(events, resetEvents...)
			}
			var boostEvents []Event
			state, boostEvents = applyHeldItemAbilityStatReductionSpeedBoost(state, targetRef)
			events = append(events, boostEvents...)
		}
	}
	return state, events
}
