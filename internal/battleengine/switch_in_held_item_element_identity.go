package battleengine

// resolveSwitchInHeldItemElementIdentity 结算成员实际换入后的携带道具属性身份特性。
//
// 该效果必须在入场形态、天气形态与变身效果完成后执行，使属性身份始终覆盖成员此刻的自然战斗画像；它不
// 查询 Item Metadata，而只读取 Battle 已冻结的 HeldItemElementID。
func resolveSwitchInHeldItemElementIdentity(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 {
		return state, nil
	}
	return applyHeldItemElementIdentity(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInHeldItemElementIdentities 按冻结阵营与槽位顺序处理双方初始上场成员的道具属性身份特性。
//
// 初始阶段同样记录实际属性变化，使 Battle 可以在创建事务中保存完整原因；已经天然为该单属性的成员只会
// 记录内部还原基线，不产生无信息事件。
func initializeSwitchInHeldItemElementIdentities(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 {
				continue
			}
			var applied []Event
			state, applied = applyHeldItemElementIdentity(state, MemberRef{Side: side.Side, Position: member.Position})
			events = append(events, applied...)
		}
	}
	return state, events
}

// applyHeldItemElementIdentity 把成员当前自然属性替换成所持道具的单一属性身份。
//
// 仅“特性开关、非空持有道具、非空冻结属性身份”三项同时满足时才生效。首次生效会捕获自然属性，供离场或
// 特性被复制替换时恢复；同一连续上场周期内的重复调用不会覆盖该基线，也不会重复产生事件。
func applyHeldItemElementIdentity(state State, actor MemberRef) (State, []Event) {
	member, found := state.member(actor.Side, actor.Position)
	if !found || member.CurrentHP == 0 || member.Terastallized || !member.SwitchInHeldItemElementIdentity || member.ItemID == 0 || member.HeldItemElementID == 0 {
		return state, nil
	}
	if len(member.HeldItemElementIdentityBaseElementIDs) == 0 {
		member.HeldItemElementIdentityBaseElementIDs = append([]Identifier(nil), member.ElementIDs...)
	}
	if len(member.ElementIDs) == 1 && member.ElementIDs[0] == member.HeldItemElementID {
		state.replaceMember(actor.Side, member)
		return state, nil
	}
	member.ElementIDs = []Identifier{member.HeldItemElementID}
	state.replaceMember(actor.Side, member)
	return state, []Event{HeldItemElementIdentityAppliedEvent{
		Type: EventKindHeldItemElementIdentityApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: actor, ItemID: member.ItemID, ElementID: member.HeldItemElementID,
	}}
}

// restoreHeldItemElementIdentity 结束当前连续上场周期中的道具属性身份，并恢复已冻结的自然属性。
//
// 该函数不会试图重新按 CreatureID 查资料或形态画像：基线已在属性替换或随后形态变化时更新，因此恢复结果
// 与运行中资料修改无关。没有生效基线时返回原值，保证不持道具成员走零成本路径。
func restoreHeldItemElementIdentity(member MemberSnapshot) MemberSnapshot {
	if member.Terastallized {
		member.HeldItemElementIdentityBaseElementIDs = nil
		member.ElementIDs = []Identifier{member.TeraElementID}
		return member
	}
	if len(member.HeldItemElementIdentityBaseElementIDs) == 0 {
		return member
	}
	member.ElementIDs = append([]Identifier(nil), member.HeldItemElementIdentityBaseElementIDs...)
	member.HeldItemElementIdentityBaseElementIDs = nil
	return member
}
