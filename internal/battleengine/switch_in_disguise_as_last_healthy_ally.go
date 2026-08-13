package battleengine

// resolveSwitchInDisguiseAsLastHealthyAlly 结算成员实际换入后的视觉身份伪装。
func resolveSwitchInDisguiseAsLastHealthyAlly(state State, slot SlotRef) State {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInDisguiseAsLastHealthyAlly {
		return state
	}
	return applySwitchInDisguiseAsLastHealthyAlly(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInDisguiseAsLastHealthyAlly 在初始入场阶段设置视觉身份，不发布战斗事件。
func initializeSwitchInDisguiseAsLastHealthyAlly(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInDisguiseAsLastHealthyAlly {
				continue
			}
			state = applySwitchInDisguiseAsLastHealthyAlly(state, MemberRef{Side: side.Side, Position: member.Position})
		}
	}
	return state
}

// applySwitchInDisguiseAsLastHealthyAlly 只写入同侧稳定顺序中最后一名可战斗队友的披露种类。
//
// ApparentCreatureID 与 CreatureID 严格分离：伤害、属性相性、技能和资料编译始终使用真实种类；伪装只供
// 状态摘要和披露投影使用。没有符合条件的队友时保持空值，不制造虚假的身份。
func applySwitchInDisguiseAsLastHealthyAlly(state State, actor MemberRef) State {
	receiver, found := state.member(actor.Side, actor.Position)
	if !found || receiver.CurrentHP == 0 || !receiver.SwitchInDisguiseAsLastHealthyAlly {
		return state
	}
	var apparent Identifier
	for _, side := range state.sides {
		if side.Side != actor.Side {
			continue
		}
		for index := len(side.Members) - 1; index >= 0; index-- {
			ally := side.Members[index]
			if ally.Position != receiver.Position && ally.CurrentHP > 0 {
				apparent = ally.CreatureID
				break
			}
		}
	}
	if apparent == 0 {
		return state
	}
	receiver.ApparentCreatureID = apparent
	state.replaceMember(actor.Side, receiver)
	return state
}
