package battleengine

import "fmt"

// SwitchInAllyHeal 是成员特性冻结到战斗快照后的入场同侧回复规则。
//
// 它只处理触发成员之外的同侧当前上场成员，不会回复后备成员或触发者自身。该规则不复用技能治疗、天气
// 回复或场地回复：没有技能目标、命中与概率，也不依赖环境状态。
type SwitchInAllyHeal struct {
	// HealDenominator 是每名接收者按最大生命计算的正回复分母。
	HealDenominator uint32 `json:"healDenominator"`
}

// validateSwitchInAllyHeal 校验成员冻结的入场同侧回复规则。
func validateSwitchInAllyHeal(value *SwitchInAllyHeal) error {
	if value == nil {
		return nil
	}
	if value.HealDenominator == 0 || value.HealDenominator > 65_535 {
		return fmt.Errorf("入场同侧回复无效: %w", ErrInvalidInitialState)
	}
	return nil
}

// cloneSwitchInAllyHeal 深复制可选的入场同侧回复规则。
func cloneSwitchInAllyHeal(value *SwitchInAllyHeal) *SwitchInAllyHeal {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// resolveSwitchInAllyHeal 结算成员实际换入且入场危害结束后的同侧回复特性。
func resolveSwitchInAllyHeal(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || member.SwitchInAllyHeal == nil {
		return state, nil
	}
	return applySwitchInAllyHeal(state, MemberRef{Side: slot.Side, Position: member.Position}, *member.SwitchInAllyHeal)
}

// initializeSwitchInAllyHeals 按冻结阵营和槽位顺序处理双方初始上场成员的同侧回复特性。
//
// 初始阶段只写入权威快照，避免第 0 回合出现尚未开始的事件；若初始快照有受伤成员，多个入场来源按稳定顺序
// 依次回复，因此不依赖 map 遍历或客户端数组顺序。
func initializeSwitchInAllyHeals(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.SwitchInAllyHeal == nil {
				continue
			}
			state, _ = applySwitchInAllyHeal(state, MemberRef{Side: side.Side, Position: member.Position}, *member.SwitchInAllyHeal)
		}
	}
	return state
}

// applySwitchInAllyHeal 对来源同侧的其它当前上场成员逐一应用固定比例回复。
func applySwitchInAllyHeal(state State, source MemberRef, rule SwitchInAllyHeal) (State, []Event) {
	positions := []MemberPosition(nil)
	for _, side := range state.sides {
		if side.Side == source.Side {
			positions = side.ActiveMembers
			break
		}
	}
	if positions == nil {
		return state, nil
	}
	events := make([]Event, 0, len(positions))
	for _, position := range positions {
		if position == source.Position {
			continue
		}
		recipient, found := state.member(source.Side, position)
		if !found || recipient.CurrentHP == 0 || recipient.CurrentHP >= recipient.MaxHP {
			continue
		}
		amount := max(uint32(1), recipient.MaxHP/rule.HealDenominator)
		amount = min(amount, recipient.MaxHP-recipient.CurrentHP)
		recipient.CurrentHP += amount
		state.replaceMember(source.Side, recipient)
		events = append(events, SwitchInAllyHealingAppliedEvent{
			Type: EventKindSwitchInAllyHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Source: source, Recipient: MemberRef{Side: source.Side, Position: recipient.Position},
			Amount: amount, CurrentHP: recipient.CurrentHP,
		})
	}
	return state, events
}
