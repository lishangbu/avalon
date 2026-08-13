package battleengine

// resolveSwitchInFormChange 结算成员实际换入且入场危害完成后的确定形态切换特性。
func resolveSwitchInFormChange(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || member.SwitchInFormChange == nil {
		return state, nil
	}
	return applySwitchInFormChange(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInFormChanges 按冻结阵营和槽位顺序结算双方初始上场成员的确定形态切换特性。
//
// 初始阶段同样记录形态变化事件，避免 Battle 在创建对局时只能看到结果而无法重建为何进入该形态。
func initializeSwitchInFormChanges(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.SwitchInFormChange == nil {
				continue
			}
			var changed []Event
			state, changed = applySwitchInFormChange(state, MemberRef{Side: side.Side, Position: member.Position})
			events = append(events, changed...)
		}
	}
	return state, events
}

// applySwitchInFormChange 在成员当前处于规则基础形态时，写入冻结的目标形态画像。
//
// 目标资料在 State 建立前已经校验并冻结；找不到画像或成员已不在基础形态时都属于无效果，而不是运行期
// 资料读取错误。只有显式声明 AddsMaximumHPDifference 的规则会补齐正最大生命差额。
func applySwitchInFormChange(state State, actor MemberRef) (State, []Event) {
	member, found := state.member(actor.Side, actor.Position)
	if !found || member.CurrentHP == 0 || member.SwitchInFormChange == nil {
		return state, nil
	}
	rule := member.SwitchInFormChange
	if member.CreatureID != rule.BaseCreatureID {
		return state, nil
	}
	profile, found := member.formProfile(rule.AlternateCreatureID)
	if !found {
		return state, nil
	}
	previousCreatureID, previousMaxHP, previousCurrentHP := member.CreatureID, member.MaxHP, member.CurrentHP
	member = applyFormProfile(member, profile)
	if rule.AddsMaximumHPDifference && member.MaxHP > previousMaxHP {
		member.CurrentHP = min(member.MaxHP, previousCurrentHP+member.MaxHP-previousMaxHP)
	}
	state.replaceMember(actor.Side, member)
	return state, []Event{FormChangedEvent{
		Type: EventKindFormChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Member: actor,
		FromCreatureID: previousCreatureID, ToCreatureID: member.CreatureID, Reason: FormChangeReasonSwitchInAbility,
	}}
}
