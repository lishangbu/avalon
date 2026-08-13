package battleengine

// resolveSwitchInRevealOpponentHighestPowerSkill 结算成员实际换入后公开对手当前最高基础威力技能的规则。
func resolveSwitchInRevealOpponentHighestPowerSkill(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInRevealOpponentHighestPowerSkill {
		return state, nil
	}
	return applySwitchInRevealOpponentHighestPowerSkill(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInRevealOpponentHighestPowerSkill 按冻结阵营和槽位顺序处理双方初始上场成员的技能公开规则。
//
// 初始阶段只产生可观察事件，不改变权威快照；实际换入会重新按照当时存活的场上对手和冻结技能快照选取，
// 因此不会从后备成员、客户端载荷或实时游戏资料读取候选技能。
func initializeSwitchInRevealOpponentHighestPowerSkill(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInRevealOpponentHighestPowerSkill {
				continue
			}
			var revealed []Event
			state, revealed = applySwitchInRevealOpponentHighestPowerSkill(state, MemberRef{Side: side.Side, Position: member.Position})
			events = append(events, revealed...)
		}
	}
	return state, events
}

// applySwitchInRevealOpponentHighestPowerSkill 选择全部存活上场对手的最高基础威力技能并产生一条公开事件。
//
// 威力相同的技能严格按 SkillID 字典序升序选择。这与规则的“按 SkillID 降序比较后取最大值”
// 完全等价，避免遍历顺序成为隐含的决胜条件。无可用对手技能时不产生空值事件。
func applySwitchInRevealOpponentHighestPowerSkill(state State, actor MemberRef) (State, []Event) {
	receiver, found := state.member(actor.Side, actor.Position)
	if !found || receiver.CurrentHP == 0 || !receiver.SwitchInRevealOpponentHighestPowerSkill {
		return state, nil
	}
	selected, found := highestPowerOpponentSkill(state, actor.Side)
	if !found {
		return state, nil
	}
	return state, []Event{OpponentSkillRevealedEvent{
		Type: EventKindOpponentSkillRevealed, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actor, Target: selected.Target, SkillID: selected.SkillID,
	}}
}

// highestPowerOpponentSkill 从接收者的全部存活上场对手中选择可公开的最高基础威力技能。
func highestPowerOpponentSkill(state State, receiverSide Side) (revealedOpponentSkill, bool) {
	selected := revealedOpponentSkill{}
	found := false
	for _, side := range state.sides {
		if side.Side == receiverSide {
			continue
		}
		for _, position := range side.ActiveMembers {
			member, memberFound := state.member(side.Side, position)
			if !memberFound || member.CurrentHP == 0 {
				continue
			}
			for _, skill := range member.Skills {
				candidate := revealedOpponentSkill{
					Target: MemberRef{Side: side.Side, Position: member.Position}, SkillID: skill.SkillID, Power: skill.Power,
				}
				if !found || candidate.Power > selected.Power || candidate.Power == selected.Power && candidate.SkillID < selected.SkillID {
					selected, found = candidate, true
				}
			}
		}
	}
	return selected, found
}

// revealedOpponentSkill 是公开事件提交前的内部确定性候选项。
type revealedOpponentSkill struct {
	Target  MemberRef
	SkillID Identifier
	Power   uint16
}
